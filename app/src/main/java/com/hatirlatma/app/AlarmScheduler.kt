package com.hatirlatma.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun pending(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, id, i, FLAGS)
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx, id))
    }

    fun schedule(ctx: Context, r: Reminder) {
        val app = ctx.applicationContext
        cancel(app, r.id)
        if (r.completed || r.notified) return

        val am = app.getSystemService(AlarmManager::class.java)
        val t = r.effectiveTime()
        val pi = pending(app, r.id)

        if (r.isAlarm) {
            val show = PendingIntent.getActivity(
                app, r.id, Intent(app, MainActivity::class.java), FLAGS
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(t, show), pi)
        } else {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            }
        }
    }
}
