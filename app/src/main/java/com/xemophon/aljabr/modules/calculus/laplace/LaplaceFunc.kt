package com.xemophon.aljabr.modules.calculus.laplace

import com.xemophon.aljabr.data.SymjaUtils

object LaplaceFunc {

    fun calculateLaplace(
        expression: String,
        isInverse: Boolean = false,
        useRationalize: Boolean = false
    ): String {
        return try {
            val cleanedExpr = SymjaUtils.prepareForSymja(expression)
            if (cleanedExpr.isBlank()) return ""

            val command = if (isInverse) {
                if (useRationalize) {
                    "Simplify[Rationalize(InverseLaplaceTransform[Rationalize($cleanedExpr), s, t])]"
                } else {
                    "Simplify[InverseLaplaceTransform[$cleanedExpr, s, t]]"
                }
            } else {
                if (useRationalize) {
                    "Simplify[Rationalize(LaplaceTransform[Rationalize($cleanedExpr), t, s])]"
                } else {
                    "Simplify[LaplaceTransform[$cleanedExpr, t, s]]"
                }
            }

            val result = SymjaUtils.evaluator.eval(command).toString()

            if (result.contains("LaplaceTransform") || result.contains("InverseLaplaceTransform") || result.contains("Indeterminate")) {
                "Error"
            } else {
                SymjaUtils.formatResult(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }
}
