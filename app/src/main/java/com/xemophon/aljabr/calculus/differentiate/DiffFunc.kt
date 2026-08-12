package com.xemophon.aljabr.calculus.differentiate

import com.xemophon.aljabr.data.SymjaUtils

object DiffFunc {
    /**
     * Warms up the CAS engine.
     */
    fun warmUp() {
        try {
            SymjaUtils.evaluator.eval("D[x, x]")
        } catch (_: Throwable) {
        }
    }

    fun differentiate(expression: String): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return ""

            val eval = SymjaUtils.evaluator
            val varsExpr = eval.eval("Variables[$cleaned]")
            val rawVars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val vars = rawVars.filter { v ->
                v.all { it.isLetter() || it.isDigit() } && !v.contains("(") && !v.contains("[")
            }
            val v = if (vars.size == 1) vars[0] else "x"

            val resStr = eval.eval("Simplify[D[$cleaned, $v]]").toString()

            if (resStr.contains("D", ignoreCase = true)) {
                return "d/d$v($expression)"
            }

            SymjaUtils.formatResult(resStr)
        } catch (_: Throwable) {
            "Error"
        }
    }

    fun differentiateWithSteps(expression: String): Pair<String, List<String>> {
        return Pair(differentiate(expression), emptyList())
    }
}
