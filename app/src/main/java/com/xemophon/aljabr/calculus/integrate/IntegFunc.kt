package com.xemophon.aljabr.calculus.integrate

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

    /**
     * Performs numerical integration using Symja's NIntegrate with a fallback 
     * to symbolic Integrate for more complex or precise cases.
     */
    fun integrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean = true,
        type: IntegralType = IntegralType.DEFINITE,
    ): Double {
        if (lower == upper) return 0.0

        // 1. Try Symja's NIntegrate (Numerical estimation, usually fast)
        val numericalRes = symjaNumericalIntegrate(expression, lower, upper, useRadians, type)
        if (!numericalRes.isNaN()) return numericalRes

        // 2. Fallback to Symbolic Integrate (More precise but potentially slower)
        return symjaSymbolicIntegrate(expression, lower, upper, useRadians, type)
    }

    /**
     * Performs symbolic integration which preserves fractions and exact values.
     */
    fun integrateSymbolic(
        expression: String,
        lower: String,
        upper: String,
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        type: IntegralType = IntegralType.DEFINITE
    ): String {
        return synchronized(SymjaUtils.evaluator) {
            try {
                val formula = constructFormula(expression, useRadians, type)
                val lStr = if (lower.isBlank()) "a" else SymjaUtils.prepareForSymja(lower)
                val uStr = if (upper.isBlank()) "b" else SymjaUtils.prepareForSymja(upper)

                val command = if (useRationalize) {
                    "Rationalize(Integrate[Rationalize($formula), {x, Rationalize($lStr), Rationalize($uStr)}])"
                } else {
                    "Integrate[$formula, {x, $lStr, $uStr}]"
                }

                val res = SymjaUtils.evaluator.eval(command).toString()
                
                if (res.contains("Integrate")) {
                    // Fallback to numerical if symbolic fails
                    val lNum = lower.toDoubleOrNull() ?: Double.NaN
                    val uNum = upper.toDoubleOrNull() ?: Double.NaN
                    if (!lNum.isNaN() && !uNum.isNaN()) {
                        val num = integrate(expression, lNum, uNum, useRadians, type)
                        if (!num.isNaN()) return num.toString()
                    }
                    return "∫($expression)dx"
                }

                SymjaUtils.formatResult(res)
            } catch (_: Exception) {
                "Error"
            }
        }
    }

    private fun symjaNumericalIntegrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean,
        type: IntegralType
    ): Double {
        return try {
            val formula = constructFormula(expression, useRadians, type)
            val lStr = formatLimit(lower)
            val uStr = formatLimit(upper)

            val res = SymjaUtils.evaluator.eval("NIntegrate[$formula, {x, $lStr, $uStr}]").toString()
            res.toDouble()
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun symjaSymbolicIntegrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean,
        type: IntegralType
    ): Double {
        return try {
            val formula = constructFormula(expression, useRadians, type)
            val lStr = formatLimit(lower)
            val uStr = formatLimit(upper)

            // Evaluate symbolically then force numerical conversion with N()
            val res = SymjaUtils.evaluator.eval("Integrate[$formula, {x, $lStr, $uStr}]").toString()
            res.toDouble()
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun constructFormula(expression: String, useRadians: Boolean, type: IntegralType): String {
        val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
        return when (type) {
            IntegralType.DEFINITE -> symjaExpr
            IntegralType.ARC -> "Sqrt(1 + (D($symjaExpr, x))^2)"
            IntegralType.XVOL -> "Pi * ($symjaExpr)^2"
            IntegralType.YVOL -> "2 * Pi * Abs(x * ($symjaExpr))"
            IntegralType.XSURF -> "2 * Pi * Abs($symjaExpr) * Sqrt(1 + (D($symjaExpr, x))^2)"
            IntegralType.YSURF -> "2 * Pi * Abs(x) * Sqrt(1 + (D($symjaExpr, x))^2)"
            else -> symjaExpr
        }
    }

    private fun formatLimit(limit: Double): String {
        return when {
            limit.isInfinite() -> if (limit < 0) "-Infinity" else "Infinity"
            else -> limit.toString()
        }
    }

    fun integrateIndefinite(expression: String, useRationalize: Boolean = false): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val command = if (useRationalize) {
                "Simplify[Rationalize(Integrate[Rationalize($cleaned), x])]"
            } else {
                "Simplify[Integrate[$cleaned, x]]"
            }
            var resStr = SymjaUtils.evaluator.eval(command).toString()

            if (resStr.contains("Integrate", ignoreCase = true)) {
                return "∫($expression)dx"
            }
// ...

            val logRegex = Regex("""Log([\[(])([^,\])]+)([)])""", RegexOption.IGNORE_CASE)
            resStr = resStr.replace(logRegex) { match ->
                val open = match.groupValues[1]
                val content = match.groupValues[2]
                val close = match.groupValues[3]
                "Log$open" + "Abs$open$content$close$close"
            }

            if (resStr == "0" && (cleaned != "0") && (cleaned != "0.0")) {
                return "∫($expression)dx"
            }

            formatResult(resStr)
        } catch (_: Exception) {
            "∫($expression)dx"
        }
    }

    fun integrateIndefiniteWithSteps(expression: String, useEigenmath: Boolean = true): Pair<String, List<CalculusStep>>? {
        return try {
            val resultAndSteps = calculusEngine.integrateWithSteps(expression, useEigenmath)
            if (resultAndSteps != null) {
                val (resExpr, steps) = resultAndSteps
                val formattedResult = formatResult(resExpr.toString())
                Pair(formattedResult, steps)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatResult(resStr: String): String {
        val formatted = SymjaUtils.formatResult(resStr)
        return "$formatted + C"
    }
}

/* TODO : Make double integral with TwoVariableGrid */