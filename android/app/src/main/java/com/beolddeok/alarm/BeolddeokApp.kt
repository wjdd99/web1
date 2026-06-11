package com.beolddeok.alarm

import android.app.Application
import com.beolddeok.alarm.alarm.AlarmScheduler

class BeolddeokApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 활성 알람을 보정 예약 (앱이 강제종료됐다 켜진 경우 대비)
        AlarmScheduler.rescheduleAllEnabled(this)
    }
}
