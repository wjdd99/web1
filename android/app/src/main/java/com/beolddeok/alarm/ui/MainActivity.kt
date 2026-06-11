package com.beolddeok.alarm.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.beolddeok.alarm.alarm.AlarmScheduler
import com.beolddeok.alarm.data.Alarm
import com.beolddeok.alarm.data.AlarmStore
import com.beolddeok.alarm.data.MissionType
import com.beolddeok.alarm.ui.theme.BeolddeokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeolddeokTheme { MainScreen() }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val store = remember { AlarmStore.get(context) }
    var alarms by remember { mutableStateOf(store.all().sortedBy { it.hour * 60 + it.minute }) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun refresh() { alarms = store.all().sortedBy { it.hour * 60 + it.minute } }

    // 권한 요청
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // 정확한 알람 권한 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = "알람 추가")
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Text(
                "⏰ 벌떡 알람",
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            if (alarms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("＋ 버튼으로 알람을 추가하세요", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { on ->
                                val updated = alarm.copy(enabled = on)
                                store.upsert(updated)
                                AlarmScheduler.reschedule(context, updated)
                                refresh()
                            },
                            onClick = { editing = alarm; showEditor = true },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        EditAlarmDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { alarm ->
                store.upsert(alarm)
                AlarmScheduler.reschedule(context, alarm)
                refresh(); showEditor = false
            },
            onDelete = { alarm ->
                AlarmScheduler.cancel(context, alarm.id)
                store.delete(alarm.id)
                refresh(); showEditor = false
            },
        )
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val missionLabel = when (alarm.mission) {
        MissionType.AI_CONVERSATION -> "AI 대화"
        MissionType.PHOTO_ESCAPE -> "사진 인증"
        MissionType.PHYSICAL -> "신체 각성"
    }
    val repeat = when {
        alarm.days.isEmpty() -> "한 번만"
        alarm.days.size == 7 -> "매일"
        alarm.days == setOf(1, 2, 3, 4, 5) -> "주중"
        alarm.days == setOf(0, 6) -> "주말"
        else -> alarm.days.sorted().joinToString(" ") { DAY_NAMES[it] }
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).clickable { onClick() }) {
                Text(
                    alarm.timeText,
                    fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline,
                )
                Text(
                    "${alarm.label.ifBlank { "알람" }} · $repeat · $missionLabel×${alarm.missionStrength}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.outline,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}
