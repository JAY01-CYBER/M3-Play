package com.j.m3play.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.ArtistItem
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.utils.completedLibraryPage
import com.j.m3play.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>()
            }.onFailure {
                reportException(it)
            }
        }
    }
}