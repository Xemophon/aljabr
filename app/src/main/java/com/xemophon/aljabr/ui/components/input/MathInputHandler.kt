package com.xemophon.aljabr.ui.components.input

import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.Constants
import com.xemophon.aljabr.ui.components.buttons.ScientificType

data class InputState(
    val text: String,
    val cursorIndex: Int
)

object MathInputHandler {

    private val binaryOperators = setOf('+', '-', '×', '÷', '*', '/', '^')

    fun isBinaryOperator(char: Char?): Boolean {
        return char != null && char in binaryOperators
    }

    fun isImplicitMultiplicationNeeded(text: String, cursorIndex: Int): Boolean {
        if (text.isEmpty() || text == "0" || text == "Error" || text == "NaN" || text == "Infinity") return false
        val clampedIndex = cursorIndex.coerceIn(0, text.length)
        val lastChar = if (clampedIndex > 0) text[clampedIndex - 1] else null
        return lastChar != null && (
            lastChar.isDigit() ||
            lastChar.isLetter() ||
            lastChar == ')' ||
            lastChar == ']' ||
            lastChar == 'π' ||
            lastChar == 'φ' ||
            lastChar == '%' ||
            lastChar == '!'
        )
    }

    fun insertText(
        currentText: String,
        cursorIndex: Int,
        toInsert: String,
        applyImplicitMultiplication: Boolean = false
    ): InputState {
        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val textBefore = currentText.substring(0, safeCursor)
        val textBeforeTrimmed = textBefore.dropLastWhile { it == ' ' }
        val lastCharTrimmed = textBeforeTrimmed.lastOrNull()

        if (currentText == "Error" || currentText == "NaN" || currentText == "Infinity") {
            val startText = if (toInsert.contains(Regex("[0-9]")) || toInsert.endsWith("(")) toInsert else "0"
            return InputState(startText, startText.length)
        }

        // Prevent invalid % insertion after operator, (, %, ., or when empty (except "0")
        if (toInsert == "%") {
            if (currentText != "0" && (lastCharTrimmed == null || isBinaryOperator(lastCharTrimmed) || lastCharTrimmed == '(' || lastCharTrimmed == '%' || lastCharTrimmed == '.')) {
                return InputState(currentText, safeCursor)
            }
        }

        // Prevent invalid ! insertion after operator, (, !, ., or when empty
        if (toInsert == "!") {
            if (lastCharTrimmed == null || isBinaryOperator(lastCharTrimmed) || lastCharTrimmed == '(' || lastCharTrimmed == '!' || lastCharTrimmed == '.') {
                return InputState(currentText, safeCursor)
            }
        }

        // Prevent multiple decimal points in a single number
        if (toInsert == ".") {
            val numberSegment = textBeforeTrimmed.takeLastWhile { it.isDigit() || it == '.' }
            if (numberSegment.contains('.')) {
                return InputState(currentText, safeCursor)
            }
            if (lastCharTrimmed == null || isBinaryOperator(lastCharTrimmed) || lastCharTrimmed == '(') {
                return insertText(currentText, safeCursor, "0.", applyImplicitMultiplication)
            }
        }

        val prefix = if (applyImplicitMultiplication && isImplicitMultiplicationNeeded(currentText, safeCursor)) "*" else ""
        val finalInsert = "$prefix$toInsert"

        if (currentText == "0") {
            val trimmedInsert = finalInsert.trim()
            if (trimmedInsert == ".") {
                return InputState("0.", 2)
            } else if (trimmedInsert == "%") {
                return InputState("0%", 2)
            } else if (trimmedInsert.length == 1 && isBinaryOperator(trimmedInsert[0])) {
                return InputState("0$finalInsert", 1 + finalInsert.length)
            } else {
                return InputState(finalInsert, finalInsert.length)
            }
        }

        val sb = StringBuilder(currentText)
        sb.insert(safeCursor, finalInsert)
        val newText = sb.toString()
        val newCursor = safeCursor + finalInsert.length
        return InputState(newText, newCursor)
    }

