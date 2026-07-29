package com.xemophon.aljabr.basicCalc

import com.xemophon.aljabr.data.SymjaUtils
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalcFuncs {
    private const val PHI = 1.618033988749895

    private val visualToMathMap = mapOf(
        "÷" to "/",
        "×" to "*",
        "√" to "sqrt",
        "π" to "pi",
        "φ" to "phi",
        "j" to "j",
        "i" to "i"
    )

    fun calculateExpression(
        input: String,
        variables: Map<String, Double> = emptyMap(),
        useRadians: Boolean = false
    ): Double {
        if (input.isBlank()) return 0.0
        return try {
            var cleanedInput = input
            visualToMathMap.forEach { (visual, math) ->
                cleanedInput = cleanedInput.replace(visual, math)
            }
            evaluate(cleanedInput, variables, useRadians)
        } catch (e: Throwable) {
            Double.NaN
        }
    }

    fun calculateSymbolic(
        input: String,
        precision: Int = 4
    ): String {
        if (input.isBlank()) return ""
        return try {
            val cleaned = SymjaUtils.prepareForSymja(input)
            val eval = SymjaUtils.evaluator
            
            val result = eval.eval(cleaned)
            val resStr = result.toString()
            
            // Priority: Complex or Symbolic constants should stay formatted
            if (resStr.contains("I") || resStr.contains("GoldenRatio") || resStr.contains("Pi") || resStr.contains("E")) {
                 SymjaUtils.formatResult(resStr)
            } else {
                // Try to format as a nice decimal if it's a pure real number
                val d = resStr.toDoubleOrNull()
                if (d != null) {
                    formatResult(d, precision)
                } else {
                    SymjaUtils.formatResult(resStr)
                }
            }
        } catch (e: Throwable) {
            "Error"
        }
    }

    private fun evaluate(
        expression: String,
        variables: Map<String, Double>,
        useRadians: Boolean
    ): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            var recursionDepth = 0
            val MAX_DEPTH = 128

            private fun checkDepth() {
                if (++recursionDepth > MAX_DEPTH) throw RuntimeException("Expression too complex")
            }

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                while (ch == ' '.code) nextChar()
                return if (pos < expression.length) Double.NaN else x
            }

            fun parseExpression(): Double {
                checkDepth()
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> {
                            val startPos = pos
                            val y = parseTerm()
                            x += if (expression.substring(startPos, pos).trim()
                                    .endsWith('%')
                            ) x * (y * 100) * 0.01 else y
                        }

                        eat('-'.code) -> {
                            val startPos = pos
                            val y = parseTerm()
                            x -= if (expression.substring(startPos, pos).trim()
                                    .endsWith('%')
                            ) x * (y * 100) * 0.01 else y
                        }

                        else -> {
                            recursionDepth--
                            return x
                        }
                    }
                }
            }

            fun parseTerm(): Double {
                checkDepth()
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val divisor = parseFactor()
                            if (divisor == 0.0) throw ArithmeticException("Division by zero")
                            x /= divisor
                        }

                        peekImplicit() -> x *= parseFactor()
                        else -> {
                            recursionDepth--
                            return x
                        }
                    }
                }
            }

            private fun peekImplicit(): Boolean {
                var tempPos = pos
                while (tempPos < expression.length && expression[tempPos] == ' ') tempPos++
                if (tempPos >= expression.length) return false
                val next = expression[tempPos]
                return next == '(' || (next in 'a'..'z') || (next in 'A'..'Z')
            }

            fun parseFactor(): Double {
                checkDepth()
                while (ch == ' '.code) nextChar()
                if (eat('+'.code)) {
                    val res = parseFactor()
                    recursionDepth--
                    return res
                }
                if (eat('-'.code)) {
                    val res = -parseFactor()
                    recursionDepth--
                    return res
                }

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) {
                    while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) nextChar()
                    val func = expression.substring(startPos, pos).lowercase()
                    x = when {
                        variables.containsKey(func) -> variables[func]!!
                        func == "pi" -> PI
                        func == "e" -> E
                        func == "phi" -> PHI
                        func == "i" || func == "j" -> 0.0 // Basic calc is real-only
                        else -> handleFunction(func)
                    }
                } else {
                    recursionDepth--
                    return Double.NaN
                }

                val res = parsePostfix(x)
                recursionDepth--
                return res
            }

            private fun handleFunction(func: String): Double {
                val arg = parseFactor()
                return when (func) {
                    "sqrt" -> sqrt(arg)
                    "sin" -> if (useRadians) sin(arg) else sin(Math.toRadians(arg))
                    "cos" -> if (useRadians) cos(arg) else cos(Math.toRadians(arg))
                    "tan" -> if (useRadians) tan(arg) else tan(Math.toRadians(arg))
                    "log" -> log10(arg)
                    "asin" -> if (useRadians) asin(arg) else Math.toDegrees(asin(arg))
                    "acos" -> if (useRadians) acos(arg) else Math.toDegrees(acos(arg))
                    "atan" -> if (useRadians) atan(arg) else Math.toDegrees(atan(arg))
                    "ln" -> ln(arg)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            }

            private fun parsePostfix(initialX: Double): Double {
                var x = initialX
                while (true) {
                    while (ch == ' '.code) nextChar()
                    when {
                        eat('%'.code) -> x /= 100.0
                        eat('!'.code) -> x = factorial(x)
                        eat('^'.code) -> {
                            val exponent = parseFactor()
                            x = handlePower(x, exponent)
                        }

                        else -> return x
                    }
                }
            }

            private fun handlePower(base: Double, exponent: Double): Double {
                return if (base < 0) {
                    when {
                        abs(exponent - (1.0 / 3.0)) < 1e-9 -> -abs(base).pow(1.0 / 3.0)
                        abs(exponent - (1.0 / 5.0)) < 1e-9 -> -abs(base).pow(1.0 / 5.0)
                        abs(exponent - (1.0 / 7.0)) < 1e-9 -> -abs(base).pow(1.0 / 7.0)
                        else -> base.pow(exponent)
                    }
                } else base.pow(exponent)
            }

            private fun factorial(n: Double): Double {
                if (n < 0 || n != floor(n)) return Double.NaN
                if (n > 170) return Double.POSITIVE_INFINITY
                var res = 1.0
                for (i in 1..n.toInt()) res *= i
                return res
            }
        }.parse()
    }

    fun formatResult(value: Double, precision: Int = 4): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "∞"

        checkPiDerivation(value)?.let { return it }
        checkPhiDerivation(value)?.let { return it }
        checkEDerivation(value)?.let { return it }

        if (value == floor(value) && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
            return value.toLong().toString()
        }
        
        val pattern = if (precision <= 0) "0" else "0." + "#".repeat(precision)
        val df = DecimalFormat(pattern).apply { roundingMode = RoundingMode.HALF_UP }
        val result = df.format(value).replace(",", ".")
        return if (result == "-0") "0" else result
    }

    private fun checkPhiDerivation(value: Double): String? {
        if (abs(value - PHI) < 1e-10) return "φ"
        if (abs(value + PHI) < 1e-10) return "-φ"
        return null
    }

    private fun checkEDerivation(value: Double): String? {
        if (abs(value - E) < 1e-10) return "e"
        if (abs(value + E) < 1e-10) return "-e"
        return null
    }

    private fun checkPiDerivation(value: Double): String? {
        if (abs(value) < 1e-12) return null
        val piRatio = value / PI
        
        // Check for integer multiples n*PI
        val n = Math.round(piRatio).toInt()
        if (abs(piRatio - n) < 1e-10) {
            return when (n) {
                0 -> null
                1 -> "π"
                -1 -> "-π"
                else -> "${n}π"
            }
        }

        // Check for fractions (m/d)*PI
        for (d in 2..16) {
            val mDouble = piRatio * d
            val m = Math.round(mDouble).toInt()
            if (abs(mDouble - m) < 1e-10) {
                val common = gcd(abs(m), d)
                val num = m / common
                val den = d / common

                return if (den == 1) {
                    when (num) {
                        1 -> "π"
                        -1 -> "-π"
                        else -> "${num}π"
                    }
                } else {
                    val sign = if (num < 0) "-" else ""
                    val absNum = abs(num)
                    val numeratorStr = if (absNum == 1) "π" else "${absNum}π"
                    "$sign$numeratorStr/$den"
                }
            }
        }
        return null
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }
}
