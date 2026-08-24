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
    fun calculateNumerical(expression: String, useRadians: Boolean, useRationalize: Boolean = false, precision: Int = 4): String {
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

                val result = if(!useRationalize) {
                    evaluator.eval("N($cleaned, $precision + 2)") // Request slightly more precision for calculation
                } else{
                    evaluator.eval("Rationalize($cleaned)")
                }
                val resStr = result.toString()

                // If it's a simple number, format it with the requested precision
                val d = resStr.toDoubleOrNull()
                if (d != null && !useRationalize) {
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
                
                // Use TeXForm to convert the expression to LaTeX. 
                // Symja's TeXForm automatically handles lists as pmatrix or similar.
                var result = evaluator.eval("TeXForm($cleaned)").toString()

                // Fix standard Symja TeXForm list output which often looks like \{ \{1, 2\}, \{3, 4\} \}
                // and convert it to a proper LaTeX pmatrix
                if (result.contains("\\{") && result.contains("\\}")) {
                    result = formatSymjaTexListToMatrix(result)
                }

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

    fun formatSymjaTexListToMatrix(texStr: String): String {
        var content = texStr.trim()
        
        // Remove outer escaped braces
        if (content.startsWith("\\{") && content.endsWith("\\}")) {
            content = content.substring(2, content.length - 2).trim()
        } else {
            return texStr
        }

        return try {
            // Check if it's a matrix (contains nested \{ \})
            if (content.contains("\\{")) {
                val rows = mutableListOf<String>()
                var depth = 0
                var current = StringBuilder()
                
                // Process characters to split rows carefully
                var i = 0
                while (i < content.length) {
                    val char = content[i]
                    if (char == '\\' && i + 1 < content.length) {
                        val next = content[i + 1]
                        if (next == '{') {
                            depth++
                            if (depth > 1) current.append("\\{")
                        } else if (next == '}') {
                            depth--
                            if (depth > 0) {
                                current.append("\\}")
                            } else {
                                rows.add(current.toString())
                                current = StringBuilder()
                            }
                        }
                        i += 2
                        continue
                    }
                    
                    if (depth > 0) {
                        current.append(char)
                    }
                    i++
                }
                
                val latexRows = rows.map { row ->
                    val elements = row.trim().split(",")
                    elements.joinToString(" & ") { it.trim() }
                }
                "\\begin{pmatrix} ${latexRows.joinToString(" \\\\ ")} \\end{pmatrix}"
            } else {
                // It's a simple vector {1, 2, 3}
                val elements = content.split(",")
                "\\begin{pmatrix} ${elements.joinToString(" \\\\ ") { it.trim() }} \\end{pmatrix}"
            }
        } catch (_: Exception) {
            texStr
        }
    }

    fun formatResult(resStr: String): String {
        var result = resStr
            .replace("ComplexInfinity", "∞")
            .replace("Infinity", "∞")
        
        // Handle Calculus notations specifically for steps
        try {
            result = result.replace(Regex("""D\[(.+?),\s*(.+?)]"""), "d/d$2($1)")
                .replace(Regex("""Integrate\[(.+?),\s*(.+?)]"""), "∫($1) d$2")
        } catch (_: Exception) {}

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
            .replace("Sec", "sec")
            .replace("Csc", "csc")
            .replace("Cot", "cot")
            .replace("Pi", "π")
            .replace("pi", "π")
            .replace("GoldenRatio", "φ")
            .replace(Regex("""(?<![a-zA-Z])I(?![a-zA-Z])"""), "j")
            .replace("E", "e")
            .replace("Sqrt", "√")

        // Handle structural leaking
        result = result.replace("Plus", "")
            .replace("Times", "")
            .replace("Power", "")
            .replace("Rational", "")
            .replace("Subtract", "")
            .replace("Divide", "")

        // First handle brackets
        result = result.replace("[", "(")
            .replace("]", ")")

        // Fourier and common identities (assuming integer n)
        try {
            result = result.replace(Regex("""cos\(n\s*π\)""", RegexOption.IGNORE_CASE), "(-1)ⁿ")
                .replace(Regex("""sin\(n\s*π\)""", RegexOption.IGNORE_CASE), "0")
                .replace(Regex("""cos\(2\s*n\s*π\)""", RegexOption.IGNORE_CASE), "1")
                .replace(Regex("""sin\(2\s*n\s*π\)""", RegexOption.IGNORE_CASE), "0")
                // Handle cases like (-1)^(2n) -> 1
                .replace(Regex("""\(-1\)\^\(2\s*n\)"""), "1")
                .replace(Regex("""\(-1\)\^\(2\s*n\s*\+\s*1\)"""), "-1")
        } catch (_: Exception) {}

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
     * Calculates the Taylor series expansion for a function.
     */
    fun calculateTaylor(expression: String, center: String, order: Int): String {
        return synchronized(evaluator) {
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return ""
                val centerClean = if (center.isBlank()) "0" else prepareForSymja(center)

                // Series[f, {x, x0, n}] calculates the series expansion
                // Normal[] converts it from SeriesData to a regular polynomial expression
                val result = evaluator.eval("Normal(Series($cleaned, {x, $centerClean, $order}))")
                formatResult(result.toString())
            } catch (_: Throwable) {
                "Error"
            }
        }
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
}
