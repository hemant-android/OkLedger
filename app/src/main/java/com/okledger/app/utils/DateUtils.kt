package com.okledger.app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    /**
     * Returns a user-friendly date string:
     * - If the given timestamp is today → returns time (e.g. "02:45 PM")
     * - Otherwise → returns date (e.g. "14 Oct 2025")
     */
    fun formatDateOrTime(timestamp: Long): String {
        val date = Date(timestamp)
        val cal = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()

        return if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}
