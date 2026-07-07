package com.example.tuscalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.example.tuscalc.R

import com.example.tuscalc.ui.theme.Dimens

sealed interface CalcButtonAction {
    data class Symbol(val text: String, val formula: String = text) : CalcButtonAction
    data class Scientific(val text: String, val type: ScientificType) : CalcButtonAction
    data class Variable(val text: String, val type: Variables) : CalcButtonAction
    data object Clear : CalcButtonAction
    data object Calculate : CalcButtonAction
    data class Backspace(@param:DrawableRes val iconRes: Int) : CalcButtonAction
}

val CalcButtonAction.isOperator: Boolean
    get() = when (this) {
        is CalcButtonAction.Symbol -> text in listOf("÷", "×", "-", "+", "C", "( )", "%", "^")
        is CalcButtonAction.Clear -> true
        is CalcButtonAction.Calculate -> true
        is CalcButtonAction.Variable -> false
        else -> false
    }

fun CalcButtonAction.toInverse(): CalcButtonAction {
    return when (this) {
        is CalcButtonAction.Scientific -> {
            when (type) {
                ScientificType.SIN -> CalcButtonAction.Scientific("asin", ScientificType.ASIN)
                ScientificType.COS -> CalcButtonAction.Scientific("acos", ScientificType.ACOS)
                ScientificType.TAN -> CalcButtonAction.Scientific("atan", ScientificType.ATAN)
                ScientificType.LOG -> CalcButtonAction.Scientific("ln", ScientificType.LN)
                ScientificType.SQRT -> CalcButtonAction.Scientific(text= "!", ScientificType.FACTORIAL)
                else -> this
            }
        }
        is CalcButtonAction.Variable -> {
            when (type) {
                Variables.X -> CalcButtonAction.Variable(text = "x", Variables.X)
                Variables.Y -> CalcButtonAction.Variable(text = "y", Variables.Y)
            }
        }
        else -> this
    }
}

enum class ScientificType { SQRT, PI, E, SIN, COS, TAN, LOG, ASIN, ACOS, ATAN, LN, FACTORIAL }
enum class Variables { X, Y }

val StandardButtonsGrid = listOf(
    listOf(CalcButtonAction.Clear, CalcButtonAction.Symbol("( )"), CalcButtonAction.Symbol("%"), CalcButtonAction.Symbol("÷", "/")),
    listOf(CalcButtonAction.Symbol("7"), CalcButtonAction.Symbol("8"), CalcButtonAction.Symbol("9"), CalcButtonAction.Symbol("×", "*")),
    listOf(CalcButtonAction.Symbol("4"), CalcButtonAction.Symbol("5"), CalcButtonAction.Symbol("6"), CalcButtonAction.Symbol("-")),
    listOf(CalcButtonAction.Symbol("1"), CalcButtonAction.Symbol("2"), CalcButtonAction.Symbol("3"), CalcButtonAction.Symbol("+")),
    listOf(CalcButtonAction.Symbol("0"), CalcButtonAction.Symbol("."), CalcButtonAction.Backspace(R.drawable.backspace), CalcButtonAction.Calculate)
)

val ScientificButtonsGrid = listOf(
    listOf(CalcButtonAction.Scientific("sin", ScientificType.SIN), CalcButtonAction.Scientific("cos", ScientificType.COS), CalcButtonAction.Scientific("tan", ScientificType.TAN), CalcButtonAction.Scientific("log", ScientificType.LOG)),
    listOf(CalcButtonAction.Scientific("√", ScientificType.SQRT), CalcButtonAction.Scientific("π", ScientificType.PI), CalcButtonAction.Scientific("e", ScientificType.E), CalcButtonAction.Symbol("^", " ^ "))
)

val GraphButtonsGrid = listOf(
    listOf(CalcButtonAction.Variable("x", Variables.X), CalcButtonAction.Variable("y", Variables.Y), CalcButtonAction.Symbol("("), CalcButtonAction.Symbol(")"), CalcButtonAction.Clear),
    listOf(CalcButtonAction.Scientific("sin", ScientificType.SIN), CalcButtonAction.Scientific("cos", ScientificType.COS), CalcButtonAction.Scientific("tan", ScientificType.TAN), CalcButtonAction.Scientific("log", ScientificType.LOG)),
    listOf(CalcButtonAction.Symbol("^"), CalcButtonAction.Scientific("√", ScientificType.SQRT), CalcButtonAction.Scientific("π", ScientificType.PI), CalcButtonAction.Scientific("e", ScientificType.E)),
    listOf(CalcButtonAction.Symbol("7"), CalcButtonAction.Symbol("8"), CalcButtonAction.Symbol("9"), CalcButtonAction.Symbol("÷", "/")),
    listOf(CalcButtonAction.Symbol("4"), CalcButtonAction.Symbol("5"), CalcButtonAction.Symbol("6"), CalcButtonAction.Symbol("×", "*")),
    listOf(CalcButtonAction.Symbol("1"), CalcButtonAction.Symbol("2"), CalcButtonAction.Symbol("3"), CalcButtonAction.Symbol("-")),
    listOf(CalcButtonAction.Symbol("0"), CalcButtonAction.Symbol("."), CalcButtonAction.Backspace(R.drawable.backspace), CalcButtonAction.Symbol("+"))
)

@Composable
fun CalcButton(
    action: CalcButtonAction,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Values based on isExpanded from Theme Dimens
    val cornerRadius = if (isExpanded) Dimens.ButtonCornerRadiusExpanded else Dimens.ButtonCornerRadiusStandard
    val textStyle = if (isExpanded) MaterialTheme.typography.labelLarge else MaterialTheme.typography.displaySmall
    val iconSize = if (isExpanded) Dimens.ButtonIconSizeExpanded else Dimens.ButtonIconSizeStandard

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    ) {
        // Explicit pattern matching via our sealed interface
        when (action) {
            is CalcButtonAction.Backspace -> {
                Icon(
                    painter = painterResource(id = action.iconRes),
                    contentDescription = "Backspace",
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }

            is CalcButtonAction.Symbol -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = contentColor
                )
            }

            is CalcButtonAction.Scientific -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = contentColor
                )
            }

            is CalcButtonAction.Variable -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = contentColor
                )
            }

            CalcButtonAction.Calculate -> {
                Text(
                    text = "=",
                    style = textStyle,
                    color = contentColor
                )
            }

            CalcButtonAction.Clear -> {
                Text(
                    text = "C",
                    style = textStyle,
                    color = contentColor
                )
            }
        }
    }
}