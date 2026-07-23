package com.tatara.ui.theme

import android.content.Context

/**
 * Theme choice lives in SharedPreferences, not Room — it is device UI state, not
 * logged data, and §2.3 forbids touching the export schema for it.
 */
class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("tatara_prefs", Context.MODE_PRIVATE)

    fun load(): AppTheme =
        prefs.getString(KEY, null)?.let { name -> AppTheme.entries.find { it.name == name } }
            ?: AppTheme.NIE

    fun save(theme: AppTheme) {
        prefs.edit().putString(KEY, theme.name).apply()
    }

    private companion object {
        const val KEY = "theme"
    }
}
