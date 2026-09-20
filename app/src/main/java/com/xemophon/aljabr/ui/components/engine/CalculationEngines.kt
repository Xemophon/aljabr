package com.xemophon.aljabr.ui.components.engine

import com.xemophon.aljabr.modules.algebra.bde.BDEFuncs
import com.xemophon.aljabr.modules.algebra.bde.BDEResult
import com.xemophon.aljabr.modules.algebra.polynomials.PolyFuncs
import com.xemophon.aljabr.modules.basic.CalcFuncs
import com.xemophon.aljabr.modules.calculus.differentiate.DiffFunc
import com.xemophon.aljabr.modules.calculus.integrate.standard.IntegFunc
import com.xemophon.aljabr.modules.calculus.integrate.geometric.GeoIntegFunc
import com.xemophon.aljabr.modules.calculus.laplace.LaplaceFunc
import com.xemophon.aljabr.modules.calculus.limits.LimitsFunc
import com.xemophon.aljabr.ui.components.buttons.IntegralType
import com.xemophon.aljabr.ui.components.screens.AnalysisFunc
import com.xemophon.aljabr.ui.components.screens.AnalysisResult
import com.xemophon.aljabr.ui.components.screens.CalculusStep
import com.xemophon.aljabr.ui.components.screens.PolynomialResult
import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IntegrationEngine {

    suspend fun integrateIndefiniteWithSteps(displayText: String, showSteps: Boolean): Pair<String, List<CalculusStep>>? {
        return withContext(Dispatchers.Default) {
            IntegFunc.integrateIndefiniteWithSteps(displayText, showSteps)
        }
    }

    fun integrateIndefinite(displayText: String, useRationalize: Boolean, useSimplify: Boolean = true): String {
        return IntegFunc.integrateIndefinite(displayText, useRationalize, useSimplify)
    }

    fun integrateCurve(
        displayText: String,
        innerLowerLimitText: String,
        lowerLimitText: String,
        upperLimitText: String,
        useRadians: Boolean,
        useRationalize: Boolean,
        useSimplify: Boolean = true,
        precision: Int,
        integType: IntegralType,
    ): String {
        val pVal = if (displayText == "0") "" else displayText
        val fullExpr = if (innerLowerLimitText.isBlank()) pVal else "$pVal, $innerLowerLimitText"
        return GeoIntegFunc.integrateCurve(
            expression = fullExpr,
            lower = lowerLimitText,
            upper = upperLimitText,
            useRadians = useRadians,
            useRationalize = useRationalize,
            useSimplify = useSimplify,
            type = integType
        )
    }

    fun integrateApplication(
        displayText: String,
        lowerLimitText: String,
        upperLimitText: String,
        useRadians: Boolean,
        useRationalize: Boolean,
        useSimplify: Boolean = true,
        precision: Int,
        integType: IntegralType
    ): String {
        return GeoIntegFunc.integrateApplication(
            expression = displayText,
            lower = lowerLimitText,
            upper = upperLimitText,
            useRadians = useRadians,
            useRationalize = useRationalize,
            useSimplify = useSimplify,
            type = integType
        )
    }

    fun integrateDoubleIndefinite(
        displayText: String,
        axis: String = "X",
        useRadians: Boolean,
        useRationalize: Boolean,
        useSimplify: Boolean = true
    ): String {
        return IntegFunc.integrateDoubleIndefinite(displayText, axis, useRadians, useRationalize, useSimplify)
    }

    fun integrateDoubleDefinite(
        displayText: String,
        lowerLimitText: String,
        upperLimitText: String,
        innerLowerLimitText: String,
        innerUpperLimitText: String,
        axis: String,
        useRadians: Boolean,
        useRationalize: Boolean,
        useSimplify: Boolean = true,
        precision: Int
    ): String {
        if (useRationalize) {
            return IntegFunc.integrateDoubleDefinite(
                expression = displayText,
                lower = lowerLimitText,
                upper = upperLimitText,
                innerLower = innerLowerLimitText,
                innerUpper = innerUpperLimitText,
                axis = axis,
                useRadians = useRadians,
                useRationalize = true,
                useSimplify = useSimplify
            )
        } else {
            val result = IntegFunc.integrateDoubleNumerical(
                expression = displayText,
                lower = lowerLimitText,
                upper = upperLimitText,
                innerLower = innerLowerLimitText,
                innerUpper = innerUpperLimitText,
                axis = axis,
                useRadians = useRadians
            )
            return if (result.isNaN()) {
                IntegFunc.integrateDoubleDefinite(
                    expression = displayText,
                    lower = lowerLimitText,
                    upper = upperLimitText,
                    innerLower = innerLowerLimitText,
                    innerUpper = innerUpperLimitText,
                    axis = axis,
                    useRadians = useRadians,
                    useRationalize = false,
                    useSimplify = useSimplify
                )
            } else {
                CalcFuncs.formatResult(result, precision)
            }
        }
    }

    fun integrateStandard(
        displayText: String,
        lowerLimitText: String,
        upperLimitText: String,
        useRadians: Boolean,
        useRationalize: Boolean,
        useSimplify: Boolean = true,
        precision: Int,
        integType: IntegralType
    ): String {
        if (useRationalize) {
            return IntegFunc.integrateSymbolic(
                expression = displayText,
                lower = lowerLimitText,
                upper = upperLimitText,
                useRadians = useRadians,
                useRationalize = true,
                useSimplify = useSimplify,
                type = integType
            )
        } else {
            val lower = CalcFuncs.calculateExpression(lowerLimitText)
            val upper = CalcFuncs.calculateExpression(upperLimitText)

            if (lower.isNaN() || upper.isNaN()) {
                return "Invalid Limits"
            }

            val result = IntegFunc.integrate(
                expression = displayText,
                lower = lower,
                upper = upper,
                useRadians = useRadians,
                type = integType
            )
            return if (result.isNaN()) {
                val symRes = IntegFunc.integrateSymbolic(
                    expression = displayText,
                    lower = lowerLimitText,
                    upper = upperLimitText,
                    useRadians = useRadians,
                    useRationalize = false,
                    useSimplify = useSimplify,
                    type = integType
                )
                if (symRes.contains("∫")) "Numerical integration failed" else symRes
            } else {
                CalcFuncs.formatResult(result, precision)
            }
        }
    }
}

