package com.hatirlatma.app

import android.content.Context
import org.json.JSONArray

object Repo {
    private const val PREF = "reminders_store"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT = "next_id"

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    @Synchronized
    fun all(c: Context): MutableList<Reminder> {
        val s = prefs(c).getString(KEY_LIST, "[]") ?: "[]"
        val arr = JSONArray(s)
        val list = mutableListOf<Reminder>()
        for (i in 0 until arr.length()) {
            list.add(Reminder.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    private fun save(c: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(c).edit().putString(KEY_LIST, arr.toString()).apply()
    }

    @Synchronized
    fun nextId(c: Context): Int {
        val p = prefs(c)
        val id = p.getInt(KEY_NEXT, 1)
        p.edit().putInt(KEY_NEXT, id + 1).apply()
        return id
    }

    @Synchronized
    fun get(c: Context, id: Int): Reminder? = all(c).firstOrNull { it.id == id }

    @Synchronized
    fun add(c: Context, r: Reminder) {
        val l = all(c)
        l.add(r)
        save(c, l)
    }

    @Synchronized
    fun update(c: Context, r: Reminder) {
        val l = all(c)
        val i = l.indexOfFirst { it.id == r.id }
        if (i >= 0) {
            l[i] = r
            save(c, l)
        }
    }

    @Synchronized
    fun delete(c: Context, id: Int) {
        val l = all(c)
        l.removeAll { it.id == id }
        save(c, l)
    }

    @Synchronized
    fun deleteCompleted(c: Context) {
        val l = all(c)
        l.removeAll { it.completed }
        save(c, l)
    }
}
