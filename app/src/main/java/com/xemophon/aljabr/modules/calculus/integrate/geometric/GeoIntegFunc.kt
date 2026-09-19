package com.xemophon.aljabr.modules.calculus.integrate.geometric

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.IntegralType

object GeoIntegFunc {

    /**
     * Warms up the CAS engine for 2D integration on a background thread.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluate { eval ->
                eval.eval("Integrate[x*y, x, y]")
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
     * Performs line integration (Scalar Curve 1 or Vector Curve 2).
     */
    fun integrateCurve(
        expression: String,
        lower: String,
        upper: String,
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        useSimplify: Boolean = true,
        type: IntegralType = IntegralType.CURVET1
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = constructFormula(expression, useRadians, type)
                val v = getIntegrationVariable(expression, type)
                val lStr = SymjaUtils.prepareForSymja(lower, useRadians)
                val uStr = SymjaUtils.prepareForSymja(upper, useRadians)

                val core = if (useRationalize) {
                    "Integrate[Rationalize[$formula], {$v, Rationalize[$lStr], Rationalize[$uStr]}]"
                } else {
                    "Integrate[$formula, {$v, $lStr, $uStr}]"
                }
                val command = if (useSimplify) "Simplify[$core]" else core

                val res = eval.eval(command).toString()

                if (res.contains("Integrate", ignoreCase = true) || res == "\$Failed") {
                    val lNum = lower.toDoubleOrNull() ?: Double.NaN
                    val uNum = upper.toDoubleOrNull() ?: Double.NaN
                    if (!lNum.isNaN() && !uNum.isNaN()) {
                        val numRes = symjaNumericalIntegrate(expression, lNum, uNum, useRadians, type)
                        if (!numRes.isNaN()) return@evaluate SymjaUtils.formatResult(numRes.toString())
                    }
                    return@evaluate categorizeIntegralResult(res, false, "∫($expression)d$v")
                }

                SymjaUtils.formatResult(res)
            }
        } catch (_: Exception) {
            "Error"
        }
    }

    /**
     * Performs geometric application integration (ArcLength, Volume, SurfaceArea).
     */
    fun integrateApplication(
        expression: String,
        lower: String,
        upper: String,
        useRadians: Boolean = true,
        useRationalize: Boolean = false,
        useSimplify: Boolean = true,
        type: IntegralType = IntegralType.ARC
    ): String {
        return try {
            SymjaUtils.evaluate { eval ->
                val formula = constructFormula(expression, useRadians, type)
                val lStr = SymjaUtils.prepareForSymja(lower, useRadians)
                val uStr = SymjaUtils.prepareForSymja(upper, useRadians)

                val core = if (useRationalize) {
                    "Integrate[Rationalize[$formula], {x, Rationalize[$lStr], Rationalize[$uStr]}]"
                } else {
                    "Integrate[$formula, {x, $lStr, $uStr}]"
                }
                val command = if (useSimplify) "Simplify[$core]" else core

                val res = eval.eval(command).toString()

                if (res.contains("Integrate", ignoreCase = true) || res == "\$Failed") {
                    val lNum = lower.toDoubleOrNull() ?: Double.NaN
                    val uNum = upper.toDoubleOrNull() ?: Double.NaN
                    if (!lNum.isNaN() && !uNum.isNaN()) {
                        val numRes = symjaNumericalIntegrate(expression, lNum, uNum, useRadians, type)
                        if (!numRes.isNaN()) return@evaluate SymjaUtils.formatResult(numRes.toString())
                    }
                    return@evaluate categorizeIntegralResult(res, false, "∫($expression)dx")
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
                res.toDoubleOrNull() ?: Double.NaN
            }
        } catch (_: Throwable) {
            Double.NaN
        }
    }

    private fun constructFormula(expression: String, useRadians: Boolean, type: IntegralType): String {
        return when (type) {
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
                val f = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val xt = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val yt = SymjaUtils.prepareForSymja(parts[2], useRadians)
                "ReplaceAll[$f, {x -> ($xt), y -> ($yt)}] * Sqrt((D($xt, t))^2 + (D($yt, t))^2)"
            }
            2 -> {
                val f = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val yx = SymjaUtils.prepareForSymja(parts[1], useRadians)
                "ReplaceAll[$f, {y -> ($yx)}] * Sqrt(1 + (D($yx, x))^2)"
            }
            else -> {
                val f = SymjaUtils.prepareForSymja(expression, useRadians)
                if (f.contains("y")) {
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
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val xt = SymjaUtils.prepareForSymja(parts[2], useRadians)
                val yt = SymjaUtils.prepareForSymja(parts[3], useRadians)
                "ReplaceAll[$p, {x -> ($xt), y -> ($yt)}] * D($xt, t) + ReplaceAll[$q, {x -> ($xt), y -> ($yt)}] * D($yt, t)"
            }
            3 -> {
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                val yx = SymjaUtils.prepareForSymja(parts[2], useRadians)
                "ReplaceAll[$p, {y -> ($yx)}] + ReplaceAll[$q, {y -> ($yx)}] * D($yx, x)"
            }
            2 -> {
                val p = SymjaUtils.prepareForSymja(parts[0], useRadians)
                val q = SymjaUtils.prepareForSymja(parts[1], useRadians)
                "ReplaceAll[$p, {y -> x}] + ReplaceAll[$q, {y -> x}] * D(x, x)"
            }
            else -> {
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
