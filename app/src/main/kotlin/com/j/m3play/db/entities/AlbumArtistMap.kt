/*
 * M3Play Project (2026)
 * Kòi Natsuko (github.com/JAY01-CYBER)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "album_artist_map",
    primaryKeys = ["albumId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class AlbumArtistMap(
    @ColumnInfo(index = true) val albumId: String,
    @ColumnInfo(index = true) val artistId: String,
    val order: Int,
)
