/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.models

data class MetroWord(
    val text: String,
    val startTime: Long,
    val endTime: Long
)

data class MetroLine(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val words: List<MetroWord> = emptyList()
)
