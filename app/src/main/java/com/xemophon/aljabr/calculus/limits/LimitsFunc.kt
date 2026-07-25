package com.xemophon.aljabr.calculus.limits

import com.xemophon.aljabr.utils.SymjaUtils

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

            // Symja Limit syntax: Limit(expr, x -> target)
            val symjaCommand = "Limit($cleanedExpr, $variable -> $cleanedTarget)"
            val result = SymjaUtils.evaluator.eval(symjaCommand)
            
            val resStr = result.toString()
            if (resStr.contains("Limit") || resStr.contains("Indeterminate")) {
                "DNE"
            } else {
                SymjaUtils.formatResult(resStr)
            }
        } catch (e: Exception) {
            "Error"
        }
    }
}
