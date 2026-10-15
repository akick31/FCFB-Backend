package com.fcfb.arceus.util.game

fun convertClockToSeconds(clock: String): Int {
    val clockArray = clock.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val minutes = clockArray[0].toInt()
    val seconds = clockArray[1].toInt()
    return minutes * 60 + seconds
}

fun convertClockToString(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
