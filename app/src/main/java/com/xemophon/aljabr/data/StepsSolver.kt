package com.xemophon.aljabr.data

import org.matheclipse.core.eval.EvalEngine
import org.matheclipse.core.eval.TeXUtilities
import org.matheclipse.core.interfaces.AbstractEvalStepListener
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import java.io.StringWriter
import org.matheclipse.core.eval.ExprEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalculusStep(
    val title: String,
    val latexInput: String,
    val latexOutput: String
)

class CalculusStepListener(private val engine: EvalEngine) : AbstractEvalStepListener() {

    private val texUtil = TeXUtilities(engine, false)
    val capturedSteps = mutableListOf<CalculusStep>()
    var currentTitle = "Apply Rule"

    /**
     * This callback fires every time the engine applies a transformation rule.
     */
    override fun add(inputExpr: IExpr, resultExpr: IExpr, recursionDepth: Int, iterationCounter: Long, hints: IAST?) {
        // Filter out deep, noisy sub-evaluations
        if (recursionDepth > 2) return

        val latexIn = toLaTeX(inputExpr)
        val latexOut = toLaTeX(resultExpr)

        if (latexIn != latexOut) {
            capturedSteps.add(
                CalculusStep(
                    title = currentTitle,
                    latexInput = latexIn,
                    latexOutput = latexOut
                )
            )
        }
    }

    private fun toLaTeX(expr: IExpr): String {
        val writer = StringWriter()
        texUtil.toTeX(expr, writer)
        return sanitizeLatex(writer.toString().trim())
    }

    private fun sanitizeLatex(input: String): String {
        return input
            .replace("\\mathrm{", "\\text{") // Normalize roman text
            .replace("E^", "e^")             // Standardize Euler's number
            .replace("I", "i")               // Standardize imaginary unit
            .trim()
    }
}

class CalculusEngine {

    suspend fun integrateWithSteps(expression: String, variable: String = "x"): List<CalculusStep> {
        return solveWithSteps("Integrate($expression, $variable)", "Apply Integration Rule")
    }

    suspend fun differentiateWithSteps(expression: String, variable: String = "x"): List<CalculusStep> {
        return solveWithSteps("D($expression, $variable)", "Apply Differentiation Rule")
    }

    private suspend fun solveWithSteps(query: String, stepTitle: String): List<CalculusStep> {
        return withContext(Dispatchers.Default) {
            val evaluator = ExprEvaluator(false, 100)
            val engine = evaluator.evalEngine
            val stepListener = CalculusStepListener(engine)
            stepListener.currentTitle = stepTitle

            try {
                engine.stepListener = stepListener
                evaluator.evaluate(query)
            } catch (e: Exception) {
                // Handle error
            } finally {
                engine.stepListener = null
            }

            stepListener.capturedSteps
        }
    }
}
