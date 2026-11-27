package com.zulfa.hijrah.notif

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zulfa.hijrah.MainActivity
import com.zulfa.hijrah.R
import com.zulfa.hijrah.data.ReminderPrefs

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ReminderScheduler.EXTRA_ID, 0)
        val name = intent.getStringExtra(ReminderScheduler.EXTRA_NAME).orEmpty()
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME).orEmpty()
        val repeat = intent.getBooleanExtra(ReminderScheduler.EXTRA_REPEAT, false)

        // Pengguna sudah mematikan pengingat: jangan tampilkan apa-apa
        if (!ReminderPrefs.isEnabled(context)) return

        showNotification(context, id, name, time)

        // Jadwalkan lagi untuk besok pada jam yang sama
        if (repeat && time.isNotEmpty()) {
            ReminderScheduler.schedule(context, id, name, time)
        }
    }

    private fun showNotification(context: Context, id: Int, name: String, time: String) {
        // Tanpa izin notifikasi (Android 13+), jangan menampilkan apa-apa
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ReminderScheduler.ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isTest = time.isEmpty()
        val title = if (isTest) context.getString(R.string.notif_test_title)
        else context.getString(R.string.notif_title, name)
        val text = if (isTest) context.getString(R.string.notif_test_text)
        else context.getString(R.string.notif_text, name, time)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}