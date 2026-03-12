package com.j.m3play.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.j.m3play.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = M3AudioPlayer(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        viewModelScope.launch {
            audioPlayer.state.collectLatest { engine ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = engine.isPlaying,
                    positionMs = engine.positionMs,
                    durationMs = engine.durationMs
                )
            }
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _uiState.value = _uiState.value.copy(queue = songs)
        playAt(startIndex)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            audioPlayer.pause()
            stopProgressUpdates()
        } else {
            if (_uiState.value.currentSong == null && _uiState.value.queue.isNotEmpty()) {
                playAt(0)
            } else {
                audioPlayer.resume()
                startProgressUpdates()
            }
        }
    }

    fun playAt(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return

        val song = queue[index]
        _uiState.value = _uiState.value.copy(currentSong = song, currentIndex = index, positionMs = 0L)
        audioPlayer.playStream(song.streamUrl)
        startProgressUpdates()
    }

    fun playNext() {
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex in _uiState.value.queue.indices) playAt(nextIndex)
    }

    fun playPrevious() {
        val previousIndex = _uiState.value.currentIndex - 1
        if (previousIndex in _uiState.value.queue.indices) {
            playAt(previousIndex)
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val position = audioPlayer.getPosition()
                _uiState.value = _uiState.value.copy(positionMs = position)
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        stopProgressUpdates()
        audioPlayer.release()
        super.onCleared()
    }
}
