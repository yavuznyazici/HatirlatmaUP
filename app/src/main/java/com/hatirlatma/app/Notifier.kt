package com.hatirlatma.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CH_REMINDER = "reminders_v1"
    const val CH_ALARM = "flash_alarm_v1"
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH_REMINDER) == null) {
            val ch = NotificationChannel(CH_REMINDER, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Görev hatırlatma bildirimleri"
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CH_ALARM) == null) {
            val ch = NotificationChannel(CH_ALARM, "Flaş Alarm", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Çalar saat gibi çalan alarmlar"
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attrs)
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 800, 500, 800)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, r: Reminder) {
        val app = ctx.applicationContext
        ensureChannels(app)
        val id = r.id

        val doneIntent = PendingIntent.getBroadcast(
            app, id * 10 + 1,
            Intent(app, ActionReceiver::class.java)
                .setAction(ActionReceiver.ACTION_COMPLETE)
                .putExtra(EXTRA_ID, id),
            FLAGS
        )
        val snoozeIntent = PendingIntent.getActivity(
            app, id * 10 + 2,
            Intent(app, SnoozeActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAGS
        )
        val openClass: Class<*> = if (r.isAlarm) AlarmActivity::class.java else MainActivity::class.java
        val openIntent = PendingIntent.getActivity(
            app, id * 10 + 3,
            Intent(app, openClass)
                .putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAGS
        )

        val b = NotificationCompat.Builder(app, if (r.isAlarm) CH_ALARM else CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (r.isAlarm) "⏰ Flaş Alarm" else "🔔 Hatırlatma")
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (r.isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Tamamlandı", doneIntent)
            .addAction(0, "Ertele", snoozeIntent)

        if (r.isAlarm) b.setFullScreenIntent(openIntent, true)

        val n = b.build()
        if (r.isAlarm) n.flags = n.flags or Notification.FLAG_INSISTENT

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(app).notify(id, n)
    }
}
