package com.mocharealm.accompanist.lyrics.phonetics

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Loads compressed APK assets by inflating them to the app cache and memory-mapping
 * the cached files. This keeps APK size down without retaining all dictionaries in RAM.
 */
class AndroidPackLoader(context: Context) : PackLoader {
    private val appContext = context.applicationContext
    private val assets = appContext.assets
    private val cacheDir = File(appContext.cacheDir, "accompanist-phonetic-packs").apply { mkdirs() }

    override fun load(packName: String): ByteSource {
        require(packName.matches(Regex("[a-zA-Z-]+\\.lpd")))
        val file = File(cacheDir, packName)
        synchronized(file.absolutePath.intern()) {
            if (!file.isFile || file.length() == 0L) {
                val temp = File(cacheDir, "$packName.tmp")
                assets.open(packName).use { input ->
                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                }
                if (file.exists()) file.delete()
                check(temp.renameTo(file)) { "Could not cache phonetic pack: $packName" }
            }
        }
        val mapping = FileInputStream(file).use { stream ->
            stream.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length()).asReadOnlyBuffer()
        }
        return object : ByteSource {
            private val data: ByteBuffer = mapping
            override val size: Int get() = data.limit()
            override fun byteAt(offset: Int): Int = data.get(offset).toInt() and 255
        }
    }
}
