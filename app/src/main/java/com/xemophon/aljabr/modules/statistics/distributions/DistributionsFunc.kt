package com.xemophon.aljabr.modules.statistics.distributions

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.modules.graphMaker.Point
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Calculation mode for probability distributions.
 */
enum class DistCalcMode {
    PDF_PMF,  // f(x) or P(X = x)
    CDF,      // P(X ≤ x)
    UPPER_CDF,// P(X ≥ x)
    RANGE     // P(x1 ≤ X ≤ x2)
}

/**
 * Domain model representing the calculation result for statistical probability distributions.
 */
data class DistributionsResult(
    val type: DistributionsType,
    val calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
    val primaryResultLabel: String,
    val primaryResultValue: String,
    val pdfOrPmfLabel: String,
    val pdfOrPmfValue: String,
    val cdfLabel: String = "P(X ≤ x)",
    val cdfValue: String,
    val upperCdfLabel: String = "P(X ≥ x)",
    val upperCdfValue: String,
    val strictCdfLabel: String? = null,
    val strictCdfValue: String? = null,
    val rangeProbabilityLabel: String? = null,
    val rangeProbabilityValue: String? = null,
    val mean: String,
    val variance: String,
    val stdDev: String,
    val summary: String = "",
    val extraInfo: Map<String, String> = emptyMap(),
)

object DistributionsFunc {

    /**
     * Constructs the graph expression string in PDF(<Distribution>,x) or CDF(<Distribution>,x) syntax.
     */
    fun getGraphExpression(
        type: DistributionsType,
        param1Str: String,
        param2Str: String = "",
        param3Str: String = "",
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF
    ): String {
        val p1 = param1Str.ifBlank { "1" }
        val p2 = param2Str.ifBlank { "1" }
        val p3 = param3Str.ifBlank { "1" }

        val fnName = if (calcMode == DistCalcMode.CDF) "CDF" else "PDF"
        val distName = when (type) {
            DistributionsType.BINOMIAL -> "BinomialDistribution($p1, $p2)"
            DistributionsType.POISSON -> "PoissonDistribution($p1)"
            DistributionsType.NORMAL -> "NormalDistribution($p1, $p2)"
            DistributionsType.STUDENT -> "StudentTDistribution($p1)"
            DistributionsType.UNIFORM -> "UniformDistribution($p1, $p2)"
            DistributionsType.HYPERGEOMETRIC -> "HypergeometricDistribution($p3, $p2, $p1)"
            DistributionsType.EXPONENTIAL -> "ExponentialDistribution($p1)"
            DistributionsType.GEOMETRIC -> "GeometricDistribution($p1)"
            DistributionsType.CHI_SQUARE -> "ChiSquareDistribution($p1)"
            DistributionsType.F_DISTRIBUTION -> "FRatioDistribution($p1, $p2)"
        }
        return "$fnName($distName, x)"
    }

    /**
     * Converts PDF(<Distribution>, x) or CDF(<Distribution>, x) user expressions to Symja AST format.
     */
    fun prepareDistributionExpression(expr: String): String {
        var cleaned = expr.trim()
        cleaned = cleaned.replace("UniformDistribution(", "UniformDistribution[{")
            .replace("UniformDistribution[", "UniformDistribution[{")
            .replace("BinomialDistribution(", "BinomialDistribution[")
            .replace("PoissonDistribution(", "PoissonDistribution[")
            .replace("NormalDistribution(", "NormalDistribution[")
            .replace("StudentTDistribution(", "StudentTDistribution[")
            .replace("HypergeometricDistribution(", "HypergeometricDistribution[")
            .replace("ExponentialDistribution(", "ExponentialDistribution[")
            .replace("GeometricDistribution(", "GeometricDistribution[")
            .replace("ChiSquareDistribution(", "ChiSquareDistribution[")
            .replace("FRatioDistribution(", "FRatioDistribution[")
            .replace("PDF(", "PDF[")
            .replace("CDF(", "CDF[")

        val openCount = cleaned.count { it == '[' }
        val closeCount = cleaned.count { it == ']' }
        if (openCount > closeCount) {
            val diff = openCount - closeCount
            val sb = StringBuilder(cleaned)
            var replaced = 0
            for (i in (sb.length - 1) downTo 0) {
                if (sb[i] == ')' && replaced < diff) {
                    sb.setCharAt(i, ']')
                    replaced++
                }
            }
            cleaned = sb.toString()
        }
        return cleaned
    }

    /**
     * Parses a string mathematical expression into a [Double].
     * Supports basic numeric input, fractions (e.g. "1/2"), and constants/functions via Symja.
     */
    fun parseExpression(expression: String): Double? {
        if (expression.isBlank()) return null
        return try {
            val cleaned = SymjaUtils.prepareForSymja(expression)
            val evalResult = SymjaUtils.evaluate { eval ->
                eval.eval("N[$cleaned]").toString()
            }
            evalResult.toDoubleOrNull()
        } catch (_: Exception) {
            expression.toDoubleOrNull()
        }
    }

