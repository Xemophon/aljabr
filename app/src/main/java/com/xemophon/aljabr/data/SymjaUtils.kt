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
        val absRegex = Regex("""\|([^|]+)\|""")
        cleaned = cleaned.replace(absRegex, "(Abs($1))")

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
            .replace("log10", "Log10", ignoreCase = true)
            .replace(Regex("(?<![a-zA-Z])log(?!10)", RegexOption.IGNORE_CASE), "Log10")
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
            } catch (_: Throwable) {
                "Error"
            }
        }
    }

    fun toLaTeX(expression: String): String {
        return synchronized(evaluator) {
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return ""
                // Use TeXForm to convert the expression to LaTeX
                var result = evaluator.eval("TeXForm($cleaned)").toString()

                // Fix standard LaTeX math functions that Symja might output in raw form
                result = result.replace("\\arcsinh", "\\operatorname{asinh}")
                    .replace("\\arccosh", "\\operatorname{acosh}")
                    .replace("\\arctanh", "\\operatorname{atanh}")
                    .replace("\\arcsech", "\\operatorname{asech}")
                    .replace("\\arccsch", "\\operatorname{acsch}")
                    .replace("\\arccoth", "\\operatorname{acoth}")
                    .replace("\\operatorname{arcsinh}", "\\operatorname{asinh}")
                    .replace("\\operatorname{arccosh}", "\\operatorname{acosh}")
                    .replace("\\operatorname{arctanh}", "\\operatorname{atanh}")
                
                // Use simple string replacements where possible
                result = result.replace("\\log", "\\ln")
                
                // For the complex ln|x| replacement, use a more robust regex or manual check
                if (result.contains("\\ln") && result.contains("\\left|")) {
                    // This is a bit complex for a simple replace, but let's try to match it carefully
                    val searchPattern = """\\ln\s*\(\s*\\left\|\s*(.+?)\s*\\right\|\s*\)"""
                    try {
                        result = result.replace(Regex(searchPattern), """\\ln\left|$1\right|""")
                    } catch (_: Exception) {}
                }

                return result
            } catch (_: Throwable) {
                expression // Fallback to raw expression on error
            }
        }
    }

    fun formatResult(resStr: String): String {
        var result = resStr
            .replace("ComplexInfinity", "∞")
            .replace("Infinity", "∞")
        
        // Handle Log(base, x) or Log(x)
        // Log10[x] -> log(x)
        // Log[10, x] -> log(x)
        // Log[x] -> ln(x)
        // Log[b, x] -> log(x, b)
        
        // Use more cautious regex for Log replacements
        try {
            result = result.replace(Regex("""Log10\[(.+?)]"""), "log($1)")
                .replace(Regex("""Log\[10,\s*(.+?)]"""), "log($1)")
                .replace(Regex("""Log\[([^,]+)]"""), "ln($1)")
                .replace(Regex("""Log\[([^,]+),\s*(.+?)]"""), "log($2, $1)")
        } catch (_: Exception) {}

        result = result.replace("ArcSin", "asin")
            .replace("ArcCos", "acos")
            .replace("ArcTan", "atan")
            .replace("Sin", "sin")
            .replace("Cos", "cos")
            .replace("Tan", "tan")
            .replace("Pi", "π")
            .replace("pi", "π")
            .replace("GoldenRatio", "φ")
            .replace(Regex("""(?<![a-zA-Z])I(?![a-zA-Z])"""), "j")
            .replace("E", "e")
            .replace("Sqrt", "√")

        // First handle brackets
        result = result.replace("[", "(")
            .replace("]", ")")

        // Safely strip brackets around absolute values
        try {
            result = result.replace(Regex("""ln\(\|(.+?)\|\)"""), "ln|$1|")
                .replace(Regex("""log\(\|(.+?)\|\)"""), "log|$1|")
                .replace(Regex("""Abs\((.+?)\)"""), "|$1|")
        } catch (_: Exception) {}

        result = result.replace("*", " × ")
            .replace("  ", " ")
            .replace(",", ", ")
            .trim()

        return result
    }

    /**
     * Parses Symja's Solve/NSolve output like {{x -> 0}, {y -> 2}} or {{x -> 1}}
     * into a list of rules like ["x -> 0", "y -> 2"] or ["x -> 1"].
     */
    fun parseSolveResult(solveRes: String): List<String> {
        if (solveRes == "{}" || solveRes.isBlank()) return emptyList()

        val results = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        
        for (char in solveRes) {
            when (char) {
                '{' -> {
                    depth++
                    if (depth > 1) {
                        if (depth > 2) current.append(char)
                        else current = StringBuilder()
                    }
                }
                '}' -> {
                    if (depth == 2) {
                        val s = current.toString().trim()
                        if (s.isNotEmpty()) results.add(s)
                    } else if (depth > 2) {
                        current.append(char)
                    }
                    depth--
                }
                else -> {
                    if (depth >= 2) {
                        current.append(char)
                    }
                }
            }
        }
        
        // Fallback for single solutions not wrapped in nested braces: {x -> 1, y -> 2}
        if (results.isEmpty() && solveRes.contains("->")) {
            val simplified = solveRes.trim().removeSurrounding("{", "}").trim()
            if (simplified.isNotEmpty() && !simplified.startsWith("{")) {
                results.add(simplified)
            }
        }
        
        return results.distinct()
    }

    fun evalWithSteps(symjaCommand: String): Pair<String, List<String>> {
        return synchronized(evaluator) {
            try {
                val evalResult = evaluator.eval(symjaCommand).toString()
                Pair(evalResult, emptyList())
            } catch (_: Throwable) {
                Pair("Error", emptyList())
            }
        }
    }

    fun calculateNumericalWithSteps(expression: String, useRadians: Boolean, precision: Int = 4): Pair<String, List<String>> {
        val result = calculateNumerical(expression, useRadians, precision)
        return Pair(result, emptyList())
    }
}
