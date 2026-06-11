package com.beolddeok.alarm.ai

import com.beolddeok.alarm.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * "AI 모닝 대화" 미션용 Claude 클라이언트.
 *
 * Anthropic Messages API 를 직접 호출한다. (Android 에서는 서버용 Java SDK 대신
 * OkHttp 직접 호출이 가볍고 적합)
 *  - 엔드포인트: POST https://api.anthropic.com/v1/messages
 *  - 모델: claude-opus-4-8
 *  - 헤더: x-api-key, anthropic-version: 2023-06-01
 *
 * ⚠️ 보안: 앱에 API 키를 내장하는 것은 안전하지 않다. 실서비스라면 본인 백엔드에
 * 프록시를 두고 그 엔드포인트를 호출하도록 바꾸세요. (여기서는 개인용 전제)
 */
class ClaudeClient(
    private val apiKey: String = BuildConfig.ANTHROPIC_API_KEY,
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    data class Turn(val role: String, val text: String)

    /** 모델의 한 번 응답: 사용자에게 할 말 + 충분히 깼는지 판정 */
    data class Reply(val say: String, val awake: Boolean)

    /**
     * 대화 히스토리를 보내고 다음 발화를 받는다.
     * 모델은 JSON {"say": "...", "awake": true/false} 으로만 답하도록 지시한다.
     */
    fun nextTurn(history: List<Turn>): Result<Reply> = runCatching {
        val messages = buildJsonArray {
            history.forEach { turn ->
                addJsonObject {
                    put("role", turn.role)
                    put("content", turn.text)
                }
            }
        }

        val body = buildJsonObject {
            put("model", "claude-opus-4-8")
            put("max_tokens", 300)
            put("system", SYSTEM_PROMPT)
            put("messages", messages)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body)
            .build()

        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
            parseReply(raw)
        }
    }

    private fun parseReply(raw: String): Reply {
        val root = json.parseToString(raw)
        // content[].text 중 첫 text 블록을 모아 JSON 추출
        val text = root["content"]?.jsonArray
            ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
            .orEmpty()

        val inner = runCatching {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            json.parseToJsonObject(text.substring(start, end + 1))
        }.getOrNull()

        val say = inner?.get("say")?.jsonPrimitive?.content ?: text.ifBlank { "한 번 더 말해줄래요?" }
        val awake = inner?.get("awake")?.jsonPrimitive?.boolean ?: false
        return Reply(say = say, awake = awake)
    }

    // Json 헬퍼 (확장)
    private fun Json.parseToString(s: String): JsonObject = decodeFromString(JsonObject.serializer(), s)
    private fun Json.parseToJsonObject(s: String): JsonObject = decodeFromString(JsonObject.serializer(), s)

    companion object {
        private val SYSTEM_PROMPT = """
            당신은 사용자를 아침에 확실히 깨우는 알람 비서입니다. 한국어로 짧고 활기차게 말하세요.
            사용자가 잠에서 깼는지 대화로 판정하는 것이 목표입니다.
            - 잠꼬대처럼 짧거나 모호하거나 동문서답이면 아직 안 깬 것으로 보고 다른 질문을 던지세요.
            - "오늘 첫 일정이 뭐예요?", "지금 몇 시 같아요?", "어젯밤 몇 시에 잤어요?" 같은
              간단하지만 또렷한 사고를 요구하는 즉흥 질문을 하세요.
            - 또렷하게 두 번 이상 제대로 답하면 깬 것으로 판단합니다.
            반드시 아래 JSON 형식 한 줄로만 답하세요(설명/마크다운 금지):
            {"say": "사용자에게 음성으로 들려줄 말", "awake": true 또는 false}
        """.trimIndent()
    }
}
