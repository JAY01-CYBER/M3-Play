/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.together

internal sealed interface TogetherGuestOp {
    data class Control(
        val action: ControlAction,
    ) : TogetherGuestOp

    data class AddTrack(
        val track: TogetherTrack,
        val mode: AddTrackMode,
    ) : TogetherGuestOp
}

internal object TogetherGuestPlaybackPlanner {
    fun planPlayTrackNow(
        roomState: TogetherRoomState,
        track: TogetherTrack,
        positionMs: Long,
        playWhenReady: Boolean,
    ): List<TogetherGuestOp> {
        if (!roomState.settings.allowGuestsToControlPlayback) return emptyList()
        val trackId = track.id.trim()
        if (trackId.isBlank()) return emptyList()

        val shouldRequestPlay = playWhenReady && !roomState.isPlaying
        val safePos = positionMs.coerceAtLeast(0L)

        val existsInHostQueue = roomState.queue.any { it.id == trackId }
        return when {
            existsInHostQueue -> {
                buildList {
                    add(TogetherGuestOp.Control(ControlAction.SeekToTrack(trackId = trackId, positionMs = safePos)))
                    if (shouldRequestPlay) add(TogetherGuestOp.Control(ControlAction.Play))
                }
            }

            roomState.settings.allowGuestsToAddTracks -> {
                buildList {
                    add(TogetherGuestOp.AddTrack(track.copy(id = trackId), AddTrackMode.PLAY_NEXT))
                    add(TogetherGuestOp.Control(ControlAction.SeekToTrack(trackId = trackId, positionMs = 0L)))
                    if (shouldRequestPlay) add(TogetherGuestOp.Control(ControlAction.Play))
                }
            }

            else -> emptyList()
        }
    }
}
