package com.hatirlatma.app

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val EXTRA_ID = "reminder_id"
const val EXTRA_ALARM = "alarm_mode"

data class Reminder(
    val id: Int,
    var text: String,
    var dueMillis: Long,
    var repeatType: Int = 0,
    var repeatDay: Int = 0,
    var hour: Int = 0,
    var minute: Int = 0,
    var completed: Boolean = false,
    var completedAt: Long = 0L,
    var isAlarm: Boolean = false,
    var snoozedUntil: Long = 0L,
    var notified: Boolean = false
) {
    fun effectiveTime(): Long = if (snoozedUntil > 0L) snoozedUntil else dueMillis

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("text", text)
        o.put("due", dueMillis)
        o.put("rt", repeatType)
        o.put("rd", repeatDay)
        o.put("h", hour)
        o.put("m", minute)
        o.put("done", completed)
        o.put("doneAt", completedAt)
        o.put("alarm", isAlarm)
        o.put("snooze", snoozedUntil)
        o.put("notified", notified)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Reminder = Reminder(
            id = o.getInt("id"),
            text = o.getString("text"),
            dueMillis = o.getLong("due"),
            repeatType = o.optInt("rt", 0),
            repeatDay = o.optInt("rd", 0),
            hour = o.optInt("h", 0),
            minute = o.optInt("m", 0),
            completed = o.optBoolean("done", false),
            completedAt = o.optLong("doneAt", 0L),
            isAlarm = o.optBoolean("alarm", false),
            snoozedUntil = o.optLong("snooze", 0L),
            notified = o.optBoolean("notified", false)
        )
    }
}

object Recur {
    const val NONE = 0
    const val DAILY = 1
    const val WEEKLY = 2
    const val MONTHLY = 3

    /** "after" zamanından sonraki ilk tekrar zamanını hesaplar. */
    fun next(type: Int, day: Int, hour: Int, minute: Int, after: Long): Long {
        if (type == MONTHLY) {
            var n = 0
            while (true) {
                val c = Calendar.getInstance()
                c.timeInMillis = after
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.MONTH, n)
                val max = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                c.set(Calendar.DAY_OF_MONTH, minOf(day, max))
                c.set(Calendar.HOUR_OF_DAY, hour)
                c.set(Calendar.MINUTE, minute)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
                if (c.timeInMillis > after) return c.timeInMillis
                n++
            }
        }
        val c = Calendar.getInstance()
        c.timeInMillis = after
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (type == WEEKLY) {
            while (c.get(Calendar.DAY_OF_WEEK) != day || c.timeInMillis <= after) {
                c.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            if (c.timeInMillis <= after) c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis
    }
}

object Fmt {
    private val full = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
    private val short = SimpleDateFormat("dd.MM.yy HH:mm", Locale("tr", "TR"))

    fun dateTime(ms: Long): String = full.format(Date(ms))
    fun shortDateTime(ms: Long): String = short.format(Date(ms))

    fun remaining(target: Long, now: Long): String {
        val past = target < now
        val from = if (past) target else now
        val to = if (past) now else target

        var months = 0
        while (true) {
            val t = Calendar.getInstance()
            t.timeInMillis = from
            t.add(Calendar.MONTH, months + 1)
            if (t.timeInMillis <= to) months++ else break
        }
        val base = Calendar.getInstance()
        base.timeInMillis = from
        base.add(Calendar.MONTH, months)
        val rem = to - base.timeInMillis

        val dayMs = 24L * 60 * 60 * 1000
        val hourMs = 60L * 60 * 1000
        val minMs = 60L * 1000
        val days = rem / dayMs
        val hours = (rem % dayMs) / hourMs
        val mins = (rem % hourMs) / minMs

        val parts = mutableListOf<String>()
        if (months > 0) parts.add("$months ay")
        if (days > 0) parts.add("$days gün")
        if (hours > 0) parts.add("$hours sa")
        if (months == 0 && days == 0L) {
            if (mins > 0) parts.add("$mins dk")
            else if (parts.isEmpty()) parts.add("1 dk'dan az")
        }
        val prefix = if (past) "Gecikti:" else "Kalan:"
        return prefix + " " + parts.joinToString(" ")
    }
}
