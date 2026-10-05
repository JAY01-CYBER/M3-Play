package com.mocharealm.accompanist.lyrics.ui.internal.layout


internal class LyricsHeightIndex(heights: IntArray, private val spacing: Int) {
    private val values = heights.copyOf()
    private val tree = DoubleArray(values.size + 1)
    val size
        get() = values.size

    private var totalExtent = 0.0
    private val highestBit = Integer.highestOneBit(values.size)

    init {
        for (i in values.indices) {
            val value = extent(i)
            totalExtent += value
            tree[i + 1] += value
            val parent = (i + 1) + ((i + 1) and -(i + 1))
            if (parent < tree.size) tree[parent] += tree[i + 1]
        }
    }

    private fun extent(index: Int) = values[index].toDouble() + if (index < size - 1) spacing else 0

    fun height(index: Int) = values[index]

    fun update(index: Int, height: Int) {
        val delta = height.toDouble() - values[index]
        if (delta == 0.0) return
        totalExtent += delta
        values[index] = height
        var i = index + 1
        while (i < tree.size) {
            tree[i] += delta
            i += i and -i
        }
    }

    fun top(index: Int): Double {
        var i = index.coerceIn(0, size)
        var sum = 0.0
        while (i > 0) {
            sum += tree[i]
            i -= i and -i
        }
        return sum
    }

    val total
        get() = totalExtent

    fun itemAt(position: Double): Int {
        if (size == 0) return 0
        var index = 0
        var sum = 0.0
        var bit = highestBit
        while (bit != 0) {
            val next = index + bit
            if (next <= size && sum + tree[next] <= position) {
                sum += tree[next]
                index = next
            }
            bit = bit shr 1
        }
        return index.coerceAtMost(size - 1)
    }
}