    fun handleSymbol(currentText: String, cursorIndex: Int, symbol: String): InputState {
        if (symbol == "0" && currentText == "0") {
            return InputState("0", 1)
        }

        if (currentText == "Error" || currentText == "NaN" || currentText == "Infinity") {
            val trimmed = symbol.trim()
            val startText = if (trimmed.length == 1 && isBinaryOperator(trimmed[0])) {
                if (trimmed == "-") "-" else "0$symbol"
            } else if (trimmed == ".") {
                "0."
            } else if (trimmed == "%") {
                "0%"
            } else {
                symbol
            }
            return InputState(startText, startText.length)
        }

        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val textBefore = currentText.substring(0, safeCursor)
        val textAfter = currentText.substring(safeCursor)

        val trimmedSymbol = symbol.trim()
        val isNewSymbolOperator = trimmedSymbol.length == 1 && isBinaryOperator(trimmedSymbol[0])

        if (isNewSymbolOperator) {
            val opChar = trimmedSymbol[0]

            val textBeforeTrimmed1 = textBefore.dropLastWhile { it == ' ' }
            val last1 = textBeforeTrimmed1.lastOrNull()

            // Do not allow binary operators (except unary - or +) at start or after '('
            if (last1 == null || last1 == '(') {
                if (opChar != '-' && opChar != '+') {
                    return InputState(currentText, safeCursor)
                }
            }

            if (last1 != null && isBinaryOperator(last1)) {
                val textBeforeTrimmed2 = textBeforeTrimmed1.dropLast(1).dropLastWhile { it == ' ' }
                val last2 = textBeforeTrimmed2.lastOrNull()

                if (isBinaryOperator(last2)) {
                    // Two operators already present (e.g. "5 * -" or "5*-")
                    if (opChar == '-') {
                        // Do not allow three consecutive operators (e.g. "5 * --")
                        return InputState(currentText, safeCursor)
                    } else {
                        // Replace both operators with the new operator (e.g., "5 * -" + "+" -> "5+")
                        val prefix = textBeforeTrimmed2.dropLast(1).dropLastWhile { it == ' ' }
                        val newText = prefix + symbol + textAfter
                        val newCursor = (prefix.length + symbol.length).coerceAtLeast(0)
                        return InputState(newText, newCursor)
                    }
                } else {
                    // One operator present (e.g. "5 + " or "5*")
                    if (opChar == '-') {
                        if (last1 == '-') {
                            // Do not allow double minus "--"
                            return InputState(currentText, safeCursor)
                        } else {
                            // Append unary minus after binary operator ("5 *" -> "5*-")
                            val newText = textBeforeTrimmed1 + symbol + textAfter
                            val newCursor = textBeforeTrimmed1.length + symbol.length
                            return InputState(newText, newCursor)
                        }
                    } else {
                        // Replace the single operator with the new operator ("5 +" + "*" -> "5*")
                        val prefix = textBeforeTrimmed1.dropLast(1).dropLastWhile { it == ' ' }
                        val newText = prefix + symbol + textAfter
                        val newCursor = (prefix.length + symbol.length).coerceAtLeast(0)
                        return InputState(newText, newCursor)
                    }
                }
            }
        }

        val lastChar = if (safeCursor > 0) currentText[safeCursor - 1] else null
        val isDigitOrDot = symbol.all { it.isDigit() || it == '.' }
        val isLastCharDigitOrDot = lastChar != null && (lastChar.isDigit() || lastChar == '.')

        val applyImplicit = (isDigitOrDot && isImplicitMultiplicationNeeded(currentText, safeCursor) && !isLastCharDigitOrDot) ||
                (symbol == "(" && isImplicitMultiplicationNeeded(currentText, safeCursor))

        return insertText(currentText, safeCursor, symbol, applyImplicitMultiplication = applyImplicit)
    }

