package com.xemophon.aljabr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.differentiate.AnalysisFunc
import com.xemophon.aljabr.differentiate.AnalysisResult
import com.xemophon.aljabr.differentiate.DiffFunc
import com.xemophon.aljabr.integrate.IntegFunc
import com.xemophon.aljabr.limits.LimitsFunc

@Composable
fun CalcBox(
    expression: String,
    result: String,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    onCursorIndexChange: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            val textStyle = MaterialTheme.typography.displayMedium.copy(
                fontSize = if (expression.length > 12) 32.sp else 40.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End
            )

            // Using a hidden or overlaid clickable area is complex for exact character positioning
            // For now, let's make the whole expression box clickable to focus the end,
            // or just allow the user to see where they are typing.

            Text(
                text = if (cursorIndex != -1 && cursorIndex < expression.length) {
                    StringBuilder(expression).insert(cursorIndex, "|").toString()
                } else if (cursorIndex == expression.length) {
                    "$expression|"
                } else {
                    expression
                },
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                lineHeight = 36.sp,
                modifier = Modifier.clickable { onCursorIndexChange(expression.length) }
            )
        }
        if (result.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (result.length > 8) 48.sp else 64.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.End
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                lineHeight = 70.sp
            )
        }
    }
}

enum class CalculatorMode { STANDARD, GRAPH, LIMITS, INTEGRATE, DIFFERENTIATE }
enum class CalculatorFocus { EXPRESSION, TARGET, INTEG_LOWER, INTEG_UPPER }

class CalcBoxViewModel : ViewModel() {
    var displayText by mutableStateOf("0")
        private set

    var cursorIndex by mutableIntStateOf(1)
        private set

    var resultText by mutableStateOf("")
        private set

    var targetText by mutableStateOf("")
        private set

    var lowerLimitText by mutableStateOf("")
        private set

    var upperLimitText by mutableStateOf("")
        private set

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var diffGridMode by mutableStateOf("Single") // "Single" or "Multiple"

    var currentFocus by mutableStateOf(CalculatorFocus.EXPRESSION)
        private set

    var calculatorMode by mutableStateOf(CalculatorMode.STANDARD)
    var limitType by mutableStateOf(LimitType.FINITE)
    var integType by mutableStateOf(IntegralType.DEFINITE)
    var calculationEnabled by mutableStateOf(true)

    private var lastExpression = ""
    private var isShowingResult = false

    fun handleAction(action: CalcButtonAction) {
        // Clear mode-specific results when any input button is pressed
        if (action !is CalcButtonAction.Calculate && action !is CalcButtonAction.Graph && action !is CalcButtonAction.Clear) {
            if (calculatorMode == CalculatorMode.INTEGRATE || calculatorMode == CalculatorMode.LIMITS) {
                resultText = ""
            }
        }

        when (action) {
            is CalcButtonAction.Calculate -> calculateResult()
            is CalcButtonAction.Clear -> clearAll()
            is CalcButtonAction.Backspace -> {
                handleBackspace()
                updateInstantResult()
            }

            is CalcButtonAction.Symbol -> {
                when (action.text) {
                    "( )", "()" -> handleBrackets()
                    "%" -> handlePercentage()
                    else -> handleSymbol(action.text)
                }
                updateInstantResult()
            }

            is CalcButtonAction.Scientific -> {
                handleScientific(action)
                updateInstantResult()
            }

            is CalcButtonAction.Variable -> {
                handleVariable(action)
                updateInstantResult()
            }

            is CalcButtonAction.Limits -> {
                switchLimitMode(action.type)
            }

            is CalcButtonAction.Integrals -> {
                switchIntegMode(action.type)
            }

            is CalcButtonAction.Graph -> { /* Handled in UI */
            }

            is CalcButtonAction.Differentiate -> {
                diffGridMode = "Multiple"
            }

            is CalcButtonAction.DifferentiateSingle -> {
                diffGridMode = "Single"
            }
        }
    }

