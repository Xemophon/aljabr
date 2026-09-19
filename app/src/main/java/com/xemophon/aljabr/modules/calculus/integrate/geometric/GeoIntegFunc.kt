package com.xemophon.aljabr.modules.calculus.integrate.geometric

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import com.xemophon.aljabr.ui.components.buttons.IntegralType
import org.matheclipse.core.eval.ExprEvaluator

data class GeoIntegResult(
    val modeTitle: String,
    val antiDerivativeResult: String? = null,
    val rawAntiDerivative: String? = null,
    val symbolicResult: String,
    val rawSymbolicResult: String = "",
    val numericResult: String? = null,
    val rawNumericResult: String? = null,
    val formulaLatex: String = "",
    val error: String? = null
)

object GeoIntegFunc {

    /**
     * Warms up the CAS engine for integration on a background thread.
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
     * Calculates symbolic and numeric results for Geometric Integration modes.
     */
    fun calculateGeoInteg(
        mode: GeoIntegMode,
        fXY: String,
        pXY: String,
        qXY: String,
        fXYZ: String,
        paramX: String,
        paramY: String,
        upperT: String,
        lowerT: String,
        upperX: String,
        lowerX: String,
        upperY: String,
        lowerY: String,
        upperZ: String,
        lowerZ: String,
        orderStr: String
    ): GeoIntegResult {
        return try {
            SymjaUtils.evaluate { eval ->
                when (mode) {
                    GeoIntegMode.ARC_LINE -> calculateArcLine(eval, paramX, paramY, lowerT, upperT)
                    GeoIntegMode.SCALAR_LINE -> calculateScalarLine(eval, fXY, paramX, paramY, lowerT, upperT)
                    GeoIntegMode.VECTOR_LINE -> calculateVectorLine(eval, pXY, qXY, paramX, paramY, lowerT, upperT)
                    GeoIntegMode.SURFACE -> calculateSurface(eval, fXY, lowerX, upperX, lowerY, upperY)
                    GeoIntegMode.VOLUME -> calculateVolume(eval, fXYZ, lowerX, upperX, lowerY, upperY, lowerZ, upperZ, orderStr)
                }
            }
        } catch (e: Exception) {
            GeoIntegResult(
                modeTitle = mode.title,
                symbolicResult = "Error",
                error = e.message ?: "Calculation failed"
            )
        }
    }

    private fun calculateArcLine(
        eval: ExprEvaluator,
        paramX: String,
        paramY: String,
        lowerT: String,
        upperT: String
    ): GeoIntegResult {
        val xt = SymjaUtils.prepareForSymja(paramX)
        val yt = SymjaUtils.prepareForSymja(paramY)
        val a = SymjaUtils.prepareForSymja(lowerT)
        val b = SymjaUtils.prepareForSymja(upperT)

        val integrand = "Sqrt[(D($xt, t))^2 + (D($yt, t))^2]"
        val indefCmd = "Simplify[Integrate[$integrand, t]]"
        val symCmd = "Simplify[Integrate[$integrand, {t, $a, $b}]]"

        val formulaLatex = "\\int_{${SymjaUtils.toLaTeX(lowerT)}}^{${SymjaUtils.toLaTeX(upperT)}} \\sqrt{\\left[x'(t)\\right]^2 + \\left[y'(t)\\right]^2} \\, dt"

        val (antiDeriv, rawAnti, symRes, rawSym, numRes, rawNum) = evaluateAntiAndSymbolicAndNumeric(
            eval = eval,
            indefCmd = indefCmd,
            symCmd = symCmd,
            integrand = integrand,
            boundsList = listOf("t" to Pair(a, b)),
            lowerBoundLatex = SymjaUtils.toLaTeX(lowerT),
            upperBoundLatex = SymjaUtils.toLaTeX(upperT)
        )

        return GeoIntegResult(
            modeTitle = "Arc Length Integral",
            antiDerivativeResult = antiDeriv,
            rawAntiDerivative = rawAnti,
            symbolicResult = symRes,
            rawSymbolicResult = rawSym,
            numericResult = numRes,
            rawNumericResult = rawNum,
            formulaLatex = formulaLatex
        )
    }

