/*
 * M3Play Project (2026)
 * Kòi Natsuko (github.com/JAY01-CYBER)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
