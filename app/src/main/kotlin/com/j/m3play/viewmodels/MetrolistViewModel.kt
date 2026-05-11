package com.j.m3play.viewmodels

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.j.m3play.models.MetroLine
import com.j.m3play.models.MetroWord

class MetrolistViewModel : ViewModel() {
    var lyricsLines = mutableStateOf<List<MetroLine>>(emptyList())
    var currentPosition = mutableLongStateOf(0L)

    fun parseLrc(lrc: String) {
        val lines = mutableListOf<MetroLine>()
        val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
        
        val rawLines = lrc.lines().mapNotNull { line ->
            regex.find(line)?.let { match ->
                val (min, sec, mil, text) = match.destructured
                val time = (min.toLong() * 60000) + (sec.toLong() * 1000) + (if (mil.length == 2) mil.toLong() * 10 else mil.toLong())
                time to text.trim()
            }
        }

        for (i in rawLines.indices) {
            val (startTime, text) = rawLines[i]
            val endTime = if (i < rawLines.size - 1) rawLines[i+1].first else startTime + 4000L
            
            val wordsList = text.split(" ")
            val words = if (wordsList.isNotEmpty()) {
                val wordDuration = (endTime - startTime) / wordsList.size
                wordsList.mapIndexed { index, wordText ->
                    MetroWord(wordText, startTime + (index * wordDuration), startTime + ((index + 1) * wordDuration))
                }
            } else emptyList()
            
            lines.add(MetroLine(text, startTime, endTime, words))
        }
        lyricsLines.value = lines
    }
}
