package com.j.m3play.data

import com.j.m3play.model.Song

object SampleCatalog {
    val trendingSongs = listOf(
        Song(
            id = "yt_1",
            title = "Midnight City Lights",
            artist = "M3 Wave",
            thumbnailUrl = "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg",
            streamUrl = "https://pipedapi.kavin.rocks/streams/5qap5aO4i9A",
            durationLabel = "3:44"
        ),
        Song(
            id = "yt_2",
            title = "Echoes of Neon",
            artist = "Liquid Beats",
            thumbnailUrl = "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg",
            streamUrl = "https://pipedapi.kavin.rocks/streams/jfKfPfyJRdk",
            durationLabel = "4:08"
        ),
        Song(
            id = "yt_3",
            title = "Night Drive",
            artist = "Aurora Lane",
            thumbnailUrl = "https://i.ytimg.com/vi/4xDzrJKXOOY/hqdefault.jpg",
            streamUrl = "https://pipedapi.kavin.rocks/streams/4xDzrJKXOOY",
            durationLabel = "2:57"
        ),
        Song(
            id = "yt_4",
            title = "Soft Horizon",
            artist = "Velvet Air",
            thumbnailUrl = "https://i.ytimg.com/vi/DWcJFNfaw9c/hqdefault.jpg",
            streamUrl = "https://pipedapi.kavin.rocks/streams/DWcJFNfaw9c",
            durationLabel = "5:01"
        )
    )
}
