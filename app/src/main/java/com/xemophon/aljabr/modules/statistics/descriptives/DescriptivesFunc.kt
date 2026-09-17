package com.xemophon.aljabr.modules.statistics.descriptives

import com.xemophon.aljabr.data.SymjaUtils
import org.hipparchus.stat.correlation.PearsonsCorrelation
import org.hipparchus.stat.regression.SimpleRegression
import kotlin.math.pow
import kotlin.math.sqrt

data class DescriptivesResult(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val mode: List<Double>,
    val varianceSample: Double,
    val variancePopulation: Double,
    val stdDevSample: Double,
    val stdDevPopulation: Double,
    val min: Double,
    val max: Double,
    val range: Double,
    val pearsonsCorrelation: PearsonsCorrelation? = null,
    val regression: SimpleRegression? = null,
    val sortedValues: List<Double>,
    val error: String? = null
)

object DescriptivesFunc {

    fun parseValue(input: String): Double? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toDoubleOrNull()?.let { return it }
        return try {
            val prep = SymjaUtils.prepareForSymja(trimmed)
            val res = SymjaUtils.evaluate { eval ->
                eval.eval("N($prep)").toString()
            }
            res.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun calculate(rawInputs: List<String>): DescriptivesResult {
        if (rawInputs.isEmpty()) {
            return DescriptivesResult(
                count = 0, sum = 0.0, mean = 0.0, median = 0.0, mode = emptyList(),
                varianceSample = 0.0, variancePopulation = 0.0, stdDevSample = 0.0,
                stdDevPopulation = 0.0, min = 0.0, max = 0.0, range = 0.0,
                sortedValues = emptyList(), error = "Array is empty. Please add data members."
            )
        }

        val parsedNumbers = mutableListOf<Double>()
        val invalidEntries = mutableListOf<String>()

        for ((index, item) in rawInputs.withIndex()) {
            if (item.isBlank()) continue
            val num = parseValue(item)
            if (num != null) {
                parsedNumbers.add(num)
            } else {
                invalidEntries.add("Item #${index + 1}: '$item'")
            }
        }

        if (invalidEntries.isNotEmpty()) {
            return DescriptivesResult(
                count = 0, sum = 0.0, mean = 0.0, median = 0.0, mode = emptyList(),
                varianceSample = 0.0, variancePopulation = 0.0, stdDevSample = 0.0,
                stdDevPopulation = 0.0, min = 0.0, max = 0.0, range = 0.0,
                sortedValues = emptyList(),
                error = "Invalid numerical input(s): ${invalidEntries.joinToString(", ")}"
            )
        }

        if (parsedNumbers.isEmpty()) {
            return DescriptivesResult(
                count = 0, sum = 0.0, mean = 0.0, median = 0.0, mode = emptyList(),
                varianceSample = 0.0, variancePopulation = 0.0, stdDevSample = 0.0,
                stdDevPopulation = 0.0, min = 0.0, max = 0.0, range = 0.0,
                sortedValues = emptyList(), error = "Array is empty. Add data members to analyze."
            )
        }

        val count = parsedNumbers.size
        val sum = parsedNumbers.sum()
        val meanVal = mean(parsedNumbers)
        val medianVal = median(parsedNumbers)
        val modeList = mode(parsedNumbers)

        val varianceSample = variance(parsedNumbers, isSample = true)
        val stdDevSample = standardDeviation(parsedNumbers, isSample = true)

        val variancePopulation = variance(parsedNumbers, isSample = false)
        val stdDevPopulation = standardDeviation(parsedNumbers, isSample = false)

        val sorted = parsedNumbers.sorted()
        val min = sorted.first()
        val max = sorted.last()
        val range = max - min

        // Pearson Correlation & Simple Linear Regression
        val indexList = List(count) { (it + 1).toDouble() }
        val regression = linearRegression(indexList, parsedNumbers)
        val pearsonsCorrelation = pearsonsCorrelation(indexList, parsedNumbers)

        return DescriptivesResult(
            count = count,
            sum = sum,
            mean = meanVal,
            median = medianVal,
            mode = modeList,
            varianceSample = varianceSample,
            variancePopulation = variancePopulation,
            stdDevSample = stdDevSample,
            stdDevPopulation = stdDevPopulation,
            min = min,
            max = max,
            range = range,
            pearsonsCorrelation = pearsonsCorrelation,
            regression = regression,
            sortedValues = sorted
        )
    }

    fun mean(data: List<Double>): Double = if (data.isEmpty()) 0.0 else data.sum() / data.size

    fun median(data: List<Double>): Double {
        if (data.isEmpty()) return 0.0
        val s = data.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    fun mode(data: List<Double>): List<Double> {
        if (data.isEmpty()) return emptyList()
        val freq = data.groupingBy { it }.eachCount()
        val maxF = freq.values.maxOrNull() ?: 0
        return if (maxF > 1) freq.filterValues { it == maxF }.keys.sorted() else emptyList()
    }

    fun variance(data: List<Double>, isSample: Boolean = true): Double {
        if (data.isEmpty()) return 0.0
        val m = mean(data)
        val sumSq = data.sumOf { (it - m).pow(2) }
        return if (isSample) (if (data.size > 1) sumSq / (data.size - 1) else 0.0) else (sumSq / data.size)
    }

    fun standardDeviation(data: List<Double>, isSample: Boolean = true): Double {
        return sqrt(variance(data, isSample))
    }

    fun pearsonsCorrelation(xData: List<Double>, yData: List<Double>): PearsonsCorrelation? {
        if (xData.size < 2 || xData.size != yData.size) return null
        val matrix = Array(xData.size) { i -> doubleArrayOf(xData[i], yData[i]) }
        return PearsonsCorrelation(matrix)
    }

    fun linearRegression(xData: List<Double>, yData: List<Double>): SimpleRegression? {
        if (xData.size < 2 || xData.size != yData.size) return null
        val regression = SimpleRegression()
        for (i in xData.indices) {
            regression.addData(xData[i], yData[i])
        }
        return regression
    }
}
