package com.xemophon.aljabr.calculus.differentiate

import com.xemophon.aljabr.utils.SymjaUtils
import kotlin.math.abs

data class AnalysisResult(
    val variables: List<String>,
    val derivatives: List<NamedExpression>,
    val localMaxima: List<String> = emptyList(),
    val localMinima: List<String> = emptyList(),
    val inflectionPoints: List<String> = emptyList(),
    val stationaryPoints: List<String> = emptyList(),
    val saddlePoints: List<String> = emptyList(),
    val error: String? = null
)

data class NamedExpression(val name: String, val expression: String)

object AnalysisFunc {

    fun fullAnalysis(expression: String): AnalysisResult {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return AnalysisResult(emptyList(), emptyList(), error = "Empty expression")

            val eval = SymjaUtils.evaluator
            
            // Get variables
            val varsExpr = eval.eval("Variables[$cleaned]")
            val vars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (vars.size > 2) {
                return AnalysisResult(vars, emptyList(), error = "Maximum 2 variables supported")
            }

            if (vars.isEmpty() || (vars.size == 1 && vars[0] != "x" && vars[0] != "y")) {
                 // Try to force 'x' if no vars found or constant
                 val firstDeriv = eval.eval("D[$cleaned, x]").toString()
                 return AnalysisResult(
                     listOf("x"),
                     listOf(NamedExpression("f'(x)", SymjaUtils.formatResult(firstDeriv)))
                 )
            }

            if (vars.size == 1) {
                val v = vars[0]
                val f1 = eval.eval("D[$cleaned, $v]").toString()
                val f2 = eval.eval("D[$cleaned, {$v, 2}]").toString()
                
                val statPointsRes = try {
                    eval.eval("Solve[D[$cleaned, $v] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }
                
                val inflPointsRes = try {
                    eval.eval("Solve[D[$cleaned, {$v, 2}] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }

                // Singular points where D[f, v] is undefined but f(v) is defined
                val singPointsRes = try {
                    val deriv = "D[$cleaned, $v]"
                    eval.eval("Solve[Denominator[Together[$deriv]] == 0, $v]").toString()
                } catch (e: Exception) { "{}" }

                val (maxima, minima, others) = classifyStationaryPoints(statPointsRes, singPointsRes, cleaned, v)

                AnalysisResult(
                    variables = vars,
                    derivatives = listOf(
                        NamedExpression("f'($v)", SymjaUtils.formatResult(f1)),
                        NamedExpression("f''($v)", SymjaUtils.formatResult(f2))
                    ),
                    localMaxima = maxima,
                    localMinima = minima,
                    stationaryPoints = others,
                    inflectionPoints = calculatePoints(inflPointsRes, cleaned, vars)
                )
            } else {
                // 2 variables: x and y usually
                val x = vars.find { it == "x" } ?: vars[0]
                val y = vars.find { it == "y" && it != x } ?: vars[1]
                
                val fx = eval.eval("D[$cleaned, $x]").toString()
                val fy = eval.eval("D[$cleaned, $y]").toString()
                val fxx = eval.eval("D[$cleaned, {$x, 2}]").toString()
                val fyy = eval.eval("D[$cleaned, {$y, 2}]").toString()
                val fxy = eval.eval("D[$cleaned, $x, $y]").toString()
                
                val critPointsRes = try {
                    eval.eval("Solve[{D[$cleaned, $x] == 0, D[$cleaned, $y] == 0}, {$x, $y}]").toString()
                } catch (e: Exception) { "Could not solve" }

                val (maxima, minima, saddles, others) = classifyStationaryPoints2D(critPointsRes, cleaned, x, y)

                AnalysisResult(
                    variables = listOf(x, y),
                    derivatives = listOf(
                        NamedExpression("f_$x", SymjaUtils.formatResult(fx)),
                        NamedExpression("f_$y", SymjaUtils.formatResult(fy)),
                        NamedExpression("f_$x$x", SymjaUtils.formatResult(fxx)),
                        NamedExpression("f_$y$y", SymjaUtils.formatResult(fyy)),
                        NamedExpression("f_$x$y", SymjaUtils.formatResult(fxy))
                    ),
                    localMaxima = maxima,
                    localMinima = minima,
                    saddlePoints = saddles,
                    stationaryPoints = others
                )
            }
        } catch (e: Exception) {
            AnalysisResult(emptyList(), emptyList(), error = e.message ?: "Analysis failed")
        }
    }

    private fun classifyStationaryPoints2D(
        solveRes: String,
        originalExpr: String,
        xVar: String,
        yVar: String
    ): Fourth<List<String>, List<String>, List<String>, List<String>> {
        val eval = SymjaUtils.evaluator
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        
        val maxima = mutableListOf<String>()
        val minima = mutableListOf<String>()
        val saddles = mutableListOf<String>()
        val others = mutableListOf<String>()

        val fxxExpr = "D[$originalExpr, {$xVar, 2}]"
        val fyyExpr = "D[$originalExpr, {$yVar, 2}]"
        val fxyExpr = "D[$originalExpr, $xVar, $yVar]"

        for (sol in solutions) {
            try {
                // sol is like "x -> 0, y -> 0"
                val fValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                val fValStr = fValExpr.toString()
                if (fValStr.contains("Infinity") || fValStr.contains("Indeterminate")) continue

                // Check for complex solutions
                val parts = sol.split(",").map { it.trim() }
                var isComplex = false
                val coords = mutableListOf<String>()
                for (part in parts) {
                    val valStr = part.split("->").last().trim()
                    val imPart = eval.eval("Im[N[$valStr]]").toString().toDoubleOrNull()
                    if (imPart == null || abs(imPart) > 1e-9) {
                        isComplex = true
                        break
                    }
                    coords.add(SymjaUtils.formatResult(valStr))
                }
                if (isComplex) continue

                val pointStr = "(${coords.joinToString(", ")}, ${SymjaUtils.formatResult(fValStr)})"

                // Hessian components
                val fxxVal = eval.eval("N[ReplaceAll[$fxxExpr, {$sol}]]").toString().toDoubleOrNull()
                val fyyVal = eval.eval("N[ReplaceAll[$fyyExpr, {$sol}]]").toString().toDoubleOrNull()
                val fxyVal = eval.eval("N[ReplaceAll[$fxyExpr, {$sol}]]").toString().toDoubleOrNull()

                if (fxxVal != null && fyyVal != null && fxyVal != null) {
                    val detH = fxxVal * fyyVal - fxyVal * fxyVal
                    if (detH > 1e-9) {
                        if (fxxVal > 1e-9) minima.add(pointStr)
                        else if (fxxVal < -1e-9) maxima.add(pointStr)
                        else others.add(pointStr)
                    } else if (detH < -1e-9) {
                        saddles.add(pointStr)
                    } else {
                        others.add(pointStr) // Inconclusive
                    }
                } else {
                    others.add(pointStr)
                }
            } catch (_: Exception) {}
        }

        return Fourth(maxima.distinct(), minima.distinct(), saddles.distinct(), others.distinct())
    }

    data class Fourth<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private fun classifyStationaryPoints(
        solveRes: String,
        singularRes: String,
        originalExpr: String,
        variable: String
    ): Triple<List<String>, List<String>, List<String>> {
        val eval = SymjaUtils.evaluator
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        val singulars = SymjaUtils.parseSolveResult(singularRes)

        val allCandidateRules = (solutions + singulars).distinct()
        
        val maxima = mutableListOf<String>()
        val minima = mutableListOf<String>()
        val others = mutableListOf<String>()

        val f2Expr = eval.eval("D[$originalExpr, {$variable, 2}]")

        for (sol in allCandidateRules) {
            try {
                val xValStr = sol.split("->").last().trim()
                // Filter complex solutions
                val imPart = eval.eval("Im[N[$xValStr]]").toString().toDoubleOrNull()
                if (imPart == null || abs(imPart) > 1e-9) continue

                // Check if f(x) exists
                val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                val yValStr = yValExpr.toString()
                if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                val pointStr = "(${SymjaUtils.formatResult(xValStr)}, ${SymjaUtils.formatResult(yValStr)})"

                // Second derivative test
                val d2ValExpr = eval.eval("ReplaceAll[$f2Expr, {$sol}]")
                val d2ValStr = d2ValExpr.toString()
                val d2Val = d2ValStr.toDoubleOrNull()

                if (d2Val != null) {
                    if (d2Val < -1e-9) maxima.add(pointStr)
                    else if (d2Val > 1e-9) minima.add(pointStr)
                    else {
                        // d2Val == 0, use neighborhood test
                        val type = neighborhoodTest(originalExpr, variable, xValStr)
                        when (type) {
                            1 -> maxima.add(pointStr)
                            -1 -> minima.add(pointStr)
                            else -> others.add(pointStr)
                        }
                    }
                } else {
                    // Symbolic result or undefined d2Val, use neighborhood test
                    val type = neighborhoodTest(originalExpr, variable, xValStr)
                    when (type) {
                        1 -> maxima.add(pointStr)
                        -1 -> minima.add(pointStr)
                        else -> others.add(pointStr)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return Triple(maxima.distinct(), minima.distinct(), others.distinct())
    }

    private fun neighborhoodTest(expr: String, variable: String, xCenterStr: String): Int {
        val eval = SymjaUtils.evaluator
        try {
            val xCenter = eval.eval("N[$xCenterStr]").toString().toDoubleOrNull() ?: return 0
            val eps = 1e-5
            
            val yCenter = eval.eval("N[ReplaceAll[$expr, $variable -> $xCenter]]").toString().toDoubleOrNull() ?: return 0
            val yLeft = eval.eval("N[ReplaceAll[$expr, $variable -> ${xCenter - eps}]]").toString().toDoubleOrNull() ?: return 0
            val yRight = eval.eval("N[ReplaceAll[$expr, $variable -> ${xCenter + eps}]]").toString().toDoubleOrNull() ?: return 0

            return if (yCenter > yLeft + 1e-11 && yCenter > yRight + 1e-11) 1 // Max
            else if (yCenter < yLeft - 1e-11 && yCenter < yRight - 1e-11) -1 // Min
            else 0
        } catch (_: Exception) {
            return 0
        }
    }

    private fun calculatePoints(solveRes: String, originalExpr: String, variables: List<String>): List<String> {
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        if (solutions.isEmpty()) return emptyList()

        val eval = SymjaUtils.evaluator
        val points = mutableListOf<String>()

        for (sol in solutions) {
            try {
                if (variables.size == 1) {
                    val xValStr = sol.split("->").last().trim()
                    // Filter complex solutions
                    val imPart = eval.eval("Im[N[$xValStr]]").toString().toDoubleOrNull()
                    if (imPart == null || abs(imPart) > 1e-9) continue

                    // Check if f(x) exists
                    val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                    val yValStr = yValExpr.toString()
                    if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                    points.add("(${SymjaUtils.formatResult(xValStr)}, ${SymjaUtils.formatResult(yValStr)})")
                } else {
                    // Expecting something like "x -> 1, y -> 2"
                    // In multi-variable case, sol might be "x -> 1, y -> 2"
                    val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                    val yValStr = yValExpr.toString()
                    if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                    val parts = sol.split(",").map { it.trim() }
                    val coords = variables.map { v ->
                        parts.find { it.startsWith(v) }?.split("->")?.last()?.trim() ?: "?"
                    }
                    points.add("(${coords.joinToString(", ") { SymjaUtils.formatResult(it) }}, ${SymjaUtils.formatResult(yValStr)})")
                }
            } catch (e: Exception) {
                // Fallback to formatting the raw solution if evaluation fails
                points.add(SymjaUtils.formatResult(sol))
            }
        }
        return points.distinct()
    }
}
