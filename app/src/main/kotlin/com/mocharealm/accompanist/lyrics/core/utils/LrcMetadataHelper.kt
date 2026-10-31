package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.Attributes

/**
 * Helper object for parsing and managing specific, known metadata from lyrics lines.
 * It identifies lines matching a [tag:value] format and processes only the tags
 * defined in the METADATA_TAGS set.
 */
object LrcMetadataHelper {

    // A set of known, standard metadata tags to be processed or removed.
    // Any other tags (like 'bg') will be ignored.
    private val METADATA_TAGS = setOf("ar", "ti", "al", "offset", "length")

    // This regex is still good for finding the general [tag:value] pattern.
    private val attributeParser = Regex("""^\[([a-zA-Z]+):\s*(.*)\]\s*$""")

    /**
     * Parses a list of lines to extract values for the known metadata tags.
     *
     * @param lines The list of strings from the lyrics file.
     * @return An [com.mocharealm.accompanist.lyrics.core.model.Attributes] object containing the parsed metadata.
     */
    fun parse(lines: List<String>): Attributes {
        val attributesMap = lines.mapNotNull { line ->
            attributeParser.find(line)?.destructured?.let { (tag, value) ->
                // Only consider the tag if it's in our known set.
                if (tag in METADATA_TAGS) {
                    tag.trim() to value.trim()
                } else {
                    null
                }
            }
        }.toMap()

        return Attributes(
            artist = attributesMap["ar"],
            title = attributesMap["ti"],
            album = attributesMap["al"],
            offset = attributesMap["offset"]?.toIntOrNull() ?: 0,
            duration = attributesMap["length"]?.toIntOrNull() ?: 0
        )
    }

    /**
     * Removes only the lines corresponding to known metadata tags.
     * Lines like [bg:...] will be ignored and therefore kept.
     *
     * @param lines The list of strings to filter.
     * @return A new list containing only non-metadata lines.
     */
    fun removeAttributes(lines: List<String>): List<String> {
        return lines.filterNot { line ->
            // Try to parse the line as a generic attribute.
            attributeParser.find(line)?.destructured?.let { (tag, _) ->
                // Check if the parsed tag is one of the known metadata tags.
                // If yes, this line should be removed (return true for filterNot).
                // If no (e.g., tag is 'bg'), this line should be kept (return false).
                tag in METADATA_TAGS
            } == true // If it doesn't match the pattern at all, keep it.
        }
    }
}