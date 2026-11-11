package com.example.cocwidget.util

/** "3d 4h", "5h 12m", "8m", or "Ready". */
fun formatRemaining(seconds: Long): String {
    if (seconds <= 0) return "Ready"
    val d = seconds / 86_400
    val h = (seconds % 86_400) / 3_600
    val m = (seconds % 3_600) / 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${maxOf(m, 1)}m"
    }
}

/** "just now", "12m ago", "3h ago", "2d ago". */
fun formatAgo(seconds: Long): String = when {
    seconds < 60 -> "just now"
    seconds < 3_600 -> "${seconds / 60}m ago"
    seconds < 86_400 -> "${seconds / 3_600}h ago"
    else -> "${seconds / 86_400}d ago"
}
