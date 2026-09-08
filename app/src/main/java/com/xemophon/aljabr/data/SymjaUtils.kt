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
    private val LN_LATEX_REGEX = Regex("""\\ln\s*\(\s*\\left\|\s*(.+?)\s*\\right\|\s*\)""")
    private val D_REGEX = Regex("""D[\(\[](.+?),\s*(.+?)[\)\]]""")
    private val INTEGRATE_REGEX = Regex("""Integrate[\(\[](.+?),\s*(.+?)[\)\]]""")
    private val LOG10_BRACKET_REGEX = Regex("""Log10[\(\[](.+?)[\)\]]""")
    private val LOG10_COMMA_REGEX = Regex("""Log[\(\[]10,\s*(.+?)[\)\]]""")
    private val LOG_SINGLE_REGEX = Regex("""Log[\(\[]([^\(\)\[\],]+)[\)\]]""")
    private val LOG_DUAL_REGEX = Regex("""Log[\(\[]([^\(\)\[\],]+),\s*(.+?)[\)\]]""")
    private val I_REGEX = Regex("""(?<![a-zA-Z])I(?![a-zA-Z])""")
    private val EULER_E_REGEX = Regex("""(?<![a-zA-Z])e(?![a-zA-Z])""")
    private val IMAGINARY_J_REGEX = Regex("""(?<![a-zA-Z])j(?![a-zA-Z])""", RegexOption.IGNORE_CASE)
    private val IMAGINARY_I_REGEX = Regex("""(?<![a-zA-Z])i(?![a-zA-Z])""", RegexOption.IGNORE_CASE)
    private val DIGIT_J_OR_I_REGEX = Regex("""(\d(?:\.\d+)?)\s*([ji])\b""", RegexOption.IGNORE_CASE)
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
            .replace("%", "/100")
            .replace("π", "Pi")
            .replace("φ", "GoldenRatio")
            .replace("√", "Sqrt")
            .replace("sqrt", "Sqrt", ignoreCase = true)
            .replace("ⁿ", "^n")
            .replace("(-1)^n", "(-1)^n")
            .replace("(-1)ⁿ", "(-1)^n")
            .replace("∞", "Infinity")

        // Convert number followed by j or i to explicit multiplication: 2j -> 2*I, 3.5i -> 3.5*I
        cleaned = cleaned.replace(DIGIT_J_OR_I_REGEX, "$1*I")

        // Replace standalone e with Euler's E
        cleaned = cleaned.replace(EULER_E_REGEX, "E")

        // Replace standalone j and i with imaginary unit I
        cleaned = cleaned.replace(IMAGINARY_J_REGEX, "I")
        cleaned = cleaned.replace(IMAGINARY_I_REGEX, "I")

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

        cleaned = replaceLogarithmsForSymja(cleaned)

        cleaned = cleaned
            .replace("arcsinh", "ArcSinh", ignoreCase = true)
            .replace("arccosh", "ArcCosh", ignoreCase = true)
            .replace("arctanh", "ArcTanh", ignoreCase = true)
            .replace("arcsech", "ArcSech", ignoreCase = true)
            .replace("arccsch", "ArcCsch", ignoreCase = true)
            .replace("arccoth", "ArcCoth", ignoreCase = true)
            .replace("asin", "ArcSin", ignoreCase = true)
            .replace("acos", "ArcCos", ignoreCase = true)
            .replace("atan", "ArcTan", ignoreCase = true)
            .replace("asec", "ArcSec", ignoreCase = true)
            .replace("acsc", "ArcCsc", ignoreCase = true)
            .replace("acot", "ArcCot", ignoreCase = true)
            .replace("sinh", "Sinh", ignoreCase = true)
            .replace("cosh", "Cosh", ignoreCase = true)
            .replace("tanh", "Tanh", ignoreCase = true)
            .replace("sech", "Sech", ignoreCase = true)
            .replace("csch", "Csch", ignoreCase = true)
            .replace("coth", "Coth", ignoreCase = true)
            .replace("sin", "Sin", ignoreCase = true)
            .replace("cos", "Cos", ignoreCase = true)
            .replace("tan", "Tan", ignoreCase = true)
            .replace("sec", "Sec", ignoreCase = true)
            .replace("csc", "Csc", ignoreCase = true)
            .replace("cot", "Cot", ignoreCase = true)
            .replace("abs", "Abs", ignoreCase = true)

        val openParens = cleaned.count { it == '(' }
        val closeParens = cleaned.count { it == ')' }
        if (openParens > closeParens) {
            cleaned += ")".repeat(openParens - closeParens)
        }
        val openBrackets = cleaned.count { it == '[' }
        val closeBrackets = cleaned.count { it == ']' }
        if (openBrackets > closeBrackets) {
            cleaned += "]".repeat(openBrackets - closeBrackets)
        }

        return cleaned
    }

    fun formatComplexNumber(real: Double, imag: Double, precision: Int = 4): String {
        val isRealZero = Math.abs(real) < 1e-10
        val isImagZero = Math.abs(imag) < 1e-10

        if (isImagZero) {
            return CalcFuncs.formatResult(real, precision)
        }

        val realFormatted = if (isRealZero) "" else CalcFuncs.formatResult(real, precision)
        val absImag = Math.abs(imag)
        val imagFormatted = if (Math.abs(absImag - 1.0) < 1e-10) "" else CalcFuncs.formatResult(absImag, precision)

        return when {
            realFormatted.isEmpty() -> {
                if (imag < 0) "-${imagFormatted}j" else "${imagFormatted}j"
            }
            imag < 0 -> {
                "$realFormatted - ${imagFormatted}j"
            }
            else -> {
                "$realFormatted + ${imagFormatted}j"
            }
        }
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

                if (!useRationalize) {
                    val realVal = eval.eval("N[Re[$cleaned], $precision + 2]").toString().toDoubleOrNull()
                    val imagVal = eval.eval("N[Im[$cleaned], $precision + 2]").toString().toDoubleOrNull()

                    if (realVal != null && imagVal != null && !realVal.isNaN() && !imagVal.isNaN()) {
                        return@evaluate formatComplexNumber(realVal, imagVal, precision)
                    }
                } else {
                    val realStr = eval.eval("Rationalize[Re[$cleaned]]").toString()
                    val imagStr = eval.eval("Rationalize[Im[$cleaned]]").toString()
                    val realVal = realStr.toDoubleOrNull()
                    val imagVal = imagStr.toDoubleOrNull()

                    if (realVal != null && imagVal != null && !realVal.isNaN() && !imagVal.isNaN()) {
                        return@evaluate formatComplexNumber(realVal, imagVal, precision)
                    }
                }

                val result = if (!useRationalize) {
                    eval.eval("N[$cleaned, $precision + 2]")
                } else {
                    eval.eval("Rationalize[$cleaned]")
                }
                val resStr = result.toString()

                val d = resStr.toDoubleOrNull()
                val formatted = if (d != null && !useRationalize) {
                    CalcFuncs.formatResult(d, precision)
                } else {
                    formatResult(resStr)
                }
                if (formatted.contains("sindeg", ignoreCase = true) ||
                    formatted.contains("cosdeg", ignoreCase = true) ||
                    formatted.contains("tandeg", ignoreCase = true) ||
                    formatted.contains("arcsindeg", ignoreCase = true) ||
                    formatted.contains("arccosdeg", ignoreCase = true) ||
                    formatted.contains("arctandeg", ignoreCase = true)
                ) {
                    return@evaluate ""
                }
                formatted
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
                    "TeXForm[FullSimplify[$cleaned, Element[n, Integers]]]"
                } else {
                    "TeXForm[$cleaned]"
                }

                var result = eval.eval(evalExpr).toString()

                if (result.contains("\\{") && result.contains("\\}")) {
                    result = formatSymjaTexListToMatrix(result)
                }

                result = simplifyLogRatiosInLaTeX(result)

                result = result.replace("\\arcsinh", "\\operatorname{asinh}")
                    .replace("\\arccosh", "\\operatorname{acosh}")
                    .replace("\\arctanh", "\\operatorname{atanh}")
                    .replace("\\arcsech", "\\operatorname{asech}")
                    .replace("\\arccsch", "\\operatorname{acsch}")
                    .replace("\\arccoth", "\\operatorname{acoth}")
                    .replace("\\operatorname{arcsinh}", "\\operatorname{asinh}")
                    .replace("\\operatorname{arccosh}", "\\operatorname{acosh}")
                    .replace("\\operatorname{arctanh}", "\\operatorname{atanh}")
                    .replace("\\text{DiracDelta}", "\\delta")
                    .replace("DiracDelta", "\\delta")

                result = result.replace("\\log_{", "LATEX_LOG_BASE_")
                    .replace("\\log10", "\\log_{10}")
                    .replace("\\log", "\\ln")
                    .replace("LATEX_LOG_BASE_", "\\log_{")

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

        result = simplifyLogRatios(result)

        try {
            result = result.replace(LOG10_BRACKET_REGEX, "log($1)")
                .replace(LOG10_COMMA_REGEX, "log($1)")
                .replace(LOG_SINGLE_REGEX, "ln($1)")
                .replace(LOG_DUAL_REGEX, "log($2, $1)")
        } catch (_: Exception) {}

        result = result.replace("ArcSinDeg", "asin")
            .replace("ArcCosDeg", "acos")
            .replace("ArcTanDeg", "atan")
            .replace("SinDeg", "sin")
            .replace("CosDeg", "cos")
            .replace("TanDeg", "tan")
            .replace("ArcSin", "asin")
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
            .replace(Regex("""\b1\s*[*×]\s*j\b"""), "j")
            .replace(Regex("""\b(\d+(?:\.\d+)?)\s*[*×]\s*j\b"""), "$1j")
            .replace(Regex("""\+\s*1j\b"""), "+ j")
            .replace(Regex("""-\s*1j\b"""), "- j")
            .replace(", ", ",")
            .replace(",", ", ")
            .replace("  ", " ")
            .trim()

        return result
    }

    fun calculateTaylor(expression: String, center: String, order: Int): String {
        return evaluate { eval ->
            try {
                val cleaned = prepareForSymja(expression)
                if (cleaned.isBlank()) return@evaluate ""
                val centerClean = if (center.isBlank()) "0" else prepareForSymja(center)

                val result = eval.eval("Normal[Series[$cleaned, {x, $centerClean, $order}]]")
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

    /**
     * Converts logarithms in input expressions to Symja AST forms.
     * Symja uses Log[x] for natural log (ln) and Log10[x] or Log[base, x] for common/custom base logarithms.
     */
    fun replaceLogarithmsForSymja(input: String): String {
        var result = input

        // 1. Subscript log notation: log_2(x) or log_{2}(x) -> Log[2, x]
        val subscriptRegex = Regex("""(?<![a-zA-Z])log_\{?([^{}()+*-/,\s]+)\}?\s*\((.+?)\)""", RegexOption.IGNORE_CASE)
        result = result.replace(subscriptRegex) { match ->
            val base = match.groupValues[1]
            val arg = match.groupValues[2]
            "Log[$base, $arg]"
        }

        // 2. log10(...) or log10|...|
        result = replaceFuncWithBalancedParens(result, "log10") { content ->
            "Log10[$content]"
        }
        val log10AbsRegex = Regex("""(?<![a-zA-Z])log10\s*\|(.+?)\|""", RegexOption.IGNORE_CASE)
        result = result.replace(log10AbsRegex) { "Log10[Abs[${it.groupValues[1]}]]" }

        // 3. log(arg, base) or log(arg)
        result = replaceFuncWithBalancedParens(result, "log") { content ->
            val topCommaIndex = findTopLevelComma(content)
            if (topCommaIndex != -1) {
                val arg = content.substring(0, topCommaIndex).trim()
                val base = content.substring(topCommaIndex + 1).trim()
                "Log[$base, $arg]"
            } else {
                "Log10[$content]"
            }
        }
        val logAbsRegex = Regex("""(?<![a-zA-Z])log\s*\|(.+?)\|""", RegexOption.IGNORE_CASE)
        result = result.replace(logAbsRegex) { "Log10[Abs[${it.groupValues[1]}]]" }

        // 4. ln(...) or ln|...|
        result = replaceFuncWithBalancedParens(result, "ln") { content ->
            "Log[$content]"
        }
        val lnAbsRegex = Regex("""(?<![a-zA-Z])ln\s*\|(.+?)\|""", RegexOption.IGNORE_CASE)
        result = result.replace(lnAbsRegex) { "Log[Abs[${it.groupValues[1]}]]" }

        val standaloneLnRegex = Regex("""(?<![a-zA-Z])ln(?![a-zA-Z0-9])""", RegexOption.IGNORE_CASE)
        result = result.replace(standaloneLnRegex, "Log")

        return result
    }

    private fun replaceFuncWithBalancedParens(
        input: String,
        funcName: String,
        transform: (content: String) -> String
    ): String {
        val pattern = "(?<![a-zA-Z])$funcName\\s*\\("
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        var result = input

        while (true) {
            val match = regex.find(result) ?: break
            val startParen = match.range.last
            val endParen = findMatchingParen(result, startParen)
            if (endParen == -1) break

            val content = result.substring(startParen + 1, endParen)
            val replacement = transform(content)
            result = result.substring(0, match.range.first) + replacement + result.substring(endParen + 1)
        }
        return result
    }

    private fun findMatchingParen(str: String, openPos: Int): Int {
        var depth = 0
        for (i in openPos until str.length) {
            when (str[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun findTopLevelComma(str: String): Int {
        var depth = 0
        for (i in str.indices) {
            when (str[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' -> if (depth == 0) return i
            }
        }
        return -1
    }

    /**
     * Simplifying helper that converts logarithmic ratios like ln(x)/ln(a), log(x)/log(a), or Log[x]/Log[a]
     * to base-a logarithm forms: log(x, a) or Log[a, x] (or log(x) / Log10[x] when a = 10).
     */
    fun simplifyLogRatios(expression: String): String {
        var result = expression

        // Plain text / Symja AST ratio: (ln|log|Log|log10|Log10)[num] / (ln|log|Log|log10|Log10)[den]
        // or (ln|log|Log|log10|Log10)(num) / (ln|log|Log|log10|Log10)(den)
        val ratioRegex = Regex(
            """(?i)\b(ln|log10|log|Log10|Log)[\(\[]([^\(\)\[\]]+|\([^\(\)]*\)|\[[^\[\]]*\])[\)\]]\s*/\s*(ln|log10|log|Log10|Log)[\(\[]([^\(\)\[\]]+|\([^\(\)]*\)|\[[^\[\]]*\])[\)\]]"""
        )

        result = result.replace(ratioRegex) { match ->
            val num = match.groupValues[2].trim()
            val den = match.groupValues[4].trim()

            when {
                den == "10" || den == "10.0" -> "log($num)"
                den.equals("e", ignoreCase = true) || den == "E" -> "ln($num)"
                num == "1" || num == "1.0" -> "0"
                else -> "log($num, $den)"
            }
        }

        return result
    }

    /**
     * Simplifies LaTeX fractions representing log ratios, e.g. \frac{\ln(x)}{\ln(a)} -> \log_{a}\left(x\right)
     */
    fun simplifyLogRatiosInLaTeX(texStr: String): String {
        var result = texStr

        // \frac{\ln(num)}{\ln(den)} or \frac{\log(num)}{\log(den)}
        val latexFracRegex = Regex(
            """\\frac\{\s*\\(ln|log)\s*(?:\(\\left\|\s*|\(|\\left\()?\s*(.+?)\s*(?:\(\\right\|\s*|\)|\\right\))?\s*\}\{\s*\\(ln|log)\s*(?:\(\\left\|\s*|\(|\\left\()?\s*(.+?)\s*(?:\(\\right\|\s*|\)|\\right\))?\s*\}"""
        )

        result = result.replace(latexFracRegex) { match ->
            val num = match.groupValues[2].trim()
            val den = match.groupValues[4].trim()

            when {
                den == "10" || den == "10.0" -> "\\log_{10}\\left($num\\right)"
                den.equals("e", ignoreCase = true) || den == "E" -> "\\ln\\left($num\\right)"
                num == "1" || num == "1.0" -> "0"
                else -> "\\log_{$den}\\left($num\\right)"
            }
        }

        return result
    }
}
