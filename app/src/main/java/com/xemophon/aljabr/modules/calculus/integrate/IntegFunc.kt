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

    private fun getIntegrationVariable(expression: String, type: IntegralType): String {
        return when (type) {
            IntegralType.CURVET1 -> {
                val parts = expression.split(",")
                if (parts.size == 3) "t" else "x"
            }
            IntegralType.CURVET2 -> {
                val parts = expression.split(",")
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
                val lStr = if (lower.isBlank()) "a" else SymjaUtils.prepareForSymja(lower)
                val uStr = if (upper.isBlank()) "b" else SymjaUtils.prepareForSymja(upper)

                val command = if (useRationalize) {
                    "Integrate[Rationalize[$formula], {$v, Rationalize[$lStr], Rationalize[$uStr]}]"
                } else {
                    "Integrate[$formula, {$v, $lStr, $uStr}]"
                }

                val res = eval.eval(command).toString()
                
                if (res.contains("Integrate")) {
                    // Fallback to numerical if symbolic fails
                    val lNum = lower.toDoubleOrNull() ?: Double.NaN
                    val uNum = upper.toDoubleOrNull() ?: Double.NaN
                    if (!lNum.isNaN() && !uNum.isNaN()) {
                        val num = integrate(expression, lNum, uNum, useRadians, type)
                        if (!num.isNaN()) return@evaluate num.toString()
                    }
                    return@evaluate "∫($expression)d$v"
                }

                SymjaUtils.formatResult(res)
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
                res.toDouble()
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
                val res = eval.eval("Integrate[$formula, {$v, $lStr, $uStr}]").toString()
                res.toDouble()
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
        val parts = expression.split(",").map { it.trim() }
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
                    "ReplaceAll[($f), {y -> x}] * Sqrt(2)"
                } else {
                    "($f) * Sqrt(1 + (D($f, x))^2)"
                }
            }
        }
    }

    private fun constructCurve2Formula(expression: String, useRadians: Boolean): String {
        val parts = expression.split(",").map { it.trim() }
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
                // P(x,y), Q(x,y) along y=x
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                "ReplaceAll[$p, {y -> x}] + ReplaceAll[$q, {y -> x}]"
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
            var resStr = SymjaUtils.evaluate { eval ->
                eval.eval(command).toString()
            }

            if (resStr.contains("Integrate", ignoreCase = true)) {
                return "∫($expression)dx"
            }

            if (resStr == "0" && (cleaned != "0") && (cleaned != "0.0")) {
                return "∫($expression)dx"
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
                var resStr = eval.eval(command).toString()

                if (resStr.contains("Integrate", ignoreCase = true)) {
                    return@evaluate "∫∫($expression) dx dy"
                }

                if (resStr == "0" && (cleaned != "0") && (cleaned != "0.0")) {
                    return@evaluate "∫∫($expression) dx dy"
                }

                formatResult(resStr)
            }
        } catch (_: Exception) {
            "∫∫($expression) dx dy"
        }
    }

    /**
     * Performs symbolic definite double integration over x and y.
     * Symja command: Integrate[expr, {x, xLower, xUpper}, {y, yLower, yUpper}]
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

                val res = eval.eval(command).toString()

                if (res.contains("Integrate", ignoreCase = true) || (res == "0" && formula != "0" && formula != "0.0")) {
                    val num = integrateDoubleNumerical(expression, lower, upper, innerLower, innerUpper, axis, useRadians)
                    if (!num.isNaN()) return@evaluate num.toString()
                    return@evaluate "∫∫($expression) dx dy"
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
                    // Region II: outer y, inner x
                    "NIntegrate[$formula, {y, $l1, $u1}, {x, $l2, $u2}]"
                } else {
                    // Region I: outer x, inner y
                    "NIntegrate[$formula, {x, $l1, $u1}, {y, $l2, $u2}]"
                }
                val res = eval.eval(command).toString()
                res.toDouble()
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }
}
