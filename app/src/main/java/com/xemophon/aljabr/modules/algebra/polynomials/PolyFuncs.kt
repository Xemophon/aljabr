package com.xemophon.aljabr.modules.algebra.polynomials

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.screens.PolynomialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PolyFuncs {

    /**
     * Finds roots and analytical properties of a polynomial.
     */
    suspend fun analyzePolynomial(expression: String, useRationalize: Boolean = false): PolynomialResult = withContext(Dispatchers.Default) {
        SymjaUtils.evaluate { eval ->
            try {
                val cleaned = if (useRationalize) {
                    eval.eval("Rationalize(${SymjaUtils.prepareForSymja(expression)})").toString()
                } else {
                    SymjaUtils.prepareForSymja(expression)
                }
                
                if (cleaned.isBlank()) return@evaluate PolynomialResult(expression, "", emptyList(), error = "Empty expression")

                // 1. Identify variable
                val varsResult = eval.eval("Variables($cleaned)").toString()
                val vars = varsResult.removeSurrounding("{", "}").split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (vars.isEmpty()) return@evaluate PolynomialResult(expression, "", emptyList(), error = "No variables found")
                val variable = vars[0]

                // 2. Find Roots (Numerical and Analytical)
                val solveCommand = if (useRationalize) {
                    "Solve($cleaned == 0, $variable)"
                } else {
                    "NSolve($cleaned == 0, $variable)"
                }
                
                val solveRes = eval.eval(solveCommand).toString()
                val rawRoots = SymjaUtils.parseSolveResult(solveRes).map { rule ->
                    rule.substringAfter("->").trim()
                }.distinct()
                val roots = rawRoots.map { SymjaUtils.formatResult(it) }

                // 3. Factored Form
                val rawFactored = try {
                    val factorRes = eval.eval("Factor($cleaned)").toString()
                    if (factorRes != cleaned) factorRes else null
                } catch (_: Exception) { null }
                val factored = rawFactored?.let { SymjaUtils.formatResult(it) }

                // 4. Partial fraction decomposition
                val pfd = try {
                    val isFraction = eval.eval("Denominator($cleaned)").toString() != "1"
                    val pfdRes = eval.eval("Apart($cleaned)").toString()
                    if (isFraction && pfdRes != cleaned && pfdRes != rawFactored) pfdRes else null
                } catch (_: Exception) { null }
                val pfdString = pfd?.let { SymjaUtils.formatResult(it) }?.takeIf { it != factored }
                val finalPfd = if (pfdString != null) pfd else null

                PolynomialResult(
                    expression = expression,
                    variable = variable,
                    roots = roots,
                    rawRoots = rawRoots,
                    factoredForm = factored,
                    rawFactoredForm = rawFactored,
                    pfdForm = pfdString,
                    rawPfdForm = finalPfd
                )
            } catch (e: Exception) {
                PolynomialResult(expression, "", emptyList(), error = e.message)
            }
        }
    }
}
