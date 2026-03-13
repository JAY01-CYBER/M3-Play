package com.zionhuang.music.core.data

import com.zionhuang.music.core.model.Song

class SongRepository {
    fun list(): List<Song> = listOf(
        Song("1", "Echo", "InnerTune"),
        Song("2", "Night Drive", "InnerTune")
    )
}
