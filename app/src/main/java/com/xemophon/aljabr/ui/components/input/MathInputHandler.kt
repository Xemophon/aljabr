package com.xemophon.aljabr.ui.components.input

import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.Constants
import com.xemophon.aljabr.ui.components.buttons.ScientificType

data class InputState(
    val text: String,
    val cursorIndex: Int
)

object MathInputHandler {

    fun isImplicitMultiplicationNeeded(text: String, cursorIndex: Int): Boolean {
        if (text.isEmpty() || text == "0") return false
        val clampedIndex = cursorIndex.coerceIn(0, text.length)
        val lastChar = if (clampedIndex > 0) text[clampedIndex - 1] else null
        return lastChar != null && (
            lastChar.isDigit() ||
            lastChar == ')' ||
            lastChar == 'x' ||
            lastChar == 'y' ||
            lastChar == 'n' ||
            lastChar == 'π' ||
            lastChar == 'e' ||
            lastChar == 'φ' ||
            lastChar == 'j' ||
            lastChar == 'i' ||
            lastChar == '%'
        )
    }

    fun insertText(
        currentText: String,
        cursorIndex: Int,
        toInsert: String,
        applyImplicitMultiplication: Boolean = false
    ): InputState {
        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val prefix = if (applyImplicitMultiplication && isImplicitMultiplicationNeeded(currentText, safeCursor)) " × " else ""
        val finalInsert = "$prefix$toInsert"

        if (currentText == "0" && !finalInsert.startsWith(" × ")) {
            return if (finalInsert == ".") {
                InputState("0.", 2)
            } else if (finalInsert == "%") {
                InputState("0%", 2)
            } else {
                InputState(finalInsert, finalInsert.length)
            }
        }

        if (currentText == "Error" || currentText == "NaN" || currentText == "Infinity") {
            val startText = if (finalInsert.contains(Regex("[0-9]"))) finalInsert else "0"
            return InputState(startText, startText.length)
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

        val safeCursor = if (cursorIndex == -1) currentText.length else cursorIndex.coerceIn(0, currentText.length)
        val isDigitOrDot = symbol.all { it.isDigit() || it == '.' }
        val lastChar = if (safeCursor > 0) currentText[safeCursor - 1] else null
        val isLastCharDigitOrDot = lastChar != null && (lastChar.isDigit() || lastChar == '.')

        val applyImplicit = isDigitOrDot && isImplicitMultiplicationNeeded(currentText, safeCursor) && !isLastCharDigitOrDot

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
