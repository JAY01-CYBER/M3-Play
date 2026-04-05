/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface PoTokenState {
    data object Idle : PoTokenState

    data class Success(
        val gvsToken: String,
        val playerToken: String,
        val visitorData: String,
    ) : PoTokenState

    data class Error(val message: String) : PoTokenState
}

@HiltViewModel
class PoTokenViewModel
@Inject
constructor() : ViewModel() {

    private val _state = MutableStateFlow<PoTokenState>(PoTokenState.Idle)
    val state: StateFlow<PoTokenState> = _state.asStateFlow()

    fun onTokensExtracted(
        visitorData: String,
        poToken: String,
        playerToken: String,
    ) {
        _state.value = PoTokenState.Success(
            gvsToken = poToken,
            playerToken = playerToken,
            visitorData = visitorData,
        )
    }

    fun onExtractionError(message: String) {
        _state.value = PoTokenState.Error(message)
    }

    fun resetState() {
        _state.value = PoTokenState.Idle
    }
}
