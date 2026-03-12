package com.j.m3play.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerEngineState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class M3AudioPlayer(private val context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(PlayerEngineState())
    val state: StateFlow<PlayerEngineState> = _state.asStateFlow()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> Unit
        }
    }

    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    } else {
        null
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    init {
        appContext.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    fun playStream(url: String) {
        if (!requestAudioFocus()) return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(url)
            setOnPreparedListener { mp ->
                mp.start()
                updateState()
            }
            setOnCompletionListener {
                updateState(isPlaying = false, position = it.duration.toLong())
            }
            prepareAsync()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
            updateState()
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                if (requestAudioFocus()) {
                    it.start()
                }
            }
            updateState()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        updateState(isPlaying = false, position = 0L)
        abandonAudioFocus()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        updateState(position = positionMs)
    }

    fun getPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    private fun updateState(isPlaying: Boolean? = null, position: Long? = null) {
        val mp = mediaPlayer
        _state.value = PlayerEngineState(
            isPlaying = isPlaying ?: (mp?.isPlaying == true),
            positionMs = position ?: (mp?.currentPosition?.toLong() ?: 0L),
            durationMs = mp?.duration?.takeIf { it > 0 }?.toLong() ?: _state.value.durationMs
        )
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    fun release() {
        appContext.unregisterReceiver(noisyReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = PlayerEngineState()
        abandonAudioFocus()
    }
}