object DifferentiationEngine {

    suspend fun differentiateWithSteps(displayText: String, showSteps: Boolean): Pair<String, List<CalculusStep>> {
        return withContext(Dispatchers.Default) {
            DiffFunc.differentiateWithSteps(displayText, showSteps)
        }
    }

    suspend fun differentiateWithAnalysis(
        displayText: String,
        diffGridMode: String,
        useRationalize: Boolean
    ): Pair<String, AnalysisResult?> {
        return withContext(Dispatchers.Default) {
            if (diffGridMode == "Complex") {
                val an = AnalysisFunc.complexAnalysis(displayText)
                val r = DiffFunc.differentiateComplex(displayText)
                Pair(r, an)
            } else {
                val an = AnalysisFunc.fullAnalysis(displayText)
                val r = DiffFunc.differentiate(displayText, useRationalize)
                Pair(r, an)
            }
        }
    }

    suspend fun fullAnalysis(displayText: String, diffGridMode: String): AnalysisResult {
        return withContext(Dispatchers.Default) {
            if (diffGridMode == "Complex") {
                AnalysisFunc.complexAnalysis(displayText)
            } else {
                AnalysisFunc.fullAnalysis(displayText)
            }
        }
    }
}

object LimitsEngine {
    suspend fun calculateLimit(displayText: String, targetText: String, useRationalize: Boolean): String {
        return withContext(Dispatchers.Default) {
            LimitsFunc.calculateLimit(displayText, "x", targetText, useRationalize)
        }
    }
}

object TaylorEngine {
    suspend fun calculateTaylor(displayText: String, targetText: String, orderText: String): String {
        return withContext(Dispatchers.Default) {
            val ord = orderText.toIntOrNull() ?: 5
            SymjaUtils.calculateTaylor(displayText, targetText, ord)
        }
    }
}

object LaplaceEngine {
    suspend fun calculateLaplace(displayText: String, laplaceMode: String, useRationalize: Boolean): String {
        return withContext(Dispatchers.Default) {
            val isInverse = laplaceMode == "Reverse"
            LaplaceFunc.calculateLaplace(displayText, isInverse = isInverse, useRationalize = useRationalize)
        }
    }
}

object OdeEngine {
    suspend fun solveOde(displayText: String, odeConditions: List<String>): BDEResult {
        return BDEFuncs.solveOde(displayText, odeConditions)
    }
}

object PolynomialEngine {
    suspend fun analyzePolynomial(displayText: String, useRationalize: Boolean): PolynomialResult {
        return PolyFuncs.analyzePolynomial(displayText, useRationalize)
    }
}
