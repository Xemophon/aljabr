package com.xemophon.aljabr.modules.statistics.hypothesis

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sqrt

enum class HypothesisFocus {
    SAMPLE_MEAN,
    STANDARD_DEVIATION,
    SAMPLE_SIZE,
    POPULATION_MEAN,
    SIGNIFICANCE_LEVEL
}

data class HypothesisResult(
    val sampleMean: Double,
    val standardDeviation: Double,
    val sampleSize: Int,
    val standardError: Double,
    val degreesOfFreedom: Int,

    // Hypothesis Testing
    val populationMean: Double? = null,
    val significanceLevel: Double? = null,
    val testStatistic: Double? = null, // z / t score

    // Z-Test Hypothesis
    val pTwoTailedZ: Double? = null,
    val pLeftTailedZ: Double? = null,
    val pRightTailedZ: Double? = null,
    val zCriticalHypo: Double? = null,
    val rejectNullZ: Boolean? = null,

    // T-Test Hypothesis
    val pTwoTailedT: Double? = null,
    val pLeftTailedT: Double? = null,
    val pRightTailedT: Double? = null,
    val tCriticalHypo: Double? = null,
    val rejectNullT: Boolean? = null,

    // Confidence Interval
    val confidenceLevel: Double? = null,

    // Z Confidence Interval
    val zCriticalConf: Double? = null,
    val marginOfErrorZ: Double? = null,
    val lowerBoundZ: Double? = null,
    val upperBoundZ: Double? = null,

    // T Confidence Interval
    val tCriticalConf: Double? = null,
    val marginOfErrorT: Double? = null,
    val lowerBoundT: Double? = null,
    val upperBoundT: Double? = null,

    val errorMessage: String? = null,
)

class HypothesisViewModel(application: Application) : AndroidViewModel(application) {

    // Inputs
    var sampleMean by mutableStateOf("")
    var standardDeviation by mutableStateOf("")
    var sampleSize by mutableStateOf("")
    var populationMean by mutableStateOf("")
    var significanceLevel by mutableStateOf("")

    // Focus & Overlay state
    var currentFocus by mutableStateOf(HypothesisFocus.SAMPLE_MEAN)
        private set

    var isFocusedMode by mutableStateOf(value = false)
        private set

    // Result Analysis State
    var resultState by mutableStateOf<HypothesisResult?>(null)

    val focusValue: String
        get() = when (currentFocus) {
            HypothesisFocus.SAMPLE_MEAN -> sampleMean
            HypothesisFocus.STANDARD_DEVIATION -> standardDeviation
            HypothesisFocus.SAMPLE_SIZE -> sampleSize
            HypothesisFocus.POPULATION_MEAN -> populationMean
            HypothesisFocus.SIGNIFICANCE_LEVEL -> significanceLevel
        }

    fun onFocusChange(focus: HypothesisFocus) {
        currentFocus = focus
        isFocusedMode = true
    }

    fun dismissFocus() {
        isFocusedMode = false
    }

    fun prevFocus() {
        val entries = HypothesisFocus.entries
        val currentIndex = entries.indexOf(currentFocus)
        val prevIndex = if ((currentIndex - 1) < 0) entries.size - 1 else currentIndex - 1
        currentFocus = entries[prevIndex]
    }

