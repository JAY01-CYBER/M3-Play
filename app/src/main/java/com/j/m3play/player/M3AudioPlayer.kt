class M3AudioPlayer(context: Context) {
    private val exoPlayer = ExoPlayer.Builder(context).build()

    fun playStream(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun stop() {
        exoPlayer.stop()
    }
}