    /**
     * Evaluates a distribution function at a specific point x in pure Kotlin.
     */
    fun evaluatePoint(
        type: DistributionsType,
        param1Str: String,
        param2Str: String = "",
        param3Str: String = "",
        x: Double,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF
    ): Double {
        val p1 = parseExpression(param1Str) ?: 1.0
        val p2 = parseExpression(param2Str) ?: 1.0
        val p3 = parseExpression(param3Str) ?: 1.0

        val isCdf = calcMode == DistCalcMode.CDF || calcMode == DistCalcMode.UPPER_CDF || calcMode == DistCalcMode.RANGE

        val rawY = when (type) {
            DistributionsType.NORMAL -> {
                val sd = if (p2 <= 0.0) 1.0 else p2
                if (isCdf) normalCdf(x, p1, sd) else normalPdf(x, p1, sd)
            }
            DistributionsType.BINOMIAL -> {
                val n = p1.roundToInt().coerceAtLeast(1)
                val p = p2.coerceIn(0.0, 1.0)
                if (isCdf) binomialCdf(n, p, floor(x).toInt())
                else {
                    val k = x.roundToInt()
                    if (abs(x - k) < 0.05 && k in 0..n) binomialPmf(n, p, k) else 0.0
                }
            }
            DistributionsType.POISSON -> {
                val lambda = if (p1 <= 0.0) 1.0 else p1
                if (isCdf) poissonCdf(lambda, floor(x).toInt())
                else {
                    val k = x.roundToInt()
                    if (abs(x - k) < 0.05 && k >= 0) poissonPmf(lambda, k) else 0.0
                }
            }
            DistributionsType.UNIFORM -> {
                val a = min(p1, p2)
                val b = max(p1, p2)
                if (a == b) 0.0
                else if (isCdf) {
                    if (x < a) 0.0 else if (x > b) 1.0 else (x - a) / (b - a)
                } else {
                    if (x in a..b) 1.0 / (b - a) else 0.0
                }
            }
            DistributionsType.EXPONENTIAL -> {
                val lambda = if (p1 <= 0.0) 1.0 else p1
                if (isCdf) {
                    if (x < 0.0) 0.0 else 1.0 - exp(-lambda * x)
                } else {
                    if (x < 0.0) 0.0 else lambda * exp(-lambda * x)
                }
            }
            DistributionsType.GEOMETRIC -> {
                val p = p1.coerceIn(0.0001, 1.0)
                if (isCdf) {
                    val k = floor(x).toInt()
                    if (k < 1) 0.0 else 1.0 - (1.0 - p).pow(k.toDouble())
                } else {
                    val k = x.roundToInt()
                    if (abs(x - k) < 0.05 && k >= 1) p * (1.0 - p).pow((k - 1).toDouble()) else 0.0
                }
            }
            DistributionsType.STUDENT -> {
                val df = if (p1 <= 0.0) 1.0 else p1
                if (isCdf) studentTCdf(df, x) else studentTPdf(df, x)
            }
            DistributionsType.CHI_SQUARE -> {
                val df = if (p1 <= 0.0) 1.0 else p1
                if (isCdf) {
                    try {
                        SymjaUtils.evaluate { eval -> eval.eval("N[CDF[ChiSquareDistribution[$df], $x]]").toString().toDouble() }
                    } catch (_: Exception) { 0.0 }
                } else {
                    try {
                        SymjaUtils.evaluate { eval -> eval.eval("N[PDF[ChiSquareDistribution[$df], $x]]").toString().toDouble() }
                    } catch (_: Exception) { 0.0 }
                }
            }
            DistributionsType.F_DISTRIBUTION -> {
                val df1 = if (p1 <= 0.0) 1.0 else p1
                val df2 = if (p2 <= 0.0) 1.0 else p2
                if (isCdf) {
                    try {
                        SymjaUtils.evaluate { eval -> eval.eval("N[CDF[FRatioDistribution[$df1, $df2], $x]]").toString().toDouble() }
                    } catch (_: Exception) { 0.0 }
                } else {
                    try {
                        SymjaUtils.evaluate { eval -> eval.eval("N[PDF[FRatioDistribution[$df1, $df2], $x]]").toString().toDouble() }
                    } catch (_: Exception) { 0.0 }
                }
            }
            DistributionsType.HYPERGEOMETRIC -> {
                val bigN = p1.roundToInt().coerceAtLeast(1)
                val bigK = p2.roundToInt().coerceIn(0, bigN)
                val sampleN = p3.roundToInt().coerceIn(0, bigN)
                if (isCdf) hypergeometricCdf(bigN, bigK, sampleN, floor(x).toInt())
                else {
                    val k = x.roundToInt()
                    if (abs(x - k) < 0.05) hypergeometricPmf(bigN, bigK, sampleN, k) else 0.0
                }
            }
        }

        if (calcMode == DistCalcMode.UPPER_CDF) {
            return (1.0 - rawY).coerceIn(0.0, 1.0)
        }
        return if (rawY.isNaN() || rawY.isInfinite()) 0.0 else rawY
    }

