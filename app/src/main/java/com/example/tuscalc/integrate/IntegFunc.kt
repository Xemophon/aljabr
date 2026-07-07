package com.example.tuscalc.integrate

import com.example.tuscalc.basicCalc.CalcFuncs
import java.util.Locale

object IntegFunc {
    fun integrate(
        expression: String,
        lower: Double,
        upper: Double,
        useRadians: Boolean = true
    ): Double {
        if (lower == upper) return 0.0
        
        // Use Simpson's 1/3 rule
        val n = 1000 // Number of intervals (must be even)
        val h = (upper - lower) / n
        
        var sum = evaluateAt(expression, lower, useRadians) + evaluateAt(expression, upper, useRadians)
        
        for (i in 1 until n) {
            val x = lower + i * h
            val factor = if (i % 2 == 0) 2 else 4
            val valAtX = evaluateAt(expression, x, useRadians)
            if (valAtX.isNaN() || valAtX.isInfinite()) return Double.NaN
            sum += factor * valAtX
        }
        
        return (h / 3) * sum
    }

    private fun evaluateAt(expression: String, x: Double, useRadians: Boolean): Double {
        return CalcFuncs.calculateExpression(expression, mapOf("x" to x), useRadians)
    }

    fun integrateIndefinite(expression: String): String {
        val cleaned = expression.replace(" ", "").replace("×", "*").replace("÷", "/")
        if (cleaned.isEmpty()) return ""

        val terms = splitTerms(cleaned)
        val integratedTerms = terms.map { integrateTerm(it) }

        if (integratedTerms.any { it == null }) {
            return "∫($expression)dx" // Return as integral if too complex
        }

        val result = integratedTerms.joinToString(" ") { term ->
            if (term!!.startsWith("-")) term else "+ $term"
        }.trim()

        val finalResult = if (result.startsWith("+ ")) result.substring(2) else result
        return if (finalResult.isEmpty()) "C" else "$finalResult + C"
    }

    private fun splitTerms(expression: String): List<String> {
        val terms = mutableListOf<String>()
        var currentTerm = StringBuilder()
        var depth = 0
        
        for (i in expression.indices) {
            val char = expression[i]
            if (depth == 0 && (char == '+' || (char == '-' && i > 0 && expression[i-1] != '(' && expression[i-1] != '^' && expression[i-1] != '*' && expression[i-1] != '/'))) {
                if (currentTerm.isNotEmpty()) terms.add(currentTerm.toString())
                currentTerm = StringBuilder()
                if (char == '-') currentTerm.append(char)
            } else {
                currentTerm.append(char)
                if (char == '(') depth++
                else if (char == ')') depth--
            }
        }
        if (currentTerm.isNotEmpty()) terms.add(currentTerm.toString())
        return terms
    }

    private fun integrateTerm(term: String): String? {
        val t = term.trim()
        
        // 1. Constant: 5 -> 5x
        if (t.matches(Regex("^[-+]?\\d+(\\.\\d+)?$"))) {
            return if (t == "0") "" else "${t}x"
        }

        // 2. x^n: x^2 -> (1/3)x^3
        val powerRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?x(\\^([-+]?\\d+\\.?\\d*))?$")
        val match = powerRegex.find(t)
        if (match != null) {
            val coeffStr = match.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            
            val expStr = match.groups[3]?.value ?: "1"
            val exp = expStr.toDoubleOrNull() ?: return null
            
            if (exp == -1.0) return if (coeff == 1.0) "ln|x|" else "${formatCoeff(coeff)}ln|x|"
            
            val newExp = exp + 1
            val newCoeff = coeff / newExp
            
            val c = formatCoeff(newCoeff)
            val e = formatExp(newExp)
            
            return when {
                e == "1" -> "${c}x"
                c == "1" -> "x^$e"
                c == "-1" -> "-x^$e"
                else -> "${c}x^$e"
            }
        }

        // 3. Trig with optional coefficient
        val trigRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?(sin|cos)\\(x\\)$")
        val trigMatch = trigRegex.find(t)
        if (trigMatch != null) {
            val coeffStr = trigMatch.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            val func = trigMatch.groups[2]?.value
            return if (func == "sin") {
                val newCoeff = -coeff
                "${formatCoeff(newCoeff)}cos(x)"
            } else {
                "${formatCoeff(coeff)}sin(x)"
            }
        }

        // 4. Exponential e^x with optional coefficient
        val expRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?e\\^x$")
        val expMatch = expRegex.find(t)
        if (expMatch != null) {
            return t
        }

        // 5. 1/x with optional coefficient
        val invRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?(1/x|x\\^-1)$")
        val invMatch = invRegex.find(t)
        if (invMatch != null) {
            val coeffStr = invMatch.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            val c = formatCoeff(coeff)
            return if (c == "") "ln|x|" else if (c == "-") "-ln|x|" else "${c}ln|x|"
        }

        // 6. Integration by Parts: x*sin(x) and x*cos(x)
        val xTrigRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?x\\*?(sin|cos)\\(x\\)$")
        val xTrigMatch = xTrigRegex.find(t)
        if (xTrigMatch != null) {
            val coeffStr = xTrigMatch.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            val func = xTrigMatch.groups[2]?.value
            val c = formatCoeff(coeff)
            return if (func == "sin") {
                // ∫ x sin(x) dx = sin(x) - x cos(x)
                if (c == "") "sin(x) - x*cos(x)" 
                else if (c == "-") "-sin(x) + x*cos(x)"
                else "${c}sin(x) - ${c}x*cos(x)"
            } else {
                // ∫ x cos(x) dx = cos(x) + x sin(x)
                if (c == "") "cos(x) + x*sin(x)"
                else if (c == "-") "-cos(x) - x*sin(x)"
                else "${c}cos(x) + ${c}x*sin(x)"
            }
        }

        // 7. Integration by Parts: x*e^x
        val xExpRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?x\\*?e\\^x$")
        val xExpMatch = xExpRegex.find(t)
        if (xExpMatch != null) {
            val coeffStr = xExpMatch.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            val c = formatCoeff(coeff)
            return if (c == "") "(x-1)e^x" else if (c == "-") "-(x-1)e^x" else "${c}(x-1)e^x"
        }

        // 8. ln(x) -> x*ln(x) - x
        val lnRegex = Regex("^([-+]?\\d*\\.?\\d*)?\\*?ln\\(x\\)$")
        val lnMatch = lnRegex.find(t)
        if (lnMatch != null) {
            val coeffStr = lnMatch.groups[1]?.value?.replace("+", "") ?: ""
            val coeff = when {
                coeffStr.isEmpty() || coeffStr == "+" -> 1.0
                coeffStr == "-" -> -1.0
                else -> coeffStr.toDoubleOrNull() ?: return null
            }
            val c = formatCoeff(coeff)
            return if (c == "") "(x*ln(x) - x)" else if (c == "-") "-(x*ln(x) - x)" else "${c}(x*ln(x) - x)"
        }

        return null // Too complex for simple rules
    }

    private fun formatCoeff(d: Double): String {
        if (d == 1.0) return ""
        if (d == -1.0) return "-"
        val s = String.format(Locale.US, "%.4f", d).replace(",", ".")
        val trimmed = s.trimEnd('0').trimEnd('.')
        return if (trimmed == "-0") "0" else trimmed
    }

    private fun formatExp(d: Double): String {
        val s = String.format(Locale.US, "%.4f", d).replace(",", ".")
        return s.trimEnd('0').trimEnd('.')
    }
}
