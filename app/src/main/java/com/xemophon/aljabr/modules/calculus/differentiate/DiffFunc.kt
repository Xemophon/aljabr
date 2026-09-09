package com.xemophon.aljabr.modules.calculus.differentiate

import com.xemophon.aljabr.modules.calculus.HybridEngine
import com.xemophon.aljabr.ui.components.screens.CalculusStep
import com.xemophon.aljabr.data.SymjaUtils

object DiffFunc {
    private val hybridEngine = HybridEngine()

    /**
     * Warms up the CAS engine.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluate { eval ->
                eval.eval("D[x, x]")
            }
        } catch (_: Throwable) {
        }
    }

    private fun selectDifferentiationVariable(vars: List<String>): String {
        if (vars.isEmpty()) return "x"
        if (vars.size == 1) return vars[0]
        for (preferred in listOf("x", "t", "z", "y", "u", "v", "w", "s")) {
            if (vars.contains(preferred)) return preferred
        }
        return vars.last()
    }

    fun differentiate(expression: String, useRationalize: Boolean = false): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return ""

            SymjaUtils.evaluate { eval ->
                val varsExpr = eval.eval("Variables[$cleaned]")
                val rawVars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val vars = rawVars.filter { v ->
                    v.all { it.isLetter() || it.isDigit() } && !v.contains("(") && !v.contains("[")
                }
                val v = selectDifferentiationVariable(vars)

                val command = if (useRationalize) {
                    "Simplify[Rationalize[D[Rationalize[$cleaned], $v]]]"
                } else {
                    "Simplify[D[$cleaned, $v]]"
                }

                val resStr = eval.eval(command).toString()

                if (resStr.contains("D", ignoreCase = true)) {
                    return@evaluate "d/d$v($expression)"
                }

                SymjaUtils.formatResult(resStr)
            }
        } catch (_: Throwable) {
            "Error"
        }
    }

    fun differentiateWithSteps(expression: String, useHybrid: Boolean = true): Pair<String, List<CalculusStep>> {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return Pair("", emptyList())

            val vStr = SymjaUtils.evaluate { eval ->
                val varsExpr = eval.eval("Variables[$cleaned]")
                val rawVars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val vars = rawVars.filter { v ->
                    v.all { it.isLetter() || it.isDigit() } && !v.contains("(") && !v.contains("[")
                }
                selectDifferentiationVariable(vars)
            }

            val steps = hybridEngine.differentiateWithSteps(expression, vStr, useHybrid)
            val finalResult = differentiate(expression)
            
            Pair(finalResult, steps)
        } catch (_: Throwable) {
            Pair(differentiate(expression), emptyList())
        }
    }

    fun differentiateComplex(expression: String): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return ""

            SymjaUtils.evaluate { eval ->
                val substituted = cleaned
                    .replace("zc", "(x - y * I)", ignoreCase = true)
                    .replace("z̄", "(x - y * I)")
                    .replace(Regex("""(?<![a-zA-Z])z(?![a-zA-Z])"""), "(x + y * I)")

                val command = "Simplify[1/2 * (D[$substituted, x] - I * D[$substituted, y])]"
                val resStr = eval.eval(command).toString()

                SymjaUtils.formatResult(resStr)
            }
        } catch (_: Throwable) {
            "Error"
        }
    }
}