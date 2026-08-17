package com.xemophon.aljabr.algebra.polynomials

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.PolynomialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PolyFuncs {

    /**
     * Finds roots and analytical properties of a polynomial.
     */
    suspend fun analyzePolynomial(expression: String): PolynomialResult = withContext(Dispatchers.Default) {
        synchronized(SymjaUtils.evaluator) {
            try {
                val cleaned = SymjaUtils.prepareForSymja(expression)
                if (cleaned.isBlank()) return@synchronized PolynomialResult(expression, "", emptyList(), error = "Empty expression")

                // 1. Identify variable
                val varsResult = SymjaUtils.evaluator.eval("Variables($cleaned)").toString()
                val vars = varsResult.removeSurrounding("{", "}").split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (vars.isEmpty()) return@synchronized PolynomialResult(expression, "", emptyList(), error = "No variables found")
                val variable = vars[0]

                // 2. Find Roots (Numerical and Analytical)
                // Using NSolve for broad coverage of complex roots
                val nSolveRes = SymjaUtils.evaluator.eval("NSolve($cleaned == 0, $variable)").toString()
                val roots = SymjaUtils.parseSolveResult(nSolveRes).map { rule ->
                    val rootVal = rule.substringAfter("->").trim()
                    SymjaUtils.formatResult(rootVal)
                }.distinct()

                // 3. Factored Form
                val factored = try {
                    val factorRes = SymjaUtils.evaluator.eval("Factor($cleaned)").toString()
                    if (factorRes != cleaned) SymjaUtils.formatResult(factorRes) else null
                } catch (_: Exception) { null }

                PolynomialResult(
                    expression = expression,
                    variable = variable,
                    roots = roots,
                    factoredForm = factored
                )
            } catch (e: Exception) {
                PolynomialResult(expression, "", emptyList(), error = e.message)
            }
        }
    }
}
