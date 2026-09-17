package com.xemophon.aljabr.modules.calculus.laplace

import com.xemophon.aljabr.data.SymjaUtils

object LaplaceFunc {

    fun calculateLaplace(
        expression: String,
        isInverse: Boolean = false,
        useRationalize: Boolean = false
    ): String {
        return try {
            var cleanedExpr = SymjaUtils.prepareForSymja(expression)
            if (cleanedExpr.isBlank()) return ""

            if (isInverse) {
                // For Inverse Laplace: target variable in F(s) is s.
                // If expression has no 's'/'S', but has 't' or 'x', map standalone 't' and 'x' to 's'.
                val hasS = cleanedExpr.contains("s", ignoreCase = false) || cleanedExpr.contains("S", ignoreCase = false)
                if (!hasS) {
                    cleanedExpr = cleanedExpr
                        .replace(Regex("""\bt\b"""), "s")
                        .replace(Regex("""\bx\b"""), "s")
                }
            } else {
                // For Forward Laplace: target variable in f(t) is t.
                // If expression has no 't', but has 's' or 'x', map standalone 's' and 'x' to 't'.
                val hasT = cleanedExpr.contains("t", ignoreCase = false)
                if (!hasT) {
                    cleanedExpr = cleanedExpr
                        .replace(Regex("""\bs\b"""), "t")
                        .replace(Regex("""\bx\b"""), "t")
                }
            }

            // Always wrap in Rationalize so floating point numbers (e.g. 0.5) are converted to exact fractions (1/2) for Symja's symbolic engine
            val command = if (isInverse) {
                "Simplify[InverseLaplaceTransform[Rationalize[$cleanedExpr], s, t]]"
            } else {
                "Simplify[LaplaceTransform[Rationalize[$cleanedExpr], t, s]]"
            }

            val result = SymjaUtils.evaluate { eval ->
                eval.eval(command).toString()
            }

            if (result.contains("LaplaceTransform") ||
                result.contains("InverseLaplaceTransform") ||
                result.contains("Indeterminate") ||
                result.contains("Error")
            ) {
                "Error"
            } else {
                SymjaUtils.formatResult(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }
}
