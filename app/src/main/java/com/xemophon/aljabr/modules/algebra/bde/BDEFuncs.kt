package com.xemophon.aljabr.modules.algebra.bde

import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.matheclipse.core.eval.ExprEvaluator
import kotlin.time.Duration.Companion.milliseconds

data class BDEResult(
    val equation: String,
    val solution: List<String>,
    val rawSolution: List<String> = emptyList(),
    val error: String? = null
)

object BDEFuncs {

    private fun prepareOdeExpression(expression: String): String {
        var cleaned = SymjaUtils.prepareForSymja(expression).trim()

        // Handle full DSolve(...) command if entered directly
        if (cleaned.startsWith("DSolve", ignoreCase = true)) {
            val inner = cleaned.substringAfter("(").substringBeforeLast(")")
            val parts = inner.split(",")
            if (parts.isNotEmpty()) {
                cleaned = parts[0].trim().removePrefix("{").removeSuffix("}")
            }
        }

        cleaned = cleaned.replace(" ", "")

        if (!cleaned.contains("==")) {
            cleaned = cleaned.replace("=", "==")
        }

        // Standardize brackets [x] to (x) for y
        cleaned = cleaned.replace(Regex("""\by('*)\[x]"""), "y$1(x)")

        // Standardize y', y'', etc. not followed by (x)
        cleaned = cleaned.replace(Regex("""\by('+)""")) { match ->
            val primes = match.groupValues[1]
            val matchEnd = match.range.last + 1
            if (matchEnd < cleaned.length && (cleaned[matchEnd] == '(' || cleaned[matchEnd] == '[')) {
                "y$primes"
            } else {
                "y$primes(x)"
            }
        }

        // Standardize standalone y not followed by ', (, or [
        cleaned = cleaned.replace(Regex("""\by(?![A-Za-z0-9_'(\[])""")) {
            "y(x)"
        }

        return cleaned
    }

    private fun tryBernoulliSubstitution(
        eval: ExprEvaluator,
        eq: String,
        conditions: List<String>
    ): Pair<List<String>, List<String>>? {
        // Try substitution u(x) = y(x)^2 -> u'(x) = 2*y(x)*y'(x)
        val eqU = eq
            .replace("y'(x)*y(x)", "(u'(x)/2)")
            .replace("y(x)*y'(x)", "(u'(x)/2)")
            .replace("y'*y", "(u'(x)/2)")
            .replace("y*y'", "(u'(x)/2)")
            .replace("y(x)^2", "u(x)")
            .replace("y^2", "u(x)")

        if (!eqU.contains("u(x)")) return null

        val condsU = conditions.map { cond ->
            cond.replace(Regex("""y\((\w+)\)\s*==\s*(-?\d+\.?\d*)""")) { match ->
                val xVal = match.groupValues[1]
                val yVal = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val uVal = yVal * yVal
                "u($xVal) == $uVal"
            }
        }

        val dsolveCmd = if (condsU.isEmpty()) {
            "DSolve[$eqU, u(x), x]"
        } else {
            val all = listOf(eqU) + condsU
            "DSolve[{${all.joinToString(", ")}}, u(x), x]"
        }

        val resU = eval.eval(dsolveCmd).toString()
        if (resU.startsWith("DSolve")) return null

        val rawUSolutions = SymjaUtils.parseSolveResult(resU).map { rule ->
            rule.substringAfter("->").trim()
        }.distinct()

        if (rawUSolutions.isEmpty()) return null

        val rawYSolutions = mutableListOf<String>()
        val formattedYSolutions = mutableListOf<String>()

        for (uSol in rawUSolutions) {
            val solPos = "Sqrt[$uSol]"
            val solNeg = "-Sqrt[$uSol]"
            rawYSolutions.add(solPos)
            rawYSolutions.add(solNeg)
            formattedYSolutions.add(SymjaUtils.formatResult(solPos))
            formattedYSolutions.add(SymjaUtils.formatResult(solNeg))
        }

        return Pair(formattedYSolutions, rawYSolutions)
    }

    suspend fun solveOde(expression: String, conditions: List<String> = emptyList()): BDEResult = withContext(Dispatchers.Default) {
        val timedOutResult = withTimeoutOrNull(5000L.milliseconds) {
            SymjaUtils.evaluate { eval ->
                try {
                    val prepared = prepareOdeExpression(expression)
                    if (prepared.isBlank()) return@evaluate BDEResult(expression, emptyList(), error = "Empty expression")

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
                        "DSolve[$prepared, y(x), x]"
                    } else {
                        val allItems = listOf(prepared) + cleanedConditions
                        "DSolve[{${allItems.joinToString(", ")}}, y(x), x]"
                    }

                    val res = eval.eval(dsolveCommand).toString()
                    var rawSolutions = SymjaUtils.parseSolveResult(res).map { rule ->
                        rule.substringAfter("->").trim()
                    }.distinct()
                    var solutions = rawSolutions.map { SymjaUtils.formatResult(it) }

                    if (res.startsWith("DSolve") || solutions.isEmpty()) {
                        // Attempt Bernoulli substitution fallback
                        val bernoulliRes = tryBernoulliSubstitution(eval, prepared, cleanedConditions)
                        if (bernoulliRes != null) {
                            solutions = bernoulliRes.first
                            rawSolutions = bernoulliRes.second
                        } else {
                            return@evaluate BDEResult(expression, emptyList(), error = "Could not solve differential equation analytically: $res")
                        }
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
