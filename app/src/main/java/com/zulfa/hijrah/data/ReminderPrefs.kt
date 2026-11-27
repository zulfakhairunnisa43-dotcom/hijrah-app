package com.zulfa.hijrah.data

import android.content.Context

object ReminderPrefs {
    private const val FILE = "reminder_prefs"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    // id salat (1-5) -> jam "HH:mm"
    fun saveTimes(context: Context, times: Map<Int, String>) {
        val editor = prefs(context).edit()
        times.forEach { (id, time) -> editor.putString("time_$id", time) }
        editor.apply()
    }

    fun getTimes(context: Context): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        for (id in 1..5) {
            prefs(context).getString("time_$id", null)?.let { result[id] = it }
        }
        return result
    }
}