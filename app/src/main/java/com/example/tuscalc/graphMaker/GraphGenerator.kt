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
        steps: Int = 1000
    ): Pair<List<List<Point>>, GraphAnalysis> {
        if (expression.isBlank()) return emptyList<List<Point>>() to GraphAnalysis()

        val allSegments = mutableListOf<MutableList<Point>>()
        var currentSegment = mutableListOf<Point>()
        
        val maxima = mutableListOf<Point>()
        val minima = mutableListOf<Point>()
        val inflections = mutableListOf<Point>()
        val vAsymptotes = mutableListOf<Float>()

        val stepSize = (maxX - minX) / steps
        val h = stepSize * 0.01 

        var lastDySign = 0
        var lastDdySign = 0

        for (i in 0..steps) {
            val x = minX + i * stepSize
            
            val y = CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians = true)
            
            if (!y.isNaN() && !y.isInfinite()) {
                val point = Point(x.toFloat(), y.toFloat())
                
                val yPlus = CalcFuncs.calculateExpression(expression, mapOf("x" to x + h), useRadians = true)
                val yMinus = CalcFuncs.calculateExpression(expression, mapOf("x" to x - h), useRadians = true)
                
                if (!yPlus.isNaN() && !yPlus.isInfinite() && !yMinus.isNaN() && !yMinus.isInfinite()) {
                    val dy = (yPlus - yMinus) / (2 * h)
                    val ddy = (yPlus - 2 * y + yMinus) / (h * h)

                    val currentDySign = if (dy > 1e-9) 1 else if (dy < -1e-9) -1 else 0
                    val currentDdySign = if (ddy > 1e-9) 1 else if (ddy < -1e-9) -1 else 0

                    if (abs(y) < 1000) {
                        // Detect Extrema
                        if (lastDySign != 0 && currentDySign != 0 && currentDySign != lastDySign) {
                            if (lastDySign > 0) maxima.add(point)
                            else minima.add(point)
                        }

                        // Detect Inflections
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
        
        if (currentSegment.isNotEmpty()) {
            allSegments.add(currentSegment)
        }

        val points = allSegments.flatten()
        val leftTrend = points.firstOrNull()?.y
        val rightTrend = points.lastOrNull()?.y

        val analysis = GraphAnalysis(
            localMaxima = maxima,
            localMinima = minima,
            inflectionPoints = inflections,
            verticalAsymptotes = vAsymptotes.distinct(),
            horizontalTrends = leftTrend to rightTrend
        )

        return allSegments to analysis
    }

    fun generatePoints(expression: String, minX: Double, maxX: Double, steps: Int = 1000): List<List<Point>> {
        return generateAndAnalyze(expression, minX, maxX, steps).first
    }
}
