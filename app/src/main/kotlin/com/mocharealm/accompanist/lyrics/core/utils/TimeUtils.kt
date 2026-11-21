package com.mocharealm.accompanist.lyrics.core.utils

internal fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

/**
 * Parse the non-negative integer in `[start, end)`, or 0 if the range is empty or
 * contains any non-digit. This mirrors `substring(start, end).toIntOrNull() ?: 0`
 * for the (non-negative) values timestamps use, but without allocating a substring.
 */
private fun String.digitsToIntOr0(start: Int, end: Int): Int {
    if (start >= end) return 0
    var value = 0
    for (i in start until end) {
        val c = this[i]
        if (c < '0' || c > '9') return 0
        value = value * 10 + (c - '0')
    }
    return value
}

/** Parse a `SS`, `SS.ms` (etc.) fragment in `[start, end)` into milliseconds. */
private fun String.parseSecondsAndMillis(start: Int, end: Int): Int {
    var dot = -1
    for (i in start until end) {
        if (this[i] == '.') {
            dot = i
            break
        }
    }
    if (dot == -1) return digitsToIntOr0(start, end) * 1000

    val seconds = digitsToIntOr0(start, dot) * 1000
    val millisStart = dot + 1
    val millisLen = end - millisStart
    if (millisLen <= 0) return seconds

    // Take the first up-to-3 digits, then scale a 1- or 2-digit fraction like the
    // old "pad to 3 chars" logic (".1" -> 100ms, ".12" -> 120ms, ".1234" -> 123ms).
    val take = if (millisLen >= 3) 3 else millisLen
    var millis = digitsToIntOr0(millisStart, millisStart + take)
    if (millisLen == 1) millis *= 100 else if (millisLen == 2) millis *= 10
    return seconds + millis
}

internal fun String.parseAsTime(): Int {
    if (this.isEmpty()) return 0

    val firstColon = this.indexOf(':')
    if (firstColon == -1) {
        return parseSecondsAndMillis(0, length)
    }

    val lastColon = this.lastIndexOf(':')
    return if (firstColon == lastColon) {
        // Format: MM:SS.ms
        val minutes = digitsToIntOr0(0, firstColon) * 60_000
        minutes + parseSecondsAndMillis(firstColon + 1, length)
    } else {
        // Format: HH:MM:SS.ms
        val hours = digitsToIntOr0(0, firstColon) * 3_600_000
        val minutes = digitsToIntOr0(firstColon + 1, lastColon) * 60_000
        hours + minutes + parseSecondsAndMillis(lastColon + 1, length)
    }
}

internal fun Int.toTimeFormattedString(): String {
    val totalMillis = this
    if (totalMillis < 0) return "00:00.000"

    val totalSeconds = totalMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = totalMillis % 1000

    val m = minutes.toString().padStart(2, '0')
    val s = seconds.toString().padStart(2, '0')
    val ms = millis.toString().padStart(3, '0')

    return "$m:$s.$ms"
}