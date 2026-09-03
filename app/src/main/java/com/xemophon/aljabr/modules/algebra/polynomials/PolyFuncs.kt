package com.xemophon.aljabr.modules.algebra.polynomials

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.screens.PolynomialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class OdeResult(
    val equation: String,
    val solution: List<String>,
    val error: String? = null
)

object PolyFuncs {

    /**
     * Finds roots and analytical properties of a polynomial.
     */
    suspend fun analyzePolynomial(expression: String, useRationalize: Boolean = false): PolynomialResult = withContext(Dispatchers.Default) {
        synchronized(SymjaUtils.evaluator) {
            try {
                val cleaned = if (useRationalize) {
                    SymjaUtils.evaluator.eval("Rationalize(${SymjaUtils.prepareForSymja(expression)})").toString()
                } else {
                    SymjaUtils.prepareForSymja(expression)
                }
                
                if (cleaned.isBlank()) return@synchronized PolynomialResult(expression, "", emptyList(), error = "Empty expression")

                // 1. Identify variable
                val varsResult = SymjaUtils.evaluator.eval("Variables($cleaned)").toString()
                val vars = varsResult.removeSurrounding("{", "}").split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (vars.isEmpty()) return@synchronized PolynomialResult(expression, "", emptyList(), error = "No variables found")
                val variable = vars[0]

                // 2. Find Roots (Numerical and Analytical)
                // Using Solve if rationalizing, otherwise NSolve
                val solveCommand = if (useRationalize) {
                    "Solve($cleaned == 0, $variable)"
                } else {
                    "NSolve($cleaned == 0, $variable)"
                }
                
                val solveRes = SymjaUtils.evaluator.eval(solveCommand).toString()
                val roots = SymjaUtils.parseSolveResult(solveRes).map { rule ->
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

    private fun prepareOdeExpression(expression: String): String {
        var cleaned = SymjaUtils.prepareForSymja(expression).replace(" ", "")

        // Uniformly transform y with zero or more primes into Derivative[count][y][x]
        val odeRegex = Regex("""\by('*)""")
        cleaned = odeRegex.replace(cleaned) { matchResult ->
            val primes = matchResult.groupValues[1]
            val count = primes.length
            "Derivative[$count][y][x]"
        }

        return cleaned
    }

    suspend fun solveOde(expression: String): OdeResult = withContext(Dispatchers.Default) {
        val timedOutResult = withTimeoutOrNull(5000L.milliseconds) {
            synchronized(SymjaUtils.evaluator) {
                try {
                    val prepared = prepareOdeExpression(expression)
                    if (prepared.isBlank()) return@synchronized OdeResult(expression, emptyList(), error = "Empty expression")

                    val eq = if (!prepared.contains("==")) {
                        prepared.replace("=", "==")
                    } else {
                        prepared
                    }

                    val dsolveCommand = "DSolve[$eq, y[x], x]"
                    val res = SymjaUtils.evaluator.eval(dsolveCommand).toString()
                    val solutions = SymjaUtils.parseSolveResult(res).map { rule ->
                        val solVal = rule.substringAfter("->").trim()
                        SymjaUtils.formatResult(solVal)
                    }.distinct()

                    if (res.startsWith("DSolve") || solutions.isEmpty()) {
                        return@synchronized OdeResult(expression, emptyList(), error = "Could not solve differential equation analytically: $res")
                    }

                    OdeResult(expression, solutions)
                } catch (e: Exception) {
                    OdeResult(expression, emptyList(), error = e.message ?: "ODE solution failed")
                }
            }
        }

        timedOutResult ?: OdeResult(expression, emptyList(), error = "Computation timed out (5s limit). The differential equation is too complex or cannot be solved analytically.")
    }
}
