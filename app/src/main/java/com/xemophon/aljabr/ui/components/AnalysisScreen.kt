package com.xemophon.aljabr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class AnalysisResult(
    val variables: List<String>,
    val derivatives: List<NamedExpression>,
    val localMaxima: List<String> = emptyList(),
    val localMinima: List<String> = emptyList(),
    val inflectionPoints: List<String> = emptyList(),
    val stationaryPoints: List<String> = emptyList(),
    val saddlePoints: List<String> = emptyList(),
    val error: String? = null
)

data class NamedExpression(val name: String, val expression: String, val rawExpression: String = "")

data class FourierResult(
    val l: String,
    val a0: String,
    val anGeneral: String? = null,
    val bnGeneral: String? = null,
    val fullSeries: String,
    val error: String? = null
)

data class PolynomialResult(
    val expression: String,
    val variable: String,
    val roots: List<String>,
    val factoredForm: String? = null,
    val error: String? = null
)

@Composable
fun AnalysisSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ResultItemCard(label: String? = null, displayText: String, rawValue: String? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (label != null) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            
            val needsLatex = remember(displayText, rawValue) {
                val toConvert = rawValue ?: displayText
                rawValue != null || toConvert.any { it.isLetter() || it == '^' || it == '/' || it == '*' || it == '(' || it == '{' || it == '}' }
            }

            val latexState = produceState<String?>(initialValue = if (!needsLatex) (rawValue ?: displayText) else null, displayText, rawValue) {
                if (needsLatex) {
                    val toConvert = rawValue ?: displayText
                    val result = withContext(Dispatchers.Default) {
                        SymjaUtils.toLaTeX(toConvert)
                    }
                    value = result
                } else {
                    value = rawValue ?: displayText
                }
            }

            val latexValue = latexState.value

            if (latexValue != null) {
                if (needsLatex || (latexValue != displayText || displayText.contains("^") || displayText.contains("/"))) {
                    Box(
                        modifier = Modifier
                            .padding(top = if (label != null) 4.dp else 0.dp)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Box(modifier = Modifier.widthIn(max = 2000.dp)) {
                            Latex(
                                latex = latexValue,
                                config = LatexConfig(
                                    fontSize = 20.sp,
                                    theme = LatexTheme.light(color = MaterialTheme.colorScheme.secondary),
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReportScreen(
    title: String,
    error: String? = null,
    onClear: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (error != null) {
            item {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            content()
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            ElevatedCard(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Clear and Return", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MatrixReport(
    title: String,
    result: String,
    onLoadA: (() -> Unit)? = null,
    onLoadB: (() -> Unit)? = null,
    onClear: () -> Unit
) {
    ReportScreen(
        title = title,
        onClear = onClear
    ) {
        item {
            AnalysisSectionHeader("Resulting Matrix / Value")
        }
        item {
            ResultItemCard(displayText = result)
        }

        if (onLoadA != null || onLoadB != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onLoadA?.let {
                        OutlinedButton(
                            onClick = it,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load into A")
                        }
                    }
                    onLoadB?.let {
                        OutlinedButton(
                            onClick = it,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load into B")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FourierReport(
    result: FourierResult,
    onClear: () -> Unit
) {
    ReportScreen(
        title = "Fourier Series Analysis",
        error = result.error,
        onClear = onClear
    ) {
        item { AnalysisSectionHeader("Transformation Parameters") }
        item { ResultItemCard("Half-period (L)", result.l) }
        item { ResultItemCard("DC Component (a₀)", result.a0) }

        if (result.anGeneral != null || result.bnGeneral != null) {
            item { AnalysisSectionHeader("General Fourier Coefficients") }
            result.anGeneral?.let { item { ResultItemCard("aₙ (Symbolic)", it) } }
            result.bnGeneral?.let { item { ResultItemCard("bₙ (Symbolic)", it) } }

            item { AnalysisSectionHeader("General Form (Summation)") }
            
            // Simplify the argument once
            val argLatex = SymjaUtils.toLaTeX("(n * Pi * x) / (${result.l})")
            
            val anPart = result.anGeneral?.let { 
                if (it == "0" || it.isBlank()) ""
                else {
                    val latex = SymjaUtils.toLaTeX(it, assumeIntegerN = true)
                    "\\left(${latex}\\right) \\cos\\left(${argLatex}\\right)" 
                }
            } ?: ""
            
            val bnPart = result.bnGeneral?.let { 
                if (it == "0" || it.isBlank()) ""
                else {
                    val latex = SymjaUtils.toLaTeX(it, assumeIntegerN = true)
                    "\\left(${latex}\\right) \\sin\\left(${argLatex}\\right)" 
                }
            } ?: ""
            
            val innerSum = when {
                anPart.isNotEmpty() && bnPart.isNotEmpty() -> "$anPart + $bnPart"
                anPart.isNotEmpty() -> anPart
                bnPart.isNotEmpty() -> bnPart
                else -> ""
            }
            
            val a0Part = if (result.a0 != "0") {
                // Try to simplify a0/2
                val a0Latex = SymjaUtils.toLaTeX("(${result.a0}) / 2")
                "$a0Latex + "
            } else ""
            
            val fullFormula = if (innerSum.isNotEmpty()) {
                "f(x) = $a0Part \\sum_{n=1}^{\\infty} \\left[ $innerSum \\right]"
            } else {
                "f(x) = ${if (a0Part.isNotEmpty()) a0Part.removeSuffix(" + ") else "0"}"
            }
            
            item { LatexResultCard(fullFormula) }
        }
    }
}

@Composable
fun LatexResultCard(latex: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.widthIn(max = 2000.dp)) {
                Latex(
                    latex = latex,
                    config = LatexConfig(
                        fontSize = 20.sp,
                        theme = LatexTheme.light(color = MaterialTheme.colorScheme.secondary),
                    )
                )
            }
        }
    }
}

@Composable
fun AnalysisReport(
    result: AnalysisResult,
    steps: List<CalculusStep> = emptyList(),
    isCalculatingSteps: Boolean = false,
    onShowStepsClick: () -> Unit = {},
    onClear: () -> Unit
) {
    ReportScreen(
        title = "Analysis Result",
        error = result.error,
        onClear = onClear
    ) {
        // Derivatives
        item { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnalysisSectionHeader("Derivatives", Modifier.weight(1f))
                if (steps.isNotEmpty() || isCalculatingSteps) {
                     Text(
                         text = "Steps",
                         color = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.clickable { onShowStepsClick() }.padding(end = 8.dp),
                         fontWeight = FontWeight.Bold
                     )
                }
            }
        }
        items(result.derivatives) { deriv ->
            ResultItemCard(deriv.name, deriv.expression, deriv.rawExpression)
        }

        // Maxima
        if (result.localMaxima.isNotEmpty()) {
            item { AnalysisSectionHeader("Local Maxima") }
            items(result.localMaxima) { point ->
                ResultItemCard("Maximum", point)
            }
        }

        // Minima
        if (result.localMinima.isNotEmpty()) {
            item { AnalysisSectionHeader("Local Minima") }
            items(result.localMinima) { point ->
                ResultItemCard("Minimum", point)
            }
        }

        // Inflection Points
        if (result.inflectionPoints.isNotEmpty()) {
            item { AnalysisSectionHeader("Inflection Points") }
            items(result.inflectionPoints) { point ->
                ResultItemCard("Inflection", point)
            }
        }

        // Saddle Points
        if (result.saddlePoints.isNotEmpty()) {
            item { AnalysisSectionHeader("Saddle Points") }
            items(result.saddlePoints) { point ->
                ResultItemCard("Saddle", point)
            }
        }

        // Other Stationary Points
        if (result.stationaryPoints.isNotEmpty()) {
            item { AnalysisSectionHeader("Stationary Points") }
            items(result.stationaryPoints) { point ->
                ResultItemCard("Stationary", point)
            }
        }
    }
}

@Composable
fun PolynomialReport(
    result: PolynomialResult,
    onClear: () -> Unit
) {
    ReportScreen(
        title = "Polynomial Analysis",
        error = result.error,
        onClear = onClear
    ) {
        // Roots Section
        if (result.roots.isNotEmpty()) {
            item { AnalysisSectionHeader("Roots (Numerical)") }
            items(result.roots) { root ->
                ResultItemCard(displayText = root)
            }
        } else {
            item { Text(text = "No roots found", style = MaterialTheme.typography.bodyLarge) }
        }

        // Factored Form
        result.factoredForm?.let { factored ->
            item { AnalysisSectionHeader("Factored Form") }
            item {
                ResultItemCard(displayText = factored)
            }
        }
    }
}

object AnalysisFunc {

    fun fullAnalysis(expression: String): AnalysisResult {
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            if (cleaned.isBlank()) return AnalysisResult(emptyList(), emptyList(), error = "Empty expression")

            val eval = SymjaUtils.evaluator
            
            // Get variables
            val varsExpr = eval.eval("Variables[$cleaned]")
            val rawVars = varsExpr.toString().removeSurrounding("{", "}").split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            // Filter to only include simple symbols (avoiding things like "Sin(x)")
            val vars = rawVars.filter { v -> 
                v.all { it.isLetter() || it.isDigit() } && !v.contains("(") && !v.contains("[")
            }

            if (vars.size > 2) {
                return AnalysisResult(vars, emptyList(), error = "Maximum 2 variables supported")
            }

            if (vars.isEmpty()) {
                 // Constant function
                 val firstDeriv = eval.eval("D[$cleaned, x]").toString()
                 return AnalysisResult(
                     listOf("x"),
                     listOf(NamedExpression("f'(x)", SymjaUtils.formatResult(firstDeriv), firstDeriv))
                 )
            }

            if (vars.size == 1) {
                val v = vars[0]
                val f1Raw = eval.eval("Simplify[D[$cleaned, $v]]").toString()
                val f2Raw = eval.eval("Simplify[D[$cleaned, {$v, 2}]]").toString()
                
                val statPointsRes = try {
                    eval.eval("Solve[D[$cleaned, $v] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }
                
                val inflPointsRes = try {
                    eval.eval("Solve[D[$cleaned, {$v, 2}] == 0, $v]").toString()
                } catch (e: Exception) { "Could not solve" }

                // Singular points where D[f, v] is undefined but f(v) is defined
                val singPointsRes = try {
                    val deriv = "D[$cleaned, $v]"
                    eval.eval("Solve[Denominator[Together[$deriv]] == 0, $v]").toString()
                } catch (e: Exception) { "{}" }

                val (maxima, minima, others) = classifyStationaryPoints(statPointsRes, singPointsRes, cleaned, v)

                AnalysisResult(
                    variables = vars,
                    derivatives = listOf(
                        NamedExpression("f'($v)", SymjaUtils.formatResult(f1Raw), f1Raw),
                        NamedExpression("f''($v)", SymjaUtils.formatResult(f2Raw), f2Raw)
                    ),
                    localMaxima = maxima,
                    localMinima = minima,
                    stationaryPoints = others,
                    inflectionPoints = calculatePoints(inflPointsRes, cleaned, vars)
                )
            } else {
                // 2 variables: x and y usually
                val x = vars.find { it == "x" } ?: vars[0]
                val y = vars.find { it == "y" && it != x } ?: vars[1]
                
                val fxRaw = eval.eval("Simplify[D[$cleaned, $x]]").toString()
                val fyRaw = eval.eval("Simplify[D[$cleaned, $y]]").toString()
                val fxxRaw = eval.eval("Simplify[D[$cleaned, {$x, 2}]]").toString()
                val fyyRaw = eval.eval("Simplify[D[$cleaned, {$y, 2}]]").toString()
                val fxyRaw = eval.eval("Simplify[D[$cleaned, $x, $y]]").toString()
                
                val critPointsRes = try {
                    eval.eval("Solve[{D[$cleaned, $x] == 0, D[$cleaned, $y] == 0}, {$x, $y}]").toString()
                } catch (e: Exception) { "Could not solve" }

                val (maxima, minima, saddles, others) = classifyStationaryPoints2D(critPointsRes, cleaned, x, y)

                AnalysisResult(
                    variables = listOf(x, y),
                    derivatives = listOf(
                        NamedExpression("f_$x", SymjaUtils.formatResult(fxRaw), fxRaw),
                        NamedExpression("f_$y", SymjaUtils.formatResult(fyRaw), fyRaw),
                        NamedExpression("f_$x$x", SymjaUtils.formatResult(fxxRaw), fxxRaw),
                        NamedExpression("f_$y$y", SymjaUtils.formatResult(fyyRaw), fyyRaw),
                        NamedExpression("f_$x$y", SymjaUtils.formatResult(fxyRaw), fxyRaw)
                    ),
                    localMaxima = maxima,
                    localMinima = minima,
                    saddlePoints = saddles,
                    stationaryPoints = others
                )
            }
        } catch (e: Exception) {
            AnalysisResult(emptyList(), emptyList(), error = e.message ?: "Analysis failed")
        }
    }

    private fun classifyStationaryPoints2D(
        solveRes: String,
        originalExpr: String,
        xVar: String,
        yVar: String
    ): Fourth<List<String>, List<String>, List<String>, List<String>> {
        val eval = SymjaUtils.evaluator
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        
        val maxima = mutableListOf<String>()
        val minima = mutableListOf<String>()
        val saddles = mutableListOf<String>()
        val others = mutableListOf<String>()

        val fxxExpr = "D[$originalExpr, {$xVar, 2}]"
        val fyyExpr = "D[$originalExpr, {$yVar, 2}]"
        val fxyExpr = "D[$originalExpr, $xVar, $yVar]"

        for (sol in solutions) {
            try {
                // sol is like "x -> 0, y -> 0"
                val fValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                val fValStr = fValExpr.toString()
                if (fValStr.contains("Infinity") || fValStr.contains("Indeterminate")) continue

                // Check for complex solutions
                val parts = sol.split(",").map { it.trim() }
                var isComplex = false
                val coords = mutableListOf<String>()
                for (part in parts) {
                    val valStr = part.split("->").last().trim()
                    val imPart = eval.eval("Im[N[$valStr]]").toString().toDoubleOrNull()
                    if (imPart == null || abs(imPart) > 1e-9) {
                        isComplex = true
                        break
                    }
                    coords.add(SymjaUtils.formatResult(valStr))
                }
                if (isComplex) continue

                val pointStr = "(${coords.joinToString(", ")}, ${SymjaUtils.formatResult(fValStr)})"

                // Hessian components
                val fxxVal = eval.eval("N[ReplaceAll[$fxxExpr, {$sol}]]").toString().toDoubleOrNull()
                val fyyVal = eval.eval("N[ReplaceAll[$fyyExpr, {$sol}]]").toString().toDoubleOrNull()
                val fxyVal = eval.eval("N[ReplaceAll[$fxyExpr, {$sol}]]").toString().toDoubleOrNull()

                if (fxxVal != null && fyyVal != null && fxyVal != null) {
                    val detH = fxxVal * fyyVal - fxyVal * fxyVal
                    if (detH > 1e-9) {
                        if (fxxVal > 1e-9) minima.add(pointStr)
                        else if (fxxVal < -1e-9) maxima.add(pointStr)
                        else others.add(pointStr)
                    } else if (detH < -1e-9) {
                        saddles.add(pointStr)
                    } else {
                        others.add(pointStr) // Inconclusive
                    }
                } else {
                    others.add(pointStr)
                }
            } catch (_: Exception) {}
        }

        return Fourth(maxima.distinct(), minima.distinct(), saddles.distinct(), others.distinct())
    }

    data class Fourth<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private fun classifyStationaryPoints(
        solveRes: String,
        singularRes: String,
        originalExpr: String,
        variable: String
    ): Triple<List<String>, List<String>, List<String>> {
        val eval = SymjaUtils.evaluator
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        val singulars = SymjaUtils.parseSolveResult(singularRes)

        val allCandidateRules = (solutions + singulars).distinct()
        
        val maxima = mutableListOf<String>()
        val minima = mutableListOf<String>()
        val others = mutableListOf<String>()

        val f2Expr = eval.eval("D[$originalExpr, {$variable, 2}]")

        for (sol in allCandidateRules) {
            try {
                val xValStr = sol.split("->").last().trim()
                // Filter complex solutions
                val imPart = eval.eval("Im[N[$xValStr]]").toString().toDoubleOrNull()
                if (imPart == null || abs(imPart) > 1e-9) continue

                // Check if f(x) exists
                val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                val yValStr = yValExpr.toString()
                if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                val pointStr = "(${SymjaUtils.formatResult(xValStr)}, ${SymjaUtils.formatResult(yValStr)})"

                // Second derivative test
                val d2ValExpr = eval.eval("ReplaceAll[$f2Expr, {$sol}]")
                val d2ValStr = d2ValExpr.toString()
                val d2Val = d2ValStr.toDoubleOrNull()

                if (d2Val != null) {
                    if (d2Val < -1e-9) maxima.add(pointStr)
                    else if (d2Val > 1e-9) minima.add(pointStr)
                    else {
                        // d2Val == 0, use neighborhood test
                        val type = neighborhoodTest(originalExpr, variable, xValStr)
                        when (type) {
                            1 -> maxima.add(pointStr)
                            -1 -> minima.add(pointStr)
                            else -> others.add(pointStr)
                        }
                    }
                } else {
                    // Symbolic result or undefined d2Val, use neighborhood test
                    val type = neighborhoodTest(originalExpr, variable, xValStr)
                    when (type) {
                        1 -> maxima.add(pointStr)
                        -1 -> minima.add(pointStr)
                        else -> others.add(pointStr)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return Triple(maxima.distinct(), minima.distinct(), others.distinct())
    }

    private fun neighborhoodTest(expr: String, variable: String, xCenterStr: String): Int {
        val eval = SymjaUtils.evaluator
        try {
            val xCenter = eval.eval("N[$xCenterStr]").toString().toDoubleOrNull() ?: return 0
            val eps = 1e-5
            
            val yCenter = eval.eval("N[ReplaceAll[$expr, $variable -> $xCenter]]").toString().toDoubleOrNull() ?: return 0
            val yLeft = eval.eval("N[ReplaceAll[$expr, $variable -> ${xCenter - eps}]]").toString().toDoubleOrNull() ?: return 0
            val yRight = eval.eval("N[ReplaceAll[$expr, $variable -> ${xCenter + eps}]]").toString().toDoubleOrNull() ?: return 0

            return if (yCenter > yLeft + 1e-11 && yCenter > yRight + 1e-11) 1 // Max
            else if (yCenter < yLeft - 1e-11 && yCenter < yRight - 1e-11) -1 // Min
            else 0
        } catch (_: Exception) {
            return 0
        }
    }

    private fun calculatePoints(solveRes: String, originalExpr: String, variables: List<String>): List<String> {
        val solutions = SymjaUtils.parseSolveResult(solveRes)
        if (solutions.isEmpty()) return emptyList()

        val eval = SymjaUtils.evaluator
        val points = mutableListOf<String>()

        for (sol in solutions) {
            try {
                if (variables.size == 1) {
                    val xValStr = sol.split("->").last().trim()
                    // Filter complex solutions
                    val imPart = eval.eval("Im[N[$xValStr]]").toString().toDoubleOrNull()
                    if (imPart == null || abs(imPart) > 1e-9) continue

                    // Check if f(x) exists
                    val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                    val yValStr = yValExpr.toString()
                    if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                    points.add("(${SymjaUtils.formatResult(xValStr)}, ${SymjaUtils.formatResult(yValStr)})")
                } else {
                    // Expecting something like "x -> 1, y -> 2"
                    // In multi-variable case, sol might be "x -> 1, y -> 2"
                    val yValExpr = eval.eval("ReplaceAll[$originalExpr, {$sol}]")
                    val yValStr = yValExpr.toString()
                    if (yValStr.contains("Infinity") || yValStr.contains("Indeterminate")) continue

                    val parts = sol.split(",").map { it.trim() }
                    val coords = variables.map { v ->
                        parts.find { it.startsWith(v) }?.split("->")?.last()?.trim() ?: "?"
                    }
                    points.add("(${coords.joinToString(", ") { SymjaUtils.formatResult(it) }}, ${SymjaUtils.formatResult(yValStr)})")
                }
            } catch (e: Exception) {
                // Fallback to formatting the raw solution if evaluation fails
                points.add(SymjaUtils.formatResult(sol))
            }
        }
        return points.distinct()
    }
}