    fun handleBrackets(currentText: String, cursorIndex: Int): InputState {
        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val openBrackets = currentText.count { it == '(' }
        val closedBrackets = currentText.count { it == ')' }

        return if (openBrackets > closedBrackets && isImplicitMultiplicationNeeded(currentText, safeCursor)) {
            insertText(currentText, safeCursor, ")", applyImplicitMultiplication = false)
        } else {
            insertText(currentText, safeCursor, "(", applyImplicitMultiplication = true)
        }
    }

    fun handleScientific(currentText: String, cursorIndex: Int, action: CalcButtonAction.Scientific): InputState {
        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)

        if (action.type == ScientificType.FACTORIAL) {
            if (currentText != "Error" && currentText != "NaN" && currentText != "Infinity") {
                return insertText(currentText, safeCursor, "!")
            }
            return InputState(currentText, safeCursor)
        }

        val toInsert = when (action.type) {
            ScientificType.SQRT -> "√("
            ScientificType.ASIN -> "asin("
            ScientificType.ACOS -> "acos("
            ScientificType.ATAN -> "atan("
            ScientificType.SINH -> "sinh("
            ScientificType.COSH -> "cosh("
            ScientificType.TANH -> "tanh("
            ScientificType.ASINH -> "asinh("
            ScientificType.ACOSH -> "acosh("
            ScientificType.ATANH -> "atanh("
            ScientificType.LN -> "ln("
            ScientificType.SIN -> "sin("
            ScientificType.COS -> "cos("
            ScientificType.TAN -> "tan("
            ScientificType.LOG -> "log("
            ScientificType.ABS -> "abs("
            else -> "${action.text.lowercase()}("
        }

        return insertText(currentText, safeCursor, toInsert, applyImplicitMultiplication = true)
    }

    fun handleConstant(currentText: String, cursorIndex: Int, action: CalcButtonAction.Constant): InputState {
        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val toInsert = when (action.type) {
            Constants.PI -> "π"
            Constants.E -> "e"
            Constants.PHI -> "φ"
            Constants.I -> "j"
            Constants.INF -> "∞"
        }

        return insertText(currentText, safeCursor, toInsert, applyImplicitMultiplication = true)
    }

    fun handleBackspace(currentText: String, cursorIndex: Int): InputState {
        if (currentText.isEmpty() || currentText == "0") return InputState("0", 1)

        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        if (safeCursor == 0) return InputState(currentText, 0)

        val textBefore = currentText.substring(0, safeCursor)
        val textAfter = currentText.substring(safeCursor)

        val tokens = listOf(
            " × asinh(", "asinh(", " × acosh(", "acosh(", " × atanh(", "atanh(",
            " × sinh(", "sinh(", " × cosh(", "cosh(", " × tanh(", "tanh(",
            " × asin(", "asin(", " × acos(", "acos(", " × atan(", "atan(",
            " × sin(", "sin(", " × cos(", "cos(", " × tan(", "tan(",
            " × log(", "log(", " × ln(", "ln(", " × abs(", "abs(", " × √(", "√(",
            " × π", "π", " × e", "e", " × φ", "φ", " × j", "j", " × i", "i",
            " ÷ ", " × ", " + ", " - ", " ^ ", "( )", "!", "÷", "×",
            " / ", " * ", "/", "*", "==", " , ", ", "
        )

        val matchedToken = tokens.find { textBefore.endsWith(it) }
        val dropCount = matchedToken?.length ?: 1

        val newTextBefore = textBefore.dropLast(dropCount)
        val newText = if (newTextBefore.isEmpty() && textAfter.isEmpty()) "0" else newTextBefore + textAfter
        val newCursor = if (newTextBefore.isEmpty() && textAfter.isEmpty()) 1 else (safeCursor - dropCount).coerceAtLeast(0)

        return InputState(newText, newCursor)
    }
}
