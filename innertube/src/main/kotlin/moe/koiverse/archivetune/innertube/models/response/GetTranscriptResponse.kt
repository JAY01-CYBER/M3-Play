/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Data Layer
 *
 * Handles data, network & storage
 * Signature: M3PLAY::DATA::CORE::V1
 */

package com.j.m3play.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptResponse(
    val actions: List<Action>?,
) {
    @Serializable
    data class Action(
        val updateEngagementPanelAction: UpdateEngagementPanelAction,
    ) {
        @Serializable
        data class UpdateEngagementPanelAction(
            val content: Content,
        ) {
            @Serializable
            data class Content(
                val transcriptRenderer: TranscriptRenderer,
            ) {
                @Serializable
                data class TranscriptRenderer(
                    val body: Body,
                ) {
                    @Serializable
                    data class Body(
                        val transcriptBodyRenderer: TranscriptBodyRenderer,
                    ) {
                        @Serializable
                        data class TranscriptBodyRenderer(
                            val cueGroups: List<CueGroup>,
                        ) {
                            @Serializable
                            data class CueGroup(
                                val transcriptCueGroupRenderer: TranscriptCueGroupRenderer,
                            ) {
                                @Serializable
                                data class TranscriptCueGroupRenderer(
                                    val cues: List<Cue>,
                                ) {
                                    @Serializable
                                    data class Cue(
                                        val transcriptCueRenderer: TranscriptCueRenderer,
                                    ) {
                                        @Serializable
                                        data class TranscriptCueRenderer(
                                            val cue: SimpleText,
                                            val startOffsetMs: Long,
                                            val durationMs: Long,
                                        ) {
                                            @Serializable
                                            data class SimpleText(
                                                val simpleText: String,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
