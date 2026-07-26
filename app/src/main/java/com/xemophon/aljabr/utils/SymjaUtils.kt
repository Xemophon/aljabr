package com.xemophon.aljabr.utils

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
            .replace("ln", "Log", ignoreCase = true)
            .replace("log", "Log10", ignoreCase = true)
    }

    /**
     * Forces numerical evaluation using N() and handles degrees if needed.
     */
    fun calculateNumerical(expression: String, useRadians: Boolean): String {
        return try {
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
            
            val result = evaluator.eval("N($cleaned)")
            formatResult(result.toString())
        } catch (e: Throwable) {
            "Error"
        }
    }

    /**
     * Converts a mathematical expression string to its LaTeX representation using Symja's TeXForm.
     */
    fun toLaTeX(expression: String): String {
        return try {
            val cleaned = prepareForSymja(expression)
            if (cleaned.isBlank()) return ""
            // Use TeXForm to convert the expression to LaTeX
            val result = evaluator.eval("TeXForm($cleaned)")
            result.toString()
        } catch (e: Throwable) {
            expression // Fallback to raw expression on error
        }
    }

    fun formatResult(resStr: String): String {
        return resStr
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
            .replace("E", "e")
            .replace("Sqrt", "√")
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
