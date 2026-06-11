package com.beolddeok.alarm.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 알람을 SharedPreferences 에 JSON 으로 저장하는 단순 저장소.
 * (재부팅 후에도 알람을 복원할 수 있도록 device-protected storage 사용)
 */
class AlarmStore private constructor(context: Context) {

    private val prefs = context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences("alarms", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<Alarm> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Alarm>>(raw) }.getOrDefault(emptyList())
    }

    fun get(id: Long): Alarm? = all().firstOrNull { it.id == id }

    fun upsert(alarm: Alarm) {
        val list = all().filter { it.id != alarm.id } + alarm
        save(list)
    }

    fun delete(id: Long) = save(all().filter { it.id != id })

    fun save(list: List<Alarm>) {
        prefs.edit().putString(KEY, json.encodeToString(list)).apply()
    }

    companion object {
        private const val KEY = "alarm_list"

        @Volatile private var instance: AlarmStore? = null

        fun get(context: Context): AlarmStore =
            instance ?: synchronized(this) {
                instance ?: AlarmStore(context.applicationContext).also { instance = it }
            }
    }
}
