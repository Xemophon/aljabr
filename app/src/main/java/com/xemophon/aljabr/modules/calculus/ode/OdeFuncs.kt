package com.xemophon.aljabr.modules.calculus.ode

import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class OdeResult(
    val equation: String,
    val solution: List<String>,
    val error: String? = null
)

object OdeFuncs {

    private fun prepareOdeExpression(expression: String): String {
        var cleaned = SymjaUtils.prepareForSymja(expression).replace(" ", "")

        // Count primes after y and transform into Derivative[count][y][x]
        val odeRegex = Regex("""\by('+)""")
        cleaned = odeRegex.replace(cleaned) { matchResult ->
            val count = matchResult.groupValues[1].length
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
