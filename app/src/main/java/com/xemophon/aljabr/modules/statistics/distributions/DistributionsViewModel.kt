package com.xemophon.aljabr.modules.statistics.distributions

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class DistributionsType {
    BINOMIAL,
    POISSON,
    NORMAL,
    STUDENT,
    UNIFORM,
    HYPERGEOMETRIC,
    EXPONENTIAL,
    GEOMETRIC,
    CHI_SQUARE,
    F_DISTRIBUTION
}

enum class DistributionsFocus {
    PARAM1, PARAM2, PARAM3, X_VAL, X2_VAL
}

class DistributionsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    var precision by mutableIntStateOf(4)
        private set

    // Active distribution type
    var type by mutableStateOf(DistributionsType.BINOMIAL)
        private set

    // Calculation mode (PDF/PMF, CDF, Upper CDF, Range)
    var calcMode by mutableStateOf(DistCalcMode.PDF_PMF)
        private set

    val isRangeMode: Boolean
        get() = calcMode == DistCalcMode.RANGE

    // Focus state
    var currentFocus by mutableStateOf(DistributionsFocus.X_VAL)
        private set

    var isFocusedMode by mutableStateOf(value = false)
        private set

    // Binomial inputs (n, p)
    var n by mutableStateOf("10")
    var p by mutableStateOf("0.5")

    // Poisson inputs (lambda)
    var lambdaPoisson by mutableStateOf("2.5")

    // Normal inputs (mean, stdDev)
    var mean by mutableStateOf("0")
    var stdDev by mutableStateOf("1")

    // Student's t inputs (df)
    var dfStudent by mutableStateOf("10")

    // Uniform inputs (a, b)
    var a by mutableStateOf("0")
    var b by mutableStateOf("10")

    // Hypergeometric inputs (N, K, n)
    var bigN by mutableStateOf("50")
    var bigK by mutableStateOf("10")
    var sampleN by mutableStateOf("5")

    // Exponential inputs (lambda)
    var lambdaExp by mutableStateOf("1.5")

    // Geometric inputs (p)
    var pGeom by mutableStateOf("0.3")

    // Chi-Square inputs (df)
    var dfChi by mutableStateOf("5")

    // F-Distribution inputs (df1, df2)
    var df1F by mutableStateOf("5")
    var df2F by mutableStateOf("10")

    // Value X & X2 (upper range limit)
    var xVal by mutableStateOf("5")
    var x2Val by mutableStateOf("8")

    // Results state
    var distributionResult by mutableStateOf<DistributionsResult?>(null)
        private set

    var resultText by mutableStateOf("")
        private set

    var isCalculating by mutableStateOf(false)
        private set

    var isGraphExpanded by mutableStateOf(false)
        private set

    fun toggleGraphExpanded() {
        isGraphExpanded = !isGraphExpanded
    }

    init {
        viewModelScope.launch {
            settingsRepository.precisionFlow.collectLatest {
                precision = it
                calculateResult()
            }
        }
        calculateResult()
    }

    // ==========================================
    // Dynamic metadata getters
    // ==========================================
    val distributionTitle: String
        get() = when (type) {
            DistributionsType.BINOMIAL -> "Binomial Distribution"
            DistributionsType.POISSON -> "Poisson Distribution"
            DistributionsType.NORMAL -> "Normal Distribution"
            DistributionsType.STUDENT -> "Student's t-Distribution"
            DistributionsType.UNIFORM -> "Uniform Distribution"
            DistributionsType.HYPERGEOMETRIC -> "Hypergeometric Distribution"
            DistributionsType.EXPONENTIAL -> "Exponential Distribution"
            DistributionsType.GEOMETRIC -> "Geometric Distribution"
            DistributionsType.CHI_SQUARE -> "Chi-Square Distribution"
            DistributionsType.F_DISTRIBUTION -> "F-Distribution"
        }

    val displayTitle: String
        get() = distributionTitle

    @Suppress("unused")
    val graphExpression: String
        get() = DistributionsFunc.getGraphExpression(
            type = type,
            param1Str = activeParam1Value,
            param2Str = activeParam2Value ?: "",
            param3Str = activeParam3Value ?: "",
            calcMode = calcMode,
        )

    val shadeMinX: Double?
        get() {
            val x1 = DistributionsFunc.parseExpression(xVal) ?: return null
            return when (calcMode) {
                DistCalcMode.PDF_PMF -> null
                DistCalcMode.CDF -> distributionLowerBound(type, activeParam1Value, activeParam2Value)
                DistCalcMode.UPPER_CDF -> x1
                DistCalcMode.RANGE -> {
                    val x2 = DistributionsFunc.parseExpression(x2Val) ?: x1
                    min(x1, x2)
                }
            }
        }

    val shadeMaxX: Double?
        get() {
            val x1 = DistributionsFunc.parseExpression(xVal) ?: return null
            return when (calcMode) {
                DistCalcMode.PDF_PMF -> null
                DistCalcMode.CDF -> x1
                DistCalcMode.UPPER_CDF -> distributionUpperBound(type, activeParam1Value, activeParam2Value)
                DistCalcMode.RANGE -> {
                    val x2 = DistributionsFunc.parseExpression(x2Val) ?: x1
                    max(x1, x2)
                }
            }
        }

    val graphMinX: Double
        get() {
            val low = distributionLowerBound(type, activeParam1Value, activeParam2Value)
            val high = distributionUpperBound(type, activeParam1Value, activeParam2Value)
            val range = max(0.5, high - low)
            return low - 0.08 * range
        }

    val graphMaxX: Double
        get() {
            val low = distributionLowerBound(type, activeParam1Value, activeParam2Value)
            val high = distributionUpperBound(type, activeParam1Value, activeParam2Value)
            val range = max(0.5, high - low)
            return high + 0.08 * range
        }

    private fun distributionLowerBound(type: DistributionsType, p1Str: String, p2Str: String?): Double {
        val p1 = DistributionsFunc.parseExpression(p1Str) ?: 0.0
        val p2 = p2Str?.let { DistributionsFunc.parseExpression(it) } ?: 0.0
        return when (type) {
            DistributionsType.BINOMIAL,
            DistributionsType.POISSON,
            DistributionsType.HYPERGEOMETRIC,
            DistributionsType.EXPONENTIAL,
            DistributionsType.CHI_SQUARE,
            DistributionsType.F_DISTRIBUTION -> 0.0
            DistributionsType.NORMAL -> p1 - 3.5 * p2
            DistributionsType.STUDENT -> -5.0
            DistributionsType.UNIFORM -> p1
            DistributionsType.GEOMETRIC -> 1.0
        }
    }

    private fun distributionUpperBound(type: DistributionsType, p1Str: String, p2Str: String?): Double {
        val p1 = DistributionsFunc.parseExpression(p1Str) ?: 10.0
        val p2 = p2Str?.let { DistributionsFunc.parseExpression(it) } ?: 1.0
        return when (type) {
            DistributionsType.BINOMIAL -> p1
            DistributionsType.POISSON -> max(10.0, 3.0 * p1)
            DistributionsType.NORMAL -> p1 + 3.5 * p2
            DistributionsType.STUDENT -> 5.0
            DistributionsType.UNIFORM -> p2
            DistributionsType.HYPERGEOMETRIC -> p1
            DistributionsType.EXPONENTIAL -> if (p1 > 0) 5.0 / p1 else 10.0
            DistributionsType.GEOMETRIC -> if (p1 > 0) 5.0 / p1 else 10.0
            DistributionsType.CHI_SQUARE -> max(10.0, 3.0 * p1)
            DistributionsType.F_DISTRIBUTION -> 5.0
        }
    }

    val activeParam1Label: String
        get() = when (type) {
            DistributionsType.BINOMIAL -> "n (trials)"
            DistributionsType.POISSON -> "λ (rate)"
            DistributionsType.NORMAL -> "μ (mean)"
            DistributionsType.STUDENT -> "df (deg. freedom)"
            DistributionsType.UNIFORM -> "a (min)"
            DistributionsType.HYPERGEOMETRIC -> "N (population)"
            DistributionsType.EXPONENTIAL -> "λ (rate)"
            DistributionsType.GEOMETRIC -> "p (prob)"
            DistributionsType.CHI_SQUARE -> "df (deg. freedom)"
            DistributionsType.F_DISTRIBUTION -> "df₁ (num df)"
        }

    val activeParam2Label: String?
        get() = when (type) {
            DistributionsType.BINOMIAL -> "p (prob)"
            DistributionsType.NORMAL -> "σ (std. dev)"
            DistributionsType.UNIFORM -> "b (max)"
            DistributionsType.HYPERGEOMETRIC -> "K (pop. successes)"
            DistributionsType.F_DISTRIBUTION -> "df₂ (den df)"
            DistributionsType.POISSON,
            DistributionsType.STUDENT,
            DistributionsType.EXPONENTIAL,
            DistributionsType.GEOMETRIC,
            DistributionsType.CHI_SQUARE -> null
        }

    val activeParam3Label: String?
        get() = when (type) {
            DistributionsType.HYPERGEOMETRIC -> "n (sample size)"
            else -> null
        }

    val activeParam1Value: String
        get() = when (type) {
            DistributionsType.BINOMIAL -> n
            DistributionsType.POISSON -> lambdaPoisson
            DistributionsType.NORMAL -> mean
            DistributionsType.STUDENT -> dfStudent
            DistributionsType.UNIFORM -> a
            DistributionsType.HYPERGEOMETRIC -> bigN
            DistributionsType.EXPONENTIAL -> lambdaExp
            DistributionsType.GEOMETRIC -> pGeom
            DistributionsType.CHI_SQUARE -> dfChi
            DistributionsType.F_DISTRIBUTION -> df1F
        }

    val activeParam2Value: String?
        get() = when (type) {
            DistributionsType.BINOMIAL -> p
            DistributionsType.NORMAL -> stdDev
            DistributionsType.UNIFORM -> b
            DistributionsType.HYPERGEOMETRIC -> bigK
            DistributionsType.F_DISTRIBUTION -> df2F
            else -> null
        }

    val activeParam3Value: String?
        get() = when (type) {
            DistributionsType.HYPERGEOMETRIC -> sampleN
            else -> null
        }

    val xLabel: String
        get() = when (type) {
            DistributionsType.BINOMIAL,
            DistributionsType.POISSON,
            DistributionsType.HYPERGEOMETRIC,
            DistributionsType.GEOMETRIC -> "k"
            DistributionsType.STUDENT -> "t"
            DistributionsType.CHI_SQUARE -> "χ²"
            DistributionsType.F_DISTRIBUTION -> "F"
            DistributionsType.NORMAL,
            DistributionsType.UNIFORM,
            DistributionsType.EXPONENTIAL -> "x"
        }

    val x2Label: String
        get() = when (type) {
            DistributionsType.BINOMIAL,
            DistributionsType.POISSON,
            DistributionsType.HYPERGEOMETRIC,
            DistributionsType.GEOMETRIC -> "k₂"
            DistributionsType.STUDENT -> "t₂"
            DistributionsType.CHI_SQUARE -> "χ²₂"
            DistributionsType.F_DISTRIBUTION -> "F₂"
            DistributionsType.NORMAL,
            DistributionsType.UNIFORM,
            DistributionsType.EXPONENTIAL -> "x₂"
        }

    val focusValue: String
        get() = when (currentFocus) {
            DistributionsFocus.PARAM1 -> activeParam1Value
            DistributionsFocus.PARAM2 -> activeParam2Value ?: ""
            DistributionsFocus.PARAM3 -> activeParam3Value ?: ""
            DistributionsFocus.X_VAL -> xVal
            DistributionsFocus.X2_VAL -> x2Val
        }

    val hasParam2: Boolean
        get() = activeParam2Label != null

    val hasParam3: Boolean
        get() = activeParam3Label != null

    // ==========================================
    // State management actions
    // ==========================================
    fun onTypeChange(newType: DistributionsType) {
        if (type != newType) {
            type = newType
            if (currentFocus == DistributionsFocus.PARAM2 && !hasParam2) {
                currentFocus = DistributionsFocus.PARAM1
            }
            if (currentFocus == DistributionsFocus.PARAM3 && !hasParam3) {
                currentFocus = DistributionsFocus.PARAM1
            }
            calculateResult()
        }
    }

    fun onCalcModeChange(newMode: DistCalcMode) {
        if (calcMode != newMode) {
            calcMode = newMode
            if (calcMode != DistCalcMode.RANGE && currentFocus == DistributionsFocus.X2_VAL) {
                currentFocus = DistributionsFocus.X_VAL
            }
            calculateResult()
        }
    }

    fun onFocusChange(focus: DistributionsFocus) {
        if (focus == DistributionsFocus.PARAM2 && !hasParam2) return
        if (focus == DistributionsFocus.PARAM3 && !hasParam3) return
        if (focus == DistributionsFocus.X2_VAL && !isRangeMode) return
        currentFocus = focus
        isFocusedMode = true
    }

    fun dismissFocus() {
        isFocusedMode = false
    }

    fun nextFocus() {
        currentFocus = when (currentFocus) {
            DistributionsFocus.PARAM1 -> if (hasParam2) DistributionsFocus.PARAM2 else if (hasParam3) DistributionsFocus.PARAM3 else DistributionsFocus.X_VAL
            DistributionsFocus.PARAM2 -> if (hasParam3) DistributionsFocus.PARAM3 else DistributionsFocus.X_VAL
            DistributionsFocus.PARAM3 -> DistributionsFocus.X_VAL
            DistributionsFocus.X_VAL -> if (isRangeMode) DistributionsFocus.X2_VAL else DistributionsFocus.PARAM1
            DistributionsFocus.X2_VAL -> DistributionsFocus.PARAM1
        }
    }

    fun prevFocus() {
        currentFocus = when (currentFocus) {
            DistributionsFocus.PARAM1 -> if (isRangeMode) DistributionsFocus.X2_VAL else DistributionsFocus.X_VAL
            DistributionsFocus.PARAM2 -> DistributionsFocus.PARAM1
            DistributionsFocus.PARAM3 -> DistributionsFocus.PARAM2
            DistributionsFocus.X_VAL -> if (hasParam3) DistributionsFocus.PARAM3 else if (hasParam2) DistributionsFocus.PARAM2 else DistributionsFocus.PARAM1
            DistributionsFocus.X2_VAL -> DistributionsFocus.X_VAL
        }
    }

    // ==========================================
    // Keypad and Input Actions
    // ==========================================
    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Symbol -> {
                if (action.formula == "( )" || action.formula == "()") {
                    handleBrackets()
                } else {
                    handleSymbol(action.formula)
                }
            }
            is CalcButtonAction.Scientific -> {
                updateCurrentField { text ->
                    MathInputHandler.handleScientific(text, text.length, action).text
                }
            }
            is CalcButtonAction.Constant -> {
                updateCurrentField { text ->
                    MathInputHandler.handleConstant(text, text.length, action).text
                }
            }
            is CalcButtonAction.Variable -> handleSymbol(action.text)
            is CalcButtonAction.Clear -> handleClear()
            is CalcButtonAction.Backspace -> handleBackspace()
            CalcButtonAction.Calculate -> calculateResult()
            CalcButtonAction.Done -> dismissFocus()
            else -> {}
        }
    }

    fun handleSymbol(symbol: String) {
        updateCurrentField { text ->
            MathInputHandler.handleSymbol(text, text.length, symbol).text
        }
    }

    private fun handleBrackets() {
        updateCurrentField { text ->
            MathInputHandler.handleBrackets(text, text.length).text
        }
    }

    fun handleBackspace() {
        updateCurrentField { text ->
            MathInputHandler.handleBackspace(text, text.length).text
        }
    }

    fun handleClear() {
        updateCurrentField { "" }
    }

    private fun updateCurrentField(update: (String) -> String) {
        when (currentFocus) {
            DistributionsFocus.PARAM1 -> {
                val updated = update(activeParam1Value)
                when (type) {
                    DistributionsType.BINOMIAL -> n = updated
                    DistributionsType.POISSON -> lambdaPoisson = updated
                    DistributionsType.NORMAL -> mean = updated
                    DistributionsType.STUDENT -> dfStudent = updated
                    DistributionsType.UNIFORM -> a = updated
                    DistributionsType.HYPERGEOMETRIC -> bigN = updated
                    DistributionsType.EXPONENTIAL -> lambdaExp = updated
                    DistributionsType.GEOMETRIC -> pGeom = updated
                    DistributionsType.CHI_SQUARE -> dfChi = updated
                    DistributionsType.F_DISTRIBUTION -> df1F = updated
                }
            }
            DistributionsFocus.PARAM2 -> {
                val updated = update(activeParam2Value ?: "")
                when (type) {
                    DistributionsType.BINOMIAL -> p = updated
                    DistributionsType.NORMAL -> stdDev = updated
                    DistributionsType.UNIFORM -> b = updated
                    DistributionsType.HYPERGEOMETRIC -> bigK = updated
                    DistributionsType.F_DISTRIBUTION -> df2F = updated
                    else -> {}
                }
            }
            DistributionsFocus.PARAM3 -> {
                val updated = update(activeParam3Value ?: "")
                when (type) {
                    DistributionsType.HYPERGEOMETRIC -> sampleN = updated
                    else -> {}
                }
            }
            DistributionsFocus.X_VAL -> xVal = update(xVal)
            DistributionsFocus.X2_VAL -> x2Val = update(x2Val)
        }
        calculateResult()
    }

    // ==========================================
    // Calculations
    // ==========================================
    fun calculateResult() {
        viewModelScope.launch {
            isCalculating = true
            try {
                val res = withContext(Dispatchers.Default) {
                    DistributionsFunc.calculate(
                        type = type,
                        param1Str = activeParam1Value,
                        param2Str = activeParam2Value ?: "",
                        param3Str = activeParam3Value ?: "",
                        xStr = xVal,
                        x2Str = x2Val,
                        calcMode = calcMode,
                        precision = precision
                    )
                }
                distributionResult = res
                resultText = res.summary
            } catch (e: Exception) {
                distributionResult = null
                resultText = e.message ?: "Error"
            } finally {
                isCalculating = false
            }
        }
    }

    @Suppress("unused")
    fun clearResult() {
        distributionResult = null
        resultText = ""
    }
}
