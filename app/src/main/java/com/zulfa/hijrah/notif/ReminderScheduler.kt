package com.zulfa.hijrah.notif

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zulfa.hijrah.MainActivity
import com.zulfa.hijrah.R
import java.util.Calendar

object ReminderScheduler {

    const val CHANNEL_ID = "ibadah_reminder"
    const val EXTRA_ID = "extra_id"
    const val EXTRA_NAME = "extra_name"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_REPEAT = "extra_repeat"

    private const val TEST_ID = 99

    val PRAYERS = listOf(1 to "Subuh", 2 to "Zuhur", 3 to "Asar", 4 to "Magrib", 5 to "Isya")

    // Channel wajib sejak Android 8, dibuat sekali (aman dipanggil berulang)
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun scheduleAll(context: Context, times: Map<Int, String>) {
        for ((id, name) in PRAYERS) {
            val time = times[id] ?: continue
            schedule(context, id, name, time)
        }
    }

    // Menjadwalkan kemunculan berikutnya dari jam "HH:mm".
    // Kalau jamnya sudah lewat hari ini, otomatis dijadwalkan besok.
    fun schedule(context: Context, id: Int, name: String, time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        setAlarm(context, id, name, time, calendar.timeInMillis, repeat = true)
    }

    // Notifikasi uji: muncul 10 detik dari sekarang, tidak diulang besok
    fun scheduleTest(context: Context) {
        setAlarm(context, TEST_ID, "", "", System.currentTimeMillis() + 10_000, repeat = false)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val ids = PRAYERS.map { it.first } + TEST_ID
        ids.forEach { id ->
            alarmManager.cancel(alarmIntent(context, id, "", "", false))
        }
    }

    private fun setAlarm(
        context: Context, id: Int, name: String, time: String, triggerAt: Long, repeat: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmIntent(context, id, name, time, repeat)
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // Alarm tepat waktu
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pending)
        } catch (e: SecurityException) {
            // Izin alarm tepat waktu belum diberikan: pakai alarm biasa (kurang presisi) supaya tidak crash
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun alarmIntent(
        context: Context, id: Int, name: String, time: String, repeat: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_REPEAT, repeat)
        }
        // requestCode = id salat, jadi tiap salat punya alarm sendiri
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}