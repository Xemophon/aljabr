package com.example.tuscalc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    expression: String,
    result: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = expression,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = if (expression.length > 12) 24.sp else 32.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 2,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = result,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (result.length > 8) 48.sp else 64.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            lineHeight = 70.sp
        )
    }
}

class CalcBoxViewModel : ViewModel() {
    var displayText by mutableStateOf("0")
        private set

    var resultText by mutableStateOf("")
        private set

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Calculate -> calculateResult()
            is CalcButtonAction.Clear -> {
                displayText = "0"
                resultText = ""
            }
            is CalcButtonAction.Backspace -> {
                handleBackspace()
                updateInstantResult()
            }
            is CalcButtonAction.Symbol -> {
                when (action.text) {
                    "( )", "()" -> handleBrackets()
                    "%" -> handlePercentage()
                    else -> handleSymbol(action.text)
                }
                updateInstantResult()
            }
            is CalcButtonAction.Scientific -> {
                handleScientific(action)
                updateInstantResult()
            }
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

    private fun updateInstantResult() {
        if (displayText == "0" || displayText.isBlank()) {
            resultText = ""
            return
        }

        try {
            val result = CalcFuncs.calculateExpression(displayText)
            resultText = if (result.isNaN()) "" else CalcFuncs.formatResult(result)
        } catch (e: Exception) {
            resultText = ""
        }
    }

    private fun calculateResult() {
        if (resultText.isNotEmpty() && resultText != "Error") {
            displayText = resultText
            resultText = ""
        } else {
            try {
                val result = CalcFuncs.calculateExpression(displayText)
                displayText = CalcFuncs.formatResult(result)
                resultText = ""
            } catch (e: Exception) {
                displayText = "Error"
                resultText = ""
            }
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
