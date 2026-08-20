package com.xemophon.aljabr.calculus.integrate

import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.calculus.CalculusEngine
import com.xemophon.aljabr.ui.components.CalculusStep
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.IntegralType

object IntegFunc {
    private val calculusEngine = CalculusEngine()

    /**
     * Warms up the CAS engine by performing a dummy evaluation.
     * This loads the internal rule sets on a background thread.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluator.eval("Integrate[x, x]")
        } catch (_: Throwable) {
        }
    }

    fun integrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean = true,
        type: IntegralType = IntegralType.DEFINITE
    ): Double {
        return try {
            if (lower == upper) return 0.0

            // Support for infinite limits or complex functions using NIntegrate fallback
            if (lower.isInfinite() || upper.isInfinite()) {
                val res = nIntegrate(expression, lower, upper, type)
                if (!res.isNaN()) return res
            }
            
            val symjaExpr = SymjaUtils.prepareForSymja(expression)
            val derivative = if (type == IntegralType.ARC || type == IntegralType.XSURF || type == IntegralType.YSURF) {
                SymjaUtils.evaluator.eval("D[$symjaExpr, x]").toString()
            } else ""

            val formula: (Double) -> Double = { x ->
                val fx = evaluateAt(expression, x, useRadians)
                when (type) {
                    IntegralType.DEFINITE -> fx
                    IntegralType.ARC -> {
                        val fpx = CalcFuncs.calculateExpression(derivative, mapOf("x" to x), useRadians)
                        Math.sqrt(1 + fpx * fpx)
                    }
                    IntegralType.XVOL -> Math.PI * fx * fx
                    IntegralType.YVOL -> 2 * Math.PI * Math.abs(x * fx)
                    IntegralType.XSURF -> {
                        val fpx = CalcFuncs.calculateExpression(derivative, mapOf("x" to x), useRadians)
                        2 * Math.PI * Math.abs(fx) * Math.sqrt(1 + fpx * fpx)
                    }
                    IntegralType.YSURF -> {
                        val fpx = CalcFuncs.calculateExpression(derivative, mapOf("x" to x), useRadians)
                        2 * Math.PI * Math.abs(x) * Math.sqrt(1 + fpx * fpx)
                    }
                    else -> fx
                }
            }

            // Use Simpson's 1/3 rule
            val n = 1000
            val h = (upper - lower) / n

            var sum = formula(lower) + formula(upper)

            val startTime = System.currentTimeMillis()
            for (i in 1 until n) {
                if (i % 50 == 0 && System.currentTimeMillis() - startTime > 2000) {
                    return Double.NaN
                }

                val x = lower + i * h
                val factor = if (i % 2 == 0) 2 else 4
                val valAtX = formula(x)
                if (valAtX.isNaN() || valAtX.isInfinite()) return Double.NaN
                sum += factor * valAtX
            }

            val result = (h / 3) * sum
            
            if (result.isNaN()) {
                return nIntegrate(expression, lower, upper, type)
            }
            result
        } catch (e: Throwable) {
            nIntegrate(expression, lower, upper, type)
        }
    }

    private fun nIntegrate(
        expression: String,
        lower: Double,
        upper: Double,
        type: IntegralType
    ): Double {
        return try {
            val symjaExpr = SymjaUtils.prepareForSymja(expression)
            val lStr = if (lower.isInfinite()) if (lower < 0) "-Infinity" else "Infinity" else lower.toString()
            val uStr = if (upper.isInfinite()) if (upper < 0) "-Infinity" else "Infinity" else upper.toString()

            val formula = when (type) {
                IntegralType.DEFINITE -> symjaExpr
                IntegralType.ARC -> "Sqrt(1 + (D($symjaExpr, x))^2)"
                IntegralType.XVOL -> "Pi * ($symjaExpr)^2"
                IntegralType.YVOL -> "2 * Pi * Abs(x * ($symjaExpr))"
                IntegralType.XSURF -> "2 * Pi * Abs($symjaExpr) * Sqrt(1 + (D($symjaExpr, x))^2)"
                IntegralType.YSURF -> "2 * Pi * Abs(x) * Sqrt(1 + (D($symjaExpr, x))^2)"
                else -> symjaExpr
            }

            val res = SymjaUtils.evaluator.eval("NIntegrate[$formula, {x, $lStr, $uStr}]").toString()
            res.toDouble()
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun evaluateAt(expression: String, x: Double, useRadians: Boolean): Double {
        return CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians)
    }

    fun integrateIndefinite(expression: String): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            var resStr = SymjaUtils.evaluator.eval("Simplify[Integrate[$cleaned, x]]").toString()

            if (resStr.contains("Integrate", ignoreCase = true)) {
                return "∫($expression)dx"
            }

            val logRegex = Regex("""Log([\[(])([^,\])]+)([\])])""", RegexOption.IGNORE_CASE)
            resStr = resStr.replace(logRegex) { match ->
                val open = match.groupValues[1]
                val content = match.groupValues[2]
                val close = match.groupValues[3]
                "Log${open}Abs${open}${content}${close}${close}"
            }

            if (resStr == "0" && cleaned != "0" && cleaned != "0.0") {
                return "∫($expression)dx"
            }

            formatResult(resStr)
        } catch (_: Exception) {
            "∫($expression)dx"
        }
    }

    suspend fun integrateIndefiniteWithSteps(expression: String, useEigenmath: Boolean = true): Pair<String, List<CalculusStep>> {
        return try {
            val steps = calculusEngine.integrateWithSteps(expression, useEigenmath)
            val finalResult = integrateIndefinite(expression)
            Pair(finalResult, steps)
        } catch (_: Exception) {
            Pair(integrateIndefinite(expression), emptyList())
        }
    }

    private fun formatResult(resStr: String): String {
        var formatted = SymjaUtils.formatResult(resStr)
        return "$formatted + C"
    }
}
