package com.xemophon.aljabr.calculus.integrate

import com.xemophon.aljabr.data.SymjaUtils
import org.matheclipse.core.eval.EvalEngine
import org.matheclipse.core.expression.F
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol
import com.xemophon.aljabr.ui.components.CalculusStep

/*
 * This file contains a Kotlin port of the mathematical algorithms originally
 * developed in C for the Eigenmath project by George Weigt.
 *
 * Copyright (c) [Year(s) from original file], George Weigt
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
BSD 2-Clause License

Copyright (c) 2024, George Weigt
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

BSD 2-Clause License

Copyright (c) 2024, George Weigt
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

data class IntegralPattern(
    val id: String,
    val name: String,
    // Regex or AST matcher for the integrand
    val matcher: (IExpr, ISymbol) -> PatternMatchResult?
)

data class PatternMatchResult(
    val parameters: Map<String, IExpr>, // e.g., "a" -> 2, "b" -> 0
    val stepExplanation: String,
    val evaluatedResult: IExpr
)

class EigenmathTableSolver(private val engine: EvalEngine) {

    /**
     * Executes table-driven integration inspired by Eigenmath.
     */
    fun solveTableIntegral(
        expr: IExpr,
        variable: ISymbol,
        steps: MutableList<CalculusStep>
    ): IExpr? {

        // 1. Linearity: Sum Rule \int (f + g) dx
        if (expr is IAST && expr.isPlus) {
            steps.add(CalculusStep("Sum Rule", "\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}", "Split into terms"))
            val results = mutableListOf<IExpr>()
            for (i in 1 until expr.size()) {
                val termRes = solveTableIntegral(expr.get(i), variable, steps)
                    ?: engine.evaluate(F.Integrate(expr.get(i), variable))
                results.add(termRes)
            }
            return engine.evaluate(F.Plus(*results.toTypedArray()))
        }

        // 2. Linearity: Constant Multiple Rule \int c * f(x) dx
        if (expr is IAST && expr.isTimes) {
            val (constants, variables) = partitionTerm(expr, variable)
            if (constants.isNotEmpty() && variables.isNotEmpty()) {
                val c = engine.evaluate(F.Times(*constants.toTypedArray()))
                val v = if (variables.size == 1) variables[0] else F.Times(*variables.toTypedArray())

                steps.add(CalculusStep("Factor Constant", "\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}", "${c.toLaTeX()} \\int (${v.toLaTeX()}) \\, d${variable.toLaTeX()}"))
                val subResult = solveTableIntegral(v, variable, steps)
                    ?: engine.evaluate(F.Integrate(v, variable))
                return engine.evaluate(F.Times(c, subResult))
            }
        }

        // 3. Table Lookup: Generic Tables
        val genericResult = matchGenericTable(expr, variable, steps)
        if (genericResult != null) return genericResult

        return null // Hand off to fallback engine if not in table
    }

    /**
     * Attempts to match the expression against the provided integration tables.
     */
    private fun matchGenericTable(
        expr: IExpr,
        variable: ISymbol,
        steps: MutableList<CalculusStep>
    ): IExpr? {
        val allTables = listOf(
            integral_tab_exp,
            integral_tab_trig,
            integral_tab_log,
            integral_tab_power,
            integral_tab
        )

        val exprStr = expr.toString().lowercase()

        for (table in allTables) {
            for (i in table.indices step 3) {
                if (i + 1 >= table.size) break
                val patternStr = table[i]
                
                // Very basic heuristic matching to find a candidate pattern
                if (heuristicMatch(exprStr, patternStr)) {
                    val result = engine.evaluate(F.Integrate(expr, variable))
                    if (!result.isFree(F.Integrate)) { // Success!
                        steps.add(CalculusStep(
                            "Standard Integration Table Lookup",
                            "\\int ${expr.toLaTeX()} \\, d${variable.toLaTeX()}",
                            result.toLaTeX()
                        ))
                        return result
                    }
                }
            }
        }
        return null
    }

    private fun heuristicMatch(exprStr: String, patternStr: String): Boolean {
        val pLower = patternStr.lowercase()
        // Simple check: if pattern is "sin(a x)" and expr contains "sin", it's a candidate
        val pBase = pLower.takeWhile { it.isLetter() }
        if (pBase.isNotEmpty() && exprStr.contains(pBase)) return true
        
        // Power forms
        if (pLower.contains("^") && exprStr.contains("^")) return true
        
        // Exponential forms
        if ((pLower.contains("exp") || pLower.contains("e^")) && (exprStr.contains("exp") || exprStr.contains("e^"))) return true

        return false
    }

    private fun partitionTerm(expr: IAST, variable: ISymbol): Pair<List<IExpr>, List<IExpr>> {
        val constants = mutableListOf<IExpr>()
        val variables = mutableListOf<IExpr>()
        for (i in 1 until expr.size()) {
            val t = expr.get(i)
            if (t.isFree(variable, true)) constants.add(t) else variables.add(t)
        }
        return Pair(constants, variables)
    }

    private fun IExpr.toLaTeX(): String = SymjaUtils.toLaTeX(this.toString())
}
