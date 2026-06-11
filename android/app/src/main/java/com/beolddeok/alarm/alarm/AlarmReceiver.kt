package com.beolddeok.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.beolddeok.alarm.data.AlarmStore

/** 알람 시각이 되면 호출되어 포그라운드 서비스를 띄운다. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return

        val store = AlarmStore.get(context)
        val alarm = store.get(alarmId) ?: return

        // 반복 알람이면 다음 회차 예약, 한 번만 울리는 알람이면 비활성화
        if (alarm.days.isEmpty()) {
            store.upsert(alarm.copy(enabled = false))
        } else {
            AlarmScheduler.reschedule(context, alarm)
        }

        AlarmService.start(context, alarmId)
    }
}