    /**
     * Generates sampled points for graphing a probability distribution.
     */
    fun generateGraphPoints(
        type: DistributionsType,
        param1Str: String,
        param2Str: String = "",
        param3Str: String = "",
        minX: Double,
        maxX: Double,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        steps: Int = 400
    ): List<List<Point>> {
        if (maxX <= minX) return emptyList()

        val isDiscrete = type in listOf(
            DistributionsType.BINOMIAL,
            DistributionsType.POISSON,
            DistributionsType.HYPERGEOMETRIC,
            DistributionsType.GEOMETRIC
        )

        val pointList = mutableListOf<Point>()

        if (isDiscrete && calcMode == DistCalcMode.PDF_PMF) {
            val startK = floor(minX).toInt()
            val endK = ceil(maxX).toInt()
            for (k in startK..endK) {
                val y = evaluatePoint(type, param1Str, param2Str, param3Str, k.toDouble(), calcMode)
                pointList.add(Point(k.toFloat(), y.toFloat()))
            }
        } else {
            val stepSize = (maxX - minX) / steps
            for (i in 0..steps) {
                val x = minX + i * stepSize
                val y = evaluatePoint(type, param1Str, param2Str, param3Str, x, calcMode)
                pointList.add(Point(x.toFloat(), y.toFloat()))
            }
        }

        return listOf(pointList)
    }

    /**
     * Formats a [Double] according to user precision settings.
     * Whole numbers are printed without trailing zeros. Small probabilities use scientific notation.
     */
    fun Double.formatPrecision(precision: Int = 4): String {
        if (this.isNaN()) return "NaN"
        if (this.isInfinite()) return if (this > 0) "∞" else "-∞"
        if (abs(this - this.roundToInt()) < 1e-12) {
            return this.roundToInt().toString()
        }
        if ((abs(this) < 1e-4) && (this != 0.0)) {
            return String.format("%.${precision}e", this)
        }
        return String.format("%.${precision}f", this)
    }

    /**
     * Main calculation entrypoint supporting up to 3 parameters, value X, optional X2 range, and calculation mode.
     */
    fun calculate(
        type: DistributionsType,
        param1Str: String,
        param2Str: String = "",
        param3Str: String = "",
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        return when (type) {
            DistributionsType.BINOMIAL -> calculateBinomial(param1Str, param2Str, xStr, x2Str, calcMode, precision)
            DistributionsType.POISSON -> calculatePoisson(param1Str, xStr, x2Str, calcMode, precision)
            DistributionsType.NORMAL -> calculateNormal(param1Str, param2Str, xStr, x2Str, calcMode, precision)
            DistributionsType.STUDENT -> calculateStudentT(param1Str, xStr, x2Str, calcMode, precision)
            DistributionsType.UNIFORM -> calculateUniform(param1Str, param2Str, xStr, x2Str, calcMode, precision)
            DistributionsType.HYPERGEOMETRIC -> calculateHypergeometric(param1Str, param2Str, param3Str, xStr, x2Str, calcMode, precision)
            DistributionsType.EXPONENTIAL -> calculateExponential(param1Str, xStr, x2Str, calcMode, precision)
            DistributionsType.GEOMETRIC -> calculateGeometric(param1Str, xStr, x2Str, calcMode, precision)
            DistributionsType.CHI_SQUARE -> calculateChiSquare(param1Str, xStr, x2Str, calcMode, precision)
            DistributionsType.F_DISTRIBUTION -> calculateFDistribution(param1Str, param2Str, xStr, x2Str, calcMode, precision)
        }
    }

    // Helper for log factorials to prevent overflow
    private fun logFactorial(n: Int): Double {
        if (n <= 1) return 0.0
        var sum = 0.0
        for (i in 2..n) {
            sum += ln(i.toDouble())
        }
        return sum
    }

    private fun logCombination(n: Int, k: Int): Double {
        if (k !in 0..n) return Double.NEGATIVE_INFINITY
        return logFactorial(n) - logFactorial(k) - logFactorial(n - k)
    }

    // ==========================================
    // 1. BINOMIAL DISTRIBUTION (n, p)
    // ==========================================
    fun calculateBinomial(
        nStr: String,
        pStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val nVal = parseExpression(nStr)?.toInt()
            ?: throw IllegalArgumentException("Invalid trials 'n'. Must be an integer ≥ 0.")
        if (nVal < 0) throw IllegalArgumentException("Number of trials 'n' cannot be negative.")

        val pVal = parseExpression(pStr)
            ?: throw IllegalArgumentException("Invalid probability 'p'. Must be between 0 and 1.")
        if ((pVal < 0.0) || (pVal > 1.0)) throw IllegalArgumentException("Probability 'p' must be in [0, 1].")

        val kVal = parseExpression(xStr)?.roundToInt()
            ?: throw IllegalArgumentException("Invalid success value 'k'. Must be an integer.")

        val pmfVal = binomialPmf(nVal, pVal, kVal)
        val cdfVal = binomialCdf(nVal, pVal, kVal)
        val strictLessVal = binomialCdf(nVal, pVal, kVal - 1)
        val upperCdfVal = 1.0 - strictLessVal
        val strictGreaterVal = 1.0 - cdfVal

        val meanVal = nVal * pVal
        val varVal = nVal * pVal * (1.0 - pVal)
        val sdVal = sqrt(varVal)

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val k2Val = parseExpression(x2Str)?.roundToInt()
            if (k2Val != null && k2Val >= kVal) {
                var sumRange = 0.0
                for (i in kVal..k2Val) {
                    sumRange += binomialPmf(nVal, pVal, i)
                }
                rangeLabelStr = "P($kVal ≤ X ≤ $k2Val)"
                rangeProbStr = sumRange.formatPrecision(precision)
            }
        }

