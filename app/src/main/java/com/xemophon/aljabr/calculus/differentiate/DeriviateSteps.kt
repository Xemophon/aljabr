package com.xemophon.aljabr.calculus.differentiate

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.eval.EvalEngine
import org.matheclipse.core.expression.F
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol

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
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

class EigenmathDerivativeSolver(private val engine: EvalEngine) {

    /**
     * Port of Eigenmath's d_scalar_scalar and routing logic
     */
    fun solveDerivative(expr: IExpr, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {

        // Base Case 1: d(x, x) = 1 (Ported from 'equal(F, X)' block)
        if (expr == variable) {
            steps.add(CalculusStep("Variable Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "1"))
            return F.C1
        }

        // Base Case 2: d(c, x) = 0 (Ported from '!iscons(F)' constant check)
        if (expr.isFree(variable, true)) {
            steps.add(CalculusStep("Constant Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "0"))
            return F.C0
        }

        if (expr is IAST) {
            return when (expr.head()) {
                F.Plus -> dSum(expr, variable, steps)
                F.Times -> dProduct(expr, variable, steps)
                F.Power -> dPower(expr, variable, steps)
                F.Sin -> dSin(expr, variable, steps)
                F.Cos -> dCos(expr, variable, steps)
                F.Tan -> dTan(expr, variable, steps)
                F.Log -> dLog(expr, variable, steps)
                F.ArcSin -> dArcSin(expr, variable, steps)
                F.ArcCos -> dArcCos(expr, variable, steps)
                F.ArcTan -> dArcTan(expr, variable, steps)
                F.Sinh -> dSinh(expr, variable, steps)
                F.Cosh -> dCosh(expr, variable, steps)
                F.Tanh -> dTanh(expr, variable, steps)
                F.ArcSinh -> dArcSinh(expr, variable, steps)
                F.ArcCosh -> dArcCosh(expr, variable, steps)
                F.ArcTanh -> dArcTanh(expr, variable, steps)
                else -> {
                    // Fallback for unsupported functions
                    engine.evaluate(F.D(expr, variable))
                }
            }
        }

        return engine.evaluate(F.D(expr, variable))
    }

    /**
     * Port of Eigenmath's 'dsum'
     */
    private fun dSum(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        steps.add(CalculusStep("Sum Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "Differentiate each term separately"))

        val terms = mutableListOf<IExpr>()
        for (i in 1 until expr.size()) {
            terms.add(solveDerivative(expr.get(i), variable, steps))
        }
        return engine.evaluate(F.Plus(*terms.toTypedArray()))
    }

    /**
     * Port of Eigenmath's 'dproduct' (Generalized for 2 terms for cleaner steps)
     */
    private fun dProduct(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        // Extract constants first to avoid cluttering steps with (0 * v + c * v')
        val (constants, variables) = partitionTerm(expr, variable)

        if (constants.isNotEmpty() && variables.isNotEmpty()) {
            val c = engine.evaluate(F.Times(*constants.toTypedArray()))
            val v = if (variables.size == 1) variables[0] else F.Times(*variables.toTypedArray())

            steps.add(CalculusStep("Constant Multiple Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "${c.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${v.toLaTeX()})"))
            val dv = solveDerivative(v, variable, steps)
            return engine.evaluate(F.Times(c, dv))
        }

        // Standard Product Rule for variables: (u*v)' = u'v + uv'
        val u = expr.get(1)
        val v = if (expr.size() == 3) expr.get(2) else F.Times(*expr.args().drop(1).toTypedArray())

        steps.add(CalculusStep("Product Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "u'v + uv'"))

        val du = solveDerivative(u, variable, steps)
        val dv = solveDerivative(v, variable, steps)

        val term1 = engine.evaluate(F.Times(du, v))
        val term2 = engine.evaluate(F.Times(u, dv))

        return engine.evaluate(F.Plus(term1, term2))
    }

    /**
     * Port of Eigenmath's 'dpower' (Logarithmic Differentiation for u^v)
     * Handles x^2, 2^x, and x^x using the exact same logic.
     */
    private fun dPower(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val u = expr.get(1) // Base
        val v = expr.get(2) // Exponent

        // If exponent is constant, use standard power rule for cleaner steps
        if (v.isFree(variable, true)) {
            val vMinusOne = engine.evaluate(F.Subtract(v, F.C1))
            val outer = engine.evaluate(F.Times(v, F.Power(u, vMinusOne)))

            steps.add(CalculusStep("Power Rule", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outer.toLaTeX()))

            // Chain rule integration
            if (u != variable) {
                steps.add(CalculusStep("Chain Rule", outer.toLaTeX(), "${outer.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${u.toLaTeX()})"))
                val du = solveDerivative(u, variable, steps)
                return engine.evaluate(F.Times(outer, du))
            }
            return outer
        }

        // Eigenmath generalized power rule: d/dx(u^v) = u^v * (v/u * du/dx + log(u) * dv/dx)
        steps.add(CalculusStep("Logarithmic Differentiation", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", "${expr.toLaTeX()} \\left(\\frac{v}{u} u' + \\ln(u) v'\\right)"))

        val du = solveDerivative(u, variable, steps)
        val dv = solveDerivative(v, variable, steps)

        val term1 = engine.evaluate(F.Times(F.Divide(v, u), du))
        val term2 = engine.evaluate(F.Times(F.Log(u), dv))
        val innerSum = engine.evaluate(F.Plus(term1, term2))

        return engine.evaluate(F.Times(expr, innerSum))
    }

    /**
     * Port of Eigenmath's 'dsin' with Chain Rule
     */
    private fun dSin(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        val outerDeriv = F.Cos(arg)

        steps.add(CalculusStep("Derivative of Sine", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    /**
     * Port of Eigenmath's 'dcos' with Chain Rule
     */
    private fun dCos(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        val outerDeriv = engine.evaluate(F.Times(F.CN1, F.Sin(arg)))

        steps.add(CalculusStep("Derivative of Cosine", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }


    private fun dTan(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)

        // Eigenmath logic: cos(arg)^-2
        val outerDeriv = engine.evaluate(F.Power(F.Cos(arg), F.CN2))

        steps.add(CalculusStep("Derivative of Tangent", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(
                CalculusStep(
                    "Chain Rule",
                    outerDeriv.toLaTeX(),
                    "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"
                )
            )
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dLog(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        val outerDeriv = engine.evaluate(F.Power(arg, F.CN1)) // 1/u

        steps.add(CalculusStep("Derivative of Natural Log", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcSin(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // 1 / sqrt(1 - arg^2)
        val outerDeriv = engine.evaluate(F.Power(F.Subtract(F.C1, F.Power(arg, F.C2)), F.Rational(F.CN1, F.C2)))

        steps.add(CalculusStep("Derivative of Arcsine", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcCos(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // -1 / sqrt(1 - arg^2)
        val outerDeriv = engine.evaluate(F.Times(F.CN1, F.Power(F.Subtract(F.C1, F.Power(arg, F.C2)), F.Rational(F.CN1, F.C2))))

        steps.add(CalculusStep("Derivative of Arccosine", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcTan(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // 1 / (1 + arg^2)
        val outerDeriv = engine.evaluate(F.Power(F.Plus(F.C1, F.Power(arg, F.C2)), F.CN1))

        steps.add(CalculusStep("Derivative of Arctangent", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dSinh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        val outerDeriv = F.Cosh(arg)

        steps.add(CalculusStep("Derivative of Sinh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dCosh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        val outerDeriv = F.Sinh(arg)

        steps.add(CalculusStep("Derivative of Cosh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dTanh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // sech(arg)^2 = 1 / cosh(arg)^2
        val outerDeriv = engine.evaluate(F.Power(F.Cosh(arg), F.CN2))

        steps.add(CalculusStep("Derivative of Tanh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcSinh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // 1 / sqrt(arg^2 + 1)
        val outerDeriv = engine.evaluate(F.Power(F.Plus(F.Power(arg, F.C2), F.C1), F.Rational(F.CN1, F.C2)))

        steps.add(CalculusStep("Derivative of Arcsinh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcCosh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // 1 / sqrt(arg^2 - 1)
        val outerDeriv = engine.evaluate(F.Power(F.Subtract(F.Power(arg, F.C2), F.C1), F.Rational(F.CN1, F.C2)))

        steps.add(CalculusStep("Derivative of Arccosh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
    }

    private fun dArcTanh(expr: IAST, variable: ISymbol, steps: MutableList<CalculusStep>): IExpr {
        val arg = expr.get(1)
        // 1 / (1 - arg^2)
        val outerDeriv = engine.evaluate(F.Power(F.Subtract(F.C1, F.Power(arg, F.C2)), F.CN1))

        steps.add(CalculusStep("Derivative of Arctanh", "$\\frac{d}{d${variable.toLaTeX()}}(${expr.toLaTeX()})$", outerDeriv.toLaTeX()))

        if (arg != variable) {
            steps.add(CalculusStep("Chain Rule", outerDeriv.toLaTeX(), "${outerDeriv.toLaTeX()} \\cdot \\frac{d}{d${variable.toLaTeX()}}(${arg.toLaTeX()})"))
            val innerDeriv = solveDerivative(arg, variable, steps)
            return engine.evaluate(F.Times(outerDeriv, innerDeriv))
        }
        return outerDeriv
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