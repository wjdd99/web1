package com.beolddeok.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 재부팅 후 저장된 알람을 다시 예약한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> AlarmScheduler.rescheduleAllEnabled(context)
        }
    }
}