        val pdfPmfLabel = "P(X = $kVal)"
        val pdfPmfFormatted = pmfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfPmfLabel to pdfPmfFormatted
            DistCalcMode.CDF -> "P(X ≤ $kVal)" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ $kVal)" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.BINOMIAL,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfPmfLabel,
            pdfOrPmfValue = pdfPmfFormatted,
            cdfLabel = "P(X ≤ $kVal)",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ $kVal)",
            upperCdfValue = upperCdfFormatted,
            strictCdfLabel = "P(X < $kVal)",
            strictCdfValue = strictLessVal.formatPrecision(precision),
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Binomial(n=$nVal, p=${pVal.formatPrecision(precision)})",
            extraInfo = mapOf("P(X > $kVal)" to strictGreaterVal.formatPrecision(precision))
        )
    }

    private fun binomialPmf(n: Int, p: Double, k: Int): Double {
        if (k < 0 || k > n) return 0.0
        if (p == 0.0) return if (k == 0) 1.0 else 0.0
        if (p == 1.0) return if (k == n) 1.0 else 0.0
        val logCoeff = logCombination(n, k)
        val logProb = logCoeff + k * ln(p) + (n - k) * ln(1.0 - p)
        return exp(logProb)
    }

    private fun binomialCdf(n: Int, p: Double, k: Int): Double {
        if (k < 0) return 0.0
        if (k >= n) return 1.0
        var sum = 0.0
        for (i in 0..k) {
            sum += binomialPmf(n, p, i)
        }
        return sum.coerceIn(0.0, 1.0)
    }

    // ==========================================
    // 2. POISSON DISTRIBUTION (lambda)
    // ==========================================
    fun calculatePoisson(
        lambdaStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val lambdaVal = parseExpression(lambdaStr)
            ?: throw IllegalArgumentException("Invalid rate 'λ'. Must be a positive number.")
        if (lambdaVal <= 0.0) throw IllegalArgumentException("Rate 'λ' must be strictly positive (> 0).")

        val kVal = parseExpression(xStr)?.roundToInt()
            ?: throw IllegalArgumentException("Invalid occurrences 'k'. Must be an integer.")

        val pmfVal = poissonPmf(lambdaVal, kVal)
        val cdfVal = poissonCdf(lambdaVal, kVal)
        val strictLessVal = poissonCdf(lambdaVal, kVal - 1)
        val upperCdfVal = 1.0 - strictLessVal
        val strictGreaterVal = 1.0 - cdfVal

        val meanVal = lambdaVal
        val varVal = lambdaVal
        val sdVal = sqrt(lambdaVal)

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val k2Val = parseExpression(x2Str)?.roundToInt()
            if (k2Val != null && k2Val >= kVal) {
                var sumRange = 0.0
                for (i in kVal..k2Val) {
                    sumRange += poissonPmf(lambdaVal, i)
                }
                rangeLabelStr = "P($kVal ≤ X ≤ $k2Val)"
                rangeProbStr = sumRange.formatPrecision(precision)
            }
        }

        val pdfPmfLabel = "P(X = $kVal)"
        val pdfPmfFormatted = pmfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfPmfLabel to pdfPmfFormatted
            DistCalcMode.CDF -> "P(X ≤ $kVal)" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ $kVal)" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.POISSON,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfPmfLabel,
            pdfOrPmfValue = pdfPmfFormatted,
            cdfLabel = "P(X ≤ $kVal)",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ $kVal)",
            upperCdfValue = upperCdfFormatted,
            strictCdfLabel = "P(X < $kVal)",
            strictCdfValue = strictLessVal.formatPrecision(precision),
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Poisson(λ=${lambdaVal.formatPrecision(precision)})",
            extraInfo = mapOf("P(X > $kVal)" to strictGreaterVal.formatPrecision(precision))
        )
    }

    private fun poissonPmf(lambda: Double, k: Int): Double {
        if (k < 0) return 0.0
        val logProb = k * ln(lambda) - lambda - logFactorial(k)
        return exp(logProb)
    }

    private fun poissonCdf(lambda: Double, k: Int): Double {
        if (k < 0) return 0.0
        var sum = 0.0
        for (i in 0..k) {
            sum += poissonPmf(lambda, i)
        }
        return sum.coerceIn(0.0, 1.0)
    }

    // ==========================================
    // 3. NORMAL DISTRIBUTION (mu, sigma)
    // ==========================================
    fun calculateNormal(
        meanStr: String,
        stdDevStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val muVal = parseExpression(meanStr)
            ?: throw IllegalArgumentException("Invalid mean 'μ'.")
        val sdVal = parseExpression(stdDevStr)
            ?: throw IllegalArgumentException("Invalid standard deviation 'σ'.")
        if (sdVal <= 0.0) throw IllegalArgumentException("Standard deviation 'σ' must be > 0.")

        val xVal = parseExpression(xStr)
            ?: throw IllegalArgumentException("Invalid value 'x'.")

        val zVal = (xVal - muVal) / sdVal
        val pdfVal = normalPdf(xVal, muVal, sdVal)
        val cdfVal = normalCdf(xVal, muVal, sdVal)
        val upperCdfVal = 1.0 - cdfVal

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val x2Val = parseExpression(x2Str)
            if (x2Val != null && x2Val >= xVal) {
                val cdf2 = normalCdf(x2Val, muVal, sdVal)
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${xVal.formatPrecision(precision)} ≤ X ≤ ${x2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${xVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(X ≤ ${xVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ ${xVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.NORMAL,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(X ≤ ${xVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ ${xVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = muVal.formatPrecision(precision),
            variance = (sdVal * sdVal).formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "N(μ=${muVal.formatPrecision(precision)}, σ=${sdVal.formatPrecision(precision)})",
            extraInfo = mapOf("Z-Score" to zVal.formatPrecision(precision))
        )
    }

    private fun erf(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1 else 1
        val absX = abs(x)

        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)

        return sign * y
    }

    private fun normalPdf(x: Double, mean: Double, stdDev: Double): Double {
        val z = (x - mean) / stdDev
        return (1.0 / (stdDev * sqrt(2.0 * PI))) * exp(-0.5 * z * z)
    }

    private fun normalCdf(x: Double, mean: Double, stdDev: Double): Double {
        val z = (x - mean) / stdDev
        return 0.5 * (1.0 + erf(z / sqrt(2.0)))
    }

    // ==========================================
    // 4. STUDENT'S T DISTRIBUTION (df)
    // ==========================================
    fun calculateStudentT(
        dfStr: String,
        tStr: String,
        t2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val dfVal = parseExpression(dfStr)
            ?: throw IllegalArgumentException("Invalid degrees of freedom 'df'.")
        if (dfVal <= 0.0) throw IllegalArgumentException("Degrees of freedom 'df' must be > 0.")

        val tVal = parseExpression(tStr)
            ?: throw IllegalArgumentException("Invalid t-score 't'.")

        val pdfVal = studentTPdf(dfVal, tVal)
        val cdfVal = studentTCdf(dfVal, tVal)
        val upperCdfVal = 1.0 - cdfVal
        val twoTailedVal = 2.0 * (1.0 - studentTCdf(dfVal, abs(tVal)))

        val meanStr = if (dfVal > 1.0) "0" else "Undefined"
        val varStr = if (dfVal > 2.0) (dfVal / (dfVal - 2.0)).formatPrecision(precision) else "Undefined"
        val sdStr = if (dfVal > 2.0) sqrt(dfVal / (dfVal - 2.0)).formatPrecision(precision) else "Undefined"

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !t2Str.isNullOrBlank()) {
            val t2Val = parseExpression(t2Str)
            if (t2Val != null && t2Val >= tVal) {
                val cdf2 = studentTCdf(dfVal, t2Val)
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${tVal.formatPrecision(precision)} ≤ T ≤ ${t2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${tVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(T ≤ ${tVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(T ≥ ${tVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(T in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.STUDENT,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(T ≤ ${tVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(T ≥ ${tVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanStr,
            variance = varStr,
            stdDev = sdStr,
            summary = "Student-t(df=${dfVal.formatPrecision(precision)})",
            extraInfo = mapOf("Two-Tailed p-value P(|T| ≥ |t|)" to twoTailedVal.formatPrecision(precision))
        )
    }

    private fun studentTPdf(df: Double, t: Double): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[PDF[StudentTDistribution[$df], $t]]").toString().toDouble()
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun studentTCdf(df: Double, t: Double): Double {
        return try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[CDF[StudentTDistribution[$df], $t]]").toString().toDouble()
            }
        } catch (_: Exception) {
            0.0
        }
    }

    // ==========================================
    // 5. UNIFORM DISTRIBUTION (a, b)
    // ==========================================
    fun calculateUniform(
        aStr: String,
        bStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val aVal = parseExpression(aStr)
            ?: throw IllegalArgumentException("Invalid lower bound 'a'.")
        val bVal = parseExpression(bStr)
            ?: throw IllegalArgumentException("Invalid upper bound 'b'.")
        if (aVal >= bVal) throw IllegalArgumentException("Lower bound 'a' must be strictly less than upper bound 'b'.")

        val xVal = parseExpression(xStr)
            ?: throw IllegalArgumentException("Invalid value 'x'.")

        val pdfVal = if (xVal in aVal..bVal) 1.0 / (bVal - aVal) else 0.0
        val cdfVal = when {
            xVal < aVal -> 0.0
            xVal > bVal -> 1.0
            else -> (xVal - aVal) / (bVal - aVal)
        }
        val upperCdfVal = 1.0 - cdfVal

        val meanVal = (aVal + bVal) / 2.0
        val varVal = (bVal - aVal).pow(2) / 12.0
        val sdVal = sqrt(varVal)

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val x2Val = parseExpression(x2Str)
            if (x2Val != null && x2Val >= xVal) {
                val cdf2 = when {
                    x2Val < aVal -> 0.0
                    x2Val > bVal -> 1.0
                    else -> (x2Val - aVal) / (bVal - aVal)
                }
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${xVal.formatPrecision(precision)} ≤ X ≤ ${x2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${xVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(X ≤ ${xVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ ${xVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.UNIFORM,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(X ≤ ${xVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ ${xVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Uniform(a=${aVal.formatPrecision(precision)}, b=${bVal.formatPrecision(precision)})"
        )
    }

    // ==========================================
    // 6. HYPERGEOMETRIC DISTRIBUTION (N, K, n)
    // ==========================================
    fun calculateHypergeometric(
        bigNStr: String,
        bigKStr: String,
        nStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val bigNVal = parseExpression(bigNStr)?.toInt()
            ?: throw IllegalArgumentException("Invalid population 'N'. Must be an integer > 0.")
        if (bigNVal <= 0) throw IllegalArgumentException("Population size 'N' must be > 0.")

        val bigKVal = parseExpression(bigKStr)?.toInt()
            ?: throw IllegalArgumentException("Invalid population successes 'K'. Must be an integer ≥ 0.")
        if (bigKVal < 0 || bigKVal > bigNVal) throw IllegalArgumentException("Successes 'K' must be between 0 and N.")

        val nVal = parseExpression(nStr)?.toInt()
            ?: throw IllegalArgumentException("Invalid sample size 'n'. Must be an integer ≥ 0.")
        if (nVal < 0 || nVal > bigNVal) throw IllegalArgumentException("Sample size 'n' must be between 0 and N.")

        val kVal = parseExpression(xStr)?.roundToInt()
            ?: throw IllegalArgumentException("Invalid success count 'k'. Must be an integer.")

        val pmfVal = hypergeometricPmf(bigNVal, bigKVal, nVal, kVal)
        val cdfVal = hypergeometricCdf(bigNVal, bigKVal, nVal, kVal)
        val strictLessVal = hypergeometricCdf(bigNVal, bigKVal, nVal, kVal - 1)
        val upperCdfVal = 1.0 - strictLessVal
        val strictGreaterVal = 1.0 - cdfVal

        val meanVal = nVal.toDouble() * bigKVal / bigNVal
        val varVal = if (bigNVal > 1) {
            nVal.toDouble() * (bigKVal.toDouble() / bigNVal) * ((bigNVal - bigKVal).toDouble() / bigNVal) * ((bigNVal - nVal).toDouble() / (bigNVal - 1))
        } else 0.0
        val sdVal = sqrt(max(0.0, varVal))

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val k2Val = parseExpression(x2Str)?.roundToInt()
            if (k2Val != null && k2Val >= kVal) {
                var sumRange = 0.0
                for (i in kVal..k2Val) {
                    sumRange += hypergeometricPmf(bigNVal, bigKVal, nVal, i)
                }
                rangeLabelStr = "P($kVal ≤ X ≤ $k2Val)"
                rangeProbStr = sumRange.formatPrecision(precision)
            }
        }

        val pdfPmfLabel = "P(X = $kVal)"
        val pdfPmfFormatted = pmfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfPmfLabel to pdfPmfFormatted
            DistCalcMode.CDF -> "P(X ≤ $kVal)" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ $kVal)" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.HYPERGEOMETRIC,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfPmfLabel,
            pdfOrPmfValue = pdfPmfFormatted,
            cdfLabel = "P(X ≤ $kVal)",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ $kVal)",
            upperCdfValue = upperCdfFormatted,
            strictCdfLabel = "P(X < $kVal)",
            strictCdfValue = strictLessVal.formatPrecision(precision),
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Hypergeometric(N=$bigNVal, K=$bigKVal, n=$nVal)",
            extraInfo = mapOf("P(X > $kVal)" to strictGreaterVal.formatPrecision(precision))
        )
    }

    private fun hypergeometricPmf(bigN: Int, bigK: Int, n: Int, k: Int): Double {
        val minK = max(0, n + bigK - bigN)
        val maxK = min(n, bigK)
        if (k < minK || k > maxK) return 0.0
        val logNum = logCombination(bigK, k) + logCombination(bigN - bigK, n - k)
        val logDen = logCombination(bigN, n)
        return exp(logNum - logDen)
    }

    private fun hypergeometricCdf(bigN: Int, bigK: Int, n: Int, k: Int): Double {
        val minK = max(0, n + bigK - bigN)
        val maxK = min(n, bigK)
        if (k < minK) return 0.0
        if (k >= maxK) return 1.0
        var sum = 0.0
        for (i in minK..k) {
            sum += hypergeometricPmf(bigN, bigK, n, i)
        }
        return sum.coerceIn(0.0, 1.0)
    }

    // ==========================================
    // 7. EXPONENTIAL DISTRIBUTION (lambda)
    // ==========================================
    fun calculateExponential(
        lambdaStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val lambdaVal = parseExpression(lambdaStr)
            ?: throw IllegalArgumentException("Invalid rate 'λ'. Must be a positive number.")
        if (lambdaVal <= 0.0) throw IllegalArgumentException("Rate 'λ' must be > 0.")

        val xVal = parseExpression(xStr)
            ?: throw IllegalArgumentException("Invalid value 'x'.")

        val pdfVal = if (xVal >= 0) lambdaVal * exp(-lambdaVal * xVal) else 0.0
        val cdfVal = if (xVal >= 0) 1.0 - exp(-lambdaVal * xVal) else 0.0
        val upperCdfVal = if (xVal >= 0) exp(-lambdaVal * xVal) else 1.0

        val meanVal = 1.0 / lambdaVal
        val varVal = 1.0 / (lambdaVal * lambdaVal)
        val sdVal = 1.0 / lambdaVal

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val x2Val = parseExpression(x2Str)
            if (x2Val != null && x2Val >= xVal) {
                val cdf2 = if (x2Val >= 0) 1.0 - exp(-lambdaVal * x2Val) else 0.0
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${xVal.formatPrecision(precision)} ≤ X ≤ ${x2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${xVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(X ≤ ${xVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ ${xVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.EXPONENTIAL,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(X ≤ ${xVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ ${xVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Exponential(λ=${lambdaVal.formatPrecision(precision)})"
        )
    }

    // ==========================================
    // 8. GEOMETRIC DISTRIBUTION (p)
    // ==========================================
    fun calculateGeometric(
        pStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val pVal = parseExpression(pStr)
            ?: throw IllegalArgumentException("Invalid probability 'p'. Must be in (0, 1].")
        if (pVal <= 0.0 || pVal > 1.0) throw IllegalArgumentException("Probability 'p' must be in (0, 1].")

        val kVal = parseExpression(xStr)?.roundToInt()
            ?: throw IllegalArgumentException("Invalid trial count 'k'. Must be an integer ≥ 1.")

        val pmfVal = if (kVal >= 1) (1.0 - pVal).pow(kVal - 1.0) * pVal else 0.0
        val cdfVal = if (kVal >= 1) 1.0 - (1.0 - pVal).pow(kVal.toDouble()) else 0.0
        val strictLessVal = if (kVal > 1) 1.0 - (1.0 - pVal).pow((kVal - 1).toDouble()) else 0.0
        val upperCdfVal = if (kVal >= 1) (1.0 - pVal).pow((kVal - 1).toDouble()) else 1.0

        val meanVal = 1.0 / pVal
        val varVal = (1.0 - pVal) / (pVal * pVal)
        val sdVal = sqrt(varVal)

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val k2Val = parseExpression(x2Str)?.roundToInt()
            if (k2Val != null && k2Val >= kVal) {
                val cdf2 = if (k2Val >= 1) 1.0 - (1.0 - pVal).pow(k2Val.toDouble()) else 0.0
                val rangeProb = cdf2 - strictLessVal
                rangeLabelStr = "P($kVal ≤ X ≤ $k2Val)"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfPmfLabel = "P(X = $kVal)"
        val pdfPmfFormatted = pmfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfPmfLabel to pdfPmfFormatted
            DistCalcMode.CDF -> "P(X ≤ $kVal)" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(X ≥ $kVal)" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(X in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.GEOMETRIC,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfPmfLabel,
            pdfOrPmfValue = pdfPmfFormatted,
            cdfLabel = "P(X ≤ $kVal)",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(X ≥ $kVal)",
            upperCdfValue = upperCdfFormatted,
            strictCdfLabel = "P(X < $kVal)",
            strictCdfValue = strictLessVal.formatPrecision(precision),
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "Geometric(p=${pVal.formatPrecision(precision)})"
        )
    }

    // ==========================================
    // 9. CHI-SQUARE DISTRIBUTION (df)
    // ==========================================
    fun calculateChiSquare(
        dfStr: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val dfVal = parseExpression(dfStr)
            ?: throw IllegalArgumentException("Invalid degrees of freedom 'df'.")
        if (dfVal <= 0.0) throw IllegalArgumentException("Degrees of freedom 'df' must be > 0.")

        val xVal = parseExpression(xStr)
            ?: throw IllegalArgumentException("Invalid value 'x'.")

        val pdfVal = try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[PDF[ChiSquareDistribution[$dfVal], $xVal]]").toString().toDouble()
            }
        } catch (_: Exception) { 0.0 }

        val cdfVal = try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[CDF[ChiSquareDistribution[$dfVal], $xVal]]").toString().toDouble()
            }
        } catch (_: Exception) { 0.0 }

        val upperCdfVal = 1.0 - cdfVal

        val meanVal = dfVal
        val varVal = 2.0 * dfVal
        val sdVal = sqrt(varVal)

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val x2Val = parseExpression(x2Str)
            if (x2Val != null && x2Val >= xVal) {
                val cdf2 = try {
                    SymjaUtils.evaluate { eval ->
                        eval.eval("N[CDF[ChiSquareDistribution[$dfVal], $x2Val]]").toString().toDouble()
                    }
                } catch (_: Exception) { 0.0 }
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${xVal.formatPrecision(precision)} ≤ χ² ≤ ${x2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${xVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(χ² ≤ ${xVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(χ² ≥ ${xVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(χ² in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.CHI_SQUARE,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(χ² ≤ ${xVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(χ² ≥ ${xVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanVal.formatPrecision(precision),
            variance = varVal.formatPrecision(precision),
            stdDev = sdVal.formatPrecision(precision),
            summary = "χ²(df=${dfVal.formatPrecision(precision)})"
        )
    }

    // ==========================================
    // 10. F-DISTRIBUTION (df1, df2)
    // ==========================================
    fun calculateFDistribution(
        df1Str: String,
        df2Str: String,
        xStr: String,
        x2Str: String? = null,
        calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
        precision: Int = 4
    ): DistributionsResult {
        val df1Val = parseExpression(df1Str)
            ?: throw IllegalArgumentException("Invalid numerator df₁.")
        val df2Val = parseExpression(df2Str)
            ?: throw IllegalArgumentException("Invalid denominator df₂.")
        if (df1Val <= 0.0 || df2Val <= 0.0) throw IllegalArgumentException("Degrees of freedom must be > 0.")

        val xVal = parseExpression(xStr)
            ?: throw IllegalArgumentException("Invalid value 'x'.")

        val pdfVal = try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[PDF[FRatioDistribution[$df1Val, $df2Val], $xVal]]").toString().toDouble()
            }
        } catch (_: Exception) { 0.0 }

        val cdfVal = try {
            SymjaUtils.evaluate { eval ->
                eval.eval("N[CDF[FRatioDistribution[$df1Val, $df2Val], $xVal]]").toString().toDouble()
            }
        } catch (_: Exception) { 0.0 }

        val upperCdfVal = 1.0 - cdfVal

        val meanStr = if (df2Val > 2.0) (df2Val / (df2Val - 2.0)).formatPrecision(precision) else "Undefined"
        val varStr = if (df2Val > 4.0) {
            val num = 2.0 * df2Val * df2Val * (df1Val + df2Val - 2.0)
            val den = df1Val * (df2Val - 2.0).pow(2) * (df2Val - 4.0)
            (num / den).formatPrecision(precision)
        } else "Undefined"
        val sdStr = if (df2Val > 4.0) {
            val num = 2.0 * df2Val * df2Val * (df1Val + df2Val - 2.0)
            val den = df1Val * (df2Val - 2.0).pow(2) * (df2Val - 4.0)
            sqrt(num / den).formatPrecision(precision)
        } else "Undefined"

        var rangeProbStr: String? = null
        var rangeLabelStr: String? = null

        if (calcMode == DistCalcMode.RANGE && !x2Str.isNullOrBlank()) {
            val x2Val = parseExpression(x2Str)
            if (x2Val != null && x2Val >= xVal) {
                val cdf2 = try {
                    SymjaUtils.evaluate { eval ->
                        eval.eval("N[CDF[FRatioDistribution[$df1Val, $df2Val], $x2Val]]").toString().toDouble()
                    }
                } catch (_: Exception) { 0.0 }
                val rangeProb = cdf2 - cdfVal
                rangeLabelStr = "P(${xVal.formatPrecision(precision)} ≤ F ≤ ${x2Val.formatPrecision(precision)})"
                rangeProbStr = rangeProb.formatPrecision(precision)
            }
        }

        val pdfLabel = "f(${xVal.formatPrecision(precision)})"
        val pdfFormatted = pdfVal.formatPrecision(precision)
        val cdfFormatted = cdfVal.formatPrecision(precision)
        val upperCdfFormatted = upperCdfVal.formatPrecision(precision)

        val (primaryLabel, primaryVal) = when (calcMode) {
            DistCalcMode.PDF_PMF -> pdfLabel to pdfFormatted
            DistCalcMode.CDF -> "P(F ≤ ${xVal.formatPrecision(precision)})" to cdfFormatted
            DistCalcMode.UPPER_CDF -> "P(F ≥ ${xVal.formatPrecision(precision)})" to upperCdfFormatted
            DistCalcMode.RANGE -> (rangeLabelStr ?: "P(F in range)") to (rangeProbStr ?: "N/A")
        }

        return DistributionsResult(
            type = DistributionsType.F_DISTRIBUTION,
            calcMode = calcMode,
            primaryResultLabel = primaryLabel,
            primaryResultValue = primaryVal,
            pdfOrPmfLabel = pdfLabel,
            pdfOrPmfValue = pdfFormatted,
            cdfLabel = "P(F ≤ ${xVal.formatPrecision(precision)})",
            cdfValue = cdfFormatted,
            upperCdfLabel = "P(F ≥ ${xVal.formatPrecision(precision)})",
            upperCdfValue = upperCdfFormatted,
            rangeProbabilityLabel = rangeLabelStr,
            rangeProbabilityValue = rangeProbStr,
            mean = meanStr,
            variance = varStr,
            stdDev = sdStr,
            summary = "F(df₁=${df1Val.formatPrecision(precision)}, df₂=${df2Val.formatPrecision(precision)})"
        )
    }
}
