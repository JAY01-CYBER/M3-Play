package com.zionhuang.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.zionhuang.music.core.data.SongRepository
import com.zionhuang.music.feature.home.HomeScreen
import com.zionhuang.music.ui.theme.InnerTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val songs = remember { SongRepository().list() }
            InnerTuneTheme {
                HomeScreen(songs = songs)
            }
        }
    }
}
