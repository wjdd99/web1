package com.beolddeok.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.beolddeok.alarm.data.Alarm
import com.beolddeok.alarm.data.AlarmStore
import java.util.Calendar

/**
 * AlarmManager.setAlarmClock() 으로 정확한 시각 알람을 등록한다.
 * setAlarmClock 은 Doze 모드에서도 정시에 깨우며, 시스템 상태바에 알람 아이콘을 표시한다.
 */
object AlarmScheduler {

    const val EXTRA_ALARM_ID = "alarm_id"

    fun reschedule(context: Context, alarm: Alarm) {
        cancel(context, alarm.id)
        if (!alarm.enabled) return

        val triggerAt = nextTriggerMillis(alarm)
        val am = context.getSystemService(AlarmManager::class.java)

        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.beolddeok.alarm.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        am.setAlarmClock(info, firePendingIntent(context, alarm.id))
    }

    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(firePendingIntent(context, alarmId))
    }

    /** 알람 발동 후, 반복 알람이면 다음 회차를 다시 예약한다. */
    fun rescheduleAllEnabled(context: Context) {
        AlarmStore.get(context).all().filter { it.enabled }.forEach { reschedule(context, it) }
    }

    private fun firePendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.beolddeok.alarm.FIRE"
            putExtra(EXTRA_ALARM_ID, alarmId)
            data = android.net.Uri.parse("beolddeok://alarm/$alarmId") // 고유 식별
        }
        return PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** 다음으로 울려야 할 시각(epoch millis) 계산. */
    fun nextTriggerMillis(alarm: Alarm, now: Calendar = Calendar.getInstance()): Long {
        val cal = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.days.isEmpty()) {
            // 한 번만: 이미 지난 시각이면 내일
            if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        // 반복: 오늘 포함 향후 7일 중 가장 가까운 요일
        for (i in 0..7) {
            val candidate = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val dow = candidate.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY(1)=0
            if (dow in alarm.days && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        return cal.timeInMillis
    }
}
