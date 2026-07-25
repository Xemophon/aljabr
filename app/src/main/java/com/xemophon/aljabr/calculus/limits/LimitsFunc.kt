package com.xemophon.aljabr.calculus.limits

import com.xemophon.aljabr.basicCalc.CalcFuncs
import kotlin.math.abs

object LimitsFunc {

    enum class Direction { LEFT, RIGHT, BOTH }

    fun calculateLimit(
        expression: String,
        variable: String = "x",
        target: String,
        direction: Direction = Direction.BOTH,
        useRadians: Boolean = true
    ): Double {
        val targetVal = when (target) {
            "∞" -> Double.POSITIVE_INFINITY
            "-∞" -> Double.NEGATIVE_INFINITY
            else -> target.toDoubleOrNull() ?: Double.NaN
        }

        if (targetVal.isNaN()) return Double.NaN

        return if (targetVal.isInfinite()) {
            calculateInfiniteLimit(expression, variable, targetVal, useRadians)
        } else {
            calculateFiniteLimit(expression, variable, targetVal, direction, useRadians)
        }
    }

    private fun evaluateAt(expression: String, variable: String, valAt: Double, useRadians: Boolean): Double {
        return try {
            CalcFuncs.calculateExpression(expression, mapOf(variable to valAt), useRadians = useRadians)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun calculateFiniteLimit(
        expression: String,
        variable: String,
        target: Double,
        direction: Direction,
        useRadians: Boolean
    ): Double {
        val epsilons = listOf(1e-4, 1e-6, 1e-8, 1e-10)
        val startTime = System.currentTimeMillis()

        val leftResults = if (direction != Direction.RIGHT) {
            epsilons.mapNotNull {
                if (System.currentTimeMillis() - startTime > 1000) return@mapNotNull null
                val res = evaluateAt(expression, variable, target - it, useRadians)
                if (res.isNaN()) null else res
            }
        } else emptyList()

        val rightResults = if (direction != Direction.LEFT) {
            epsilons.mapNotNull {
                if (System.currentTimeMillis() - startTime > 1000) return@mapNotNull null
                val res = evaluateAt(expression, variable, target + it, useRadians)
                if (res.isNaN()) null else res
            }
        } else emptyList()

        if (leftResults.isEmpty() && rightResults.isEmpty()) return Double.NaN

        val leftLimit = if (leftResults.isNotEmpty()) leftResults.last() else Double.NaN
        val rightLimit = if (rightResults.isNotEmpty()) rightResults.last() else Double.NaN

        return when (direction) {
            Direction.LEFT -> leftLimit
            Direction.RIGHT -> rightLimit
            Direction.BOTH -> {
                if (leftLimit.isNaN()) return rightLimit
                if (rightLimit.isNaN()) return leftLimit

                if (abs(leftLimit - rightLimit) < 1e-4) {
                    (leftLimit + rightLimit) / 2.0
                } else if (leftLimit.isInfinite() && rightLimit.isInfinite() &&
                    ((leftLimit > 0 && rightLimit > 0) || (leftLimit < 0 && rightLimit < 0))
                ) {
                    leftLimit
                } else {
                    Double.NaN
                }
            }
        }
    }

    private fun calculateInfiniteLimit(
        expression: String,
        variable: String,
        target: Double,
        useRadians: Boolean
    ): Double {
        val largeValues = if (target > 0) {
            listOf(1e4, 1e6, 1e8, 1e10)
        } else {
            listOf(-1e4, -1e6, -1e8, -1e10)
        }
        val startTime = System.currentTimeMillis()

        val results = largeValues.mapNotNull {
            if (System.currentTimeMillis() - startTime > 1000) return@mapNotNull null
            val res = evaluateAt(expression, variable, it, useRadians)
            if (res.isNaN()) null else res
        }
        if (results.isEmpty()) return Double.NaN

        val last = results.last()
        if (abs(last) > 1e9) {
            return if (last > 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        }

        val prev = if (results.size > 1) results[results.size - 2] else last

        return if (abs(last - prev) < 1e-4 || (last.isInfinite() && prev.isInfinite())) {
            last
        } else {
            last
        }
    }
}
