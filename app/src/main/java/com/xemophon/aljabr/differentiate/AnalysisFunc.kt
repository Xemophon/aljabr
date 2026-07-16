package com.xemophon.aljabr.differentiate

import com.xemophon.aljabr.utils.SymjaUtils

data class AnalysisResult(
    val variables: List<String>,
    val derivatives: List<NamedExpression>,
    val stationaryPoints: List<String> = emptyList(),
    val inflectionPoints: List<String> = emptyList(),
    val error: String? = null
)

data class NamedExpression(val name: String, val expression: String)

object AnalysisFunc {

    fun fullAnalysis(expression: String): AnalysisResult {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return AnalysisResult(emptyList(), emptyList(), error = "Empty expression")

            val eval = SymjaUtils.evaluator
            
            // Get variables
            val varsExpr = eval.eval("Variables[$cleaned]")
            val vars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (vars.size > 2) {
                return AnalysisResult(vars, emptyList(), error = "Maximum 2 variables supported")
            }

            if (vars.isEmpty() || (vars.size == 1 && vars[0] != "x" && vars[0] != "y")) {
                 // Try to force 'x' if no vars found or constant
                 val firstDeriv = eval.eval("D[$cleaned, x]").toString()
                 return AnalysisResult(
                     listOf("x"),
                     listOf(NamedExpression("f'(x)", SymjaUtils.formatResult(firstDeriv)))
                 )
            }

            if (vars.size == 1) {
                val v = vars[0]
                val f1 = eval.eval("D[$cleaned, $v]").toString()
                val f2 = eval.eval("D[$cleaned, {$v, 2}]").toString()
                
                val statPoints = try {
                    eval.eval("Solve[D[$cleaned, $v] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }
                
                val inflPoints = try {
                    eval.eval("Solve[D[$cleaned, {$v, 2}] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }

                AnalysisResult(
                    variables = vars,
                    derivatives = listOf(
                        NamedExpression("f'($v)", SymjaUtils.formatResult(f1)),
                        NamedExpression("f''($v)", SymjaUtils.formatResult(f2))
                    ),
                    stationaryPoints = calculatePoints(statPoints, cleaned, vars),
                    inflectionPoints = calculatePoints(inflPoints, cleaned, vars)
                )
            } else {
                // 2 variables: x and y usually
                val x = vars.find { it == "x" } ?: vars[0]
                val y = vars.find { it == "y" && it != x } ?: vars[1]
                
                val fx = eval.eval("D[$cleaned, $x]").toString()
                val fy = eval.eval("D[$cleaned, $y]").toString()
                
                val critPoints = try {
                    eval.eval("Solve[{D[$cleaned, $x] == 0, D[$cleaned, $y] == 0}, {$x, $y}]").toString()
                } catch (e: Exception) { "Could not solve" }

                AnalysisResult(
                    variables = listOf(x, y),
                    derivatives = listOf(
                        NamedExpression("f_$x", SymjaUtils.formatResult(fx)),
                        NamedExpression("f_$y", SymjaUtils.formatResult(fy))
                    ),
                    stationaryPoints = calculatePoints(critPoints, cleaned, vars)
                )
            }
        } catch (e: Exception) {
            AnalysisResult(emptyList(), emptyList(), error = e.message ?: "Analysis failed")
        }
    }

    private fun calculatePoints(solveRes: String, originalExpr: String, variables: List<String>): List<String> {
        if (solveRes == "{}" || solveRes == "Could not solve") return emptyList()

        val eval = SymjaUtils.evaluator
        val solutions = solveRes.removeSurrounding("{", "}")
            .split("}, {")
            .map { it.removeSurrounding("{", "}").trim() }
            .filter { it.isNotEmpty() }

        return solutions.map { sol ->
            try {
                if (variables.size == 1) {
                    val xVal = sol.split("->").last().trim()
                    val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                    val yVal = SymjaUtils.formatResult(yValExpr.toString())
                    "(${SymjaUtils.formatResult(xVal)}, $yVal)"
                } else {
                    // Expecting something like "x -> 1, y -> 2"
                    val parts = sol.split(",").map { it.trim() }
                    val coords = variables.map { v ->
                        parts.find { it.startsWith(v) }?.split("->")?.last()?.trim() ?: "?"
                    }
                    "(${coords.joinToString(", ") { SymjaUtils.formatResult(it) }})"
                }
            } catch (e: Exception) {
                SymjaUtils.formatResult(sol)
            }
        }
    }
}
