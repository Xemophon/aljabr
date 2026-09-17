package com.xemophon.aljabr.modules.calculus.laplace

import com.xemophon.aljabr.data.SymjaUtils

object LaplaceFunc {

    private val STANDALONE_S_REGEX = Regex("""\bs\b""", RegexOption.IGNORE_CASE)
    private val STANDALONE_T_REGEX = Regex("""\bt\b""", RegexOption.IGNORE_CASE)
    private val STANDALONE_X_REGEX = Regex("""\bx\b""", RegexOption.IGNORE_CASE)

    fun calculateLaplace(
        expression: String,
        isInverse: Boolean = false,
        useRationalize: Boolean = false
    ): String {
        return try {
            var rawExpr = expression.trim()
            if (rawExpr.isBlank()) return ""

            if (isInverse) {
                // For Inverse Laplace: target variable in F(s) is s.
                // Check for standalone 's' using word boundary regex BEFORE prepareForSymja
                val hasS = STANDALONE_S_REGEX.containsMatchIn(rawExpr)
                if (!hasS) {
                    rawExpr = rawExpr
                        .replace(STANDALONE_T_REGEX, "s")
                        .replace(STANDALONE_X_REGEX, "s")
                }
            } else {
                // For Direct Laplace: target variable in f(t) is t.
                // Check for standalone 't' using word boundary regex BEFORE prepareForSymja
                val hasT = STANDALONE_T_REGEX.containsMatchIn(rawExpr)
                if (!hasT) {
                    rawExpr = rawExpr
                        .replace(STANDALONE_S_REGEX, "t")
                        .replace(STANDALONE_X_REGEX, "t")
                }
            }

            val cleanedExpr = SymjaUtils.prepareForSymja(rawExpr)

            // Wrap in Rationalize so floating point numbers (e.g. 0.5) are converted to exact fractions (1/2) for Symja's symbolic engine
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
                result.contains("ComplexInfinity") ||
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
