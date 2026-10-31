package com.mocharealm.accompanist.lyrics.ui.internal.effects

import androidx.compose.animation.core.Easing

internal class NewtonPolynomialInterpolationEasing(points: List<Pair<Double, Double>>) : Easing {
    constructor(vararg points: Pair<Double, Double>) : this(points.toList())

    private val dividedDifferences: List<Double>
    private val xValues: List<Double>

    init {
        require(points.map { it.first }.toSet().size == points.size) {
            "All x-coordinates of the points must be unique."
        }

        val n = points.size
        xValues = points.map { it.first }

        val table = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            table[i][0] = points[i].second
        }

        for (j in 1 until n) {
            for (i in j until n) {
                table[i][j] =
                    (table[i][j - 1] - table[i - 1][j - 1]) / (xValues[i] - xValues[i - j])
            }
        }

        dividedDifferences = List(n) { i -> table[i][i] }
    }

    override fun transform(fraction: Float): Float {
        val x = fraction.toDouble()
        val n = xValues.size - 1
        var result = dividedDifferences[n]

        // Use Horner's method for efficient calculation
        for (i in (n - 1) downTo 0) {
            result = result * (x - xValues[i]) + dividedDifferences[i]
        }
        return result.toFloat()
    }
}

internal val Swell = Swell(0.1)

internal fun Swell(swell: Double = 0.1): NewtonPolynomialInterpolationEasing {
    return NewtonPolynomialInterpolationEasing(0.0 to 0.0, 0.5 to swell, 1.0 to 0.0)
}

internal val Bounce = NewtonPolynomialInterpolationEasing(0.0 to 0.0, 0.7 to 1.0, 1.0 to 0.0)
