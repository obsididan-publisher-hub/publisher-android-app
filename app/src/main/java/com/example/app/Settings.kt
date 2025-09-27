package com.example.app

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    operator fun get(key: String): String? = prefs.getString(key, null)

    operator fun set(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key)
            else putString(key, value)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}