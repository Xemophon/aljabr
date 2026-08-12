package com.xemophon.aljabr.calculus.limits

import com.xemophon.aljabr.data.SymjaUtils

object LimitsFunc {

    fun calculateLimit(
        expression: String,
        variable: String = "x",
        target: String
    ): String {
        return try {
            val cleanedExpr = SymjaUtils.prepareForSymja(expression)
            val cleanedTarget = when (target) {
                "∞" -> "Infinity"
                "-∞" -> "-Infinity"
                else -> SymjaUtils.prepareForSymja(target)
            }

            val symjaCommand = "Simplify[Limit($cleanedExpr, $variable -> $cleanedTarget)]"
            val result = SymjaUtils.evaluator.eval(symjaCommand).toString()

            if (result.contains("Limit") || result.contains("Indeterminate")) {
                "DNE"
            } else {
                SymjaUtils.formatResult(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    fun calculateLimitWithSteps(
        expression: String,
        variable: String = "x",
        target: String
    ): Pair<String, List<String>> {
        return Pair(calculateLimit(expression, variable, target), emptyList())
    }
}
