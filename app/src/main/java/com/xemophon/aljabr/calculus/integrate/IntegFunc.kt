package com.xemophon.aljabr.calculus.integrate

import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.data.SymjaUtils

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
        useRadians: Boolean = true
    ): Double {
        return try {
            if (lower == upper) return 0.0

            // Use Simpson's 1/3 rule
            val n = 1000
            val h = (upper - lower) / n

            var sum = evaluateAt(expression, lower, useRadians) + evaluateAt(
                expression,
                upper,
                useRadians
            )

            val startTime = System.currentTimeMillis()
            for (i in 1 until n) {
                if (i % 50 == 0 && System.currentTimeMillis() - startTime > 2000) {
                    return Double.NaN
                }

                val x = lower + i * h
                val factor = if (i % 2 == 0) 2 else 4
                val valAtX = evaluateAt(expression, x, useRadians)
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
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return ""

            // Use the shared evaluator with relaxed syntax
            // We use Integrate[...] square brackets to be explicit for the CAS engine
            val result = SymjaUtils.evaluator.eval("Integrate[$cleaned, x]")
            var resStr = result.toString()

            // If Symja couldn't solve it, it returns the input string Integrate(...)
            if (resStr.contains("Integrate", ignoreCase = true)) {
                return "∫($expression)dx"
            }

            // Replace Log[x] with Log[Abs[x]] for standard calculus notation ln|x|
            // This is a common preference for indefinite integrals in many curricula
            // Handles both Log[...] and Log(...) notation
            val logRegex = Regex("Log([\\[(])([^)\\]]+)([])])", RegexOption.IGNORE_CASE)
            resStr = resStr.replace(logRegex) { match ->
                val open = match.groupValues[1]
                val content = match.groupValues[2]
                val close = match.groupValues[3]
                "Log${open}Abs${open}${content}${close}${close}"
            }

            // Debug check: If input was NOT zero but result IS zero, something is likely wrong
            // with how the variable or expression is being parsed on this device.
            if (resStr == "0" && cleaned != "0" && cleaned != "0.0") {
                return "∫($expression)dx" // Fallback to symbolic notation
            }

            formatResult(resStr)
        } catch (e: Throwable) {
            // Log the error message in the result for debugging if it fails
            "Error: ${e.message?.take(20) ?: "unknown"}"
        }
    }

    private fun formatResult(resStr: String): String {
        var formatted = SymjaUtils.formatResult(resStr)
        return "$formatted + C"
    }
}