    private fun calculateScalarLine(
        eval: ExprEvaluator,
        fXY: String,
        paramX: String,
        paramY: String,
        lowerT: String,
        upperT: String
    ): GeoIntegResult {
        val f = SymjaUtils.prepareForSymja(fXY)
        val xt = SymjaUtils.prepareForSymja(paramX)
        val yt = SymjaUtils.prepareForSymja(paramY)
        val a = SymjaUtils.prepareForSymja(lowerT)
        val b = SymjaUtils.prepareForSymja(upperT)

        val integrand = "ReplaceAll[$f, {x -> ($xt), y -> ($yt)}] * Sqrt[(D($xt, t))^2 + (D($yt, t))^2]"
        val indefCmd = "Simplify[Integrate[$integrand, t]]"
        val symCmd = "Simplify[Integrate[$integrand, {t, $a, $b}]]"

        val formulaLatex = "\\int_{${SymjaUtils.toLaTeX(lowerT)}}^{${SymjaUtils.toLaTeX(upperT)}} f(x(t), y(t)) \\sqrt{\\left(x'(t)\\right)^2 + \\left(y'(t)\\right)^2} \\, dt"

        val (antiDeriv, rawAnti, symRes, rawSym, numRes, rawNum) = evaluateAntiAndSymbolicAndNumeric(
            eval = eval,
            indefCmd = indefCmd,
            symCmd = symCmd,
            integrand = integrand,
            boundsList = listOf("t" to Pair(a, b)),
            lowerBoundLatex = SymjaUtils.toLaTeX(lowerT),
            upperBoundLatex = SymjaUtils.toLaTeX(upperT)
        )

        return GeoIntegResult(
            modeTitle = "Scalar Line Integral",
            antiDerivativeResult = antiDeriv,
            rawAntiDerivative = rawAnti,
            symbolicResult = symRes,
            rawSymbolicResult = rawSym,
            numericResult = numRes,
            rawNumericResult = rawNum,
            formulaLatex = formulaLatex
        )
    }

    private fun calculateVectorLine(
        eval: ExprEvaluator,
        pXY: String,
        qXY: String,
        paramX: String,
        paramY: String,
        lowerT: String,
        upperT: String
    ): GeoIntegResult {
        val p = SymjaUtils.prepareForSymja(pXY)
        val q = SymjaUtils.prepareForSymja(qXY)
        val xt = SymjaUtils.prepareForSymja(paramX)
        val yt = SymjaUtils.prepareForSymja(paramY)
        val a = SymjaUtils.prepareForSymja(lowerT)
        val b = SymjaUtils.prepareForSymja(upperT)

        val integrand = "ReplaceAll[$p, {x -> ($xt), y -> ($yt)}] * D($xt, t) + ReplaceAll[$q, {x -> ($xt), y -> ($yt)}] * D($yt, t)"
        val indefCmd = "Simplify[Integrate[$integrand, t]]"
        val symCmd = "Simplify[Integrate[$integrand, {t, $a, $b}]]"

        val formulaLatex = "\\int_{${SymjaUtils.toLaTeX(lowerT)}}^{${SymjaUtils.toLaTeX(upperT)}} \\left[ P(x(t),y(t)) x'(t) + Q(x(t),y(t)) y'(t) \\right] dt"

        val (antiDeriv, rawAnti, symRes, rawSym, numRes, rawNum) = evaluateAntiAndSymbolicAndNumeric(
            eval = eval,
            indefCmd = indefCmd,
            symCmd = symCmd,
            integrand = integrand,
            boundsList = listOf("t" to Pair(a, b)),
            lowerBoundLatex = SymjaUtils.toLaTeX(lowerT),
            upperBoundLatex = SymjaUtils.toLaTeX(upperT)
        )

        return GeoIntegResult(
            modeTitle = "Vector Line Integral",
            antiDerivativeResult = antiDeriv,
            rawAntiDerivative = rawAnti,
            symbolicResult = symRes,
            rawSymbolicResult = rawSym,
            numericResult = numRes,
            rawNumericResult = rawNum,
            formulaLatex = formulaLatex
        )
    }

