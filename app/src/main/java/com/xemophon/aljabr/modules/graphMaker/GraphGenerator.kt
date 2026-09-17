package com.xemophon.aljabr.modules.graphMaker

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import com.xemophon.aljabr.modules.statistics.distributions.DistributionsFunc
import org.matheclipse.core.interfaces.IAST
import kotlin.math.abs

data class Point(val x: Float, val y: Float)

data class GraphAnalysis(
    val localMaxima: List<Point> = emptyList(),
    val localMinima: List<Point> = emptyList(),
    val inflectionPoints: List<Point> = emptyList(),
    val verticalAsymptotes: List<Float> = emptyList(),
    val horizontalAsymptotes: List<Float> = emptyList(),
    val horizontalTrends: Pair<Float?, Float?> = null to null // left, right
)

object GraphGenerator {
    private data class AnalysisCache(
        val expression: String,
        val firstDerivative: String?,
        val secondDerivative: String?,
        val globalRootsExtrema: List<Double>,
        val globalRootsInflections: List<Double>,
        val globalVerticalAsymptotes: List<Float>,
        val globalHorizontalAsymptotes: List<Float>
    )

    private var cache: AnalysisCache? = null

    /**
     * Clears the internal analysis cache to free up memory.
     */
    fun clearCache() {
        cache = null
    }

    fun generateAndAnalyze(
        expression: String,
        minX: Double,
        maxX: Double,
        minY: Double = -10.0,
        maxY: Double = 10.0,
        @Suppress("UNUSED_PARAMETER") steps: Int = 400 // Reduced steps for performance if 2D
    ): Pair<List<List<Point>>, GraphAnalysis> {
        if (expression.isBlank()) return emptyList<List<Point>>() to GraphAnalysis()

        // Handle implicit equations like x^2 + y^2 = 1
        if (expression.contains("=")) {
            return generateImplicit(expression, minX, maxX, minY, maxY)
        }

        // Handle PDF/CDF distribution functions
        if (expression.contains("PDF", ignoreCase = true) ||
            expression.contains("CDF", ignoreCase = true) ||
            expression.contains("Distribution", ignoreCase = true)
        ) {
            val distSegments = evaluateDistributionPoints(expression, minX, maxX)
            if (distSegments.flatten().isNotEmpty()) {
                return distSegments to GraphAnalysis()
            }
        }

        val allSegments = mutableListOf<MutableList<Point>>()
        var currentSegment = mutableListOf<Point>()

        // For f(x), we use more steps
        val fSteps = 1000
        val stepSize = (maxX - minX) / fSteps

        for (i in 0..fSteps) {
            val x = minX + i * stepSize
            val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)

            if (!y.isNaN() && !y.isInfinite()) {
                val point = Point(x.toFloat(), y.toFloat())

                if (currentSegment.isNotEmpty()) {
                    val prev = currentSegment.last()
                    val dyJump = abs(point.y - prev.y)
                    val slope = dyJump / stepSize
                    if (slope > 5000 && (point.y * prev.y < 0 || abs(point.y) > 40 || abs(prev.y) > 40)) {
                        allSegments.add(currentSegment)
                        currentSegment = mutableListOf()
                    }
                }
                currentSegment.add(point)
            } else {
                if (currentSegment.isNotEmpty()) {
                    allSegments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            }
        }
        if (currentSegment.isNotEmpty()) allSegments.add(currentSegment)

        // Symja based analysis for extremas, inflections and asymptotes
        val (maxima, minima, inflections, vAsymptotesSymja, hAsymptotesSymja) = analyzeWithSymja(expression, minX, maxX)

        val points = allSegments.flatten()
        return allSegments to GraphAnalysis(
            localMaxima = maxima,
            localMinima = minima,
            inflectionPoints = inflections,
            verticalAsymptotes = vAsymptotesSymja,
            horizontalAsymptotes = hAsymptotesSymja,
            horizontalTrends = (points.firstOrNull()?.y) to (points.lastOrNull()?.y)
        )
    }

