package com.xemophon.aljabr.calculus

import com.xemophon.aljabr.calculus.integrate.HybridIntegrationSolver
import com.xemophon.aljabr.calculus.differentiate.EigenmathDerivativeSolver
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.interfaces.ISymbol

class CalculusEngine {

    /**
     * Entry point for integration steps.
     * Uses HybridIntegrationSolver for algorithmic and pattern-based steps.
     * @param useEugene If true, use the hybrid solver; otherwise fall back.
     */
    fun integrateWithSteps(expression: String, useEugene: Boolean = true): List<CalculusStep> {
        val steps = mutableListOf<CalculusStep>()
        if (!useEugene) return steps // Fallback to Symja (currently empty steps)

        try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val engine = SymjaUtils.evaluator.evalEngine
            val expr = engine.parse(cleaned)
            
            // Simple variable detection: default to x, or first variable found
            val xSymbol = engine.parse("x") as ISymbol
            val v = if (expr.isFree(xSymbol)) {
                val vars = SymjaUtils.evaluator.eval("Variables[$cleaned]")
                if (vars.isAST && vars.size() > 1) engine.parse(vars.get(1).toString()) as ISymbol else xSymbol
            } else {
                xSymbol
            }

            val solver = HybridIntegrationSolver(engine)
            val res = solver.solveIntegral(expr, v, steps)
            
            if (steps.isEmpty() && res != null) {
                steps.add(CalculusStep(
                    "General Integration",
                    "$\\int ${SymjaUtils.toLaTeX(cleaned)} \\, d${v.toString()}$",
                    SymjaUtils.toLaTeX(res.toString())
                ))
            }
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