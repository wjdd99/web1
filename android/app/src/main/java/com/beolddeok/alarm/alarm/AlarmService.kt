package com.beolddeok.alarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.beolddeok.alarm.R
import com.beolddeok.alarm.data.AlarmStore
import com.beolddeok.alarm.ui.ring.AlarmActivity

/**
 * 알람이 울리는 동안 살아있는 포그라운드 서비스.
 * - 알람음 재생 + 진동
 * - 잠금화면 위 풀스크린 알람 화면(AlarmActivity)을 띄움
 * - 미션 완료 시 AlarmActivity 가 stop() 을 호출
 */
class AlarmService : Service() {

    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId < 0) {
            stopSelf(); return START_NOT_STICKY
        }
        currentAlarmId = alarmId

        startForeground(NOTIF_ID, buildNotification(alarmId))
        acquireWakeLock()
        startSound()
        startVibration(alarmId)
        launchRingScreen(alarmId)
        return START_STICKY
    }

    private fun launchRingScreen(alarmId: Long) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    private fun buildNotification(alarmId: Long): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "알람", NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "알람이 울릴 때 표시됩니다"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
        val label = AlarmStore.get(this).get(alarmId)?.label?.takeIf { it.isNotBlank() } ?: "알람"
        val fullScreen = PendingIntent.getActivity(
            this, alarmId.toInt(),
            Intent(this, AlarmActivity::class.java)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("⏰ $label")
            .setContentText("일어나세요! 미션을 완료해야 꺼집니다.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .build()
    }

    private fun startSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
        // 알람 볼륨을 최대로
        val audio = getSystemService(AudioManager::class.java)
        audio.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0,
        )
    }

    private fun startVibration(alarmId: Long) {
        if (AlarmStore.get(this).get(alarmId)?.vibrate != true) return
        vibrator = getSystemService(Vibrator::class.java)
        val pattern = longArrayOf(0, 600, 400, 600, 400, 900)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "beolddeok:alarm",
        ).apply { acquire(10 * 60 * 1000L) }
    }

    override fun onDestroy() {
        ringtone?.stop()
        vibrator?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        currentAlarmId = -1L
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 4242
        private const val CHANNEL_ID = "alarm_ring"

        /** 현재 울리는 알람 id (AlarmActivity 가 참고) */
        @Volatile var currentAlarmId: Long = -1L
            private set

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
