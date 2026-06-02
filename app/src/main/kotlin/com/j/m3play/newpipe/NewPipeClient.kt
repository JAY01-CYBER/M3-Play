package com.j.m3play.newpipe

import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import com.j.m3play.innertube.models.SongItem
import com.j.m3play.innertube.models.YTItem
import com.j.m3play.innertube.models.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object NewPipeClient {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init() {
        val downloader = M3Downloader(client = okHttpClient)
        NewPipe.init(downloader)
    }

    suspend fun getPlaylistSongs(playlistUrl: String): List<YTItem> = withContext(Dispatchers.IO) {
        try {
            val extractor = ServiceList.YouTube.getPlaylistExtractor(playlistUrl)
            extractor.fetchPage()
            
            extractor.initialPage.items.mapNotNull { item ->
                if (item is StreamInfoItem) {
                    SongItem(
                        id = item.url.substringAfter("v=").substringBefore("&"), 
                        title = item.name,
                        artists = listOf(Artist(name = item.uploaderName, id = null)),
                        thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                        explicit = false 
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
