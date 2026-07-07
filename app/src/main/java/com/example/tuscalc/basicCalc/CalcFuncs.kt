package com.example.tuscalc.basicCalc

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
    fun calculateExpression(input: String, variables: Map<String, Double> = emptyMap(), useRadians: Boolean = false): Double {
        if (input.isBlank()) return 0.0
        return try {
            // Clean up the input string: replace visual operators with math ones
            val cleanedInput = input.replace("÷", "/")
                .replace("×", "*")
                .replace("√", "sqrt")
                .replace("π", "pi")

            evaluate(cleanedInput, variables, useRadians)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun evaluate(expression: String, variables: Map<String, Double>, useRadians: Boolean): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

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
                // Skip trailing spaces
                while (ch == ' '.code) nextChar()
                if (pos < expression.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> {
                            val startPos = pos
                            val y = parseTerm()
                            if (expression.substring(startPos, pos).trim().endsWith('%')) {
                                x += x * (y * 100) * 0.01
                            } else {
                                x += y
                            }
                        }
                        eat('-'.code) -> {
                            val startPos = pos
                            val y = parseTerm()
                            if (expression.substring(startPos, pos).trim().endsWith('%')) {
                                x -= x * (y * 100) * 0.01
                            } else {
                                x -= y
                            }
                        }
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val divisor = parseFactor()
                            if (divisor == 0.0) throw ArithmeticException("Division by zero")
                            x /= divisor
                        }
                        // Implicit multiplication ONLY if we have '(' or a letter (function/constant/variable)
                        // AND we haven't just processed an operator (which would have been eaten by the 'when' above)
                        peekImplicit() -> x *= parseFactor()
                        else -> return x
                    }
                }
            }

            private fun peekImplicit(): Boolean {
                var tempPos = pos
                while (tempPos < expression.length && expression[tempPos] == ' ') {
                    tempPos++
                }
                if (tempPos >= expression.length) return false
                val next = expression[tempPos]

                return next == '(' || (next in 'a'..'z')
            }

            fun parseFactor(): Double {
                // Skip leading spaces
                while (ch == ' '.code) nextChar()

                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code) // Auto-close: try to eat, but don't fail if missing
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions or variables
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = expression.substring(startPos, pos)
                    x = when {
                        variables.containsKey(func) -> variables[func]!!
                        func == "pi" -> PI
                        func == "e" -> E
                        listOf("sqrt", "sin", "cos", "tan", "log", "ln", "atan", "acos", "asin").contains(func) -> {
                            val arg = parseFactor()
                            when (func) {
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
                        else -> throw RuntimeException("Unknown function or variable: $func")
                    }
                } else {
                    return Double.NaN
                }

                // Postfix operators
                while (true) {
                    while (ch == ' '.code) nextChar() // Skip spaces before postfix operators
                    if (eat('%'.code)) {
                        x /= 100.0
                    } else if (eat('!'.code)) {
                        x = factorial(x)
                    } else if (eat('^'.code)) {
                        val exponent = parseFactor()
                        x = when {
                            x < 0 && abs(exponent - (1.0 / 3.0)) < 1e-9 -> -abs(x).pow(1.0 / 3.0)
                            x < 0 && abs(exponent - (1.0 / 5.0)) < 1e-9 -> -abs(x).pow(1.0 / 5.0)
                            x < 0 && abs(exponent - (1.0 / 7.0)) < 1e-9 -> -abs(x).pow(1.0 / 7.0)
                            else -> x.pow(exponent)
                        }
                    } else {
                        break
                    }
                }

                return x
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

    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Infinity"

        // If it's effectively an integer, show as integer
        if (value == floor(value) && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            return value.toLong().toString()
        }

        // Using DecimalFormat for precise rounding to 4th digit
        val df = DecimalFormat("0.####")
        df.roundingMode = RoundingMode.HALF_UP
        val result = df.format(value).replace(",", ".")
        return if (result == "-0") "0" else result
    }
}