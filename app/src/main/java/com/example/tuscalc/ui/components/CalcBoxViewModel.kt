package com.example.tuscalc.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.tuscalc.basicCalc.CalcFuncs

@Composable
fun CalcBox(
    num: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = num,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (num.length > 8) 48.sp else 64.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2,
            lineHeight = 70.sp
        )
    }
}

class CalcBoxViewModel : ViewModel() {
    var displayText by mutableStateOf("0")
        private set

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Calculate -> calculateResult()
            is CalcButtonAction.Clear -> displayText = "0"
            is CalcButtonAction.Backspace -> handleBackspace()
            is CalcButtonAction.Symbol -> {
                when (action.text) {
                    "( )", "()" -> handleBrackets()
                    "%" -> handlePercentage()
                    else -> handleSymbol(action.text)
                }
            }
            is CalcButtonAction.Scientific -> handleScientific(action)
        }
    }

    private fun handleSymbol(symbol: String) {
        if (symbol == "0" && displayText == "0") return
        
        if (displayText == "Error" || displayText == "NaN" || displayText == "Infinity") {
            displayText = if (symbol.contains(Regex("[0-9]"))) symbol else "0"
            return
        }

        if (displayText == "0") {
            displayText = symbol
        } else {
            displayText += symbol
        }
    }

    private fun handleBrackets() {
        val openBrackets = displayText.count { it == '(' }
        val closedBrackets = displayText.count { it == ')' }
        val lastChar = displayText.trim().lastOrNull()

        displayText = when {
            displayText == "0" -> "("
            openBrackets > closedBrackets -> {
                if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || displayText.endsWith("pi") || displayText.endsWith("e"))) {
                    "$displayText)"
                } else {
                    "$displayText("
                }
            }
            else -> {
                if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || displayText.endsWith("pi") || displayText.endsWith("e"))) {
                    "$displayText * ("
                } else {
                    if (displayText == "0") "(" else "$displayText("
                }
            }
        }
    }

    private fun handlePercentage() {
        if (displayText != "0" && displayText != "Error") {
            displayText += "%"
        }
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        if (action.type == ScientificType.FACTORIEL) {
            if (displayText != "Error" && displayText != "NaN" && displayText != "Infinity") {
                displayText += "!"
            }
            return
        }

        val lastChar = displayText.trim().lastOrNull()
        val isPrevDigitOrParen = lastChar != null && (lastChar.isDigit() || lastChar == ')') && displayText != "0"
        val prefix = if (isPrevDigitOrParen) " * " else ""

        // If starting fresh, clear the default "0" unless multiplying
        if (displayText == "0" && prefix.isEmpty()) displayText = ""

        displayText += when(action.type) {
            ScientificType.SQRT -> "${prefix}sqrt("
            ScientificType.PI -> "${prefix}pi"
            ScientificType.E -> "${prefix}e"
            ScientificType.ASIN -> "${prefix}asin("
            ScientificType.ACOS -> "${prefix}acos("
            ScientificType.ATAN -> "${prefix}atan("
            ScientificType.LN -> "${prefix}ln("
            ScientificType.SIN -> "${prefix}sin("
            ScientificType.COS -> "${prefix}cos("
            ScientificType.TAN -> "${prefix}tan("
            ScientificType.LOG -> "${prefix}log("
            else -> "$prefix${action.text.lowercase()}("
        }
    }

    private fun calculateResult() {
        displayText = try {
            val result = CalcFuncs.calculateExpression(displayText)
            CalcFuncs.formatResult(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun handleBackspace() {
        val tokens = listOf(
            " * asin(", "asin(", " * acos(", "acos(", " * atan(", "atan(",
            " * sin(", "sin(", " * cos(", "cos(", " * tan(", "tan(",
            " * log(", "log(", " * ln(", "ln(", " * sqrt(", "sqrt(",
            " * pi", "pi", " * e", "e",
            " / ", " * ", " + ", " - ", " ^ ", "( )", "!"
        )
        val matchedToken = tokens.find { displayText.endsWith(it) }
        if (matchedToken != null) {
            displayText = displayText.removeSuffix(matchedToken)
            if (displayText.isEmpty()) displayText = "0"
        } else if (displayText.length > 1) {
            displayText = displayText.dropLast(1)
        } else {
            displayText = "0"
        }
    }
}
