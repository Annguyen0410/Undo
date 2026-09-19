package com.zhiend.regretnote.data

import androidx.compose.ui.graphics.Color

/**
 * The eight check-in categories a user can pick from. Each carries a display
 * label and a soft, harmonised accent colour used across chips and charts —
 * muted so they sit quietly on the dark canvas instead of shouting over it.
 */
enum class Category(val label: String, val color: Color) {
    COMMUNICATION("Communication", Color(0xFFE6937E)),
    HEALTH("Health", Color(0xFF7FC79B)),
    WORK("Work", Color(0xFF84A9D6)),
    MONEY("Money", Color(0xFFC4C8D2)),
    TIME("Time", Color(0xFFB89AD8)),
    COURAGE("Courage", Color(0xFFDBB974)),
    RELATIONSHIPS("Relationships", Color(0xFFE18AA6)),
    OTHER("Other", Color(0xFF9CA6B2));

    companion object {
        /** Maps a stored [Category.name] back to its enum, falling back to [OTHER]. */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}