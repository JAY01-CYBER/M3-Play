/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.j.m3play.spotify.models.SpotifyPlaylist
import javax.inject.Inject

@HiltViewModel
class SpotifyLibraryViewModel @Inject constructor(
    private val repository: SpotifyLibraryRepository,
) : ViewModel() {
    val playlists: StateFlow<List<SpotifyPlaylist>> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isRefreshing: StateFlow<Boolean> =
        repository.isRefreshing.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val errorMessage: StateFlow<String?> =
        repository.errorMessage.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreCachedPlaylists()
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshPlaylists()
        }
    }
}
