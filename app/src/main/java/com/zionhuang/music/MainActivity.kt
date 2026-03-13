package com.zionhuang.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import com.zionhuang.music.core.data.SongRepository
import com.zionhuang.music.feature.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val songs = remember { SongRepository().list() }

            Surface(color = MaterialTheme.colorScheme.background) {
                HomeScreen(songs = songs)
            }
        }
    }
}
