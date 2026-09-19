package com.xemophon.aljabr.modules.calculus.integrate.standard

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
        useSimplify: Boolean = true,
        type: IntegralType = IntegralType.DEFINITE,
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val isIndefinite = lower.isBlank() && upper.isBlank()

                val core = if (isIndefinite) {
                    if (useRationalize) "Integrate[Rationalize[$formula], x]" else "Integrate[$formula, x]"
                } else {
                    val lStr = SymjaUtils.prepareForSymja(lower)
                    val uStr = SymjaUtils.prepareForSymja(upper)
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {x, Rationalize[$lStr], Rationalize[$uStr]}]"
                    } else {
                        "Integrate[$formula, {x, $lStr, $uStr}]"
                    }
                }
                val command = if (useSimplify) "Simplify[$core]" else core

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

    fun integrateIndefinite(
        expression: String,
        useRationalize: Boolean = false,
        useSimplify: Boolean = true
    ): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val core = if (useRationalize) "Integrate[Rationalize[$cleaned], x]" else "Integrate[$cleaned, x]"
            val command = if (useSimplify) "Simplify[$core]" else core
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

    /**
     * Performs symbolic indefinite double integration over two variables (x and y).
     * Region I (axis == "X"): dy dx -> inner y, outer x. Symja command: Integrate[expr, y, x]
     * Region II (axis == "Y"): dx dy -> inner x, outer y. Symja command: Integrate[expr, x, y]
     * Formats constants as + C₁(x) + C₂(y) for Region I, or + C₁(y) + C₂(x) for Region II.
     */
    fun integrateDoubleIndefinite(
        expression: String,
        axis: String = "X",
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        useSimplify: Boolean = true
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val cleaned = SymjaUtils.prepareForSymja(expression, useRadians)
                val core = if (axis == "Y") {
                    if (useRationalize) "Integrate[Rationalize[$cleaned], x, y]" else "Integrate[$cleaned, x, y]"
                } else {
                    if (useRationalize) "Integrate[Rationalize[$cleaned], y, x]" else "Integrate[$cleaned, y, x]"
                }
                val command = if (useSimplify) "Simplify[$core]" else core
                val resStr = eval.eval(command).toString()

                val diffs = if (axis == "Y") "dx dy" else "dy dx"
                if (resStr.contains("Integrate", ignoreCase = true) || resStr == "\$Failed") {
                    return@evaluate categorizeIntegralResult(resStr, false, "∫∫($expression) $diffs")
                }

                val formatted = SymjaUtils.formatResult(resStr)
                if (axis == "Y") "$formatted + C₁(y) + C₂(x)" else "$formatted + C₁(x) + C₂(y)"
            }
        } catch (_: Exception) {
            val diffs = if (axis == "Y") "dx dy" else "dy dx"
            "∫∫($expression) $diffs"
        }
    }

    /**
     * Performs symbolic definite double integration over x and y.
     * Region I (axis == "X"): outer x (a to b), inner y (c(x) to d(x)). Symja: Integrate[expr, {x, a, b}, {y, c, d}]
     * Region II (axis == "Y"): outer y (a to b), inner x (c(y) to d(y)). Symja: Integrate[expr, {y, a, b}, {x, c, d}]
     */
    fun integrateDoubleDefinite(
        expression: String,
        lower: String,
        upper: String,
        innerLower: String,
        innerUpper: String,
        axis: String = "X",
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        useSimplify: Boolean = true
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val l1 = if (lower.isBlank()) "a" else SymjaUtils.prepareForSymja(lower, useRadians)
                val u1 = if (upper.isBlank()) "b" else SymjaUtils.prepareForSymja(upper, useRadians)
                val l2 = if (innerLower.isBlank()) "c" else SymjaUtils.prepareForSymja(innerLower, useRadians)
                val u2 = if (innerUpper.isBlank()) "d" else SymjaUtils.prepareForSymja(innerUpper, useRadians)

                val core = if (axis == "Y") {
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {y, Rationalize[$l1], Rationalize[$u1]}, {x, Rationalize[$l2], Rationalize[$u2]}]"
                    } else {
                        "Integrate[$formula, {y, $l1, $u1}, {x, $l2, $u2}]"
                    }
                } else {
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {x, Rationalize[$l1], Rationalize[$u1]}, {y, Rationalize[$l2], Rationalize[$u2]}]"
                    } else {
                        "Integrate[$formula, {x, $l1, $u1}, {y, $l2, $u2}]"
                    }
                }
                val command = if (useSimplify) "Simplify[$core]" else core

                val res = eval.eval(command).toString()

                if (res.contains("Integrate", ignoreCase = true) || res == "\$Failed") {
                    val num = integrateDoubleNumerical(expression, lower, upper, innerLower, innerUpper, axis, useRadians)
                    if (!num.isNaN()) return@evaluate SymjaUtils.formatResult(num.toString())
                    val diffs = if (axis == "Y") "dx dy" else "dy dx"
                    return@evaluate categorizeIntegralResult(res, false, "∫∫($expression) $diffs")
                }

                SymjaUtils.formatResult(res)
            }
        } catch (_: Exception) {
            "Error"
        }
    }

    /**
     * Performs numerical double integration using Symja's NIntegrate.
     */
    fun integrateDoubleNumerical(
        expression: String,
        lower: String,
        upper: String,
        innerLower: String,
        innerUpper: String,
        axis: String = "X",
        useRadians: Boolean = true
    ): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val l1 = if (lower.isBlank()) "0" else SymjaUtils.prepareForSymja(lower, useRadians)
                val u1 = if (upper.isBlank()) "1" else SymjaUtils.prepareForSymja(upper, useRadians)
                val l2 = if (innerLower.isBlank()) "0" else SymjaUtils.prepareForSymja(innerLower, useRadians)
                val u2 = if (innerUpper.isBlank()) "1" else SymjaUtils.prepareForSymja(innerUpper, useRadians)

                val command = if (axis == "Y") {
                    "NIntegrate[$formula, {y, $l1, $u1}, {x, $l2, $u2}]"
                } else {
                    "NIntegrate[$formula, {x, $l1, $u1}, {y, $l2, $u2}]"
                }
                val res = eval.eval(command).toString()
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }
}
