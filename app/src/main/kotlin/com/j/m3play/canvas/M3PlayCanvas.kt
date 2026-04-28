package com.j.m3play.canvas

object M3PlayCanvas {
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us"
    ): CanvasArtwork? {
         
        val tidalCanvas = MonochromeApiCanvas.getBySongArtist(song, artist)
        if (tidalCanvas != null && !tidalCanvas.preferredAnimationUrl.isNullOrBlank()) {
            return tidalCanvas
        }

        
        val appleCanvasUrl = AppleMusicArtistBackgroundProvider.getByArtistName(artist, storefront)
        if (!appleCanvasUrl.isNullOrBlank()) {
            return CanvasArtwork(
                name = song,
                artist = artist,
                videoUrl = appleCanvasUrl,
                animated = appleCanvasUrl
            )
        }

      
        return null
    }
}