    fun nextFocus() {
        val entries = HypothesisFocus.entries
        val currentIndex = entries.indexOf(currentFocus)
        val nextIndex = (currentIndex + 1) % entries.size
        currentFocus = entries[nextIndex]
    }

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Symbol -> handleSymbol(action.formula)
            is CalcButtonAction.Clear -> updateCurrentField { "" }
            is CalcButtonAction.Backspace -> handleBackspace()
            CalcButtonAction.Calculate -> {
                dismissFocus()
                compute()
            }
            CalcButtonAction.Done -> dismissFocus()
            else -> {}
        }
    }

    fun handleSymbol(symbol: String) {
        updateCurrentField { text ->
            MathInputHandler.handleSymbol(text, text.length, symbol).text
        }
    }

    fun handleBackspace() {
        updateCurrentField { text ->
            MathInputHandler.handleBackspace(text, text.length).text
        }
    }

    private fun updateCurrentField(update: (String) -> String) {
        when (currentFocus) {
            HypothesisFocus.SAMPLE_MEAN -> sampleMean = update(sampleMean)
            HypothesisFocus.STANDARD_DEVIATION -> standardDeviation = update(standardDeviation)
            HypothesisFocus.SAMPLE_SIZE -> sampleSize = update(sampleSize)
            HypothesisFocus.POPULATION_MEAN -> populationMean = update(populationMean)
            HypothesisFocus.SIGNIFICANCE_LEVEL -> significanceLevel = update(significanceLevel)
        }
    }

    fun compute() {
        val xBar = sampleMean.toDoubleOrNull()
        val s = standardDeviation.toDoubleOrNull()
        val nDouble = sampleSize.toDoubleOrNull()
        val n = nDouble?.toInt()

        if ((xBar == null) || (s == null) || (s <= 0.0) || (n == null) || (n <= 0)) {
            resultState = HypothesisResult(
                sampleMean = 0.0,
                standardDeviation = 0.0,
                sampleSize = 0,
                standardError = 0.0,
                degreesOfFreedom = 0,
                errorMessage = "Please enter valid numbers for Sample Mean, Standard Deviation (> 0), and Sample Size (> 0).",
            )
            return
        }

        val se = s / sqrt(n.toDouble())
        val df = n - 1

        // Parse Significance Level (default to 0.05 if blank)
        var alphaInput = significanceLevel.toDoubleOrNull() ?: 0.05
        if (alphaInput > 1.0) alphaInput /= 100.0
        val alpha = if ((alphaInput <= 0.0) || (alphaInput >= 1.0)) 0.05 else alphaInput

        val confLevelVal = 1.0 - alpha

        var popMeanVal: Double? = null
        var testStat: Double? = null

        var pTwoZVal: Double? = null
        var pLeftZVal: Double? = null
        var pRightZVal: Double? = null
        var critValHypoZ: Double? = null
        var rejectZ: Boolean? = null

        var pTwoTVal: Double? = null
        var pLeftTVal: Double? = null
        var pRightTVal: Double? = null
        var critValHypoT: Double? = null
        var rejectT: Boolean? = null

        if (populationMean.isNotBlank()) {
            val mu0 = populationMean.toDoubleOrNull()
            if (mu0 != null) {
                popMeanVal = mu0
                val stat = (xBar - mu0) / se
                testStat = stat

                // Z-distribution calculations
                val pLZ = normalCdf(stat)
                val pRZ = 1.0 - pLZ
                val p2Z = 2.0 * min(pLZ, pRZ)
                pLeftZVal = pLZ
                pRightZVal = pRZ
                pTwoZVal = p2Z

                // T-distribution calculations
                val pLT = studentTCdf(df.toDouble(), stat)
                val pRT = 1.0 - pLT
                val p2T = 2.0 * (1.0 - studentTCdf(df.toDouble(), abs(stat)))
                pLeftTVal = pLT
                pRightTVal = pRT
                pTwoTVal = p2T

                rejectZ = (p2Z < alpha)
                rejectT = (p2T < alpha)
                critValHypoZ = inverseNormalCdf(1.0 - (alpha / 2.0))
                critValHypoT = inverseStudentTCdf(1.0 - (alpha / 2.0), df)
            }
        }

        // Confidence Interval calculations (C = 1 - alpha)
        val alphaConf = alpha

        // Z Confidence Interval
        val critZ = inverseNormalCdf(1.0 - (alphaConf / 2.0))
        val marginZ = critZ * se
        val lowerZ = xBar - marginZ
        val upperZ = xBar + marginZ

        // T Confidence Interval
        val critT = inverseStudentTCdf(1.0 - (alphaConf / 2.0), df)
        val marginT = critT * se
        val lowerT = xBar - marginT
        val upperT = xBar + marginT

        resultState = HypothesisResult(
            sampleMean = xBar,
            standardDeviation = s,
            sampleSize = n,
            standardError = se,
            degreesOfFreedom = df,
            populationMean = popMeanVal,
            significanceLevel = alpha,
            testStatistic = testStat,
            pTwoTailedZ = pTwoZVal,
            pLeftTailedZ = pLeftZVal,
            pRightTailedZ = pRightZVal,
            zCriticalHypo = critValHypoZ,
            rejectNullZ = rejectZ,
            pTwoTailedT = pTwoTVal,
            pLeftTailedT = pLeftTVal,
            pRightTailedT = pRightTVal,
            tCriticalHypo = critValHypoT,
            rejectNullT = rejectT,
            confidenceLevel = confLevelVal,
            zCriticalConf = critZ,
            marginOfErrorZ = marginZ,
            lowerBoundZ = lowerZ,
            upperBoundZ = upperZ,
            tCriticalConf = critT,
            marginOfErrorT = marginT,
            lowerBoundT = lowerT,
            upperBoundT = upperT,
        )
    }

    fun clear() {
        sampleMean = ""
        standardDeviation = ""
        sampleSize = ""
        populationMean = ""
        significanceLevel = ""
        resultState = null
    }

    fun clearResult() {
        resultState = null
    }

    // Probability Math Helpers
    private fun erf(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1 else 1
        val absX = abs(x)
        val t = 1.0 / (1.0 + (p * absX))
        val poly = (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t
        val y = 1.0 - (poly * exp(-(absX * absX)))
        return sign * y
    }

    private fun normalCdf(z: Double): Double {
        return 0.5 * (1.0 + erf(z / sqrt(2.0)))
    }

    private fun studentTCdf(df: Double, t: Double): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[CDF[StudentTDistribution[$df], $t]]").toString().toDouble()
            }
        } catch (_: Exception) {
            0.5 * (1.0 + erf(t / sqrt(2.0)))
        }
    }

    private fun inverseNormalCdf(p: Double): Double {
        if (p <= 0.0) return Double.NEGATIVE_INFINITY
        if (p >= 1.0) return Double.POSITIVE_INFINITY

        val a = doubleArrayOf(
            -3.969683028665376e+01,  2.209460984245205e+02,
            -2.759285104469687e+02,  1.383577518672690e+02,
            -3.066479806614716e+01,  2.506628277459239e+00,
        )
        val b = doubleArrayOf(
            -5.447609879822406e+01,  1.615858368580409e+02,
            -1.556989798598866e+02,  6.680131188771972e+01,
            -1.328068155288572e+01,
        )
        val c = doubleArrayOf(
            -7.784894002430293e-03, -3.223964580411365e-01,
            -2.400758277161838e-01,  2.549732539343734e+00,
            4.374664141464968e+00,  2.938163982698783e+00,
        )
        val d = doubleArrayOf(
            7.784695709041462e-03,  3.224671290700398e-01,
            2.445134137142996e-01,  3.754408661907416e+00,
        )

        val pLow = 0.02425
        val pHigh = 1.0 - pLow

        return when {
            p < pLow -> {
                val q = sqrt(-2.0 * ln(p))
                ((((c[0]*q + c[1])*q + c[2])*q + c[3])*q + c[4])*q + c[5] /
                        ((((d[0]*q + d[1])*q + d[2])*q + d[3])*q + 1.0)
            }
            p <= pHigh -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0]*r + a[1])*r + a[2])*r + a[3])*r + a[4])*r + a[5])*q /
                        (((((b[0]*r + b[1])*r + b[2])*r + b[3])*r + b[4])*r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - p))
                -(((((c[0]*q + c[1])*q + c[2])*q + c[3])*q + c[4])*q + c[5]) /
                        ((((d[0]*q + d[1])*q + d[2])*q + d[3])*q + 1.0)
            }
        }
    }

    private fun inverseStudentTCdf(p: Double, df: Int): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[Quantile[StudentTDistribution[$df], $p]]").toString().toDouble()
            }
        } catch (_: Exception) {
            val z = inverseNormalCdf(p)
            val z3 = z * z * z
            z + (z3 + z) / (4.0 * df) + (5.0 * z3 * z * z + 16.0 * z3 + 3.0 * z) / (96.0 * df * df)
        }
    }
}
