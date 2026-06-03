/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j.m3play.innertube.YouTube
import com.j.m3play.innertube.models.filterExplicit
import com.j.m3play.innertube.models.filterVideo
import com.j.m3play.innertube.pages.SearchSummaryPage
import com.j.m3play.constants.HideExplicitKey
import com.j.m3play.constants.HideVideoKey
import com.j.m3play.models.ItemsPage
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get
import com.j.m3play.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.getStateFlow("query", "")
    val filter = savedStateHandle.getStateFlow<YouTube.SearchFilter?>("filter", null)

    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    var viewStateMap = mutableStateMapOf<YouTube.SearchFilter, ItemsPage>()

    fun search(query: String) {
        // Handle read-only state flow by updating its underlying value if needed, 
        // though normally you'd map it. Assuming you have a way to update it, 
        // but if query is just a flow from saved state, we trigger the search directly.
        viewModelScope.launch {
            if (filter.value == null) {
                YouTube.searchSummary(query)
                    .onSuccess { resultPage ->
                        // FIX 1: View model me hi directly safe mapping kar di taaki Type Mismatch na aaye
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        summaryPage = if (hideExplicit) {
                            resultPage.copy(summaries = resultPage.summaries.map { summary ->
                                summary.copy(items = summary.items.filterExplicit(true))
                            })
                        } else {
                            resultPage
                        }
                    }.onFailure {
                        reportException(it)
                    }
            } else {
                val currentFilter = filter.value!!
                YouTube.search(query, currentFilter)
                    .onSuccess { result ->
                        viewStateMap[currentFilter] =
                            ItemsPage(
                                result.items
                                    .distinctBy { it.id }
                                    .filterExplicit(
                                        context.dataStore.get(
                                            HideExplicitKey,
                                            false
                                        )
                                    ).filterVideo(context.dataStore.get(HideVideoKey, false)),
                                result.continuation,
                            )
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    fun loadMore() {
        // FIX 2: filter.value?.value hata kar direct filter.value liya hai (Map key match karne ke liye)
        val currentFilter = filter.value 
        viewModelScope.launch {
            if (currentFilter == null) return@launch
            val viewState = viewStateMap[currentFilter] ?: return@launch
            val continuation = viewState.continuation
            
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                    
                viewStateMap[currentFilter] = ItemsPage(
                    // FIX 3: Naye load hue items pe bhi explicit/video filter laga diya taaki list consistent rahe
                    (viewState.items + searchResult.items)
                        .distinctBy { it.id }
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        .filterVideo(context.dataStore.get(HideVideoKey, false)),
                    searchResult.continuation
                )
            }
        }
    }
}
