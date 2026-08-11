package com.xemophon.aljabr.data

import com.xemophon.aljabr.basicCalc.CalcFuncs
import org.matheclipse.core.eval.ExprEvaluator
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IEvalStepListener
import org.matheclipse.core.interfaces.IExpr

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
            } catch (e: Throwable) {
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
                    .replace(Regex("\\\\log(?!_)"), "\\ln")
                    .replace(Regex("\\\\ln\\s*\\(\\s*\\\\left\\|\\s*(.+?)\\s*\\\\right\\|\\s*\\)"), "\\ln\\left|$1\\right|")

                return result
            } catch (e: Throwable) {
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
        result = result.replace(Regex("Log10\\[([^]]+)]"), "log($1)")
            .replace(Regex("Log\\[10,\\s*([^]]+)]"), "log($1)")
            .replace(Regex("Log\\[([^,]+)]"), "ln($1)")
            .replace(Regex("Log\\[([^,]+),\\s*([^]]+)]"), "log($2, $1)")

        result = result.replace("ArcSin", "asin")
            .replace("ArcCos", "acos")
            .replace("ArcTan", "atan")
            .replace("Sin", "sin")
            .replace("Cos", "cos")
            .replace("Tan", "tan")
            .replace("Pi", "π")
            .replace("pi", "π")
            .replace("GoldenRatio", "φ")
            .replace(Regex("(?<![a-zA-Z])I(?![a-zA-Z])"), "j")
            .replace(Regex("(?<![a-zA-Z])i(?![a-zA-Z])"), "j")
            .replace("E", "e")
            .replace("Sqrt", "√")

        // First handle brackets
        result = result.replace("[", "(")
            .replace("]", ")")

        // Safely strip brackets around absolute values to avoid unbalanced brackets
        result = result.replace(Regex("ln\\(\\|([^|]+)\\|\\)"), "ln|$1|")
            .replace(Regex("log\\(\\|([^|]+)\\|\\)"), "log|$1|")
            .replace(Regex("Abs\\(([^)]+)\\)"), "|$1|")
            .replace("*", " × ")
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

        // Match content inside the inner-most braces: {rule1, rule2, ...}
        // This handles {{x -> 0}, {x -> 1}} and {{x -> 0, y -> 1}}
        val regex = Regex("\\{([^\\{\\}]+)\\}")
        return regex.findAll(solveRes)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    /**
     * Evaluates a Symja command and captures intermediate steps.
     */
    fun evalWithSteps(symjaCommand: String): Pair<String, List<String>> {
        val steps = mutableListOf<String>()
        val maxSteps = 100

        return synchronized(evaluator) {
            val engine = evaluator.evalEngine
            val oldListener = engine.stepListener
            val oldTrace = engine.isTraceMode

            try {
                engine.isTraceMode = true
                
                // Try using the built-in Steps() function first for human-readable steps
                try {
                    // Try to get symbolic steps
                    val stepsExpr = evaluator.eval("Steps($symjaCommand)")
                    // In Symja, human-readable steps are usually returned as a List
                    if (stepsExpr is IAST && stepsExpr.isList && stepsExpr.size() > 1) {
                        for (i in 1 until stepsExpr.size()) {
                            val step = stepsExpr.get(i)
                            val stepStr = when {
                                step is IAST && (step.isRule || step.head().toString() == "Rule") -> {
                                    val hint = step.arg1().toString().removeSurrounding("\"")
                                    val trans = formatResult(step.arg2().toString())
                                    "$hint: $trans"
                                }
                                step is IAST && step.isList && step.size() >= 3 -> {
                                    val hint = step.arg1().toString().removeSurrounding("\"")
                                    val trans = formatResult(step.arg2().toString())
                                    "$hint: $trans"
                                }
                                else -> formatResult(step.toString())
                            }
                            if (stepStr.isNotEmpty() && !stepStr.contains("Steps(")) {
                                steps.add(stepStr)
                            }
                        }
                        if (steps.isNotEmpty()) {
                            val finalRes = evaluator.eval(symjaCommand).toString()
                            return Pair(finalRes, steps)
                        }
                    }
                } catch (_: Exception) {}

                engine.stepListener = object : IEvalStepListener {
                    override fun getHint(): String? = null
                    override fun setHint(hint: String?) {}
                    override fun setUp(expr: IExpr?, recursionDepth: Int, stackMarker: Any?) {}
                    override fun tearDown(result: IExpr, recursionDepth: Int, commitTraceFrame: Boolean, stackMarker: Any?) {}
                    override fun add(inputExpr: IExpr?, resultExpr: IExpr?, recursionDepth: Int, iterationCounter: Long, listOfHints: IAST?) {
                        if (inputExpr != null && resultExpr != null && steps.size < maxSteps) {
                            val inputStr = inputExpr.toString()
                            val resultStr = resultExpr.toString()
                            if (inputStr != resultStr && !inputStr.startsWith("TeXForm") && !resultStr.startsWith("TeXForm")) {
                                val step = "${formatResult(inputStr)} → ${formatResult(resultStr)}"
                                if (steps.isEmpty() || steps.last() != step) {
                                    steps.add(step)
                                }
                            }
                        }
                    }
                }

                val result = evaluator.eval(symjaCommand).toString()
                
                // Fallback to Trace if no steps were captured by the listener
                if (steps.isEmpty()) {
                    try {
                        // Use Trace with a higher depth to ensure we see transformations
                        val traceExpr = evaluator.eval("Trace[$symjaCommand, _ -> _, TraceInternal -> True]")
                        if (traceExpr is IAST) {
                            val traceSteps = mutableListOf<String>()
                            flattenTrace(traceExpr, traceSteps)
                            
                            val filteredTrace = traceSteps.filter { 
                                it != symjaCommand && it != result && !it.startsWith("Trace") 
                            }.distinct()

                            if (filteredTrace.isNotEmpty()) {
                                var prev = symjaCommand
                                for (current in filteredTrace) {
                                    if (prev != current) {
                                        steps.add("${formatResult(prev)} → ${formatResult(current)}")
                                    }
                                    prev = current
                                    if (steps.size >= maxSteps) break
                                }
                                steps.add("${formatResult(prev)} → ${formatResult(result)}")
                            }
                        }
                    } catch (_: Exception) {}
                }

                Pair(result, steps)
            } catch (e: Throwable) {
                Pair("Error", emptyList())
            } finally {
                engine.isTraceMode = oldTrace
                engine.stepListener = oldListener
            }
        }
    }

    /**
     * Forces numerical evaluation using N() and captures intermediate steps.
     */
    fun calculateNumericalWithSteps(expression: String, useRadians: Boolean, precision: Int = 4): Pair<String, List<String>> {
        val steps = mutableListOf<String>()
        val maxSteps = 100

        return synchronized(evaluator) {
            val engine = evaluator.evalEngine
            val oldListener = engine.stepListener
            val oldTrace = engine.isTraceMode

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
                if (cleaned.isBlank()) return Pair("", emptyList())

                engine.isTraceMode = true
                engine.stepListener = object : IEvalStepListener {
                    override fun getHint(): String? = null
                    override fun setHint(hint: String?) {}
                    override fun setUp(expr: IExpr?, recursionDepth: Int, stackMarker: Any?) {}
                    override fun tearDown(result: IExpr, recursionDepth: Int, commitTraceFrame: Boolean, stackMarker: Any?) {}
                    override fun add(inputExpr: IExpr?, resultExpr: IExpr?, recursionDepth: Int, iterationCounter: Long, listOfHints: IAST?) {
                        if (inputExpr != null && resultExpr != null && steps.size < maxSteps) {
                            val inputStr = inputExpr.toString()
                            val resultStr = resultExpr.toString()
                            if (inputStr != resultStr && !inputStr.startsWith("N(") && !resultStr.startsWith("N(")) {
                                val step = "${formatResult(inputStr)} → ${formatResult(resultStr)}"
                                if (steps.isEmpty() || steps.last() != step) {
                                    steps.add(step)
                                }
                            }
                        }
                    }
                }

                val result = evaluator.eval("N($cleaned, $precision + 2)").toString()
                
                if (steps.isEmpty()) {
                    try {
                        val traceExpr = evaluator.eval("Trace[N[$cleaned, $precision + 2]]")
                        if (traceExpr is IAST) {
                            val traceSteps = mutableListOf<String>()
                            flattenTrace(traceExpr, traceSteps)
                            val uniqueSteps = traceSteps.distinct()
                            for (i in 0 until uniqueSteps.size - 1) {
                                steps.add("${formatResult(uniqueSteps[i])} → ${formatResult(uniqueSteps[i+1])}")
                                if (steps.size >= maxSteps) break
                            }
                        }
                    } catch (_: Exception) {}
                }

                val d = result.toDoubleOrNull()
                val resFormatted = if (d != null) {
                    CalcFuncs.formatResult(d, precision)
                } else {
                    formatResult(result)
                }
                
                Pair(resFormatted, steps)
            } catch (e: Throwable) {
                Pair("Error", emptyList())
            } finally {
                engine.isTraceMode = oldTrace
                engine.stepListener = oldListener
            }
        }
    }

    private fun flattenTrace(expr: IExpr, steps: MutableList<String>) {
        if (expr is IAST) {
            // Symja Trace returns a list where some elements are sub-lists (nested traces)
            // and others are the expressions themselves.
            for (i in 1 until expr.size()) {
                val arg = expr.get(i)
                if (arg is IAST && arg.isList) {
                    flattenTrace(arg, steps)
                } else {
                    val s = arg.toString()
                    // Filter out very noisy internal functions
                    val noisy = listOf(
                        "FreeQ", "MemberQ", "Type", "IAST", "Rule", "Set", "SetDelayed", 
                        "Module", "Condition", "Rubj", "èqq", "ùumq", "LjstQ", "PolynomjalQ",
                        "If", "SameQ", "IntegerQ", "NumberQ", "SymbolQ", "CompoundExpression",
                        "Block", "Catch", "Throw", "Return", "Break", "Continue", "True", "False",
                        "ReplaceAll", "Replace", "Map", "Apply", "Flatten", "Variables", "Evaluate"
                    )
                    // Only keep steps that look like math and aren't internal logic
                    if (s.isNotEmpty() && !s.startsWith("Trace") && noisy.none { s.contains(it) }) {
                        // Check if it's a structural transformation (contains operators or variables)
                        val isMath = s.any { it in "+-*/^()[]" } || s.any { it.isLowerCase() }
                        // Also ensure it's not just a single variable or number (too granular)
                        if (isMath && s.length > 1) {
                            steps.add(s)
                        }
                    }
                }
            }
        } else {
            val s = expr.toString()
            val noisy = listOf("FreeQ", "MemberQ", "Rubj", "èqq", "ùumq")
            if (s.isNotEmpty() && noisy.none { s.contains(it) } && s.length > 1) {
                steps.add(s)
            }
        }
    }
}
