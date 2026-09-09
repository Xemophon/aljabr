package com.xemophon.aljabr.ui.components.engine

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import com.xemophon.aljabr.modules.graphMaker.GraphGenerator
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.Constants
import com.xemophon.aljabr.ui.components.buttons.IntegralType
import com.xemophon.aljabr.ui.components.buttons.LimitType
import com.xemophon.aljabr.ui.components.buttons.ScientificType
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import com.xemophon.aljabr.ui.components.screens.AnalysisResult
import com.xemophon.aljabr.ui.components.screens.CalculusStep
import com.xemophon.aljabr.ui.components.screens.PolynomialResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class CalculatorMode { STANDARD, GRAPH, LIMITS, INTEGRATE, DIFFERENTIATE, POLYNOMIALS, TAYLOR, LAPLACE, ODE }
enum class CalculatorFocus { EXPRESSION, TARGET, INTEG_LOWER, INTEG_UPPER, INTEG_INNER_LOWER, INTEG_INNER_UPPER, ORDER }

class CalcBoxViewModel @JvmOverloads constructor(
    application: Application,
    odeStateHolder: OdeStateHolder = DefaultOdeStateHolder()
) : AndroidViewModel(application), OdeStateHolder by odeStateHolder {

    private val settingsRepository = SettingsRepository(application)

    val isOdeConditionFocused: Boolean
        get() = isOdeConditionFocused(calculatorMode)

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

    var innerLowerLimitText by mutableStateOf("")
        private set

    var innerUpperLimitText by mutableStateOf("")
        private set

    var orderText by mutableStateOf("5")
        private set

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var polynomialResult by mutableStateOf<PolynomialResult?>(null)
        private set

    var isCalculating by mutableStateOf(false)
        private set

    var diffGridMode by mutableStateOf("Single") // "Single" or "Multiple"
    var laplaceMode by mutableStateOf("Laplace") // "Laplace" or "Reverse"

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
        if (action !is CalcButtonAction.Calculate && action !is CalcButtonAction.Graph && action !is CalcButtonAction.Clear) {
            if (calculatorMode == CalculatorMode.INTEGRATE || calculatorMode == CalculatorMode.LIMITS ||
                calculatorMode == CalculatorMode.POLYNOMIALS || calculatorMode == CalculatorMode.TAYLOR ||
                calculatorMode == CalculatorMode.LAPLACE || calculatorMode == CalculatorMode.ODE) {
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
                when (action.formula) {
                    "( )", "()" -> handleBrackets()
                    "%" -> handlePercentage()
                    else -> handleSymbol(action.formula)
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

            is CalcButtonAction.Misc -> {
                handleSymbol(action.text)
                updateInstantResult()
            }

            is CalcButtonAction.Limits -> {
                switchLimitMode(action.type)
            }

            is CalcButtonAction.Integrals -> {
                val isSameType = when (action.type) {
                    IntegralType.XVOL, IntegralType.YVOL -> integType == IntegralType.XVOL || integType == IntegralType.YVOL
                    IntegralType.XSURF, IntegralType.YSURF -> integType == IntegralType.XSURF || integType == IntegralType.YSURF
                    IntegralType.DOUBLE, IntegralType.NDOUBLE -> integType == IntegralType.DOUBLE || integType == IntegralType.NDOUBLE
                    else -> action.type == integType
                }

                if (isSameType && (action.type == IntegralType.XVOL || action.type == IntegralType.YVOL ||
                            action.type == IntegralType.XSURF || action.type == IntegralType.YSURF || action.type == IntegralType.NDOUBLE)) {
                    integrationAxis = if (integrationAxis == "X") "Y" else "X"
                    if (action.type == IntegralType.DOUBLE || action.type == IntegralType.NDOUBLE) {
                        integType = action.type
                    } else {
                        integType = when (integType) {
                            IntegralType.XVOL -> IntegralType.YVOL
                            IntegralType.YVOL -> IntegralType.XVOL
                            IntegralType.XSURF -> IntegralType.YSURF
                            IntegralType.YSURF -> IntegralType.XSURF
                            else -> integType
                        }
                    }
                } else {
                    switchIntegMode(action.type)
                }
            }

            is CalcButtonAction.Graph -> { /* Handled in UI */ }

            is CalcButtonAction.Differentiate -> {
                diffGridMode = "Multiple"
            }

            is CalcButtonAction.DifferentiateSingle -> {
                diffGridMode = "Single"
            }

            is CalcButtonAction.DifferentiateComplex -> {
                diffGridMode = "Complex"
            }
            is CalcButtonAction.Laplace -> {
                laplaceMode = action.text
            }
            CalcButtonAction.Done -> { /* Handled in UI */ }

            else -> {}
        }
    }

    fun toggleAngleUnit() {
        viewModelScope.launch {
            settingsRepository.setUseRadians(!useRadians)
        }
    }

    fun setFocus(focus: CalculatorFocus) {
        currentFocus = focus
        setOdeConditionFocus(-1)
        if (resultText.isNotEmpty()) resultText = ""
        if (focus == CalculatorFocus.EXPRESSION) {
            cursorIndex = displayText.length
        } else {
            cursorIndex = -1
        }
    }

    fun updateCursorIndex(index: Int) {
        cursorIndex = index.coerceIn(0, displayText.length)
        if (cursorIndex != -1) {
            currentFocus = CalculatorFocus.EXPRESSION
            setOdeConditionFocus(-1)
        }
    }

    private fun handleSymbol(symbol: String) {
        if (isOdeConditionFocused) {
            odeConditions[odeConditionFocusIndex] += symbol
            return
        }

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
            when (currentFocus) {
                CalculatorFocus.INTEG_LOWER -> {
                    lowerLimitText += symbol
                    return
                }
                CalculatorFocus.INTEG_UPPER -> {
                    upperLimitText += symbol
                    return
                }
                CalculatorFocus.INTEG_INNER_LOWER -> {
                    innerLowerLimitText += symbol
                    return
                }
                CalculatorFocus.INTEG_INNER_UPPER -> {
                    innerUpperLimitText += symbol
                    return
                }
                else -> {}
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
            resultText = ""
            isShowingResult = false
        }

        val state = MathInputHandler.insertText(
            currentText = displayText,
            cursorIndex = cursorIndex,
            toInsert = toInsert,
            applyImplicitMultiplication = applyImplicitMultiplication
        )
        displayText = state.text
        cursorIndex = state.cursorIndex
    }

    private fun isImplicitMultiplicationNeeded(): Boolean {
        return MathInputHandler.isImplicitMultiplicationNeeded(displayText, cursorIndex)
    }

    private fun handleBrackets() {
        if (isOdeConditionFocused) {
            val currentCond = odeConditions[odeConditionFocusIndex]
            val openBrackets = currentCond.count { it == '(' }
            val closedBrackets = currentCond.count { it == ')' }
            val toInsert = if (openBrackets > closedBrackets && currentCond.isNotEmpty() && (currentCond.last().isDigit() || currentCond.last() == 'y' || currentCond.last() == '\'')) {
                ")"
            } else {
                "("
            }
            odeConditions[odeConditionFocusIndex] += toInsert
            return
        }

        if (cursorIndex == -1) {
            cursorIndex = displayText.length
            currentFocus = CalculatorFocus.EXPRESSION
        }
        val state = MathInputHandler.handleBrackets(displayText, cursorIndex)
        displayText = state.text
        cursorIndex = state.cursorIndex
    }

    private fun handlePercentage() {
        if (isOdeConditionFocused) {
            odeConditions[odeConditionFocusIndex] += "%"
            return
        }

        if (displayText != "Error") {
            insertText("%")
        }
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
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
            ScientificType.ABS -> "abs("
            else -> "${action.text.lowercase()}("
        }

        if (isOdeConditionFocused) {
            odeConditions[odeConditionFocusIndex] += toInsert
            return
        }

        if (calculatorMode == CalculatorMode.INTEGRATE && (
                    currentFocus == CalculatorFocus.INTEG_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_UPPER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_UPPER)) {
            when (currentFocus) {
                CalculatorFocus.INTEG_LOWER -> lowerLimitText += toInsert
                CalculatorFocus.INTEG_UPPER -> upperLimitText += toInsert
                CalculatorFocus.INTEG_INNER_LOWER -> innerLowerLimitText += toInsert
                CalculatorFocus.INTEG_INNER_UPPER -> innerUpperLimitText += toInsert
                else -> {}
            }
            return
        }

        if (action.type == ScientificType.FACTORIAL) {
            if (displayText != "Error" && displayText != "NaN" && displayText != "Infinity") {
                insertText("!")
            }
            return
        }

        insertText(toInsert, applyImplicitMultiplication = true)
    }

    private fun handleConstant(action: CalcButtonAction.Constant) {
        if (isOdeConditionFocused) {
            val toInsert = when (action.type) {
                Constants.PI -> "π"
                Constants.E -> "e"
                Constants.PHI -> "φ"
                Constants.I -> "j"
                Constants.INF -> "∞"
            }
            odeConditions[odeConditionFocusIndex] += toInsert
            return
        }

        if (calculatorMode == CalculatorMode.INTEGRATE && (
                    currentFocus == CalculatorFocus.INTEG_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_UPPER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_UPPER)) {
            when (currentFocus) {
                CalculatorFocus.INTEG_LOWER -> lowerLimitText += action.text
                CalculatorFocus.INTEG_UPPER -> upperLimitText += action.text
                CalculatorFocus.INTEG_INNER_LOWER -> innerLowerLimitText += action.text
                CalculatorFocus.INTEG_INNER_UPPER -> innerUpperLimitText += action.text
                else -> {}
            }
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
        if (isOdeConditionFocused) {
            odeConditions[odeConditionFocusIndex] += action.text
            return
        }

        if (calculatorMode == CalculatorMode.INTEGRATE && (
                    currentFocus == CalculatorFocus.INTEG_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_UPPER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_LOWER ||
                    currentFocus == CalculatorFocus.INTEG_INNER_UPPER)) {
            when (currentFocus) {
                CalculatorFocus.INTEG_LOWER -> lowerLimitText += action.text
                CalculatorFocus.INTEG_UPPER -> upperLimitText += action.text
                CalculatorFocus.INTEG_INNER_LOWER -> innerLowerLimitText += action.text
                CalculatorFocus.INTEG_INNER_UPPER -> innerUpperLimitText += action.text
                else -> {}
            }
            return
        }

        val varText = if (calculatorMode == CalculatorMode.LAPLACE) {
            if (laplaceMode == "Reverse") "s" else "t"
        } else {
            action.text
        }

        insertText(varText, applyImplicitMultiplication = true)
    }

    private fun hasImaginaryUnit(input: String): Boolean {
        return input.contains("j", ignoreCase = true) || input.contains("i", ignoreCase = true)
    }

    private fun hasEmptyFunctions(input: String): Boolean {
        if (input.isBlank()) return false
        val funcs = listOf(
            "arcsinh", "arccosh", "arctanh", "asinh", "acosh", "atanh",
            "arcsin", "arccos", "arctan", "asin", "acos", "atan",
            "sinh", "cosh", "tanh", "sin", "cos", "tan",
            "log10", "log", "ln", "abs", "sqrt", "√"
        )

        for (func in funcs) {
            var index = 0
            while (index < input.length) {
                val found = input.indexOf(func, index, ignoreCase = true)
                if (found == -1) break

                val isStartValid = found == 0 || !input[found - 1].isLetter()
                if (isStartValid) {
                    var endPos = found + func.length
                    if (endPos < input.length && input[endPos] == '(') {
                        endPos++
                    }

                    var argString = input.substring(endPos)
                    val closeParen = argString.indexOf(')')
                    if (closeParen != -1) {
                        argString = argString.substring(0, closeParen)
                    }

                    val hasOperand = argString.any { ch ->
                        ch.isDigit() || ch in setOf('π', 'e', 'φ', 'j', 'i', 'x', 'y', 'z', 't', 's')
                    } || listOf("pi", "phi", "inf", "infinity").any { argString.contains(it, ignoreCase = true) }

                    if (!hasOperand) {
                        return true
                    }
                }
                index = found + func.length
            }
        }
        return false
    }

    private fun isUnwantedFunctionOutput(res: String): Boolean {
        if (res.isBlank()) return true
        val lower = res.lowercase()
        return lower.contains("sindeg") ||
               lower.contains("cosdeg") ||
               lower.contains("tandeg") ||
               lower.contains("arcsindeg") ||
               lower.contains("arccosdeg") ||
               lower.contains("arctandeg") ||
               lower.contains("sin()") ||
               lower.contains("cos()") ||
               lower.contains("tan()") ||
               lower.contains("asin()") ||
               lower.contains("acos()") ||
               lower.contains("atan()") ||
               lower.contains("sqrt()") ||
               lower.contains("log()") ||
               lower.contains("ln()") ||
               lower.contains("√()")
    }

    private fun computeBasicResult(input: String): String {
        if (hasEmptyFunctions(input)) {
            return ""
        }
        val res = if (useRationalize || hasImaginaryUnit(input)) {
            SymjaUtils.calculateNumerical(input, useRadians, useRationalize, precision)
        } else {
            val numResult = CalcFuncs.calculateExpression(input, useRadians = useRadians)
            if (numResult.isNaN()) {
                SymjaUtils.calculateNumerical(input, useRadians, useRationalize, precision)
            } else {
                CalcFuncs.formatResult(numResult, precision)
            }
        }
        return if (isUnwantedFunctionOutput(res)) "" else res
    }

    private fun updateInstantResult() {
        if (!calculationEnabled || displayText == "0" || displayText.isBlank() ||
            calculatorMode == CalculatorMode.LIMITS ||
            calculatorMode == CalculatorMode.INTEGRATE ||
            calculatorMode == CalculatorMode.DIFFERENTIATE ||
            calculatorMode == CalculatorMode.TAYLOR ||
            calculatorMode == CalculatorMode.POLYNOMIALS ||
            calculatorMode == CalculatorMode.LAPLACE ||
            calculatorMode == CalculatorMode.ODE
        ) {
            resultText = ""
            return
        }

        if (!shouldPerformInstantCalculation(displayText)) {
            resultText = ""
            return
        }

        try {
            val result = computeBasicResult(displayText)
            resultText = if (result == "Error") "" else result
        } catch (_: Exception) {
            resultText = ""
        }
    }

    private fun shouldPerformInstantCalculation(input: String): Boolean {
        if (hasEmptyFunctions(input)) {
            return false
        }
        val operators = setOf('+', '-', '×', '÷', '*', '/', '^', '%', '(', '√', 'π', 'e', 'φ', 'j', 'i', 'x', 'y')
        val hasScientific = listOf(
            "sin", "cos", "tan", "log", "ln", "asin", "acos", "atan",
            "abs", "sinh", "cosh", "tanh", "asinh", "acosh", "atanh", "sqrt"
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
        if (calculatorMode == CalculatorMode.LAPLACE) {
            runLaplaceCalculation()
            return
        }
        if (calculatorMode == CalculatorMode.ODE) {
            runOdeCalculation()
            return
        }

        stepsList.clear()

        if (resultText.isNotEmpty() && resultText != "Error") {
            displayText = resultText
            resultText = ""
            cursorIndex = displayText.length
        } else {
            try {
                val result = computeBasicResult(displayText)
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
        isCalculating = true
        viewModelScope.launch {
            try {
                val res = LimitsEngine.calculateLimit(displayText, targetText, useRationalize)
                lastExpression = displayText
                resultText = res
                cursorIndex = -1
                isShowingResult = true
            } catch (e: Exception) {
                resultText = "Error"
                isShowingResult = false
            } finally {
                isCalculating = false
            }
        }
    }

    private fun runIntegrationCalculation() {
        if (displayText.isEmpty() || displayText == "0") return

        stepsList.clear()

        when (integType) {
            IntegralType.CURVET2 -> {
                try {
                    resultText = IntegrationEngine.integrateCurve(
                        displayText = displayText,
                        innerLowerLimitText = innerLowerLimitText,
                        lowerLimitText = lowerLimitText,
                        upperLimitText = upperLimitText,
                        useRadians = useRadians,
                        useRationalize = useRationalize,
                        precision = precision,
                        integType = integType
                    )
                } catch (e: Exception) {
                    resultText = "Calculation Error"
                }
            }
            IntegralType.DOUBLE -> {
                try {
                    val res = IntegrationEngine.integrateDoubleIndefinite(
                        displayText = displayText,
                        useRadians = useRadians,
                        useRationalize = useRationalize
                    )
                    if (res.isNotEmpty()) {
                        resultText = res
                    }
                } catch (e: Exception) {
                    resultText = "Indefinite Error"
                }
            }
            IntegralType.NDOUBLE -> {
                try {
                    resultText = IntegrationEngine.integrateDoubleDefinite(
                        displayText = displayText,
                        lowerLimitText = lowerLimitText,
                        upperLimitText = upperLimitText,
                        innerLowerLimitText = innerLowerLimitText,
                        innerUpperLimitText = innerUpperLimitText,
                        axis = integrationAxis,
                        useRadians = useRadians,
                        useRationalize = useRationalize,
                        precision = precision
                    )
                } catch (e: Exception) {
                    resultText = "Calculation Error"
                }
            }
            IntegralType.INDEFINITE -> {
                if (showSteps) {
                    viewModelScope.launch {
                        isCalculatingSteps = true
                        try {
                            val resultAndSteps = IntegrationEngine.integrateIndefiniteWithSteps(displayText, showSteps)
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
                                val res = IntegrationEngine.integrateIndefinite(displayText, useRationalize)
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
                        val res = IntegrationEngine.integrateIndefinite(displayText, useRationalize)
                        if (res.isNotEmpty()) {
                            resultText = res
                        }
                    } catch (e: Exception) {
                        resultText = "Indefinite Error"
                    }
                }
            }
            else -> {
                try {
                    resultText = IntegrationEngine.integrateStandard(
                        displayText = displayText,
                        lowerLimitText = lowerLimitText,
                        upperLimitText = upperLimitText,
                        useRadians = useRadians,
                        useRationalize = useRationalize,
                        precision = precision,
                        integType = integType
                    )
                } catch (e: Exception) {
                    resultText = "Calculation Error"
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
                    val (result, steps) = DifferentiationEngine.differentiateWithSteps(displayText, showSteps)
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
            isCalculating = true
            viewModelScope.launch {
                try {
                    val (res, analysis) = DifferentiationEngine.differentiateWithAnalysis(
                        displayText,
                        diffGridMode,
                        useRationalize
                    )
                    analysisResult = analysis
                    if (res.isNotEmpty()) {
                        resultText = res
                    }
                    isShowingResult = true
                } catch (e: Exception) {
                    resultText = "Error"
                } finally {
                    isCalculating = false
                }
            }
        }
    }

    fun runFullAnalysis() {
        if (displayText.isEmpty() || displayText == "0") return
        isCalculating = true
        viewModelScope.launch {
            try {
                analysisResult = DifferentiationEngine.fullAnalysis(displayText, diffGridMode)
            } catch (_: Exception) {
            } finally {
                isCalculating = false
            }
        }
    }

    private fun runPolynomialCalculation() {
        if (displayText.isEmpty() || displayText == "0") return
        isCalculating = true
        viewModelScope.launch {
            try {
                polynomialResult = PolynomialEngine.analyzePolynomial(displayText, useRationalize)
                isShowingResult = true
            } catch (_: Exception) {
            } finally {
                isCalculating = false
            }
        }
    }

    private fun runOdeCalculation() {
        if (displayText.isEmpty() || displayText == "0") return
        isCalculating = true
        viewModelScope.launch {
            try {
                BDEResult = OdeEngine.solveOde(displayText, odeConditions)
                isShowingResult = true
            } catch (_: Exception) {
            } finally {
                isCalculating = false
            }
        }
    }

    private fun runTaylorCalculation() {
        if (displayText.isBlank()) return
        stepsList.clear()
        isCalculating = true
        viewModelScope.launch {
            try {
                val res = TaylorEngine.calculateTaylor(displayText, targetText, orderText)
                if (res != "Error" && res.isNotEmpty()) {
                    resultText = res
                    isShowingResult = true
                }
            } catch (e: Exception) {
                resultText = "Error"
            } finally {
                isCalculating = false
            }
        }
    }

    private fun runLaplaceCalculation() {
        if (displayText.isBlank() || displayText == "0") return
        stepsList.clear()
        isCalculating = true
        viewModelScope.launch {
            try {
                val res = LaplaceEngine.calculateLaplace(displayText, laplaceMode, useRationalize)
                lastExpression = displayText
                resultText = res
                cursorIndex = -1
                isShowingResult = true
            } catch (e: Exception) {
                resultText = "Error"
                isShowingResult = false
            } finally {
                isCalculating = false
            }
        }
    }

    private fun clearAll() {
        if ((calculatorMode == CalculatorMode.LIMITS || calculatorMode == CalculatorMode.POLYNOMIALS ||
                    calculatorMode == CalculatorMode.TAYLOR || calculatorMode == CalculatorMode.LAPLACE ||
                    calculatorMode == CalculatorMode.ODE) && isShowingResult) {
            resultText = ""
            isShowingResult = false
            polynomialResult = null
            clearOdeState()
            cursorIndex = displayText.length
            return
        }

        displayText = "0"
        cursorIndex = 1
        resultText = ""
        analysisResult = null
        polynomialResult = null
        clearOdeState()
        targetText = if (calculatorMode == CalculatorMode.LIMITS && limitType == LimitType.INFINITE) "∞" else ""
        lowerLimitText = ""
        upperLimitText = ""
        innerLowerLimitText = ""
        innerUpperLimitText = ""
        orderText = "5"
        isShowingResult = false

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
            type == IntegralType.XSURF || type == IntegralType.YSURF ||
            type == IntegralType.NDOUBLE || type == IntegralType.CURVET1 || type == IntegralType.CURVET2) {
            currentFocus = CalculatorFocus.INTEG_LOWER
        } else {
            currentFocus = CalculatorFocus.EXPRESSION
        }
        lowerLimitText = ""
        upperLimitText = ""
        innerLowerLimitText = ""
        innerUpperLimitText = ""
        resultText = ""
    }

    private fun handleBackspace() {
        if (calculatorMode == CalculatorMode.ODE && odeConditionFocusIndex >= 0 && odeConditionFocusIndex in odeConditions.indices) {
            if (odeConditions[odeConditionFocusIndex].isNotEmpty()) {
                odeConditions[odeConditionFocusIndex] = odeConditions[odeConditionFocusIndex].dropLast(1)
            }
            return
        }

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
            if (currentFocus == CalculatorFocus.INTEG_INNER_LOWER && innerLowerLimitText.isNotEmpty()) {
                innerLowerLimitText = innerLowerLimitText.dropLast(1)
                return
            }
            if (currentFocus == CalculatorFocus.INTEG_INNER_UPPER && innerUpperLimitText.isNotEmpty()) {
                innerUpperLimitText = innerUpperLimitText.dropLast(1)
                return
            }
        }

        if (cursorIndex == -1) {
            cursorIndex = displayText.length
            resultText = ""
            isShowingResult = false
        }

        if (cursorIndex <= 0) return

        val textBefore = displayText.substring(0, cursorIndex)
        val textAfter = displayText.substring(cursorIndex)

        val tokens = listOf(
            " × asinh(", "asinh(", " × acosh(", "acosh(", " × atanh(", "atanh(",
            " × sinh(", "sinh(", " × cosh(", "cosh(", " × tanh(", "tanh(",
            " × asin(", "asin(", " × acos(", "acos(", " × atan(", "atan(",
            " × sin(", "sin(", " × cos(", "cos(", " × tan(", "tan(",
            " × log(", "log(", " × ln(", "ln(", " × abs(", "abs(", " × √(", "√(",
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
