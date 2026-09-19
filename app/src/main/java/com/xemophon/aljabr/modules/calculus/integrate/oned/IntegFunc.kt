package com.xemophon.aljabr.modules.calculus.integrate.oned

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.modules.calculus.HybridEngine
import com.xemophon.aljabr.ui.components.buttons.IntegralType
import com.xemophon.aljabr.ui.components.screens.CalculusStep

object IntegFunc {
    private val hybridEngine = HybridEngine()

    /**
     * Warms up the CAS engine by performing a dummy evaluation.
     * This loads the internal rule sets on a background thread.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluate { eval ->
                eval.eval("Integrate[x, x]")
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Performs numerical integration using Symja's NIntegrate with a fallback 
     * to symbolic Integrate for 1D single-variable functions f(x).
     */
    fun integrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean = true,
        type: IntegralType = IntegralType.DEFINITE,
    ): Double {
        if (lower == upper) return 0.0

        val numericalRes = symjaNumericalIntegrate(expression, lower, upper, useRadians)
        if (!numericalRes.isNaN()) return numericalRes

        return symjaSymbolicIntegrate(expression, lower, upper, useRadians)
    }

    /**
     * Performs symbolic 1D integration for f(x).
     */
    fun integrateSymbolic(
        expression: String,
        lower: String,
        upper: String,
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        type: IntegralType = IntegralType.DEFINITE,
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val isIndefinite = lower.isBlank() && upper.isBlank()

                val command = if (isIndefinite) {
                    if (useRationalize) "Simplify[Integrate[Rationalize[$formula], x]]" else "Simplify[Integrate[$formula, x]]"
                } else {
                    val lStr = SymjaUtils.prepareForSymja(lower)
                    val uStr = SymjaUtils.prepareForSymja(upper)
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {x, Rationalize[$lStr], Rationalize[$uStr]}]"
                    } else {
                        "Integrate[$formula, {x, $lStr, $uStr}]"
                    }
                }

                val res = eval.eval(command).toString()

                if ((res.contains("Integrate", ignoreCase = true)) || (res == "\$Failed")) {
                    if (!isIndefinite) {
                        val lNum = lower.toDoubleOrNull() ?: Double.NaN
                        val uNum = upper.toDoubleOrNull() ?: Double.NaN
                        if (!lNum.isNaN() && !uNum.isNaN()) {
                            val num = integrate(expression, lNum, uNum, useRadians, type)
                            if (!num.isNaN()) return@evaluate num.toString()
                        }
                    }
                    return@evaluate categorizeIntegralResult(res, isNumerical = false, defaultFallback = "∫($expression)dx")
                }

                val formatted = SymjaUtils.formatResult(res)
                if (isIndefinite) "$formatted + C" else formatted
            }
        } catch (_: Exception) {
            "Error"
        }
    }

    private fun symjaNumericalIntegrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean
    ): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val lStr = formatLimit(lower)
                val uStr = formatLimit(upper)

                val res = eval.eval("NIntegrate[$formula, {x, $lStr, $uStr}]").toString()
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun symjaSymbolicIntegrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean
    ): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val lStr = formatLimit(lower)
                val uStr = formatLimit(upper)

                val res = eval.eval("N[Integrate[$formula, {x, $lStr, $uStr}]]").toString()
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
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
                "Simplify[Integrate[Rationalize[$cleaned], x]]"
            } else {
                "Simplify[Integrate[$cleaned, x]]"
            }
            val resStr = SymjaUtils.evaluate { eval ->
                eval.eval(command).toString()
            }

            if (resStr.contains("Integrate", ignoreCase = true) || resStr == "\$Failed") {
                return categorizeIntegralResult(resStr, false, "∫($expression)dx")
            }

            formatResult(resStr)
        } catch (_: Exception) {
            "∫($expression)dx"
        }
    }

    fun integrateIndefiniteWithSteps(expression: String, useHybrid: Boolean = true): Pair<String, List<CalculusStep>>? {
        return try {
            val resultAndSteps = hybridEngine.integrateWithSteps(expression, useHybrid)
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

    fun categorizeIntegralResult(resStr: String, isNumerical: Boolean = false, defaultFallback: String = "Error"): String {
        return when {
            resStr.contains("Infinity", ignoreCase = true) ||
            resStr.contains("ComplexInfinity", ignoreCase = true) ||
            resStr.contains("DirectedInfinity", ignoreCase = true) ||
            resStr.contains("idiv", ignoreCase = true) -> "Diverges (∞)"

            resStr.contains("Indeterminate", ignoreCase = true) -> "Undefined"

            resStr.contains("Integrate", ignoreCase = true) || resStr == "\$Failed" -> {
                if (isNumerical) "Numerical integration failed" else "No closed form solution"
            }

            resStr.contains("NIntegrate", ignoreCase = true) -> "Numerical integration failed"

            else -> defaultFallback
        }
    }
}
