package com.example.tuscalc.graphMaker

import com.example.tuscalc.basicCalc.CalcFuncs
import com.example.tuscalc.utils.SymjaUtils
import org.matheclipse.core.interfaces.IAST
import kotlin.math.abs

data class Point(val x: Float, val y: Float)

data class GraphAnalysis(
    val localMaxima: List<Point> = emptyList(),
    val localMinima: List<Point> = emptyList(),
    val inflectionPoints: List<Point> = emptyList(),
    val verticalAsymptotes: List<Float> = emptyList(),
    val horizontalTrends: Pair<Float?, Float?> = null to null // left, right
)

object GraphGenerator {
    fun generateAndAnalyze(
        expression: String,
        minX: Double,
        maxX: Double,
        minY: Double = -10.0,
        maxY: Double = 10.0,
        steps: Int = 400 // Reduced steps for performance if 2D
    ): Pair<List<List<Point>>, GraphAnalysis> {
        if (expression.isBlank()) return emptyList<List<Point>>() to GraphAnalysis()

        // Handle implicit equations like x^2 + y^2 = 1
        if (expression.contains("=")) {
            return generateImplicit(expression, minX, maxX, minY, maxY)
        }

        val allSegments = mutableListOf<MutableList<Point>>()
        var currentSegment = mutableListOf<Point>()
        val vAsymptotes = mutableListOf<Float>()

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
                        vAsymptotes.add(((point.x + prev.x) / 2))
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

        // Symja based analysis for extremas and inflections
        val (maxima, minima, inflections) = analyzeWithSymja(expression, minX, maxX)

        val points = allSegments.flatten()
        return allSegments to GraphAnalysis(
            localMaxima = maxima,
            localMinima = minima,
            inflectionPoints = inflections,
            verticalAsymptotes = vAsymptotes.distinct(),
            horizontalTrends = (points.firstOrNull()?.y) to (points.lastOrNull()?.y)
        )
    }

    private fun analyzeWithSymja(
        expression: String,
        minX: Double,
        maxX: Double
    ): Triple<List<Point>, List<Point>, List<Point>> {
        val maxima = mutableListOf<Point>()
        val minima = mutableListOf<Point>()
        val inflections = mutableListOf<Point>()

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)

            // 1. Find Extrema: f'(x) = 0
            val rootsExtrema = findRoots("D[$cleaned, x]", minX, maxX)

            // 2. Find Inflections: f''(x) = 0
            val d2Expr = SymjaUtils.evaluator.eval("D[$cleaned, {x, 2}]")
            val d2Str = SymjaUtils.formatResult(d2Expr.toString())
            val rootsInflections = findRoots("D[$cleaned, {x, 2}]", minX, maxX)

            // Classify extrema using 2nd derivative
            for (x in rootsExtrema) {
                val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)
                if (y.isNaN() || y.isInfinite()) continue

                // Evaluate f''(x) using CalcFuncs for numerical safety
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
        } catch (_: Exception) {
        }

        return Triple(maxima, minima, inflections)
    }

    private fun findRoots(
        derivativeExpr: String,
        minX: Double,
        maxX: Double
    ): List<Double> {
        val roots = mutableListOf<Double>()
        try {
            // Evaluates roots and returns them as a list {val1, val2, ...}
            // We use NSolve[expr == 0, x] to get numerical roots.
            val result = SymjaUtils.evaluator.eval("x /. NSolve[($derivativeExpr) == 0, x]")

            if (result is IAST) {
                // IAST indices are 1-based for arguments
                for (i in 1 until result.size()) {
                    val resStr = result.get(i).toString()
                    val value = resStr.toDoubleOrNull() ?: Double.NaN
                    if (!value.isNaN() && value >= minX && value <= maxX) {
                        roots.add(value)
                    }
                }
            } else {
                // Single value case (not a list)
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

        // We evaluate f(x,y) = LHS - RHS
        val gridSteps = 80 // 80x80 grid for performance
        val stepX = (maxX - minX) / gridSteps
        val stepY = (maxY - minY) / gridSteps

        val segments = mutableListOf<List<Point>>()

        // Simple Marching Squares-like approach to find zero crossings
        val grid = Array(gridSteps + 1) { DoubleArray(gridSteps + 1) }
        for (i in 0..gridSteps) {
            val x = minX + i * stepX
            for (j in 0..gridSteps) {
                val y = minY + j * stepY
                val valL =
                    CalcFuncs.calculateExpression(lhs, mapOf("x" to x, "y" to y), useRadians = true)
                val valR =
                    CalcFuncs.calculateExpression(rhs, mapOf("x" to x, "y" to y), useRadians = true)
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

                // Check for zero crossings on edges
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

    fun generatePoints(
        expression: String,
        minX: Double,
        maxX: Double,
        steps: Int = 1000
    ): List<List<Point>> {
        return generateAndAnalyze(expression, minX, maxX, steps = steps).first
    }
}
