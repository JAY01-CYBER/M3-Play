package com.j.m3play.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val durationLabel: String = "--:--"
)
