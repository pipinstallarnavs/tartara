package com.tatara.ui.theme

import android.content.Context

/**
 * §7.3 — remembers which tier crossing has already had its fold played. Like the
 * theme choice this is device UI state ("have I seen this animation"), not logged
 * data, so it stays out of Room and out of the export schema (§2.3).
 *
 * On first read the current highest crossing is treated as already celebrated, so
 * shipping this feature never replays a fold for a tier crossed long ago.
 */
class FoldPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("tatara_prefs", Context.MODE_PRIVATE)

    /**
     * Returns true when [highestCrossingId] is a crossing this device has not
     * celebrated yet. Records it either way, so a fold plays at most once.
     */
    fun claimUnseen(highestCrossingId: Long): Boolean {
        if (highestCrossingId <= 0L) return false
        val seen = prefs.getLong(KEY, UNSET)
        prefs.edit().putLong(KEY, highestCrossingId).apply()
        // First run on an existing install: adopt the current state silently.
        return seen != UNSET && highestCrossingId > seen
    }

    private companion object {
        const val KEY = "lastCelebratedCrossingId"
        const val UNSET = -1L
    }
}
