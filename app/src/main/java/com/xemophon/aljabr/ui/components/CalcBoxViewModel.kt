package com.xemophon.aljabr.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.calculus.differentiate.AnalysisFunc
import com.xemophon.aljabr.calculus.differentiate.AnalysisResult
import com.xemophon.aljabr.calculus.differentiate.DiffFunc
import com.xemophon.aljabr.calculus.integrate.IntegFunc
import com.xemophon.aljabr.calculus.limits.LimitsFunc
import com.xemophon.aljabr.data.SettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.app.Application

@Composable
fun CalcBox(
    expression: String,
    result: String,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    onCursorIndexChange: (Int) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        val expressionFontSize by animateFloatAsState(
            targetValue = if (expression.length > 12) 32f else 40f,
            label = "ExpressionFontSize"
        )

        Box(contentAlignment = Alignment.CenterEnd) {
            val textStyle = MaterialTheme.typography.displayMedium.copy(
                fontSize = expressionFontSize.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End
            )

            val annotatedExpression = buildAnnotatedString {
                if (cursorIndex != -1 && cursorIndex <= expression.length) {
                    append(expression.substring(0, cursorIndex))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                        append("|")
                    }
                    append(expression.substring(cursorIndex))
                } else {
                    append(expression)
                }
            }

            Text(
                text = annotatedExpression,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                lineHeight = expressionFontSize.sp * 1.1f,
                modifier = Modifier.clickable { onCursorIndexChange(expression.length) }
            )
        }

        AnimatedContent(
            targetState = result,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
            },
            label = "ResultAnimation"
        ) { targetResult ->
            if (targetResult.isNotEmpty()) {
                val resultFontSize by animateFloatAsState(
                    targetValue = if (targetResult.length > 8) 48f else 64f,
                    label = "ResultFontSize"
                )
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = targetResult,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = resultFontSize.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        lineHeight = resultFontSize.sp * 1.1f
                    )
                }
            }
        }
    }
}

enum class CalculatorMode { STANDARD, GRAPH, LIMITS, INTEGRATE, DIFFERENTIATE }
enum class CalculatorFocus { EXPRESSION, TARGET, INTEG_LOWER, INTEG_UPPER }

class CalcBoxViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    init {
        viewModelScope.launch {
            settingsRepository.useRadiansFlow.collectLatest {
                useRadians = it
                updateInstantResult()
            }
        }
    }

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
    var useRadians by mutableStateOf(false)

    private var lastExpression = ""
    private var isShowingResult = false

    fun handleAction(action: CalcButtonAction) {
        // Clear mode-specific results when any input button is pressed
        if (action !is CalcButtonAction.Calculate && action !is CalcButtonAction.Graph && action !is CalcButtonAction.Clear) {
            if (calculatorMode == CalculatorMode.INTEGRATE || calculatorMode == CalculatorMode.LIMITS) {
                resultText = ""
                isShowingResult = false
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

            is CalcButtonAction.Constant -> {
                handleConstant(action)
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

    fun toggleAngleUnit() {
        viewModelScope.launch {
            settingsRepository.setUseRadians(!useRadians)
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

        val isDigitOrDot = symbol.all { it.isDigit() || it == '.' }
        val lastChar = if (cursorIndex > 0) displayText[cursorIndex - 1] else null
        val isLastCharDigitOrDot = lastChar != null && (lastChar.isDigit() || lastChar == '.')
        
        val applyImplicit = isDigitOrDot && isImplicitMultiplicationNeeded() && !isLastCharDigitOrDot
        
        insertText(symbol, applyImplicitMultiplication = applyImplicit)
        isShowingResult = false
    }

    private fun insertText(toInsert: String, applyImplicitMultiplication: Boolean = false) {
        val prefix = if (applyImplicitMultiplication && isImplicitMultiplicationNeeded()) " × " else ""
        val finalInsert = "$prefix$toInsert"

        if (displayText == "0" && !finalInsert.startsWith(" × ")) {
            if (finalInsert == ".") {
                displayText = "0."
                cursorIndex = 2
            } else {
                displayText = finalInsert
                cursorIndex = finalInsert.length
            }
        } else {
            val sb = StringBuilder(displayText)
            sb.insert(cursorIndex, finalInsert)
            displayText = sb.toString()
            cursorIndex += finalInsert.length
        }
    }

    private fun isImplicitMultiplicationNeeded(): Boolean {
        if (displayText == "0") return false
        val lastChar = if (cursorIndex > 0) displayText[cursorIndex - 1] else null
        return lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'x' || lastChar == 'y' || lastChar == 'π' || lastChar == 'e' || lastChar == 'φ' || lastChar == 'i' || lastChar == '%')
    }

    private fun handleBrackets() {
        val openBrackets = displayText.count { it == '(' }
        val closedBrackets = displayText.count { it == ')' }

        if (openBrackets > closedBrackets && isImplicitMultiplicationNeeded()) {
            insertText(")", applyImplicitMultiplication = false)
        } else {
            insertText("(", applyImplicitMultiplication = true)
        }
    }

    private fun handlePercentage() {
        if (displayText != "0" && displayText != "Error") {
            insertText("%")
        }
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        if (calculatorMode == CalculatorMode.INTEGRATE && (currentFocus == CalculatorFocus.INTEG_LOWER || currentFocus == CalculatorFocus.INTEG_UPPER)) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) lowerLimitText += action.text else upperLimitText += action.text
            return
        }

        if (action.type == ScientificType.FACTORIAL) {
            if (displayText != "Error" && displayText != "NaN" && displayText != "Infinity") {
                insertText("!")
            }
            return
        }

        val toInsert = when (action.type) {
            ScientificType.SQRT -> "√("
            ScientificType.ASIN -> "asin("
            ScientificType.ACOS -> "acos("
            ScientificType.ATAN -> "atan("
            ScientificType.LN -> "ln("
            ScientificType.SIN -> "sin("
            ScientificType.COS -> "cos("
            ScientificType.TAN -> "tan("
            ScientificType.LOG -> "log("
            else -> "${action.text.lowercase()}("
        }

        insertText(toInsert, applyImplicitMultiplication = true)
    }

    private fun handleConstant(action: CalcButtonAction.Constant) {
        if (calculatorMode == CalculatorMode.INTEGRATE && (currentFocus == CalculatorFocus.INTEG_LOWER || currentFocus == CalculatorFocus.INTEG_UPPER)) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) lowerLimitText += action.text else upperLimitText += action.text
            return
        }

        val toInsert = when (action.type) {
            Constants.PI -> "π"
            Constants.E -> "e"
            Constants.PHI -> "φ"
            Constants.I -> "i"
        }

        insertText(toInsert, applyImplicitMultiplication = true)
    }

    private fun handleVariable(action: CalcButtonAction.Variable) {
        if (calculatorMode == CalculatorMode.INTEGRATE && (currentFocus == CalculatorFocus.INTEG_LOWER || currentFocus == CalculatorFocus.INTEG_UPPER)) {
            if (currentFocus == CalculatorFocus.INTEG_LOWER) lowerLimitText += action.text else upperLimitText += action.text
            return
        }

        insertText(action.text, applyImplicitMultiplication = true)
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
            val result = CalcFuncs.calculateSymbolic(displayText)
            resultText = if (result == "Error") "" else result
        } catch (_: Exception) {
            resultText = ""
        }
    }

    private fun shouldPerformInstantCalculation(input: String): Boolean {
        val operators = setOf('+', '-', '×', '÷', '*', '/', '^', '%', '(', '√', 'π', 'e', 'φ', 'i', 'x', 'y')
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
                val result = CalcFuncs.calculateSymbolic(displayText)
                if (result != "Error" && result.isNotEmpty()) {
                    displayText = result
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

            lastExpression = displayText
            resultText = res
            cursorIndex = -1
            isShowingResult = true
        } catch (e: Exception) {
            resultText = "Error"
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

                val result = IntegFunc.integrate(displayText, lower, upper, useRadians = useRadians)
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
            resultText = ""
            isShowingResult = false
            cursorIndex = displayText.length
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

        val textBefore = displayText.substring(0, cursorIndex)
        val textAfter = displayText.substring(cursorIndex)

        val tokens = listOf(
            " × asin(", "asin(", " × acos(", "acos(", " × atan(", "atan(",
            " × sin(", "sin(", " × cos(", "cos(", " × tan(", "tan(",
            " × log(", "log(", " × ln(", "ln(", " × √(", "√(",
            " × π", "π", " × e", "e", " × φ", "φ", " × i", "i",
            " ÷ ", " × ", " + ", " - ", " ^ ", "( )", "!", "÷", "×"
        )

        val matchedToken = tokens.find { textBefore.endsWith(it) }
        val dropCount = matchedToken?.length ?: 1

        displayText = textBefore.dropLast(dropCount) + textAfter
        cursorIndex -= dropCount

        if (displayText.isEmpty()) {
            displayText = "0"
            cursorIndex = 1
        }
    }
}
