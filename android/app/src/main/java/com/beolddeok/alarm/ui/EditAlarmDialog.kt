package com.beolddeok.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.beolddeok.alarm.data.Alarm
import com.beolddeok.alarm.data.MissionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    initial: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = initial?.hour ?: 7,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    var mission by remember { mutableStateOf(initial?.mission ?: MissionType.AI_CONVERSATION) }
    var strength by remember { mutableStateOf(initial?.missionStrength ?: 3) }
    var vibrate by remember { mutableStateOf(initial?.vibrate ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    if (initial == null) "새 알람" else "알람 편집",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                TimePicker(state = timeState)

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(20) },
                    label = { Text("이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("반복", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES.forEachIndexed { idx, name ->
                        val on = idx in days
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (on) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickableNoRipple {
                                    days = if (on) days - idx else days + idx
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(name, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                    }
                }

                Text("깨우기 미션", style = MaterialTheme.typography.labelLarge)
                MissionChoice("🗣️ AI 모닝 대화", "또렷하게 대화해야 꺼짐", mission == MissionType.AI_CONVERSATION) {
                    mission = MissionType.AI_CONVERSATION
                }
                MissionChoice("📷 침대 탈출 사진 인증", "등록한 장소 사진을 찍어야 꺼짐", mission == MissionType.PHOTO_ESCAPE) {
                    mission = MissionType.PHOTO_ESCAPE
                }
                MissionChoice("🏃 신체 각성 (흔들기/걷기)", "몸을 움직여야 꺼짐", mission == MissionType.PHYSICAL) {
                    mission = MissionType.PHYSICAL
                }

                val strengthLabel = when (mission) {
                    MissionType.AI_CONVERSATION -> "대화 통과 횟수"
                    MissionType.PHOTO_ESCAPE -> "찍을 장소 수"
                    MissionType.PHYSICAL -> "강도 (흔들기 횟수 = ×30)"
                }
                Text("$strengthLabel: $strength", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = strength.toFloat(),
                    onValueChange = { strength = it.toInt().coerceIn(1, 7) },
                    valueRange = 1f..7f, steps = 5,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vibrate, onCheckedChange = { vibrate = it })
                    Text("진동 사용")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (initial != null) {
                        TextButton(onClick = { onDelete(initial) }) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("취소") }
                    Button(onClick = {
                        onSave(
                            Alarm(
                                id = initial?.id ?: System.currentTimeMillis(),
                                hour = timeState.hour,
                                minute = timeState.minute,
                                label = label.trim(),
                                days = days,
                                enabled = true,
                                mission = mission,
                                missionStrength = strength,
                                vibrate = vibrate,
                                photoHashes = initial?.photoHashes ?: emptyList(),
                            ),
                        )
                    }) { Text("저장") }
                }

                if (mission == MissionType.PHOTO_ESCAPE) {
                    Text(
                        "※ 사진 인증은 첫 알람이 울릴 때 현재 위치를 '기준 장소'로 등록하고, " +
                            "다음부터는 그 장소로 가서 같은 사진을 찍어야 꺼집니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionChoice(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth().clickableNoRipple(onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
