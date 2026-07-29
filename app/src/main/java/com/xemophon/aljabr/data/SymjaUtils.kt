package com.xemophon.aljabr.data

import com.xemophon.aljabr.basicCalc.CalcFuncs
import org.matheclipse.core.eval.ExprEvaluator

object SymjaUtils {
    // Shared evaluator to avoid expensive re-initialization.
    val evaluator by lazy {
        ExprEvaluator().apply {
            evalEngine.isRelaxedSyntax = true
            evalEngine.recursionLimit = 128
            evalEngine.iterationLimit = 500
        }
    }

    fun prepareForSymja(expression: String, useRadians: Boolean = true): String {
        var cleaned = expression
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "Pi")
            .replace("e", "E")
            .replace("φ", "GoldenRatio")
            .replace("j", "I", ignoreCase = true)
            .replace("i", "I", ignoreCase = true)
            .replace("√", "Sqrt")
            .replace("sqrt", "Sqrt", ignoreCase = true)

        // Convert |x| to Abs(x)
        val absRegex = Regex("\\|([^|]+)\\|")
        cleaned = cleaned.replace(absRegex, "Abs($1)")

        if (!useRadians) {
            cleaned = cleaned
                .replace("asin(", "ArcSinDeg(", ignoreCase = true)
                .replace("acos(", "ArcCosDeg(", ignoreCase = true)
                .replace("atan(", "ArcTanDeg(", ignoreCase = true)
                .replace("sin(", "SinDeg(", ignoreCase = true)
                .replace("cos(", "CosDeg(", ignoreCase = true)
                .replace("tan(", "TanDeg(", ignoreCase = true)
        }

        return cleaned
            .replace("asin", "ArcSin", ignoreCase = true)
            .replace("acos", "ArcCos", ignoreCase = true)
            .replace("atan", "ArcTan", ignoreCase = true)
            .replace("sin", "Sin", ignoreCase = true)
            .replace("cos", "Cos", ignoreCase = true)
            .replace("tan", "Tan", ignoreCase = true)
            .replace("log", "Log10", ignoreCase = true)
            .replace("ln", "Log", ignoreCase = true)
    }

    /**
     * Forces numerical evaluation using N() and handles degrees if needed.
     */
    fun calculateNumerical(expression: String, useRadians: Boolean, precision: Int = 4): String {
        return synchronized(evaluator) {
            try {
                if (!useRadians) {
                    evaluator.eval("SinDeg[x_] := Sin[x * Degree]")
                    evaluator.eval("CosDeg[x_] := Cos[x * Degree]")
                    evaluator.eval("TanDeg[x_] := Tan[x * Degree]")
                    evaluator.eval("ArcSinDeg[x_] := ArcSin[x] / Degree")
                    evaluator.eval("ArcCosDeg[x_] := ArcCos[x] / Degree")
                    evaluator.eval("ArcTanDeg[x_] := ArcTan[x] / Degree")
                }

                val cleaned = prepareForSymja(expression, useRadians)
                if (cleaned.isBlank()) return ""

                val result = evaluator.eval("N($cleaned, $precision + 2)") // Request slightly more precision for calculation
                val resStr = result.toString()

                // If it's a simple number, format it with the requested precision
                val d = resStr.toDoubleOrNull()
                if (d != null) {
                    CalcFuncs.formatResult(d, precision)
                } else {
                    formatResult(resStr)
                }
            } catch (e: Throwable) {
                "Error"
            }
        }
    }

    /**
     * Converts a mathematical expression string to its LaTeX representation using Symja's TeXForm.
     */
    fun toLaTeX(expression: String): String {
        return synchronized(evaluator) {
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return ""
                // Use TeXForm to convert the expression to LaTeX
                val result = evaluator.eval("TeXForm($cleaned)").toString()

                // Fix standard LaTeX math functions that Symja might output in raw form
                result.replace("\\arcsinh", "\\operatorname{asinh}")
                    .replace("\\arccosh", "\\operatorname{acosh}")
                    .replace("\\arctanh", "\\operatorname{atanh}")
                    .replace("\\arcsech", "\\operatorname{asech}")
                    .replace("\\arccsch", "\\operatorname{acsch}")
                    .replace("\\arccoth", "\\operatorname{acoth}")
                    .replace("\\operatorname{arcsinh}", "\\operatorname{asinh}")
                    .replace("\\operatorname{arccosh}", "\\operatorname{acosh}")
                    .replace("\\operatorname{arctanh}", "\\operatorname{atanh}")
                    .replace("\\log", "\\ln")

            } catch (e: Throwable) {
                expression // Fallback to raw expression on error
            }
        }
    }

    fun formatResult(resStr: String): String {
        return resStr
            .replace("ComplexInfinity", "∞")
            .replace("Infinity", "∞")
            .replace("Log10", "log")
            .replace("Log", "ln")
            .replace("ArcSin", "asin")
            .replace("ArcCos", "acos")
            .replace("ArcTan", "atan")
            .replace("Sin", "sin")
            .replace("Cos", "cos")
            .replace("Tan", "tan")
            .replace("Pi", "π")
            .replace("pi", "π")
            .replace("GoldenRatio", "φ")
            .replace("I", "j")
            .replace("i", "j")
            .replace("E", "e")
            .replace("Sqrt", "√")
            .replace(Regex("Abs\\[([^]]+)]"), "|$1|")
            .replace(Regex("Abs\\(([^)]+)\\)"), "|$1|")
            .replace("*", " × ")
            .replace("  ", " ")
            .replace("[", "(")
            .replace("]", ")")
            .replace(",", ", ")
            .trim()
    }

    /**
     * Parses Symja's Solve/NSolve output like {{x -> 0}, {y -> 2}} or {{x -> 1}}
     * into a list of rules like ["x -> 0", "y -> 2"] or ["x -> 1"].
     */
    fun parseSolveResult(solveRes: String): List<String> {
        if (solveRes == "{}" || solveRes.isBlank()) return emptyList()

        // Match content inside the inner-most braces: {rule1, rule2, ...}
        // This handles {{x -> 0}, {x -> 1}} and {{x -> 0, y -> 1}}
        val regex = Regex("\\{([^\\{\\}]+)\\}")
        return regex.findAll(solveRes)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }
}