package com.xemophon.aljabr.calculus

import com.xemophon.aljabr.calculus.integrate.EigenmathTableSolver
import com.xemophon.aljabr.calculus.differentiate.EigenmathDerivativeSolver
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.interfaces.ISymbol

class CalculusEngine {

    /**
     * Entry point for integration steps.
     * Uses EigenmathTableSolver for pattern-based steps.
     * @param useEugene If true, use the table-driven solver; otherwise fall back.
     */
    fun integrateWithSteps(expression: String, useEugene: Boolean = true): List<CalculusStep> {
        val steps = mutableListOf<CalculusStep>()
        if (!useEugene) return steps // Fallback to Symja (currently empty steps)

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val engine = SymjaUtils.evaluator.evalEngine
            val expr = engine.parse(cleaned)
            val x = SymjaUtils.evaluator.eval("x") as ISymbol

            val solver = EigenmathTableSolver(engine)
            solver.solveTableIntegral(expr, x, steps)
        } catch (_: Exception) {
            // Silently fail and return whatever steps were collected or empty
        }
        return steps
    }

    /**
     * Entry point for differentiation steps.
     * @param useEugene If true, use the rule-based solver; otherwise fall back.
     */
    fun differentiateWithSteps(expression: String, variable: String = "x", useEugene: Boolean = true): List<CalculusStep> {
        val steps = mutableListOf<CalculusStep>()
        if (!useEugene) return steps

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val engine = SymjaUtils.evaluator.evalEngine
            val expr = engine.parse(cleaned)
            val v = SymjaUtils.evaluator.eval(variable) as ISymbol

            val solver = EigenmathDerivativeSolver(engine)
            solver.solveDerivative(expr, v, steps)
        } catch (_: Exception) {
            // Silently fail
        }
        return steps
    }
}