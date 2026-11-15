package com.horus.supplydrops.util

/**
 * Utility helpers for formatting durations and placeholders.
 */
object TimeFormatter {

    /**
     * Formats [totalSeconds] into `mm:ss` (or `hh:mm:ss` if >= 1 hour).
     */
    fun formatDuration(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val seconds = safeSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