    private fun calculateSurface(
        eval: ExprEvaluator,
        fXY: String,
        lowerX: String,
        upperX: String,
        lowerY: String,
        upperY: String
    ): GeoIntegResult {
        val f = SymjaUtils.prepareForSymja(fXY)
        val ax = SymjaUtils.prepareForSymja(lowerX)
        val bx = SymjaUtils.prepareForSymja(upperX)
        val cy = SymjaUtils.prepareForSymja(lowerY)
        val dy = SymjaUtils.prepareForSymja(upperY)

        val integrand = "($f) * Sqrt[1 + (D($f, x))^2 + (D($f, y))^2]"
        val indefCmd = "Simplify[Integrate[Integrate[$integrand, y], x]]"
        val symCmd = "Simplify[Integrate[Integrate[$integrand, {y, $cy, $dy}], {x, $ax, $bx}]]"

        val formulaLatex = "S = \\iint_R f(x,y) \\sqrt{1 + \\left(\\frac{\\partial f}{\\partial x}\\right)^2 + \\left(\\frac{\\partial f}{\\partial y}\\right)^2} \\, dA"

        val (antiDeriv, rawAnti, symRes, rawSym, numRes, rawNum) = evaluateAntiAndSymbolicAndNumeric(
            eval = eval,
            indefCmd = indefCmd,
            symCmd = symCmd,
            integrand = integrand,
            boundsList = listOf("y" to Pair(cy, dy), "x" to Pair(ax, bx))
        )

        return GeoIntegResult(
            modeTitle = "Surface Integral",
            antiDerivativeResult = antiDeriv,
            rawAntiDerivative = rawAnti,
            symbolicResult = symRes,
            rawSymbolicResult = rawSym,
            numericResult = numRes,
            rawNumericResult = rawNum,
            formulaLatex = formulaLatex
        )
    }

    private fun calculateVolume(
        eval: ExprEvaluator,
        fXYZ: String,
        lowerX: String,
        upperX: String,
        lowerY: String,
        upperY: String,
        lowerZ: String,
        upperZ: String,
        orderStr: String
    ): GeoIntegResult {
        val f = SymjaUtils.prepareForSymja(fXYZ)
        val ax = SymjaUtils.prepareForSymja(lowerX)
        val bx = SymjaUtils.prepareForSymja(upperX)
        val cy = SymjaUtils.prepareForSymja(lowerY)
        val dy = SymjaUtils.prepareForSymja(upperY)
        val ez = SymjaUtils.prepareForSymja(lowerZ)
        val fz = SymjaUtils.prepareForSymja(upperZ)

        val boundsMap = mapOf(
            "dx" to ("x" to Pair(ax, bx)),
            "dy" to ("y" to Pair(cy, dy)),
            "dz" to ("z" to Pair(ez, fz))
        )

        val diffTokens = orderStr.split(" ").filter { it.startsWith("d") }
        val boundsList = diffTokens.mapNotNull { boundsMap[it] }

        var symCmd = f
        var indefCmd = f
        for ((v, bounds) in boundsList) {
            symCmd = "Integrate[$symCmd, {$v, ${bounds.first}, ${bounds.second}}]"
            indefCmd = "Integrate[$indefCmd, $v]"
        }
        symCmd = "Simplify[$symCmd]"
        indefCmd = "Simplify[$indefCmd]"

        val formulaLatex = "\\iiint_R f(x,y,z) \\, dV"

        val (antiDeriv, rawAnti, symRes, rawSym, numRes, rawNum) = evaluateAntiAndSymbolicAndNumeric(
            eval = eval,
            indefCmd = indefCmd,
            symCmd = symCmd,
            integrand = f,
            boundsList = boundsList
        )

        return GeoIntegResult(
            modeTitle = "Volume Integral",
            antiDerivativeResult = antiDeriv,
            rawAntiDerivative = rawAnti,
            symbolicResult = symRes,
            rawSymbolicResult = rawSym,
            numericResult = numRes,
            rawNumericResult = rawNum,
            formulaLatex = formulaLatex
        )
    }

