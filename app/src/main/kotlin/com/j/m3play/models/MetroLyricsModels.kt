package com.j.m3play.models

data class MetroWord(
    val text: String,
    val startTime: Long,
    val endTime: Long
)

data class MetroLine(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val words: List<MetroWord> = emptyList()
)
