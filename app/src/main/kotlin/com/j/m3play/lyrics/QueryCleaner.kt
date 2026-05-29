package com.j.m3play.lyrics

object QueryCleaner {

    fun cleanTitle(title: String): List<String> {
        val results = mutableListOf<String>()

        results.add(title)

        val cleaned = title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?]"), "")
            .replace("official", "", true)
            .replace("video", "", true)
            .replace("lyrics", "", true)
            .trim()

        results.add(cleaned)

        val noFeat = cleaned
            .replace(Regex("feat\\..*", RegexOption.IGNORE_CASE), "")
            .trim()

        results.add(noFeat)

        return results.distinct()
    }
}
