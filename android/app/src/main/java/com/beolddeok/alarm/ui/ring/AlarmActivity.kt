package com.beolddeok.alarm.ui.ring

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beolddeok.alarm.alarm.AlarmScheduler
import com.beolddeok.alarm.alarm.AlarmService
import com.beolddeok.alarm.data.Alarm
import com.beolddeok.alarm.data.AlarmStore
import com.beolddeok.alarm.data.MissionType
import com.beolddeok.alarm.ui.ring.missions.AiConversationMission
import com.beolddeok.alarm.ui.ring.missions.PhotoMission
import com.beolddeok.alarm.ui.ring.missions.PhysicalMission
import com.beolddeok.alarm.ui.theme.BeolddeokTheme

/** 알람이 울릴 때 잠금화면 위로 뜨는 풀스크린 미션 화면. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 잠금화면 위로 켜기 (구버전 호환 플래그도 병행)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            .takeIf { it >= 0 } ?: AlarmService.currentAlarmId
        val alarm = AlarmStore.get(this).get(alarmId)

        setContent {
            BeolddeokTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (alarm == null) {
                        finishAndStop()
                    } else {
                        RingContent(alarm = alarm, onSolved = { onMissionSolved(alarm) })
                    }
                }
            }
        }
    }

    private fun onMissionSolved(alarm: Alarm) {
        // 사진 미션에서 새 기준 사진을 등록했을 수 있으니 저장은 미션 쪽에서 처리됨.
        finishAndStop()
    }

    private fun finishAndStop() {
        AlarmService.stop(this)
        finish()
    }

    // 뒤로가기로 빠져나가 알람을 끄지 못하게 막는다 (미션을 풀어야만 종료)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* 무시 */ }
}

@Composable
private fun RingContent(alarm: Alarm, onSolved: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(alarm.timeText, fontSize = 64.sp, fontWeight = FontWeight.Black)
        Text(
            alarm.label.ifBlank { "알람" },
            fontSize = 22.sp, color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "😴 미션을 완료해야 알람이 꺼져요!",
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        when (alarm.mission) {
            MissionType.AI_CONVERSATION -> AiConversationMission(alarm, onSolved)
            MissionType.PHOTO_ESCAPE -> PhotoMission(alarm, onSolved)
            MissionType.PHYSICAL -> PhysicalMission(alarm, onSolved)
        }
    }
}
