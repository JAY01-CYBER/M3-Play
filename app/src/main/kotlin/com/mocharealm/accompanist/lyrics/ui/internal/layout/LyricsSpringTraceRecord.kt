package com.mocharealm.accompanist.lyrics.ui.internal.layout

import com.mocharealm.accompanist.lyrics.ui.composable.list.LyricsScrollChain

/** Binary schema v1; all integer and floating point fields are little-endian IEEE-754. */
internal object LyricsSpringTraceRecord {
    const val FRAME = 1
    const val STATE_EVENT = 2
    const val SCROLL_ACTOR = 3

    fun state(
        type: Int,
        sequence: Int,
        elapsedNanos: Long,
        requestedSeconds: Float,
        integratedSeconds: Float,
        eventCode: Int,
        eventIndex: Int,
        eventValue: Double,
        eventValue2: Double,
        eventFloat: Float,
        base: Double,
        limit: Float,
        focus: Int,
        first: Int,
        end: Int,
        active: Boolean,
        settings: LyricsScrollChain?,
        rowCount: Int,
        row: (Int, Writer) -> Unit,
    ): ByteArray {
        val writer = Writer(98 + rowCount * 48)
        writer.putByte(type)
        writer.putInt(sequence)
        writer.putLong(elapsedNanos)
        writer.putFloat(requestedSeconds)
        writer.putFloat(integratedSeconds)
        writer.putInt(eventCode)
        writer.putInt(eventIndex)
        writer.putDouble(eventValue)
        writer.putDouble(eventValue2)
        writer.putFloat(eventFloat)
        writer.putDouble(base)
        writer.putFloat(limit)
        writer.putInt(focus)
        writer.putInt(first)
        writer.putInt(end)
        writer.putByte(if (active) 1 else 0)
        writer.putFloat(settings?.stiffness ?: 0f)
        writer.putFloat(settings?.damping ?: 0f)
        writer.putFloat(settings?.coupling ?: 0f)
        writer.putFloat(settings?.distanceFalloff ?: 0f)
        writer.putFloat(settings?.minResponse ?: 0f)
        writer.putInt(rowCount)
        for (index in 0 until rowCount) row(index, writer)
        return writer.toByteArray()
    }

    class Writer(initialCapacity: Int) {
        private var bytes = ByteArray(initialCapacity)
        private var position = 0

        fun putByte(value: Int) {
            ensure(1)
            bytes[position++] = value.toByte()
        }

        fun putInt(value: Int) {
            ensure(4)
            repeat(4) { shift -> bytes[position++] = (value ushr (shift * 8)).toByte() }
        }

        fun putLong(value: Long) {
            ensure(8)
            repeat(8) { shift -> bytes[position++] = (value ushr (shift * 8)).toByte() }
        }

        fun putFloat(value: Float) = putInt(value.toBits())

        fun putDouble(value: Double) = putLong(value.toBits())

        fun toByteArray(): ByteArray = if (position == bytes.size) bytes else bytes.copyOf(position)

        private fun ensure(count: Int) {
            if (position + count <= bytes.size) return
            bytes = bytes.copyOf(maxOf(bytes.size * 2, position + count))
        }
    }
}

internal object LyricsSpringTraceEvent {
    const val CONFIGURE = 100
    const val RETAIN = 101
    const val LAYOUT = 102
    const val CONTENT_SHIFT = 103
    const val REBASE = 104
    const val FOCUS = 105
    const val RESET = 106
    const val FOLLOW_SCROLL = 107
    const val FOLLOW_REQUEST = 108
    const val CLICK_TARGET_CLEARED = 109
    const val AUTO_FOLLOW_SUSPENDED = 110
    const val FOLLOW_TARGET = 111
    const val ANIMATION_START_OR_RETARGET = 112
    const val CLICK_HANDOFF = 113
    const val GEOMETRY_RETARGET = 114
    const val DIRECT_SCROLL_TARGET = 115
    const val SCROLL_FRAME = 120
}
