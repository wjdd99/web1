package com.beolddeok.alarm.ui.ring.missions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.beolddeok.alarm.ai.ClaudeClient
import com.beolddeok.alarm.data.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * AI 모닝 대화 미션 — Claude 가 음성으로 말을 걸고, 또렷하게 대화해야 꺼진다.
 * 음성 인식(STT) → Claude → 음성 합성(TTS) 루프를 돌며, 모델이 "깸"으로
 * missionStrength 번 판정하면 완료.
 *
 * API 키가 없으면 오프라인 폴백(문장 따라 입력) 으로 대체된다.
 */
@Composable
fun AiConversationMission(alarm: Alarm, onSolved: () -> Unit) {
    val context = LocalContext.current
    val claude = remember { ClaudeClient() }

    if (!claude.isConfigured || !SpeechRecognizer.isRecognitionAvailable(context)) {
        OfflineTypingFallback(alarm, onSolved)
        return
    }

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { micGranted = it }
    LaunchedEffect(Unit) { if (!micGranted) permLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    if (!micGranted) {
        Text("음성 인식을 위해 마이크 권한이 필요합니다.", color = MaterialTheme.colorScheme.error)
        return
    }

    // TTS 준비
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { st ->
            if (st == TextToSpeech.SUCCESS) tts.value?.language = Locale.KOREAN
        }
        tts.value = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    var assistantSay by remember { mutableStateOf("좋은 아침이에요! 일어났는지 확인할게요.") }
    var userHeard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var passes by remember { mutableStateOf(0) }
    val target = alarm.missionStrength

    LaunchedEffect(micGranted) {
        // TTS 초기화 대기
        var waited = 0
        while (tts.value == null && waited < 30) { kotlinx.coroutines.delay(100); waited++ }
        val engine = tts.value ?: return@LaunchedEffect

        val history = mutableListOf(
            ClaudeClient.Turn("user", "(알람이 방금 울렸습니다. 저를 깨우는 대화를 시작해 주세요.)"),
        )

        while (passes < target) {
            val reply = withContext(Dispatchers.IO) { claude.nextTurn(history) }
                .getOrElse { ClaudeClient.Reply("한 번 더 또렷하게 말해줄래요?", false) }
            history.add(ClaudeClient.Turn("assistant", reply.say))
            assistantSay = reply.say
            if (reply.awake) passes++
            if (passes >= target) { onSolved(); return@LaunchedEffect }

            speakAndWait(engine, reply.say)

            listening = true
            val heard = listenOnce(context)
            listening = false
            userHeard = heard
            history.add(ClaudeClient.Turn("user", heard.ifBlank { "(잘 안 들림)" }))
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🗣️", fontSize = 56.sp)
        Text("통과: $passes / $target", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("🤖 $assistantSay", Modifier.padding(16.dp), fontSize = 18.sp)
        }
        if (userHeard.isNotBlank()) {
            Text("🙂 \"$userHeard\"", color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(8.dp))
        }
        if (listening) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("듣는 중…")
            }
        }
    }
}

/** TTS 로 말하고 끝날 때까지 대기 */
private suspend fun speakAndWait(tts: TextToSpeech, text: String) =
    suspendCancellableCoroutine { cont ->
        val id = "say-${System.nanoTime()}"
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
            override fun onError(utteranceId: String?, errorCode: Int) {
                if (cont.isActive) cont.resume(Unit)
            }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

/** 한 번 음성 인식하여 텍스트 반환 (실패 시 빈 문자열) */
private suspend fun listenOnce(context: Context): String =
    suspendCancellableCoroutine { cont ->
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        fun finish(text: String) {
            recognizer.destroy()
            if (cont.isActive) cont.resume(text)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                finish(text)
            }
            override fun onError(error: Int) = finish("")
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        cont.invokeOnCancellation { runCatching { recognizer.destroy() } }
        recognizer.startListening(intent)
    }

/** API 키가 없을 때의 오프라인 폴백: 문장을 그대로 따라 입력 */
@Composable
private fun OfflineTypingFallback(alarm: Alarm, onSolved: () -> Unit) {
    val phrases = remember {
        listOf(
            "나는 지금 당장 일어난다",
            "더 자면 지각이다 일어나자",
            "아침 공기가 상쾌하다",
            "벌떡 일어나서 물 한 잔 마시자",
            "오늘도 좋은 하루를 시작합니다",
        ).shuffled()
    }
    var index by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(false) }
    val target = alarm.missionStrength.coerceAtMost(phrases.size)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "🔌 오프라인 모드 (AI 키 미설정)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text("통과: $index / $target", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(phrases[index % phrases.size], Modifier.padding(16.dp), fontSize = 20.sp)
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("위 문장을 그대로 입력") },
            isError = shake,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                if (input.trim() == phrases[index % phrases.size]) {
                    input = ""; index++
                    if (index >= target) onSolved()
                } else shake = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("확인") }
    }
}
