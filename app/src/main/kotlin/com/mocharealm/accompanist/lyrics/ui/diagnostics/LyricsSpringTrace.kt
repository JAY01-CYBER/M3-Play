package com.mocharealm.accompanist.lyrics.ui.diagnostics

/** Receives compact binary records from the lyrics spring diagnostic stream. */
public fun interface LyricsSpringTraceSink {
    /** The byte array belongs to the caller and will not be changed after this call. */
    public fun onRecord(record: ByteArray)
}

/** Installs an optional sink for binary spring and scroll-animation diagnostics. */
public object LyricsSpringTrace {
    private var sink: LyricsSpringTraceSink? = null

    /** Install a non-blocking sink. Records are emitted on the UI thread. */
    public fun install(sink: LyricsSpringTraceSink) {
        this.sink = sink
    }

    /** Stop emitting records. */
    public fun clear() {
        sink = null
    }

    internal fun currentSink(): LyricsSpringTraceSink? = sink
}
