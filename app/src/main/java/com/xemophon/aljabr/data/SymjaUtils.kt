package com.xemophon.aljabr.data

import com.xemophon.aljabr.basicCalc.CalcFuncs
import org.matheclipse.core.eval.ExprEvaluator
import org.matheclipse.core.eval.steps.RuleDescription
import org.matheclipse.core.eval.steps.TraceStackSteps
import org.matheclipse.core.expression.F
import org.matheclipse.core.expression.S
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IEvalStepListener
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol

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
     * Evaluates a Symja command and captures intermediate steps using a single-pass listener.
     */
    fun evalWithSteps(symjaCommand: String): Pair<String, List<String>> {
        val steps = mutableListOf<String>()

        return synchronized(evaluator) {
            val engine = evaluator.evalEngine
            val oldListener = engine.stepListener
            val oldTrace = engine.isTraceMode
            val oldRewriteRule = org.matheclipse.core.basic.Config.TRACE_REWRITE_RULE
            val oldUserSteps = org.matheclipse.core.basic.Config.USER_STEPS_PARSER

            try {
                engine.isTraceMode = true
                org.matheclipse.core.basic.Config.TRACE_REWRITE_RULE = true
                org.matheclipse.core.basic.Config.USER_STEPS_PARSER = true
                
                // Disable caches to force re-evaluation with steps
                engine.rubiASTCache = null
                
                // Capture steps using TraceStackSteps (permissive)
                val stepsListener = TraceStackSteps { true }
                engine.setStepListener(stepsListener)
                
                // Enable Rubi step reporting specifically
                try {
                    // Internal Rubi showsteps symbol
                    val showStepsSymbol = F.symbol("§\$showsteps")
                    showStepsSymbol.assignValue(S.True, false)
                    evaluator.eval("\$showsteps = True")
                } catch (_: Exception) {}

                val evalResult = evaluator.eval(symjaCommand).toString()
                val startFormatted = formatResult(symjaCommand)
                val finalFormatted = formatResult(evalResult)

                // Extract structured steps from the listener
                val rawSteps = stepsListener.toString { true }
                if (rawSteps.isNotEmpty()) {
                    val lines = rawSteps.split("\n").filter { it.contains("$$") }
                    for (line in lines) {
                        try {
                            val parts = line.split("$$")
                            val mathPart = parts[0].trim()
                            val metaPart = parts[1].trim()
                            
                            val metaItems = metaPart.split(" -- ")
                            val ruleId = if (metaItems.size > 1) metaItems[1] else ""
                            
                            // Map Rule ID to a human description if available
                            val description = if (ruleId.isNotEmpty() && RuleDescription.EN_RULES.containsKey(ruleId)) {
                                RuleDescription.EN_RULES.get(ruleId)
                            } else if (metaItems.size > 2) {
                                metaItems[2] // Use the internal hint string if available
                            } else {
                                null
                            }
                            
                            val (lhs, rhs) = if (mathPart.contains(" -> ")) {
                                val mParts = mathPart.split(" -> ", limit = 2)
                                mParts[0].trim() to mParts[1].trim()
                            } else {
                                "" to mathPart
                            }

                            // Filter out internal noise
                            val noisy = listOf("FreeQ", "MemberQ", "Type", "IAST", "Rubj", "èqq", "ùumq", "LjstQ", "PolynomjalQ", "If", "SameQ")
                            if (noisy.any { mathPart.contains(it) }) continue

                            if (rhs.isNotEmpty() && rhs != lhs && rhs.length > 2) {
                                val formattedStep = if (description != null) {
                                    "$description: ${formatResult(rhs)}"
                                } else {
                                    formatResult(rhs)
                                }
                                
                                if (steps.isEmpty() || steps.last() != formattedStep) {
                                    steps.add(formattedStep)
                                    if (steps.size > 50) break
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
                
                // Fallback: If no steps were captured, try Trace[expr, TraceInternal -> True]
                if (steps.size <= 1) {
                    try {
                        val traceResult = evaluator.eval("Trace[$symjaCommand, _ -> _, TraceInternal -> True]")
                        if (traceResult is IAST && traceResult.isList) {
                            val traceSteps = mutableListOf<String>()
                            flattenTrace(traceResult, traceSteps)
                            for (s in traceSteps.distinct()) {
                                val formatted = formatResult(s)
                                if (formatted != startFormatted && formatted != finalFormatted && formatted.length > 2) {
                                    if (steps.isEmpty() || steps.last() != formatted) {
                                        steps.add(formatted)
                                    }
                                }
                                if (steps.size > 20) break
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Ensure the starting expression is the first step if meaningful
                if (steps.isNotEmpty() && steps.first() != startFormatted) {
                    if (!steps.first().contains(startFormatted) && !steps.first().contains(":")) {
                        steps.add(0, startFormatted)
                    }
                } else if (steps.isEmpty()) {
                    steps.add(startFormatted)
                }

                // Add final result if missing
                if (steps.isEmpty() || (steps.last() != finalFormatted && !steps.last().contains(finalFormatted))) {
                    steps.add(finalFormatted)
                }

                Pair(evalResult, steps)
            } catch (e: Throwable) {
                Pair("Error", emptyList())
            } finally {
                engine.isTraceMode = oldTrace
                engine.stepListener = oldListener
                org.matheclipse.core.basic.Config.TRACE_REWRITE_RULE = oldRewriteRule
                org.matheclipse.core.basic.Config.USER_STEPS_PARSER = oldUserSteps
            }
        }
    }

    private fun flattenTrace(expr: IExpr, steps: MutableList<String>) {
        if (expr is IAST) {
            for (i in 1 until expr.size()) {
                val arg = expr.get(i)
                if (arg is IAST && arg.isList) {
                    flattenTrace(arg, steps)
                } else {
                    val s = arg.toString()
                    val noisy = listOf("FreeQ", "MemberQ", "Rubj", "èqq", "ùumq")
                    if (s.isNotEmpty() && !s.startsWith("Trace") && noisy.none { s.contains(it) }) {
                        if (s.any { it in "+-*/^()[]" } || s.any { it.isLowerCase() }) {
                            if (s.length > 2) steps.add(s)
                        }
                    }
                }
            }
        } else {
            val s = expr.toString()
            if (s.length > 2) steps.add(s)
        }
    }

    /**
     * Forces numerical evaluation using N() and captures intermediate steps.
     */
    fun calculateNumericalWithSteps(expression: String, useRadians: Boolean, precision: Int = 4): Pair<String, List<String>> {
        val steps = mutableListOf<String>()

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
                        if (inputExpr != null && resultExpr != null && steps.size < 50) {
                            val inputStr = inputExpr.toString()
                            val resultStr = resultExpr.toString()
                            // Only capture meaningful numerical transformations
                            if (inputStr != resultStr && !inputStr.startsWith("N(") && inputStr.any { it.isLetter() }) {
                                val step = "${formatResult(inputStr)} → ${formatResult(resultStr)}"
                                if (steps.isEmpty() || steps.last() != step) {
                                    steps.add(step)
                                }
                            }
                        }
                    }
                }

                val result = evaluator.eval("N($cleaned, $precision + 2)").toString()
                
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
}
