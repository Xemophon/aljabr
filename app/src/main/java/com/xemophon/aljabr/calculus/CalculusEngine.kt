package com.xemophon.aljabr.calculus

import com.xemophon.aljabr.calculus.integrate.IntegrationSolver
import com.xemophon.aljabr.calculus.differentiate.DerivativeSolver
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.interfaces.ISymbol

class CalculusEngine {

    /**
     * Entry point for integration steps.
     * Uses IntegrationSolver for algorithmic and pattern-based steps.
     * @param useHybridSolver If true, use the hybrid solver; otherwise fall back.
     */
    fun integrateWithSteps(expression: String, useHybridSolver: Boolean = true): Pair<org.matheclipse.core.interfaces.IExpr, List<CalculusStep>>? {
        val steps = mutableListOf<CalculusStep>()
        if (!useHybridSolver) return null

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val engine = SymjaUtils.evaluator.evalEngine
            val expr = engine.parse(cleaned)

            // Simple variable detection: default to x, or first variable found
            val xSymbol = engine.parse("x") as ISymbol
            val v = if (expr.isFree(xSymbol)) {
                val vars = SymjaUtils.evaluator.eval("Variables[$cleaned]")
                if (vars.isAST && (vars.size() > 1)) engine.parse(vars[1].toString()) as ISymbol else xSymbol
            } else {
                xSymbol
            }

            val solver = IntegrationSolver(engine)
            val res = solver.solveIntegral(expr, v, steps)

            return res?.let { Pair(it, steps) }
        } catch (_: Exception) {
        }
        return null
    }

    /**
     * Entry point for differentiation steps.
     * @param useHybridSolver If true, use the rule-based solver; otherwise fall back.
     */
    fun differentiateWithSteps(expression: String, variable: String = "x", useHybridSolver: Boolean = true): List<CalculusStep> {
        val steps = mutableListOf<CalculusStep>()
        if (!useHybridSolver) return steps

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val engine = SymjaUtils.evaluator.evalEngine
            val expr = engine.parse(cleaned)
            val v = SymjaUtils.evaluator.eval(variable) as ISymbol

            val solver = DerivativeSolver(engine)
            solver.solveDerivative(expr, v, steps)
        } catch (_: Exception) {
            // Silently fail
        }
        return steps
    }
}