package com.xemophon.aljabr.calculus.integrate

import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.IntegralType

object IntegFunc {

    /**
     * Warms up the CAS engine by performing a dummy evaluation.
     * This loads the internal rule sets on a background thread.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluator.eval("1+1")
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

            (h / 3) * sum
        } catch (e: Throwable) {
            Double.NaN
        }
    }

    private fun evaluateAt(expression: String, x: Double, useRadians: Boolean): Double {
        return CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians)
    }

    fun integrateIndefinite(expression: String): String {
        return integrateIndefiniteWithSteps(expression).first
    }

    fun integrateIndefiniteWithSteps(expression: String): Pair<String, List<String>> {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return Pair("", emptyList())

            val (rawResult, steps) = SymjaUtils.evalWithSteps("Integrate[$cleaned, x]")
            val result = SymjaUtils.evaluator.eval("Simplify[$rawResult]").toString()
            var resStr = result

            // If Symja couldn't solve it, it returns the input string Integrate(...)
            if (resStr.contains("Integrate", ignoreCase = true)) {
                return Pair("∫($expression)dx", emptyList())
            }

            // Replace Log[x] with Log[Abs[x]] for standard calculus notation ln|x|
            // We use [^,]+ to ensure we only match single-argument natural logs
            val logRegex = Regex("Log([\\[(])([^,)\\]]+)([])])", RegexOption.IGNORE_CASE)
            resStr = resStr.replace(logRegex) { match ->
                val open = match.groupValues[1]
                val content = match.groupValues[2]
                val close = match.groupValues[3]
                "Log${open}Abs${open}${content}${close}${close}"
            }

            if (resStr == "0" && cleaned != "0" && cleaned != "0.0") {
                return Pair("∫($expression)dx", emptyList())
            }

            Pair(formatResult(resStr), steps)
        } catch (e: Throwable) {
            Pair("Error: ${e.message?.take(20) ?: "unknown"}", emptyList())
        }
    }

    private fun formatResult(resStr: String): String {
        var formatted = SymjaUtils.formatResult(resStr)
        return "$formatted + C"
    }
}
