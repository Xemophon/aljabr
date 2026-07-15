package com.xemophon.aljabr.differentiate

import com.xemophon.aljabr.utils.SymjaUtils

object DiffFunc {

    /**
     * Warms up the CAS engine.
     */
    fun warmUp() {
        Thread {
            try {
                SymjaUtils.evaluator.eval("D[x, x]")
            } catch (_: Throwable) {
            }
        }.start()
    }

    fun differentiate(expression: String): String {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return ""

            // Use D[...] for differentiation in Symja
            val result = SymjaUtils.evaluator.eval("D[$cleaned, x]")
            val resStr = result.toString()

            if (resStr.contains("D", ignoreCase = true)) {
                return "d/dx($expression)"
            }

            SymjaUtils.formatResult(resStr)
        } catch (e: Throwable) {
            "Error"
        }
    }
}