    private fun evaluateAntiAndSymbolicAndNumeric(
        eval: ExprEvaluator,
        indefCmd: String,
        symCmd: String,
        integrand: String,
        boundsList: List<Pair<String, Pair<String, String>>>,
        lowerBoundLatex: String? = null,
        upperBoundLatex: String? = null
    ): Tuple6Result {
        // 1. Anti-derivative evaluation (indefinite integral before bounds substitution)
        val rawAnti: String? = try {
            val res = eval.eval(indefCmd).toString()
            if (res.contains("Integrate", ignoreCase = true) || res == "\$Failed") null else res
        } catch (_: Exception) {
            null
        }

        val antiDerivLatex = if (rawAnti != null) {
            val latexPart = SymjaUtils.toLaTeX(rawAnti)
            if (lowerBoundLatex != null && upperBoundLatex != null) {
                "\\left[ $latexPart \\right]_{$lowerBoundLatex}^{$upperBoundLatex}"
            } else {
                latexPart
            }
        } else {
            null
        }

        // 2. Symbolic definite integral evaluation
        val rawSym = try {
            eval.eval(symCmd).toString()
        } catch (_: Exception) {
            "\$Failed"
        }

        val symRes = if (rawSym.contains("Integrate", ignoreCase = true) || rawSym == "\$Failed") {
            "No closed form solution"
        } else {
            SymjaUtils.formatResult(rawSym)
        }

        // 3. Numeric evaluation (forced decimal float)
        var rawNum: String? = null
        var numRes: String? = null

        if (rawSym != "No closed form solution" && rawSym != "\$Failed") {
            try {
                val calculatedNum = SymjaUtils.calculateNumerical(rawSym, useRadians = true, precision = 6)
                if (calculatedNum.isNotEmpty() && calculatedNum != symRes) {
                    rawNum = calculatedNum
                    numRes = calculatedNum
                }
            } catch (_: Exception) {
            }
        }

        if (numRes == null && (rawSym == "No closed form solution" || rawSym == "\$Failed")) {
            try {
                var nCmd = integrand
                for ((v, bounds) in boundsList) {
                    nCmd = "NIntegrate[$nCmd, {$v, ${bounds.first}, ${bounds.second}}]"
                }
                val nResStr = eval.eval(nCmd).toString()
                val nDouble = nResStr.toDoubleOrNull()
                if (nDouble != null && !nDouble.isNaN()) {
                    val formattedN = CalcFuncs.formatResult(nDouble, 6)
                    rawNum = formattedN
                    numRes = formattedN
                }
            } catch (_: Exception) {
            }
        }

        val finalNumericResult = if (numRes != null && numRes != symRes) numRes else null
        val finalRawNumericResult = if (numRes != null && numRes != symRes) rawNum else null

        return Tuple6Result(
            antiDeriv = antiDerivLatex,
            rawAnti = rawAnti,
            symRes = symRes,
            rawSym = if (symRes == "No closed form solution") "" else rawSym,
            numRes = finalNumericResult,
            rawNum = finalRawNumericResult
        )
    }

    private data class Tuple6Result(
        val antiDeriv: String?,
        val rawAnti: String?,
        val symRes: String,
        val rawSym: String,
        val numRes: String?,
        val rawNum: String?
    )

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
                val xt = SymjaUtils.prepareForSymja(expression)
                val lStr = SymjaUtils.prepareForSymja(lower)
                val uStr = SymjaUtils.prepareForSymja(upper)
                val res = eval.eval("Simplify[Integrate[$xt, {t, $lStr, $uStr}]]").toString()
                SymjaUtils.formatResult(res)
            }
        } catch (_: Exception) {
            "Error"
        }
    }

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
                val xt = SymjaUtils.prepareForSymja(expression)
                val lStr = SymjaUtils.prepareForSymja(lower)
                val uStr = SymjaUtils.prepareForSymja(upper)
                val res = eval.eval("Simplify[Integrate[$xt, {x, $lStr, $uStr}]]").toString()
                SymjaUtils.formatResult(res)
            }
        } catch (_: Exception) {
            "Error"
        }
    }
}
