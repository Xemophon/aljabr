package com.xemophon.aljabr.data

import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import org.matheclipse.core.eval.ExprEvaluator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object SymjaUtils {

    private class EvaluatorPool(private val maxPoolSize: Int = 4) {
        private val pool = ConcurrentLinkedQueue<ExprEvaluator>()

        fun <T> evaluate(block: (ExprEvaluator) -> T): T {
            val eval = pool.poll() ?: createEvaluator()
            try {
                return block(eval)
            } finally {
                if (pool.size < maxPoolSize) {
                    pool.offer(eval)
                }
            }
        }

        private fun createEvaluator(): ExprEvaluator {
            return ExprEvaluator().apply {
                evalEngine.isRelaxedSyntax = true
                evalEngine.recursionLimit = 128
                evalEngine.iterationLimit = 500
            }
        }
    }

    private val pool = EvaluatorPool()

    /**
     * Executes a block safely using a pooled [ExprEvaluator].
     */
    fun <T> evaluate(block: (ExprEvaluator) -> T): T = pool.evaluate(block)

    // Shared default evaluator instance kept for legacy compatibility.
    val evaluator: ExprEvaluator by lazy {
        ExprEvaluator().apply {
            evalEngine.isRelaxedSyntax = true
            evalEngine.recursionLimit = 128
            evalEngine.iterationLimit = 500
        }
    }

    // Pre-compiled regexes for high performance
    private val ABS_REGEX = Regex("""\|([^|]+)\|""")
    private val LOG10_REGEX = Regex("""(?<![a-zA-Z])log(?!10)""", RegexOption.IGNORE_CASE)
    private val LN_LATEX_REGEX = Regex("""\\ln\s*\(\s*\\left\|\s*(.+?)\s*\\right\|\s*\)""")
    private val D_REGEX = Regex("""D\[(.+?),\s*(.+?)]""")
    private val INTEGRATE_REGEX = Regex("""Integrate\[(.+?),\s*(.+?)]""")
    private val LOG10_BRACKET_REGEX = Regex("""Log10\[(.+?)]""")
    private val LOG10_COMMA_REGEX = Regex("""Log\[10,\s*(.+?)]""")
    private val LOG_SINGLE_REGEX = Regex("""Log\[([^,]+)]""")
    private val LOG_DUAL_REGEX = Regex("""Log\[([^,]+),\s*(.+?)]""")
    private val I_REGEX = Regex("""(?<![a-zA-Z])I(?![a-zA-Z])""")
    private val LN_ABS_REGEX = Regex("""ln\(\|(.+?)\|\)""")
    private val LOG_ABS_REGEX = Regex("""log\(\|(.+?)\|\)""")
    private val ABS_BRACKET_REGEX = Regex("""Abs\((.+?)\)""")
    private val TRIG_REGEX = Regex("""(cos|sin|Cos|Sin)\s*[( \[]([^()\[\]]*n[^()\[\]]*)[)\]]""", RegexOption.IGNORE_CASE)

    // Thread-safe LRU Cache for LaTeX string outputs
    private val lateXCache = ConcurrentHashMap<String, String>()

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
            .replace("ⁿ", "^n")
            .replace("(-1)^n", "(-1)^n")
            .replace("(-1)ⁿ", "(-1)^n")

        // Convert |x| to Abs(x)
        cleaned = cleaned.replace(ABS_REGEX, "(Abs($1))")

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
            .replace(LOG10_REGEX, "Log10")
            .replace("ln", "Log", ignoreCase = true)
    }

    /**
     * Forces numerical evaluation using N() and handles degrees if needed.
     */
    fun calculateNumerical(expression: String, useRadians: Boolean, useRationalize: Boolean = false, precision: Int = 4): String {
        return evaluate { eval ->
            try {
                if (!useRadians) {
                    eval.eval("SinDeg[x_] := Sin[x * Degree]")
                    eval.eval("CosDeg[x_] := Cos[x * Degree]")
                    eval.eval("TanDeg[x_] := Tan[x * Degree]")
                    eval.eval("ArcSinDeg[x_] := ArcSin[x] / Degree")
                    eval.eval("ArcCosDeg[x_] := ArcCos[x] / Degree")
                    eval.eval("ArcTanDeg[x_] := ArcTan[x] / Degree")
                }

                val cleaned = prepareForSymja(expression, useRadians)
                if (cleaned.isBlank()) return@evaluate ""

                val result = if (!useRationalize) {
                    eval.eval("N($cleaned, $precision + 2)")
                } else {
                    eval.eval("Rationalize($cleaned)")
                }
                val resStr = result.toString()

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

    fun toLaTeX(expression: String, assumeIntegerN: Boolean = false): String {
        val cacheKey = "$expression|$assumeIntegerN"
        lateXCache[cacheKey]?.let { return it }

        val formatted = evaluate { eval ->
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return@evaluate ""

                val evalExpr = if (assumeIntegerN) {
                    "TeXForm(FullSimplify[$cleaned, Element[n, Integers]])"
                } else {
                    "TeXForm($cleaned)"
                }

                var result = eval.eval(evalExpr).toString()

                if (result.contains("\\{") && result.contains("\\}")) {
                    result = formatSymjaTexListToMatrix(result)
                }

                result = result.replace("\\arcsinh", "\\operatorname{asinh}")
                    .replace("\\arccosh", "\\operatorname{acosh}")
                    .replace("\\arctanh", "\\operatorname{atanh}")
                    .replace("\\arcsech", "\\operatorname{asech}")
                    .replace("\\arccsch", "\\operatorname{acsch}")
                    .replace("\\arccoth", "\\operatorname{acoth}")
                    .replace("\\operatorname{arcsinh}", "\\operatorname{asinh}")
                    .replace("\\operatorname{arccosh}", "\\operatorname{acosh}")
                    .replace("\\operatorname{arctanh}", "\\operatorname{atanh}")
                    .replace("\\log", "\\ln")
                    .replace("\\text{DiracDelta}", "\\delta")
                    .replace("DiracDelta", "\\delta")

                if (result.contains("\\ln") && result.contains("\\left|")) {
                    try {
                        result = result.replace(LN_LATEX_REGEX, """\\ln\left|$1\right|""")
                    } catch (_: Exception) {}
                }

                result
            } catch (_: Throwable) {
                expression
            }
        }

        if (formatted.isNotEmpty() && lateXCache.size < 500) {
            lateXCache[cacheKey] = formatted
        }
        return formatted
    }

    fun formatSymjaTexListToMatrix(texStr: String): String {
        var content = texStr.trim()

        if (content.startsWith("\\{") && content.endsWith("\\}")) {
            content = content.substring(2, content.length - 2).trim()
        } else {
            return texStr
        }

        return try {
            if (content.contains("\\{")) {
                val rows = mutableListOf<String>()
                var depth = 0
                var current = StringBuilder()

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
            .replace("DiracDelta", "δ")

        try {
            result = result.replace(D_REGEX, "d/d$2($1)")
                .replace(INTEGRATE_REGEX, "∫($1) d$2")
        } catch (_: Exception) {}

        try {
            result = result.replace(LOG10_BRACKET_REGEX, "log($1)")
                .replace(LOG10_COMMA_REGEX, "log($1)")
                .replace(LOG_SINGLE_REGEX, "ln($1)")
                .replace(LOG_DUAL_REGEX, "log($2, $1)")
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
            .replace(I_REGEX, "j")
            .replace("E", "e")
            .replace("Sqrt", "√")

        result = result.replace("Plus", "")
            .replace("Times", "")
            .replace("Power", "")
            .replace("Rational", "")
            .replace("Subtract", "")
            .replace("Divide", "")

        result = result.replace("[", "(")
            .replace("]", ")")

        try {
            result = tryRecognizeTrigPatterns(result)
        } catch (_: Exception) {}

        try {
            result = result.replace(LN_ABS_REGEX, "ln|$1|")
                .replace(LOG_ABS_REGEX, "log|$1|")
                .replace(ABS_BRACKET_REGEX, "|$1|")
        } catch (_: Exception) {}

        result = result.replace("*", " × ")
            .replace("  ", " ")
            .replace(",", ", ")
            .trim()

        return result
    }

    fun calculateTaylor(expression: String, center: String, order: Int): String {
        return evaluate { eval ->
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return@evaluate ""
                val centerClean = if (center.isBlank()) "0" else prepareForSymja(center)

                val result = eval.eval("Normal(Series($cleaned, {x, $centerClean, $order}))")
                formatResult(result.toString())
            } catch (_: Throwable) {
                "Error"
            }
        }
    }

    private fun tryRecognizeTrigPatterns(input: String): String {
        var output = input

        val matches = TRIG_REGEX.findAll(input).toList().distinctBy { it.value }

        for (match in matches) {
            val fullMatch = match.value
            val evalTerm = prepareForSymja(fullMatch)

            try {
                val (val1, val2, val3) = evaluate { eval ->
                    Triple(
                        eval.eval("Simplify[ReplaceAll[$evalTerm, n -> 1]]").toString().removeSuffix(".0"),
                        eval.eval("Simplify[ReplaceAll[$evalTerm, n -> 2]]").toString().removeSuffix(".0"),
                        eval.eval("Simplify[ReplaceAll[$evalTerm, n -> 3]]").toString().removeSuffix(".0")
                    )
                }

                val simplified = when {
                    val1 == "-1" && val2 == "1" && val3 == "-1" -> "(-1)ⁿ"
                    val1 == "1" && val2 == "-1" && val3 == "1" -> "-(-1)ⁿ"
                    val1 == "0" && val2 == "0" && val3 == "0" -> "0"
                    val1 == "1" && val2 == "1" && val3 == "1" -> "1"
                    val1 == "-1" && val2 == "-1" && val3 == "-1" -> "-1"
                    else -> null
                }

                if (simplified != null) {
                    output = output.replace(fullMatch, simplified)
                }
            } catch (_: Exception) {}
        }

        try {
            output = output.replace(Regex("""\(-1\)\^n"""), "(-1)ⁿ")
                .replace(Regex("""\(-1\)\^\(\s*n\s*\+\s*1\s*\)"""), "-(-1)ⁿ")
                .replace(Regex("""\(-1\)\^\(\s*n\s*-\s*1\s*\)"""), "-(-1)ⁿ")
                .replace(Regex("""\(-1\)\^\(\s*2\s*n\s*\)"""), "1")
                .replace(Regex("""\(-1\)\^\(\s*2\s*n\s*\+\s*1\s*\)"""), "-1")
                .replace(Regex("""\(-1\)\^\(\s*2\s*n\s*-\s*1\s*\)"""), "-1")
        } catch (_: Exception) {}

        try {
            output = output.replace(Regex("""\b0\s*[×*]\s*[\w()]+\b"""), "0")
                .replace(Regex("""\b[\w()]+\s*[×*]\s*0\b"""), "0")

            output = output.replace(Regex("""\b1\s*[×*]\s*"""), "")
                .replace(Regex("""\s*[×*]\s*1\b"""), "")

            output = output.replace(Regex("""\+\s*0\b"""), "")
                .replace(Regex("""\b0\s*\+\s*"""), "")
                .replace(Regex("""-\s*0\b"""), "")

            output = output.replace("+-", "-")
                .replace("-+", "-")
                .replace("--", "+")
                .replace("++", "+")
                .replace("  ", " ")
        } catch (_: Exception) {}

        return output
    }

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

        if (results.isEmpty() && solveRes.contains("->")) {
            val simplified = solveRes.trim().removeSurrounding("{", "}").trim()
            if (simplified.isNotEmpty() && !simplified.startsWith("{")) {
                results.add(simplified)
            }
        }

        return results.distinct()
    }
}
