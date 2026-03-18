package com.j.m3play.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.filterExplicit
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.LastNewReleaseCheckKey
import com.j.m3play.db.MusicDatabase
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get
import com.j.m3play.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    private val _hasNewReleases = MutableStateFlow(false)
    val hasNewReleases = _hasNewReleases.asStateFlow()

    init {
        viewModelScope.launch {
            YouTube
                .newReleaseAlbums()
                .onSuccess { albums ->
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }

                    val sortedAlbums = albums
                        .sortedBy { album ->
                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                            val firstArtistKey =
                                artistIds.firstNotNullOfOrNull { artistId ->
                                    if (artistId in favouriteArtists.values) {
                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                    } else {
                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                    }
                                } ?: Int.MAX_VALUE
                            firstArtistKey
                        }.filterExplicit(context.dataStore.get(HideExplicitKey, false))

                    _newReleaseAlbums.value = sortedAlbums

                    // Verificar si hay nuevos lanzamientos
                    checkForNewReleases()

                }.onFailure {
                    reportException(it)
                }
        }
    }



    private suspend fun checkForNewReleases() {
        try {
            val lastCheckTime = context.dataStore.get(LastNewReleaseCheckKey, 0L)
            val currentTime = System.currentTimeMillis()

            // Si es la primera vez que se verifica, no mostrar notificación
            if (lastCheckTime == 0L) {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, currentTime)
                }}
                _hasNewReleases.value = false
                return
            }

            // Si hay álbumes y ha pasado suficiente tiempo desde la última verificación
            val hasNewReleases = _newReleaseAlbums.value.isNotEmpty() &&
                    (currentTime - lastCheckTime) > (24 * 60 * 60 * 1000) // 24 horas

            _hasNewReleases.value = hasNewReleases

        } catch (e: Exception) {
            reportException(e)
            _hasNewReleases.value = false
        }
    }

    fun markNewReleasesAsSeen() {
        viewModelScope.launch {
            try {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, System.currentTimeMillis())
                }}
                _hasNewReleases.value = false
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }
}