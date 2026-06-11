package com.beolddeok.alarm.ui.ring.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beolddeok.alarm.data.Alarm
import kotlin.math.sqrt

/**
 * 신체 각성 미션 — 폰을 격하게 흔들어야 꺼짐.
 * 가속도 크기가 임계값을 넘는 "흔들기"를 missionStrength × 30 회 감지하면 완료.
 */
@Composable
fun PhysicalMission(alarm: Alarm, onSolved: () -> Unit) {
    val context = LocalContext.current
    val target = remember { alarm.missionStrength * 30 }
    var shakes by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = e.values
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (g > SHAKE_THRESHOLD && now - lastShakeTime > 120) {
                    lastShakeTime = now
                    shakes++
                    if (shakes >= target) onSolved()
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📱💪", fontSize = 56.sp)
        Text(
            "폰을 힘차게 흔드세요!",
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Text("$shakes / $target", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { (shakes.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}

private const val SHAKE_THRESHOLD = 2.2f // g
