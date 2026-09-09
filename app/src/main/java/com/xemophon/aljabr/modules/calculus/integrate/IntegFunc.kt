package com.xemophon.aljabr.modules.calculus.integrate

import com.xemophon.aljabr.modules.calculus.HybridEngine
import com.xemophon.aljabr.ui.components.screens.CalculusStep
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.IntegralType

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
     * Splits string by top-level commas, respecting parenthetical / bracket depth.
     */
    fun splitTopLevelCommas(input: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth > 0) depth--
                ',' -> {
                    if (depth == 0) {
                        result.add(sb.toString().trim())
                        sb.clear()
                        continue
                    }
                }
            }
            sb.append(ch)
        }
        if (sb.isNotEmpty() || result.isNotEmpty()) {
            result.add(sb.toString().trim())
        }
        return result
    }

    private fun getIntegrationVariable(expression: String, type: IntegralType): String {
        return when (type) {
            IntegralType.CURVET1 -> {
                val parts = splitTopLevelCommas(expression)
                if (parts.size == 3) "t" else "x"
            }
            IntegralType.CURVET2 -> {
                val parts = splitTopLevelCommas(expression)
                if (parts.size == 4) "t" else "x"
            }
            else -> "x"
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
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = constructFormula(expression, useRadians, type)
                val v = getIntegrationVariable(expression, type)

                val isIndefinite = lower.isBlank() && upper.isBlank()

                val command = if (isIndefinite) {
                    if (useRationalize) "Simplify[Integrate[Rationalize[$formula], $v]]" else "Simplify[Integrate[$formula, $v]]"
                } else {
                    val lStr = SymjaUtils.prepareForSymja(lower)
                    val uStr = SymjaUtils.prepareForSymja(upper)
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {$v, Rationalize[$lStr], Rationalize[$uStr]}]"
                    } else {
                        "Integrate[$formula, {$v, $lStr, $uStr}]"
                    }
                }

                val res = eval.eval(command).toString()

                if (res.contains("Integrate", ignoreCase = true) || res == "\$Failed") {
                    if (!isIndefinite) {
                        val lNum = lower.toDoubleOrNull() ?: Double.NaN
                        val uNum = upper.toDoubleOrNull() ?: Double.NaN
                        if (!lNum.isNaN() && !uNum.isNaN()) {
                            val num = integrate(expression, lNum, uNum, useRadians, type)
                            if (!num.isNaN()) return@evaluate num.toString()
                        }
                    }
                    return@evaluate categorizeIntegralResult(res, false, "∫($expression)d$v")
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
        useRadians: Boolean,
        type: IntegralType
    ): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = constructFormula(expression, useRadians, type)
                val v = getIntegrationVariable(expression, type)
                val lStr = formatLimit(lower)
                val uStr = formatLimit(upper)

                val res = eval.eval("NIntegrate[$formula, {$v, $lStr, $uStr}]").toString()
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
        useRadians: Boolean,
        type: IntegralType
    ): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = constructFormula(expression, useRadians, type)
                val v = getIntegrationVariable(expression, type)
                val lStr = formatLimit(lower)
                val uStr = formatLimit(upper)

                // Evaluate symbolically then force numerical conversion with N()
                val res = eval.eval("N[Integrate[$formula, {$v, $lStr, $uStr}]]").toString()
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun constructFormula(expression: String, useRadians: Boolean, type: IntegralType): String {
        return when (type) {
            IntegralType.DEFINITE -> SymjaUtils.prepareForSymja(expression, useRadians)
            IntegralType.ARC -> {
                val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
                "Sqrt(1 + (D($symjaExpr, x))^2)"
            }
            IntegralType.XVOL -> {
                val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
                "Pi * ($symjaExpr)^2"
            }
            IntegralType.YVOL -> {
                val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
                "2 * Pi * Abs(x * ($symjaExpr))"
            }
            IntegralType.XSURF -> {
                val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
                "2 * Pi * Abs($symjaExpr) * Sqrt(1 + (D($symjaExpr, x))^2)"
            }
            IntegralType.YSURF -> {
                val symjaExpr = SymjaUtils.prepareForSymja(expression, useRadians)
                "2 * Pi * Abs(x) * Sqrt(1 + (D($symjaExpr, x))^2)"
            }
            IntegralType.CURVET1 -> constructCurve1Formula(expression, useRadians)
            IntegralType.CURVET2 -> constructCurve2Formula(expression, useRadians)
            else -> SymjaUtils.prepareForSymja(expression, useRadians)
        }
    }

    private fun constructCurve1Formula(expression: String, useRadians: Boolean): String {
        val parts = splitTopLevelCommas(expression)
        return when (parts.size) {
            3 -> {
                // f(x,y), x(t), y(t) -> \int_a^b f(x(t), y(t)) \sqrt{x'(t)^2 + y'(t)^2} dt
                val f = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val xt = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val yt = SymjaUtils.prepareForSymja(parts[2], useRadians)
                "ReplaceAll[$f, {x -> ($xt), y -> ($yt)}] * Sqrt((D($xt, t))^2 + (D($yt, t))^2)"
            }
            2 -> {
                // f(x,y), y(x)
                val f = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val yx = SymjaUtils.prepareForSymja(parts[1], useRadians)
                "ReplaceAll[$f, {y -> ($yx)}] * Sqrt(1 + (D($yx, x))^2)"
            }
            else -> {
                // f(x,y) or f(x)
                val f = SymjaUtils.prepareForSymja(expression, useRadians)
                if (f.contains("y")) {
                    // Explicitly treat default path as y = x when y is present without an explicit curve
                    "ReplaceAll[($f), {y -> x}] * Sqrt(1 + (D(x, x))^2)"
                } else {
                    "($f) * Sqrt(1 + (D($f, x))^2)"
                }
            }
        }
    }

    private fun constructCurve2Formula(expression: String, useRadians: Boolean): String {
        val parts = splitTopLevelCommas(expression)
        return when (parts.size) {
            4 -> {
                // P(x,y), Q(x,y), x(t), y(t) -> \int_a^b [P(x(t), y(t)) x'(t) + Q(x(t), y(t)) y'(t)] dt
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val xt = SymjaUtils.prepareForSymja(parts[2], useRadians)
                val yt = SymjaUtils.prepareForSymja(parts[3], useRadians)
                "ReplaceAll[$p, {x -> ($xt), y -> ($yt)}] * D($xt, t) + ReplaceAll[$q, {x -> ($xt), y -> ($yt)}] * D($yt, t)"
            }
            3 -> {
                // P(x,y), Q(x,y), y(x)
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val yx = SymjaUtils.prepareForSymja(parts[2], useRadians)
                "ReplaceAll[$p, {y -> ($yx)}] + ReplaceAll[$q, {y -> ($yx)}] * D($yx, x)"
            }
            2 -> {
                // P(x,y), Q(x,y) along explicit default path y = x (y'(x) = 1)
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                "ReplaceAll[$p, {y -> x}] + ReplaceAll[$q, {y -> x}] * D(x, x)"
            }
            else -> {
                // f(x)
                val f = SymjaUtils.prepareForSymja(expression, useRadians)
                "($f) * D($f, x)"
            }
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

    /**
     * Performs symbolic indefinite double integration over two variables (x and y).
     * Symja command: Integrate[expr, x, y]
     * Formats constants as + C₁(x) + C₂(y)
     */
    fun integrateDoubleIndefinite(
        expression: String,
        useRadians: Boolean = true,
        useRationalize: Boolean = false
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val cleaned = SymjaUtils.prepareForSymja(expression, useRadians)
                val command = if (useRationalize) {
                    "Simplify[Integrate[Rationalize[$cleaned], x, y]]"
                } else {
                    "Simplify[Integrate[$cleaned, x, y]]"
                }
                val resStr = eval.eval(command).toString()

                if (resStr.contains("Integrate", ignoreCase = true) || resStr == "\$Failed") {
                    return@evaluate categorizeIntegralResult(resStr, false, "∫∫($expression) dx dy")
                }

                val formatted = SymjaUtils.formatResult(resStr)
                "$formatted + C₁(x) + C₂(y)"
            }
        } catch (_: Exception) {
            "∫∫($expression) dx dy"
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
        useRationalize: Boolean = false
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = SymjaUtils.prepareForSymja(expression, useRadians)
                val l1 = if (lower.isBlank()) "a" else SymjaUtils.prepareForSymja(lower, useRadians)
                val u1 = if (upper.isBlank()) "b" else SymjaUtils.prepareForSymja(upper, useRadians)
                val l2 = if (innerLower.isBlank()) "c" else SymjaUtils.prepareForSymja(innerLower, useRadians)
                val u2 = if (innerUpper.isBlank()) "d" else SymjaUtils.prepareForSymja(innerUpper, useRadians)

                val command = if (axis == "Y") {
                    // Region II: outer y (l1..u1), inner x (l2..u2)
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {y, Rationalize[$l1], Rationalize[$u1]}, {x, Rationalize[$l2], Rationalize[$u2]}]"
                    } else {
                        "Integrate[$formula, {y, $l1, $u1}, {x, $l2, $u2}]"
                    }
                } else {
                    // Region I: outer x (l1..u1), inner y (l2..u2)
                    if (useRationalize) {
                        "Integrate[Rationalize[$formula], {x, Rationalize[$l1], Rationalize[$u1]}, {y, Rationalize[$l2], Rationalize[$u2]}]"
                    } else {
                        "Integrate[$formula, {x, $l1, $u1}, {y, $l2, $u2}]"
                    }
                }

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
     * Supports both constant and variable bounds (e.g. inner bounds depending on outer variable).
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
                    // Region II: outer y (l1..u1), inner x (l2..u2)
                    "NIntegrate[$formula, {y, $l1, $u1}, {x, $l2, $u2}]"
                } else {
                    // Region I: outer x (l1..u1), inner y (l2..u2)
                    "NIntegrate[$formula, {x, $l1, $u1}, {y, $l2, $u2}]"
                }
                val res = eval.eval(command).toString()
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    /**
     * Categorizes CAS output into explicit mathematical results (divergence, undefined, numerical failure, no closed form).
     */
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
