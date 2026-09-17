package com.xemophon.aljabr.ui.components.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.data.SymjaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reusable horizontal scrollable view that renders LaTeX formulas or raw expressions.
 * Performs Symja-to-LaTeX conversion asynchronously if needed.
 */
@Composable
fun ScrollableLatexView(
    expression: String,
    modifier: Modifier = Modifier,
    isAlreadyLatex: Boolean = false,
    rawValue: String? = null,
    fontSize: TextUnit = 24.sp,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val needsLatex = remember(expression, rawValue, isAlreadyLatex) {
        if (isAlreadyLatex) return@remember true
        val target = rawValue ?: expression
        rawValue != null || target.any { it.isLetter() || it == '^' || it == '/' || it == '*' || it == '(' || it == '{' || it == '}' }
    }

    val latexState = produceState<String?>(
        initialValue = if (isAlreadyLatex) expression else if (!needsLatex) (rawValue ?: expression) else null,
        expression,
        rawValue,
        isAlreadyLatex
    ) {
        if (isAlreadyLatex) {
            value = expression
        } else if (needsLatex) {
            val toConvert = rawValue ?: expression
            value = withContext(Dispatchers.Default) {
                SymjaUtils.toLaTeX(toConvert)
            }
        } else {
            value = rawValue ?: expression
        }
    }

    val latexContent = latexState.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        if (latexContent != null) {
            if (needsLatex || latexContent.any { it == '^' || it == '/' }) {
                Box(
                    modifier = Modifier.widthIn(max = 2000.dp)
                ) {
                    Latex(
                        latex = latexContent,
                        config = LatexConfig(
                            fontSize = fontSize,
                            theme = LatexTheme.light(color = color)
                        )
                    )
                }
            } else {
                Text(
                    text = latexContent,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = fontSize),
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Elevated card wrapping a ScrollableLatexView for math report items.
 */
@Composable
fun LatexDisplayCard(
    expression: String,
    modifier: Modifier = Modifier,
    isAlreadyLatex: Boolean = false,
    rawValue: String? = null,
    fontSize: TextUnit = 20.sp,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            ScrollableLatexView(
                expression = expression,
                isAlreadyLatex = isAlreadyLatex,
                rawValue = rawValue,
                fontSize = fontSize,
                color = color
            )
        }
    }
}
