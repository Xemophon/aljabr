package com.xemophon.aljabr.modules.algebra.bde

import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class BDEResult(
    val equation: String,
    val solution: List<String>,
    val rawSolution: List<String> = emptyList(),
    val error: String? = null
)

object BDEFuncs {

    private fun prepareOdeExpression(expression: String): String {
        var cleaned = SymjaUtils.prepareForSymja(expression).replace(" ", "")

        val odeRegex = Regex("""\by('*)""")
        cleaned = odeRegex.replace(cleaned) { matchResult ->
            val primes = matchResult.groupValues[1]
            val matchEnd = matchResult.range.last + 1
            val followedByParen = matchEnd < cleaned.length && cleaned[matchEnd] == '('

            if (primes.isNotEmpty()) {
                val count = primes.length
                if (followedByParen) {
                    "Derivative[$count][y]"
                } else {
                    "Derivative[$count][y][x]"
                }
            } else {
                if (!followedByParen) {
                    "Derivative[0][y][x]"
                } else {
                    "y"
                }
            }
        }

        return cleaned
    }

    suspend fun solveOde(expression: String, conditions: List<String> = emptyList()): BDEResult = withContext(Dispatchers.Default) {
        val timedOutResult = withTimeoutOrNull(5000L.milliseconds) {
            SymjaUtils.evaluate { eval ->
                try {
                    val prepared = prepareOdeExpression(expression)
                    if (prepared.isBlank()) return@evaluate BDEResult(expression, emptyList(), error = "Empty expression")

                    val eq = if (!prepared.contains("==")) {
                        prepared.replace("=", "==")
                    } else {
                        prepared
                    }

                    val cleanedConditions = conditions
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { cond ->
                            val cPrep = SymjaUtils.prepareForSymja(cond).replace(" ", "")
                            if (!cPrep.contains("==")) {
                                cPrep.replace("=", "==")
                            } else {
                                cPrep
                            }
                        }

                    val dsolveCommand = if (cleanedConditions.isEmpty()) {
                        "DSolve[$eq, y(x), x]"
                    } else {
                        val allItems = listOf(eq) + cleanedConditions
                        "DSolve[{${allItems.joinToString(", ")}}, y(x), x]"
                    }

                    val res = eval.eval(dsolveCommand).toString()
                    val rawSolutions = SymjaUtils.parseSolveResult(res).map { rule ->
                        rule.substringAfter("->").trim()
                    }.distinct()
                    val solutions = rawSolutions.map { SymjaUtils.formatResult(it) }

                    if (res.startsWith("DSolve") || solutions.isEmpty()) {
                        return@evaluate BDEResult(expression, emptyList(), error = "Could not solve differential equation analytically: $res")
                    }

                    BDEResult(expression, solutions, rawSolutions)
                } catch (e: Exception) {
                    BDEResult(expression, emptyList(), error = e.message ?: "ODE solution failed")
                }
            }
        }

        timedOutResult ?: BDEResult(expression, emptyList(), error = "Computation timed out (5s limit). The differential equation is too complex or cannot be solved analytically.")
    }
}
