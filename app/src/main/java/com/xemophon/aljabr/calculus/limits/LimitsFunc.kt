package com.xemophon.aljabr.calculus.limits

import com.xemophon.aljabr.data.SymjaUtils

object LimitsFunc {

    fun calculateLimit(
        expression: String,
        variable: String = "x",
        target: String
    ): String {
        return calculateLimitWithSteps(expression, variable, target).first
    }

    fun calculateLimitWithSteps(
        expression: String,
        variable: String = "x",
        target: String
    ): Pair<String, List<String>> {
        return try {
            val cleanedExpr = SymjaUtils.prepareForSymja(expression)
            val cleanedTarget = when (target) {
                "∞" -> "Infinity"
                "-∞" -> "-Infinity"
                else -> SymjaUtils.prepareForSymja(target)
            }

            val symjaCommand = "Limit($cleanedExpr, $variable -> $cleanedTarget)"
            val (result, steps) = SymjaUtils.evalWithSteps(symjaCommand)

            val resStr = result
            if (resStr.contains("Limit") || resStr.contains("Indeterminate")) {
                Pair("DNE", emptyList())
            } else {
                Pair(SymjaUtils.formatResult(resStr), steps)
            }
        } catch (e: Exception) {
            Pair("Error", emptyList())
        }
    }
}
