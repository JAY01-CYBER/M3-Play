package com.zionhuang.music.core.data

import com.zionhuang.music.core.model.Song

class SongRepository {
    fun list(): List<Song> = listOf(
        Song("1", "Echoes", "InnerTune", "3:22", "1.2M plays"),
        Song("2", "Night Drive", "InnerTune", "4:10", "870K plays"),
        Song("3", "Gravity", "Aria Nova", "2:58", "540K plays"),
        Song("4", "Velvet Sky", "Lunar Coast", "3:46", "430K plays"),
        Song("5", "Midnight Pulse", "InnerTune", "3:35", "790K plays"),
        Song("6", "Falling Lights", "Noir Bloom", "4:21", "310K plays"),
        Song("7", "Afterglow", "InnerTune", "3:12", "650K plays"),
        Song("8", "City Waves", "Atlas Sound", "3:50", "280K plays")
    )
}