    private fun evaluateDistributionPoints(
        expression: String,
        minX: Double,
        maxX: Double,
        steps: Int = 500
    ): List<List<Point>> {
        val allSegments = mutableListOf<MutableList<Point>>()
        var currentSegment = mutableListOf<Point>()
        val stepSize = (maxX - minX) / steps

        try {
            val prep = DistributionsFunc.prepareDistributionExpression(expression)
            val cleaned = SymjaUtils.prepareForSymja(prep)
            val tableExpr = "N[Table[{x, $cleaned}, {x, $minX, $maxX, $stepSize}]]"

            val result = SymjaUtils.evaluate { eval -> eval.eval(tableExpr) }
            if (result is IAST) {
                for (i in 1 until result.size()) {
                    val elem = result.get(i)
                    if (elem is IAST && elem.size() >= 3) {
                        val x = elem.get(1).toString().toDoubleOrNull() ?: Double.NaN
                        val y = elem.get(2).toString().toDoubleOrNull() ?: Double.NaN

                        if (!x.isNaN() && !y.isNaN() && !y.isInfinite()) {
                            currentSegment.add(Point(x.toFloat(), y.toFloat()))
                        } else if (currentSegment.isNotEmpty()) {
                            allSegments.add(currentSegment)
                            currentSegment = mutableListOf()
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        if (currentSegment.isNotEmpty()) allSegments.add(currentSegment)
        return allSegments
    }

    private fun analyzeWithSymja(
        expression: String,
        minX: Double,
        maxX: Double
    ): Quintuple<List<Point>, List<Point>, List<Point>, List<Float>, List<Float>> {
        val maxima = mutableListOf<Point>()
        val minima = mutableListOf<Point>()
        val inflections = mutableListOf<Point>()

        try {
            val currentCache = if (cache?.expression == expression) {
                cache
            } else {
                val cleaned = SymjaUtils.prepareForSymja(expression)

                // Precompute derivatives
                val d2Str = SymjaUtils.evaluate { eval ->
                    eval.eval("D[$cleaned, x]")
                    val d2Expr = eval.eval("D[$cleaned, {x, 2}]")
                    SymjaUtils.formatResult(d2Expr.toString())
                }

                // Find roots in a very large "global" range once
                val globalRootsExtrema = findRoots("D[$cleaned, x]", -10000.0, 10000.0)
                val globalRootsInflections = findRoots("D[$cleaned, {x, 2}]", -10000.0, 10000.0)

                // --- Global Asymptote Detection ---
                val globalVAsymptotes = mutableListOf<Float>()
                val globalHAsymptotes = mutableListOf<Float>()

                // 1. Vertical: Use Together to simplify fractions (e.g. tan(x) = sin/cos)
                val denRoots = findRoots("Denominator[Together[$cleaned]]", -1000.0, 1000.0)
                for (x in denRoots) {
                    val (limitLeft, limitRight) = SymjaUtils.evaluate { eval ->
                        val l1 = eval.eval("Limit[$cleaned, x -> $x, Direction -> 1]").toString()
                        val l2 = eval.eval("Limit[$cleaned, x -> $x, Direction -> -1]").toString()
                        l1 to l2
                    }
                    if (limitLeft.contains("Infinity") || limitRight.contains("Infinity")) {
                        globalVAsymptotes.add(x.toFloat())
                    }
                }

                // 2. Horizontal
                val (limitInfStr, limitNegInfStr) = SymjaUtils.evaluate { eval ->
                    val lInf = eval.eval("Limit[$cleaned, x -> Infinity]").toString()
                    val lNegInf = eval.eval("Limit[$cleaned, x -> -Infinity]").toString()
                    lInf to lNegInf
                }
                listOf(limitInfStr, limitNegInfStr).forEach { limitStr ->
                    val value = limitStr.toDoubleOrNull()
                    if (value != null && !value.isInfinite() && !value.isNaN()) {
                        globalHAsymptotes.add(value.toFloat())
                    }
                }

                AnalysisCache(
                    expression = expression,
                    firstDerivative = cleaned,
                    secondDerivative = d2Str,
                    globalRootsExtrema = globalRootsExtrema,
                    globalRootsInflections = globalRootsInflections,
                    globalVerticalAsymptotes = globalVAsymptotes.distinct(),
                    globalHorizontalAsymptotes = globalHAsymptotes.distinct()
                ).also { cache = it }
            }

            val d2Str = currentCache?.secondDerivative ?: ""
            val rootsExtrema = currentCache?.globalRootsExtrema?.filter { it in minX..maxX } ?: emptyList()
            val rootsInflections = currentCache?.globalRootsInflections?.filter { it in minX..maxX } ?: emptyList()
            val vAsymptotes = currentCache?.globalVerticalAsymptotes?.filter { it.toDouble() in minX..maxX } ?: emptyList()
            val hAsymptotes = currentCache?.globalHorizontalAsymptotes ?: emptyList()

            // Classify extrema using 2nd derivative
            for (x in rootsExtrema) {
                val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)
                if (y.isNaN() || y.isInfinite()) continue

                try {
                    val ddyVal = CalcFuncs.calculateExpression(d2Str, mapOf("x" to x), useRadians = true)
                    if (!ddyVal.isNaN()) {
                        if (ddyVal < -1e-7) maxima.add(Point(x.toFloat(), y.toFloat()))
                        else if (ddyVal > 1e-7) minima.add(Point(x.toFloat(), y.toFloat()))
                    }
                } catch (_: Exception) {
                }
            }

            for (x in rootsInflections) {
                val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)
                if (y.isNaN() || y.isInfinite()) continue
                inflections.add(Point(x.toFloat(), y.toFloat()))
            }

            return Quintuple(maxima, minima, inflections, vAsymptotes, hAsymptotes)

        } catch (_: Exception) {
        }

        return Quintuple(maxima, minima, inflections, emptyList(), emptyList())
    }

    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    private fun findRoots(
        derivativeExpr: String,
        minX: Double,
        maxX: Double
    ): List<Double> {
        val roots = mutableListOf<Double>()
        try {
            val result = SymjaUtils.evaluate { eval -> eval.eval("x /. NSolve[($derivativeExpr) == 0, x]") }

            if (result is IAST) {
                for (i in 1 until result.size()) {
                    val resStr = result.get(i).toString()
                    val value = resStr.toDoubleOrNull() ?: Double.NaN
                    if (!value.isNaN() && value >= minX && value <= maxX) {
                        roots.add(value)
                    }
                }
            } else {
                val value = result.toString().toDoubleOrNull() ?: Double.NaN
                if (!value.isNaN() && value >= minX && value <= maxX) {
                    roots.add(value)
                }
            }
        } catch (_: Exception) {
        }
        return roots.distinct()
    }

    private fun generateImplicit(
        expression: String,
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double
    ): Pair<List<List<Point>>, GraphAnalysis> {
        val parts = expression.split("=")
        if (parts.size != 2) return emptyList<List<Point>>() to GraphAnalysis()

        val lhs = parts[0]
        val rhs = parts[1]

        val gridSteps = 80
        val stepX = (maxX - minX) / gridSteps
        val stepY = (maxY - minY) / gridSteps

        val segments = mutableListOf<List<Point>>()

        val grid = Array(gridSteps + 1) { DoubleArray(gridSteps + 1) }
        for (i in 0..gridSteps) {
            val x = minX + i * stepX
            for (j in 0..gridSteps) {
                val y = minY + j * stepY
                val valL = CalcFuncs.calculateExpression(lhs, mapOf("x" to x, "y" to y), useRadians = true)
                val valR = CalcFuncs.calculateExpression(rhs, mapOf("x" to x, "y" to y), useRadians = true)
                grid[i][j] = valL - valR
            }
        }

        for (i in 0 until gridSteps) {
            for (j in 0 until gridSteps) {
                val x1 = minX + i * stepX
                val x2 = minX + (i + 1) * stepX
                val y1 = minY + j * stepY
                val y2 = minY + (j + 1) * stepY

                val v00 = grid[i][j]
                val v10 = grid[i + 1][j]
                val v01 = grid[i][j + 1]
                val v11 = grid[i + 1][j + 1]

                val crossingPoints = mutableListOf<Point>()

                if (v00 * v10 <= 0) crossingPoints.add(lerpPoint(v00, v10, x1, x2, y1, y1))
                if (v10 * v11 <= 0) crossingPoints.add(lerpPoint(v10, v11, x2, x2, y1, y2))
                if (v11 * v01 <= 0) crossingPoints.add(lerpPoint(v11, v01, x2, x1, y2, y2))
                if (v01 * v00 <= 0) crossingPoints.add(lerpPoint(v01, v00, x1, x1, y2, y1))

                if (crossingPoints.size >= 2) {
                    segments.add(listOf(crossingPoints[0], crossingPoints[1]))
                }
            }
        }

        return segments to GraphAnalysis()
    }

    private fun lerpPoint(
        v1: Double,
        v2: Double,
        x1: Double,
        x2: Double,
        y1: Double,
        y2: Double
    ): Point {
        if (abs(v1 - v2) < 1e-9) return Point(x1.toFloat(), y1.toFloat())
        val t = (0 - v1) / (v2 - v1)
        return Point(
            (x1 + t * (x2 - x1)).toFloat(),
            (y1 + t * (y2 - y1)).toFloat()
        )
    }

    @Suppress("unused")
    fun generatePoints(
        expression: String,
        minX: Double,
        maxX: Double,
        steps: Int = 1000
    ): List<List<Point>> {
        return generateAndAnalyze(expression, minX, maxX, steps = steps).first
    }
}
