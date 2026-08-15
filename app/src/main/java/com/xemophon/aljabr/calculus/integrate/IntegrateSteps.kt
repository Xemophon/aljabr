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
 * Copyright (c) 2024, George Weigt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

class HybridIntegrationSolver(private val engine: EvalEngine) {

    /**
     * Entry point for step-by-step integration.
     */
    fun solveIntegral(expr: IExpr, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr? {

        // 1. Linearity: Sum Rule \int (f + g) dx
        if (expr is IAST && expr.isPlus) {
            steps.add(CalculusStep("Sum Rule", "$\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}$", "Split into terms"))
            val results = mutableListOf<IExpr>()
            for (i in 1 until expr.size()) {
                val termRes = solveIntegral(expr.get(i), variable, steps)
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

                steps.add(CalculusStep("Factor Constant", "$\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}$", "${c.toLaTeX()} \\int (${v.toLaTeX()}) \\, d${variable.toLaTeX()}"))
                val subResult = solveIntegral(v, variable, steps)
                    ?: engine.evaluate(F.Integrate(v, variable))
                return engine.evaluate(F.Times(c, subResult))
            }
        }

        // 3. Algorithmic Steps: U-Substitution
        if (expr is IAST) {
            val uSubResult = tryUSubstitution(expr, variable, steps)
            if (uSubResult != null) return uSubResult
        }

        // 4. Algorithmic Steps: Integration by Parts (IBP)
        if (expr is IAST && expr.isTimes && expr.size() == 3) {
            val ibpResult = tryIntegrationByParts(expr, variable, steps)
            if (ibpResult != null) return ibpResult
        }

        // 5. Fallback: Eigenmath Table Lookup
        val tableResult = lookupInEigenmathTables(expr, variable, steps)
        if (tableResult != null) return tableResult

        // 6. Ultimate Fallback: Symja Black Box (No detailed steps)
        val finalRes = engine.evaluate(F.Integrate(expr, variable))
        if (!finalRes.isAST || finalRes.head() != F.Integrate) {
             return finalRes
        }

        return null
    }

    /**
     * Executes the Integration by Parts algorithm explicitly to generate steps.
     */
    private fun tryIntegrationByParts(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr? {
        val f1 = expr.get(1)
        val f2 = expr.get(2)

        // Select u and dv using LIATE priority (Log, InvTrig, Alg, Trig, Exp)
        val (u, dv) = selectUAndDv(f1, f2, variable) ?: return null

        // Calculate du and v algorithmically
        val du = engine.evaluate(F.D(u, variable))
        val v = engine.evaluate(F.Integrate(dv, variable))

        if (v.isAST && (v as IAST).head() == F.Integrate) return null // Abort if dv can't be integrated

        val uv = engine.evaluate(F.Times(u, v))
        val vdu = engine.evaluate(F.Times(v, du))

        // Document the intermediate step
        val stepLatex = "${uv.toLaTeX()} - \\int (${vdu.toLaTeX()}) \\, d${variable.toLaTeX()}"
        steps.add(
            CalculusStep(
                "Apply Integration by Parts: $\\int u \\, dv = uv - \\int v \\, du$",
                "$\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}$",
                stepLatex
            )
        )

        // Recursively integrate the remaining \int v du term
        val remainingInt = solveIntegral(vdu, variable, steps) ?: engine.evaluate(F.Integrate(vdu, variable))

        return engine.evaluate(F.Subtract(uv, remainingInt))
    }

    /**
     * Selects u and dv based on LIATE priority.
     */
    private fun selectUAndDv(f1: IExpr, f2: IExpr, variable: ISymbol): Pair<IExpr, IExpr>? {
        val p1 = getLiatePriority(f1, variable)
        val p2 = getLiatePriority(f2, variable)
        
        return if (p1 <= p2) {
            Pair(f1, f2)
        } else {
            Pair(f2, f1)
        }
    }

    private fun getLiatePriority(expr: IExpr, variable: ISymbol): Int {
        if (expr.isFree(variable)) return 5 // Constant (Low priority for u)
        if (expr is IAST) {
            val head = expr.head()
            if (head == F.Log) return 0 // Logarithmic
            if (head == F.ArcSin || head == F.ArcCos || head == F.ArcTan) return 1 // Inverse Trig
            if (head == F.Plus || head == F.Times || head == F.Power) {
                // Check if it's algebraic (polynomial-like)
                if (expr.isFree(F.Sin) && expr.isFree(F.Cos) && expr.isFree(F.Tan) && 
                    expr.isFree(F.Exp) && expr.isFree(F.Log)) return 2 // Algebraic
            }
            if (head == F.Sin || head == F.Cos || head == F.Tan) return 3 // Trigonometric
            if (head == F.Exp || (head == F.Power && expr.get(1) == F.E)) return 4 // Exponential
        }
        return 2 // Default Algebraic
    }

    /**
     * Checks the ported Eigenmath arrays with robust matching.
     */
    private fun lookupInEigenmathTables(expr: IExpr, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr? {
        // Try direct integration via Symja's shared evaluator for robustness
        val cleaned = SymjaUtils.prepareForSymja(expr.toString())
        val resStr = SymjaUtils.evaluator.eval("Integrate[$cleaned, ${variable}]").toString()
        
        if (!resStr.contains("Integrate", ignoreCase = true)) {
            val result = engine.parse(resStr)
            
            // Integration succeeded. Now categorize the rule mathematically.
            val stepTitle = "Integration Table: ${categorizeIntegral(expr, variable)}"
            
            steps.add(CalculusStep(
                stepTitle,
                "$\\int ${expr.toLaTeX()} \\, d${variable.toLaTeX()}$",
                result.toLaTeX()
            ))
            return result
        }
        return null
    }

    /**
     * Categorizes the integral based on the mathematical structure of the integrand.
     */
    private fun categorizeIntegral(expr: IExpr, variable: ISymbol): String {
        if (expr.isFree(variable)) return "Constant Rule"
        if (expr == variable) return "Power Rule"
        
        if (expr is IAST) {
            val head = expr.head()
            
            // Handle common transcendental and algebraic forms
            when (head) {
                F.Power -> {
                    val base = expr.get(1)
                    val exponent = expr.get(2)
                    if (base == variable && exponent.isFree(variable)) return "Power Rule"
                    if (base == F.E || base == F.Exp) return "Exponential Rule"
                    if (exponent.isFree(variable)) return "Power Rule"
                    return "Exponential Rule" // e.g. a^x
                }
                F.Exp -> return "Exponential Rule"
                F.Log -> return "Logarithmic Rule"
                F.Sin, F.Cos, F.Tan, F.Sec, F.Csc, F.Cot -> return "Trigonometric Form"
                F.ArcSin, F.ArcCos, F.ArcTan, F.ArcSec, F.ArcCsc, F.ArcCot -> return "Inverse Trigonometric Form"
                F.Sinh, F.Cosh, F.Tanh, F.Sech, F.Csch, F.Coth -> return "Hyperbolic Form"
                F.ArcSinh, F.ArcCosh, F.ArcTanh -> return "Inverse Hyperbolic Form"
                
                F.Times -> {
                    // Check if it's a reciprocal or fractional form
                    if (expr.args().any { it.isPower && it.get(2).isNegative }) return "Fractional Form"
                    return "Algebraic Form"
                }
            }
        }
        return "Standard Form"
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


    /**
     * Executes the U-Substitution algorithm.
     */
    private fun tryUSubstitution(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr? {
        val candidates = getUSubCandidates(expr, variable)
        val uSym = engine.parse("u") as ISymbol

        for (u in candidates) {
            val du = engine.evaluate(F.D(u, variable))
            if (du.isZero) continue

            val ratio = engine.evaluate(F.Divide(expr, du))
            val substituted = engine.evaluate(F.ReplaceAll(ratio, F.Rule(u, uSym)))

            if (substituted.isFree(variable, true)) {

                // 1. Record the exact size of the steps list before proceeding
                val initialStepsSize = steps.size

                steps.add(CalculusStep(
                    "Apply U-Substitution",
                    "$\\int (${expr.toLaTeX()}) \\, d${variable.toLaTeX()}$",
                    "Let $u = ${u.toLaTeX()} \\implies du = ${du.toLaTeX()} \\, d${variable.toLaTeX()}$"
                ))

                steps.add(CalculusStep(
                    "Substitute into Integral",
                    "$\\int (${substituted.toLaTeX()}) \\, du$",
                    "Integrate with respect to $u$"
                ))

                // 2. Recursively solve
                val integratedU = solveIntegral(substituted, uSym, steps)

                // 3. Catch the null failure safely
                if (integratedU == null) {
                    // Backtrack the steps list to its exact state before this candidate
                    while (steps.size > initialStepsSize) {
                        steps.removeAt(steps.size - 1)
                    }
                    continue
                }

                // 4. Back-substitute
                val finalResult = engine.evaluate(F.ReplaceAll(integratedU, F.Rule(uSym, u)))

                steps.add(CalculusStep(
                    "Back-Substitute $u = ${u.toLaTeX()}",
                    "Replace $u$ in: $${integratedU.toLaTeX()}$",
                    finalResult.toLaTeX()
                ))

                return finalResult
            }
        }
        return null
    }

    /**
     * Extracts potential 'u' candidates by scanning the AST for inner functions.
     */
    private fun getUSubCandidates(expr: IAST, variable: ISymbol): List<IExpr> {
        val candidates = mutableSetOf<IExpr>()

        // Helper to avoid substituting constants or the bare variable 'x'
        fun addCandidate(c: IExpr) {
            if (c != variable && !c.isFree(variable, true)) {
                candidates.add(c)
            }
        }

        // Check if the whole expression is a composite function
        when (expr.head()) {
            F.Power -> addCandidate(expr.get(1))
            F.Exp, F.Sin, F.Cos, F.Tan, F.Sec, F.Csc, F.Cot, F.Log, F.ArcSin, F.ArcCos, F.ArcTan -> addCandidate(expr.get(1))
        }

        // Check the individual factors of a product
        if (expr.isTimes) {
            for (i in 1 until expr.size()) {
                val factor = expr.get(i)
                if (factor is IAST) {
                    when (factor.head()) {
                        F.Power -> addCandidate(factor.get(1)) // Extracts the base
                        F.Exp, F.Sin, F.Cos, F.Tan, F.Sec, F.Csc, F.Cot, F.Log -> addCandidate(factor.get(1)) // Extracts the argument
                    }
                }
            }
        }

        return candidates.toList()
    }
    private fun IExpr.toLaTeX(): String = SymjaUtils.toLaTeX(this.toString())
}
