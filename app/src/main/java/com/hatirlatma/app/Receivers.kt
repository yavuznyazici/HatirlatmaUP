package com.hatirlatma.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val r = Repo.get(context, id) ?: return
        if (r.completed) return
        r.notified = true
        Repo.update(context, r)
        Notifier.show(context, r)
    }
}

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPLETE = "com.hatirlatma.app.ACTION_COMPLETE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPLETE) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            Actions.complete(context, id)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.all(context)
            .filter { !it.completed && !it.notified }
            .forEach { AlarmScheduler.schedule(context, it) }
    }
}
