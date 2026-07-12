package com.example.tuscalc.graphMaker

import com.example.tuscalc.basicCalc.CalcFuncs
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
    /**
     * Generates segments of points and analyzes them during the evaluation phase.
     */
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

        val maxima = mutableListOf<Point>()
        val minima = mutableListOf<Point>()
        val inflections = mutableListOf<Point>()
        val vAsymptotes = mutableListOf<Float>()

        // For f(x), we use more steps
        val fSteps = 1000
        val stepSize = (maxX - minX) / fSteps
        val h = stepSize * 0.01

        var lastDySign = 0
        var lastDdySign = 0

        for (i in 0..fSteps) {
            val x = minX + i * stepSize
            val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)

            if (!y.isNaN() && !y.isInfinite()) {
                val point = Point(x.toFloat(), y.toFloat())

                val yPlus = CalcFuncs.calculateExpression(
                    expression,
                    mapOf("x" to x + h),
                    useRadians = true
                )
                val yMinus = CalcFuncs.calculateExpression(
                    expression,
                    mapOf("x" to x - h),
                    useRadians = true
                )

                if (!yPlus.isNaN() && !yPlus.isInfinite() && !yMinus.isNaN() && !yMinus.isInfinite()) {
                    val dy = (yPlus - yMinus) / (2 * h)
                    val ddy = (yPlus - 2 * y + yMinus) / (h * h)

                    val currentDySign = if (dy > 1e-9) 1 else if (dy < -1e-9) -1 else 0
                    val currentDdySign = if (ddy > 1e-9) 1 else if (ddy < -1e-9) -1 else 0

                    if (abs(y) < 1000) {
                        if (lastDySign != 0 && currentDySign != 0 && currentDySign != lastDySign) {
                            if (lastDySign > 0) maxima.add(point)
                            else minima.add(point)
                        }
                        if (lastDdySign != 0 && currentDdySign != 0 && currentDdySign != lastDdySign) {
                            inflections.add(point)
                        }
                    }

                    if (currentDySign != 0) lastDySign = currentDySign
                    if (currentDdySign != 0) lastDdySign = currentDdySign
                } else {
                    lastDySign = 0
                    lastDdySign = 0
                }

                if (currentSegment.isNotEmpty()) {
                    val prev = currentSegment.last()
                    val dyJump = abs(point.y - prev.y)
                    val slope = dyJump / stepSize
                    if (slope > 5000 && (point.y * prev.y < 0 || abs(point.y) > 40 || abs(prev.y) > 40)) {
                        vAsymptotes.add(((point.x + prev.x) / 2))
                        allSegments.add(currentSegment)
                        currentSegment = mutableListOf()
                        lastDySign = 0
                        lastDdySign = 0
                    }
                }
                currentSegment.add(point)
            } else {
                if (currentSegment.isNotEmpty()) {
                    allSegments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
                lastDySign = 0
                lastDdySign = 0
            }
        }
        if (currentSegment.isNotEmpty()) allSegments.add(currentSegment)

        val points = allSegments.flatten()
        return allSegments to GraphAnalysis(
            localMaxima = maxima,
            localMinima = minima,
            inflectionPoints = inflections,
            verticalAsymptotes = vAsymptotes.distinct(),
            horizontalTrends = (points.firstOrNull()?.y) to (points.lastOrNull()?.y)
        )
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
