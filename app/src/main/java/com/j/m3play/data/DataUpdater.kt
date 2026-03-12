package com.j.m3play.data

import com.j.m3play.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataUpdater {
    private val _catalog = MutableStateFlow(SampleCatalog.trendingSongs)
    val catalog: StateFlow<List<Song>> = _catalog.asStateFlow()

    suspend fun refresh() {
        delay(500)
        _catalog.value = _catalog.value.shuffled()
    }
}
