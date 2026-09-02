package com.xemophon.aljabr.ui.components.screens

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.xemophon.aljabr.modules.algebra.polynomials.PolyFuncs
import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import com.xemophon.aljabr.modules.calculus.differentiate.DiffFunc
import com.xemophon.aljabr.modules.graphMaker.GraphGenerator
import com.xemophon.aljabr.modules.calculus.integrate.IntegFunc
import com.xemophon.aljabr.modules.calculus.limits.LimitsFunc
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.Constants
import com.xemophon.aljabr.ui.components.buttons.IntegralType
import com.xemophon.aljabr.ui.components.buttons.LimitType
import com.xemophon.aljabr.ui.components.buttons.ScientificType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalcBox(
    expression: String,
    result: String,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    onCursorIndexChange: (Int) -> Unit = {},
    showStepsButton: Boolean = false,
    onShowStepsClick: () -> Unit = {}
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
        horizontalAlignment = Alignment.End,
    ) {
        if (showStepsButton) {
            IconButton(
                onClick = onShowStepsClick,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Show Steps",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                val expressionFontSize by animateFloatAsState(
                    targetValue = if (expression.length > 12) 32f else 40f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "ExpressionFontSize"
                )

                val textStyle = MaterialTheme.typography.displayMedium.copy(
                    fontSize = expressionFontSize.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End
                )

                val annotatedExpression = buildAnnotatedString {
                    if (cursorIndex != -1 && cursorIndex <= expression.length) {
                        append(expression.substring(0, cursorIndex))
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = cursorAlpha
                                )
                            )
                        ) {
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
                    lineHeight = expressionFontSize.sp * 1.1f,
                    modifier = Modifier.clickable { onCursorIndexChange(expression.length) }
                )
            }
        }

        AnimatedContent(
            targetState = result,
            transitionSpec = {
                val isAppearing = targetState.isNotEmpty() && initialState.isEmpty()
                val isDisappearing = targetState.isEmpty() && initialState.isNotEmpty()

                if (isAppearing) {
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } +
                            fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.92f))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                } else if (isDisappearing) {
                    fadeIn(animationSpec = tween(90))
                        .togetherWith(
                            slideOutVertically { it / 3 } +
                                    fadeOut(animationSpec = tween(180)) +
                                    scaleOut(targetScale = 0.92f)
                        )
                } else {
                    // Directional scroll based on numerical change
                    val isIncreasing = (targetState.toDoubleOrNull() ?: 0.0) >= (initialState.toDoubleOrNull() ?: 0.0)
                    val slideOffset = { height: Int -> if (isIncreasing) height else -height }

                    (slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = slideOffset
                    ) + fadeIn(animationSpec = tween(150)))
                        .togetherWith(
                            slideOutVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetY = { -slideOffset(it) }
                            ) + fadeOut(animationSpec = tween(150))
                        )
                } using SizeTransform(clip = false)
            },
            label = "ResultAnimation",
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) { targetResult ->
            if (targetResult.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val resultFontSize by animateFloatAsState(
                        targetValue = if (targetResult.length > 8) 48f else 64f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ResultFontSize"
                    )

                    Text(
                        text = targetResult,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = resultFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

enum class CalculatorMode { STANDARD, GRAPH, LIMITS, INTEGRATE, DIFFERENTIATE, POLYNOMIALS, TAYLOR }
enum class CalculatorFocus { EXPRESSION, TARGET, INTEG_LOWER, INTEG_UPPER, ORDER }

class CalcBoxViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    var precision by mutableIntStateOf(4)
        private set

    var useRationalize by mutableStateOf(false)

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

    var orderText by mutableStateOf("5")
        private set

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var polynomialResult by mutableStateOf<PolynomialResult?>(null)
        private set

    var diffGridMode by mutableStateOf("Single") // "Single" or "Multiple"

    var currentFocus by mutableStateOf(CalculatorFocus.EXPRESSION)
        private set

    var calculatorMode by mutableStateOf(CalculatorMode.STANDARD)
    var limitType by mutableStateOf(LimitType.FINITE)
    var integType by mutableStateOf(IntegralType.DEFINITE)
    var calculationEnabled by mutableStateOf(value = true)
    var useRadians by mutableStateOf(false)
    var showSteps by mutableStateOf(false)
        private set
    var autoClearCache by mutableStateOf(false)
        private set

    var showStepsSheet by mutableStateOf(false)

    var integrationAxis by mutableStateOf("X") // "X" or "Y"

    var isCalculatingSteps by mutableStateOf(false)
        private set

    val stepsList = mutableStateListOf<CalculusStep>()

    private var lastExpression = ""
    private var isShowingResult = false

    fun handleAction(action: CalcButtonAction) {
        // Clear mode-specific results when any input button is pressed
        if (action !is CalcButtonAction.Calculate && action !is CalcButtonAction.Graph && action !is CalcButtonAction.Clear) {
            if (calculatorMode == CalculatorMode.INTEGRATE || calculatorMode == CalculatorMode.LIMITS || 
                calculatorMode == CalculatorMode.POLYNOMIALS || calculatorMode == CalculatorMode.TAYLOR) {
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
                val isSameType = when (action.type) {
                    IntegralType.XVOL, IntegralType.YVOL -> integType == IntegralType.XVOL || integType == IntegralType.YVOL
                    IntegralType.XSURF, IntegralType.YSURF -> integType == IntegralType.XSURF || integType == IntegralType.YSURF
                    else -> action.type == integType
                }

                if (isSameType && (action.type == IntegralType.XVOL || action.type == IntegralType.YVOL ||
                            action.type == IntegralType.XSURF || action.type == IntegralType.YSURF)) {
                    integrationAxis = if (integrationAxis == "X") "Y" else "X"
                    integType = when (integType) {
                        IntegralType.XVOL -> IntegralType.YVOL
                        IntegralType.YVOL -> IntegralType.XVOL
                        IntegralType.XSURF -> IntegralType.YSURF
                        IntegralType.YSURF -> IntegralType.XSURF
                        else -> integType
                    }
                } else {
                    switchIntegMode(action.type)
                }
            }

            is CalcButtonAction.Graph -> { /* Handled in UI */
            }

            is CalcButtonAction.Differentiate -> {
                diffGridMode = "Multiple"
            }

            is CalcButtonAction.DifferentiateSingle -> {
                diffGridMode = "Single"
            }
            CalcButtonAction.Done -> { /* TODO */ }
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

        if (calculatorMode == CalculatorMode.TAYLOR) {
            if (currentFocus == CalculatorFocus.TARGET) {
                if (symbol.matches(Regex("[0-9.-]+"))) targetText += symbol
                return
            }
            if (currentFocus == CalculatorFocus.ORDER) {
                if (symbol.matches(Regex("[0-9]+"))) orderText += symbol
                return
            }
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
        if (cursorIndex == -1) {
            cursorIndex = displayText.length
            currentFocus = CalculatorFocus.EXPRESSION
        }

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
        return lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'x' || lastChar == 'y' || lastChar == 'π' || lastChar == 'e' || lastChar == 'φ' || lastChar == 'j' || lastChar == 'i' || lastChar == '%')
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
            Constants.I -> "j"
            Constants.INF -> "∞"
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
            calculatorMode == CalculatorMode.DIFFERENTIATE ||
            calculatorMode == CalculatorMode.TAYLOR
        ) {
            resultText = ""
            return
        }

        if (!shouldPerformInstantCalculation(displayText)) {
            resultText = ""
            return
        }

        try {
            val result = if (useRationalize || displayText.contains("j", ignoreCase = true)) {
                SymjaUtils.calculateNumerical(displayText, useRadians, useRationalize, precision)
            } else {
                val numResult = CalcFuncs.calculateExpression(displayText, useRadians = useRadians)
                CalcFuncs.formatResult(numResult, precision)
            }
            resultText = if (result == "Error") "" else result
        } catch (_: Exception) {
            resultText = ""
        }
    }

    private fun shouldPerformInstantCalculation(input: String): Boolean {
        val operators = setOf('+', '-', '×', '÷', '*', '/', '^', '%', '(', '√', 'π', 'e', 'φ', 'j', 'i', 'x', 'y')
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
        if (calculatorMode == CalculatorMode.POLYNOMIALS) {
            runPolynomialCalculation()
            return
        }
        if (calculatorMode == CalculatorMode.TAYLOR) {
            runTaylorCalculation()
            return
        }

        stepsList.clear()

        // Standard calculation logic - No steps needed
        if (resultText.isNotEmpty() && resultText != "Error") {
            displayText = resultText
            resultText = ""
            cursorIndex = displayText.length
        } else {
            try {
                val result = if (useRationalize || displayText.contains("j", ignoreCase = true)) {
                    SymjaUtils.calculateNumerical(displayText, useRadians, useRationalize, precision)
                } else {
                    val numResult = CalcFuncs.calculateExpression(displayText, useRadians = useRadians)
                    CalcFuncs.formatResult(numResult, precision)
                }
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
        stepsList.clear()

        try {
            val res = LimitsFunc.calculateLimit(displayText, "x", targetText, useRationalize)

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

        stepsList.clear()

        if (integType != IntegralType.INDEFINITE) {
            if (useRationalize) {
                // Symbolic approach preserves fractions
                try {
                    val res = IntegFunc.integrateSymbolic(
                        expression = displayText,
                        lower = lowerLimitText,
                        upper = upperLimitText,
                        useRadians = useRadians,
                        useRationalize = true,
                        type = integType
                    )
                    resultText = res
                } catch (e: Exception) {
                    resultText = "Calculation Error"
                }
            } else {
                try {
                    // Evaluate limits first in case they are expressions like "pi" or "sqrt(2)"
                    val lower = CalcFuncs.calculateExpression(lowerLimitText)
                    val upper = CalcFuncs.calculateExpression(upperLimitText)

                    if (lower.isNaN() || upper.isNaN()) {
                        resultText = "Invalid Limits"
                        return
                    }

                    val result = IntegFunc.integrate(
                        expression = displayText,
                        lower = lower,
                        upper = upper,
                        useRadians = useRadians,
                        type = integType
                    )
                    if (result.isNaN()) {
                        resultText = "No convergence"
                    } else {
                        resultText = CalcFuncs.formatResult(result, precision)
                    }
                } catch (e: Exception) {
                    resultText = "Calculation Error"
                }
            }
        } else {
            if (showSteps) {
                viewModelScope.launch {
                    isCalculatingSteps = true
                    try {
                        val resultAndSteps = withContext(Dispatchers.Default) {
                            IntegFunc.integrateIndefiniteWithSteps(displayText, showSteps)
                        }
                        
                        if (resultAndSteps != null) {
                            val (result, steps) = resultAndSteps
                            if (result.isNotEmpty()) {
                                resultText = result
                                if (steps.isNotEmpty()) {
                                    stepsList.addAll(steps)
                                    showStepsSheet = true
                                }
                            }
                        } else {
                            // Fallback to standard integration without steps
                            val res = IntegFunc.integrateIndefinite(displayText, useRationalize)
                            if (res.isNotEmpty()) {
                                resultText = res
                            }
                        }
                    } catch (e: Exception) {
                        resultText = "Indefinite Error"
                    } finally {
                        isCalculatingSteps = false
                    }
                }
            } else {
                try {
                    val res = IntegFunc.integrateIndefinite(displayText, useRationalize)
                    if (res.isNotEmpty()) {
                        resultText = res
                    }
                } catch (e: Exception) {
                    resultText = "Indefinite Error"
                }
            }
        }
    }

    private fun runDifferentiateCalculation() {
        if (displayText.isEmpty() || displayText == "0") return
        stepsList.clear()
        
        if (showSteps) {
            viewModelScope.launch {
                isCalculatingSteps = true
                showStepsSheet = true
                try {
                    val (result, steps) = withContext(Dispatchers.Default) {
                        DiffFunc.differentiateWithSteps(displayText, showSteps)
                    }
                    if (result.isNotEmpty()) {
                        resultText = result
                        if (steps.isNotEmpty()) {
                            stepsList.addAll(steps)
                            showStepsSheet = true
                        }
                    }
                    isShowingResult = true
                } catch (e: Exception) {
                    resultText = "Error"
                } finally {
                    isCalculatingSteps = false
                }
            }
        } else {
            try {
                analysisResult = AnalysisFunc.fullAnalysis(displayText)
                val res = DiffFunc.differentiate(displayText, useRationalize)
                if (res.isNotEmpty()) {
                    resultText = res
                }
                isShowingResult = true
            } catch (e: Exception) {
                resultText = "Error"
            }
        }
    }

    fun runFullAnalysis() {
        if (displayText.isEmpty() || displayText == "0") return
        viewModelScope.launch {
            try {
                analysisResult = withContext(Dispatchers.Default) {
                    AnalysisFunc.fullAnalysis(displayText)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    private fun runPolynomialCalculation() {
        if (displayText.isEmpty() || displayText == "0") return
        viewModelScope.launch {
            try {
                polynomialResult = PolyFuncs.analyzePolynomial(displayText, useRationalize)
                isShowingResult = true
            } catch (e: Exception) {
                // resultText = "Error"
            }
        }
    }

    private fun runTaylorCalculation() {
        if (displayText.isBlank()) return
        stepsList.clear()

        try {
            val ord = orderText.toIntOrNull() ?: 5
            val res = SymjaUtils.calculateTaylor(displayText, targetText, ord)

            if (res != "Error" && res.isNotEmpty()) {
                resultText = res
                isShowingResult = true
            }
        } catch (e: Exception) {
            resultText = "Error"
        }
    }

    private fun clearAll() {
        if ((calculatorMode == CalculatorMode.LIMITS || calculatorMode == CalculatorMode.POLYNOMIALS || calculatorMode == CalculatorMode.TAYLOR) && isShowingResult) {
            resultText = ""
            isShowingResult = false
            polynomialResult = null
            cursorIndex = displayText.length
            return
        }

        displayText = "0"
        cursorIndex = 1
        resultText = ""
        analysisResult = null
        polynomialResult = null
        targetText =
            if (calculatorMode == CalculatorMode.LIMITS && limitType == LimitType.INFINITE) "∞" else ""
        lowerLimitText = ""
        upperLimitText = ""
        orderText = "5"
        isShowingResult = false

        // Clear App Cache and Graph Cache if enabled
        GraphGenerator.clearCache()
        if (autoClearCache) {
            StorageUtils.clearAppCache(getApplication())
        }
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
        if (type == IntegralType.DEFINITE || type == IntegralType.ARC ||
            type == IntegralType.XVOL || type == IntegralType.YVOL ||
            type == IntegralType.XSURF || type == IntegralType.YSURF) {
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

        if (calculatorMode == CalculatorMode.TAYLOR) {
            if (currentFocus == CalculatorFocus.TARGET && targetText.isNotEmpty()) {
                targetText = targetText.dropLast(1)
                return
            }
            if (currentFocus == CalculatorFocus.ORDER && orderText.isNotEmpty()) {
                orderText = orderText.dropLast(1)
                return
            }
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
            " × π", "π", " × e", "e", " × φ", "φ", " × j", "j", " × i", "i",
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

    init {
        viewModelScope.launch {
            settingsRepository.useRadiansFlow.collectLatest {
                useRadians = it
                updateInstantResult()
            }
        }
        viewModelScope.launch {
            settingsRepository.precisionFlow.collectLatest {
                precision = it
                updateInstantResult()
            }
        }
        viewModelScope.launch {
            settingsRepository.showStepsFlow.collectLatest {
                showSteps = it
            }
        }
        viewModelScope.launch {
            settingsRepository.autoClearCacheFlow.collectLatest {
                autoClearCache = it
            }
        }
        viewModelScope.launch {
            settingsRepository.useRationalizeFlow.collectLatest {
                useRationalize = it
                updateInstantResult()
            }
        }
    }
}
