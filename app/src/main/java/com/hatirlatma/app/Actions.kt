package com.hatirlatma.app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val SNOOZE_OPTIONS: List<Pair<String, Int>> = listOf(
    "5 dakika sonra" to 5,
    "15 dakika sonra" to 15,
    "30 dakika sonra" to 30,
    "1 saat sonra" to 60,
    "3 saat sonra" to 180,
    "Yarın aynı saatte" to 1440
)

object Actions {
    fun complete(ctx: Context, id: Int) {
        val app = ctx.applicationContext
        val r = Repo.get(app, id) ?: return
        val now = System.currentTimeMillis()

        if (r.repeatType != Recur.NONE) {
            // Bu seferi arşive kopyala, asıl görevi bir sonraki tekrara taşı
            Repo.add(
                app,
                r.copy(
                    id = Repo.nextId(app),
                    repeatType = Recur.NONE,
                    completed = true,
                    completedAt = now,
                    snoozedUntil = 0L,
                    notified = true
                )
            )
            r.dueMillis = Recur.next(r.repeatType, r.repeatDay, r.hour, r.minute, maxOf(now, r.dueMillis))
            r.snoozedUntil = 0L
            r.notified = false
            Repo.update(app, r)
            AlarmScheduler.schedule(app, r)
        } else {
            r.completed = true
            r.completedAt = now
            Repo.update(app, r)
            AlarmScheduler.cancel(app, r.id)
        }
        NotificationManagerCompat.from(app).cancel(id)
    }

    fun snooze(ctx: Context, id: Int, minutes: Int) {
        val app = ctx.applicationContext
        val r = Repo.get(app, id) ?: return
        r.snoozedUntil = System.currentTimeMillis() + minutes * 60_000L
        r.notified = false
        Repo.update(app, r)
        AlarmScheduler.schedule(app, r)
        NotificationManagerCompat.from(app).cancel(id)
    }

    fun restore(ctx: Context, id: Int) {
        val app = ctx.applicationContext
        val r = Repo.get(app, id) ?: return
        r.completed = false
        r.completedAt = 0L
        r.snoozedUntil = 0L
        r.notified = r.dueMillis <= System.currentTimeMillis()
        Repo.update(app, r)
        AlarmScheduler.schedule(app, r)
    }
}

fun showSnoozeDialog(
    activity: AppCompatActivity,
    id: Int,
    onSnoozed: () -> Unit,
    onCancel: () -> Unit
) {
    val labels = SNOOZE_OPTIONS.map { it.first }.toTypedArray()
    MaterialAlertDialogBuilder(activity)
        .setTitle("Ne kadar ertelensin?")
        .setItems(labels) { _, which ->
            Actions.snooze(activity, id, SNOOZE_OPTIONS[which].second)
            onSnoozed()
        }
        .setOnCancelListener { onCancel() }
        .show()
}