    fun setFocus(focus: CalculatorFocus) {
        currentFocus = focus
        if (resultText.isNotEmpty()) resultText = ""
        if (focus == CalculatorFocus.EXPRESSION) {
            cursorIndex = displayText.length
        } else {
            cursorIndex = -1 // Hide cursor in main expression when target/limit is focused
        }
    }

    fun updateCursorIndex(index: Int) {
        cursorIndex = index.coerceIn(0, displayText.length)
        if (cursorIndex != -1) {
            currentFocus = CalculatorFocus.EXPRESSION
        }
    }

    private fun handleSymbol(symbol: String) {
        if (calculatorMode == CalculatorMode.LIMITS && currentFocus == CalculatorFocus.TARGET) {
            if (limitType == LimitType.FINITE) {
                if (symbol.matches(Regex("[0-9.-]+"))) {
                    targetText += symbol
                }
            }
            return
        }

        if (calculatorMode == CalculatorMode.INTEGRATE) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) {
                if (symbol.matches(Regex("[0-9.-]+"))) lowerLimitText += symbol
                return
            }
            if (currentFocus == CalculatorFocus.INTEG_UPPER) {
                if (symbol.matches(Regex("[0-9.-]+"))) upperLimitText += symbol
                return
            }
        }

        if (symbol == "0" && displayText == "0") return

        if (displayText == "Error" || displayText == "NaN" || displayText == "Infinity") {
            displayText = if (symbol.contains(Regex("[0-9]"))) symbol else "0"
            cursorIndex = displayText.length
            return
        }

        insertText(symbol)
        isShowingResult = false
    }

    private fun insertText(toInsert: String) {
        if (displayText == "0" && !toInsert.startsWith(" × ")) {
            displayText = toInsert
            cursorIndex = toInsert.length
        } else {
            val sb = StringBuilder(displayText)
            sb.insert(cursorIndex, toInsert)
            displayText = sb.toString()
            cursorIndex += toInsert.length
        }
    }

    private fun handleBrackets() {
        val openBrackets = displayText.count { it == '(' }
        val closedBrackets = displayText.count { it == ')' }
        val lastChar = if (cursorIndex > 0) displayText[cursorIndex - 1] else null

        val toInsert = when {
            displayText == "0" -> "("
            openBrackets > closedBrackets -> {
                if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == 'x' || lastChar == 'y')) {
                    ")"
                } else {
                    "("
                }
            }

            else -> {
                val prefix =
                    if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == 'x' || lastChar == 'y')) " × " else ""
                if (displayText == "0") "(" else "${prefix}("
            }
        }

        insertText(toInsert)
    }

    private fun handlePercentage() {
        if (displayText != "0" && displayText != "Error") {
            displayText += "%"
        }
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        if (calculatorMode == CalculatorMode.INTEGRATE) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) {
                lowerLimitText += action.text
                return
            }
            if (currentFocus == CalculatorFocus.INTEG_UPPER) {
                upperLimitText += action.text
                return
            }
        }

        if (action.type == ScientificType.FACTORIAL) {
            if (displayText != "Error" && displayText != "NaN" && displayText != "Infinity") {
                val sb = StringBuilder(displayText)
                sb.insert(cursorIndex, "!")
                displayText = sb.toString()
                cursorIndex += 1
            }
            return
        }

        val lastChar = if (cursorIndex > 0) displayText[cursorIndex - 1] else null
        val isPrevDigitOrParen =
            lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'x' || lastChar == 'y' || lastChar == 'π' || lastChar == 'e') && displayText != "0"
        val prefix = if (isPrevDigitOrParen) " × " else ""

        val toInsert = when (action.type) {
            ScientificType.SQRT -> "${prefix}√("
            ScientificType.PI -> "${prefix}π"
            ScientificType.E -> "${prefix}e"
            ScientificType.ASIN -> "${prefix}asin("
            ScientificType.ACOS -> "${prefix}acos("
            ScientificType.ATAN -> "${prefix}atan("
            ScientificType.LN -> "${prefix}ln("
            ScientificType.SIN -> "${prefix}sin("
            ScientificType.COS -> "${prefix}cos("
            ScientificType.TAN -> "${prefix}tan("
            ScientificType.LOG -> "${prefix}log("
            else -> "$prefix${action.text.lowercase()}("
        }

        insertText(toInsert)
    }

    private fun handleVariable(action: CalcButtonAction.Variable) {
        if (calculatorMode == CalculatorMode.INTEGRATE) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) {
                lowerLimitText += action.text
                return
            }
            if (currentFocus == CalculatorFocus.INTEG_UPPER) {
                upperLimitText += action.text
                return
            }
        }

        val lastChar = if (cursorIndex > 0) displayText[cursorIndex - 1] else null
        val isPrevDigitOrParen =
            lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'x' || lastChar == 'y' || lastChar == 'π' || lastChar == 'e') && displayText != "0"
        val prefix = if (isPrevDigitOrParen) " × " else ""

        val toInsert = "$prefix${action.text}"

        insertText(toInsert)
    }

    private fun updateInstantResult() {
        if (!calculationEnabled || displayText == "0" || displayText.isBlank() ||
            calculatorMode == CalculatorMode.LIMITS ||
            calculatorMode == CalculatorMode.INTEGRATE ||
            calculatorMode == CalculatorMode.DIFFERENTIATE
        ) {
            resultText = ""
            return
        }

        if (!shouldPerformInstantCalculation(displayText)) {
            resultText = ""
            return
        }

        try {
            val result = CalcFuncs.calculateExpression(displayText)
            resultText = if (result.isNaN()) "" else CalcFuncs.formatResult(result)
        } catch (_: Exception) {
            resultText = ""
        }
    }

    private fun shouldPerformInstantCalculation(input: String): Boolean {
        val operators = setOf('+', '-', '×', '÷', '*', '/', '^', '%', '(', '√', 'π', 'e', 'x', 'y')
        val hasScientific = listOf(
            "sin",
            "cos",
            "tan",
            "log",
            "ln",
            "asin",
            "acos",
            "atan"
        ).any { input.contains(it) }
        return input.any { it in operators } || hasScientific
    }

    private fun calculateResult() {
        if (displayText.isEmpty() || displayText == "0") return

        if (calculatorMode == CalculatorMode.LIMITS) {
            runLimitCalculation()
            return
        }
        if (calculatorMode == CalculatorMode.INTEGRATE) {
            runIntegrationCalculation()
            return
        }
        if (calculatorMode == CalculatorMode.DIFFERENTIATE) {
            runDifferentiateCalculation()
            return
        }

        // Standard calculation logic
        if (resultText.isNotEmpty() && resultText != "Error") {
            displayText = resultText
            resultText = ""
            cursorIndex = displayText.length
        } else {
            try {
                val result = CalcFuncs.calculateExpression(displayText)
                if (!result.isNaN()) {
                    displayText = CalcFuncs.formatResult(result)
                    resultText = ""
                    cursorIndex = displayText.length
                }
            } catch (e: Exception) {
                displayText = "Error"
                resultText = ""
                cursorIndex = displayText.length
            }
        }
    }

    private fun runLimitCalculation() {
        if (displayText.isBlank() || targetText.isBlank()) return
        try {
            val res = LimitsFunc.calculateLimit(displayText, "x", targetText)
            val formatted = if (res.isNaN()) "DNE" else CalcFuncs.formatResult(res)

            lastExpression = displayText
            displayText = formatted
            cursorIndex = displayText.length
            resultText = ""
            isShowingResult = true
        } catch (e: Exception) {
            displayText = "Error"
            isShowingResult = false
        }
    }

    private fun runIntegrationCalculation() {
        if (displayText.isEmpty() || displayText == "0") return

        if (integType == IntegralType.DEFINITE) {
            try {
                // Evaluate limits first in case they are expressions like "pi" or "sqrt(2)"
                val lower = CalcFuncs.calculateExpression(lowerLimitText)
                val upper = CalcFuncs.calculateExpression(upperLimitText)

                if (lower.isNaN() || upper.isNaN()) {
                    resultText = "Invalid Limits"
                    return
                }

                val result = IntegFunc.integrate(displayText, lower, upper)
                if (result.isNaN()) {
                    resultText = "No convergence"
                } else {
                    resultText = CalcFuncs.formatResult(result)
                }
            } catch (e: Exception) {
                resultText = "Definite Error"
            }
        } else {
            try {
                val res = IntegFunc.integrateIndefinite(displayText)
                if (res.isNotEmpty()) {
                    resultText = res
                }
            } catch (e: Exception) {
                resultText = "Indefinite Error"
            }
        }
    }

    private fun runDifferentiateCalculation() {
        if (displayText.isEmpty() || displayText == "0") return
        try {
            analysisResult = AnalysisFunc.fullAnalysis(displayText)
            // Still set resultText for basic compatibility if needed
            val res = DiffFunc.differentiate(displayText)
            if (res.isNotEmpty()) {
                resultText = res
            }
        } catch (e: Exception) {
            resultText = "Error"
        }
    }

    private fun clearAll() {
        if (calculatorMode == CalculatorMode.LIMITS && isShowingResult) {
            displayText = lastExpression
            cursorIndex = displayText.length
            isShowingResult = false
            return
        }

        displayText = "0"
        cursorIndex = 1
        resultText = ""
        analysisResult = null
        targetText =
            if (calculatorMode == CalculatorMode.LIMITS && limitType == LimitType.INFINITE) "∞" else ""
        lowerLimitText = ""
        upperLimitText = ""
        isShowingResult = false
    }

    private fun switchLimitMode(type: LimitType) {
        limitType = type
        targetText = if (type == LimitType.INFINITE) "∞" else ""
        if (type == LimitType.FINITE) {
            currentFocus = CalculatorFocus.TARGET
        }
        resultText = ""
    }

    private fun switchIntegMode(type: IntegralType) {
        integType = type
        if (type == IntegralType.DEFINITE) {
            currentFocus = CalculatorFocus.INTEG_LOWER
        } else {
            currentFocus = CalculatorFocus.EXPRESSION
            lowerLimitText = ""
            upperLimitText = ""
        }
        resultText = ""
    }

    private fun handleBackspace() {
        if (calculatorMode == CalculatorMode.LIMITS && currentFocus == CalculatorFocus.TARGET) {
            if (targetText.isNotEmpty() && limitType == LimitType.FINITE) {
                targetText = targetText.dropLast(1)
            }
            return
        }

        if (calculatorMode == CalculatorMode.INTEGRATE) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER && lowerLimitText.isNotEmpty()) {
                lowerLimitText = lowerLimitText.dropLast(1)
                return
            }
            if (currentFocus == CalculatorFocus.INTEG_UPPER && upperLimitText.isNotEmpty()) {
                upperLimitText = upperLimitText.dropLast(1)
                return
            }
        }

        if (cursorIndex <= 0) return

        val tokens = listOf(
            " × asin(", "asin(", " × acos(", "acos(", " × atan(", "atan(",
            " × sin(", "sin(", " × cos(", "cos(", " × tan(", "tan(",
            " × log(", "log(", " × ln(", "ln(", " × √(", "√(",
            " × π", "π", " × e", "e",
            " ÷ ", " × ", " + ", " - ", " ^ ", "( )", "!", "÷", "×"
        )

        val textBeforeCursor = displayText.substring(0, cursorIndex)
        val matchedToken = tokens.find { textBeforeCursor.endsWith(it) }

        if (matchedToken != null) {
            val newTextBefore = textBeforeCursor.removeSuffix(matchedToken)
            val textAfter = displayText.substring(cursorIndex)
            displayText = newTextBefore + textAfter
            cursorIndex -= matchedToken.length
        } else {
            val newTextBefore = textBeforeCursor.dropLast(1)
            val textAfter = displayText.substring(cursorIndex)
            displayText = newTextBefore + textAfter
            cursorIndex -= 1
        }

        if (displayText.isEmpty()) {
            displayText = "0"
            cursorIndex = 1
        }
    }
}
