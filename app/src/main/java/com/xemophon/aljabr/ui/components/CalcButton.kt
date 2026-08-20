package com.xemophon.aljabr.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.xemophon.aljabr.R
import com.xemophon.aljabr.ui.theme.Dimens

sealed interface CalcButtonAction {
    data class Symbol(val text: String, val formula: String = text) : CalcButtonAction
    data class Scientific(val text: String, val type: ScientificType) : CalcButtonAction
    data class Constant(val text: String, val type: Constants) : CalcButtonAction
    data class Variable(val text: String, val type: Variables) : CalcButtonAction
    data class Limits(val text: String, val type: LimitType) : CalcButtonAction
    data class Integrals(val text: String, val type: IntegralType) : CalcButtonAction
    data class Differentiate(val text: String = "d/dx") : CalcButtonAction
    data class DifferentiateSingle(val text: String = "d/dx") : CalcButtonAction
    data object Clear : CalcButtonAction
    data object Calculate : CalcButtonAction
    data object Graph : CalcButtonAction
    data object Done : CalcButtonAction
    data class Backspace(@param:DrawableRes val iconRes: Int) : CalcButtonAction
}

fun CalcButtonAction.toInverse(): CalcButtonAction {
    return when (this) {
        is CalcButtonAction.Scientific -> {
            when (type) {
                ScientificType.SIN -> CalcButtonAction.Scientific("sin⁻¹", ScientificType.ASIN)
                ScientificType.COS -> CalcButtonAction.Scientific("cos⁻¹", ScientificType.ACOS)
                ScientificType.TAN -> CalcButtonAction.Scientific("tan⁻¹", ScientificType.ATAN)
                ScientificType.LOG -> CalcButtonAction.Symbol("10^", "10 ^ ")
                ScientificType.LN -> CalcButtonAction.Constant("e", Constants.E)
                ScientificType.SQRT -> CalcButtonAction.Scientific(text = "!", ScientificType.FACTORIAL)
                else -> this
            }
        }

        is CalcButtonAction.Constant -> {
            when (type) {
                Constants.PI -> CalcButtonAction.Constant("j", Constants.I)
                Constants.E -> CalcButtonAction.Scientific("ln", ScientificType.LN)
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

enum class ScientificType { SQRT, SIN, COS, TAN, LOG, ASIN, ACOS, ATAN, LN, FACTORIAL }

enum class Constants { PI, I, PHI, E, INF}
enum class Variables { X, Y }
enum class LimitType { FINITE, INFINITE }
enum class IntegralType { DEFINITE, INDEFINITE, ARC, XSURF, YSURF, XVOL, YVOL}

val ShortButtonGrid : List<List<CalcButtonAction>> = listOf(
    listOf(
        CalcButtonAction.Symbol("7"),
        CalcButtonAction.Symbol("8"),
        CalcButtonAction.Symbol("9"),
        CalcButtonAction.Symbol("÷", "/")
    ),
    listOf(
        CalcButtonAction.Symbol("4"),
        CalcButtonAction.Symbol("5"),
        CalcButtonAction.Symbol("6"),
        CalcButtonAction.Symbol("×", "*")
    ),
    listOf(
        CalcButtonAction.Symbol("1"),
        CalcButtonAction.Symbol("2"),
        CalcButtonAction.Symbol("3"),
        CalcButtonAction.Symbol("-")
    ),
    listOf(
        CalcButtonAction.Symbol("0"),
        CalcButtonAction.Symbol("."),
        CalcButtonAction.Symbol("( )"),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Clear,
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Constant("φ", Constants.PHI)
    )
)
val StandardButtonsGrid : List<List<CalcButtonAction>> = listOf(
    listOf(
        CalcButtonAction.Clear,
        CalcButtonAction.Symbol("( )"),
        CalcButtonAction.Symbol("%"),
        CalcButtonAction.Symbol("÷", "/")
    ),
    listOf(
        CalcButtonAction.Symbol("7"),
        CalcButtonAction.Symbol("8"),
        CalcButtonAction.Symbol("9"),
        CalcButtonAction.Symbol("×", "*")
    ),
    listOf(
        CalcButtonAction.Symbol("4"),
        CalcButtonAction.Symbol("5"),
        CalcButtonAction.Symbol("6"),
        CalcButtonAction.Symbol("-")
    ),
    listOf(
        CalcButtonAction.Symbol("1"),
        CalcButtonAction.Symbol("2"),
        CalcButtonAction.Symbol("3"),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Symbol("0"),
        CalcButtonAction.Symbol("."),
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Calculate
    )
)

val ScientificButtonsGrid : List<List<CalcButtonAction>> = listOf(
    listOf(
        CalcButtonAction.Scientific("sin", ScientificType.SIN),
        CalcButtonAction.Scientific("cos", ScientificType.COS),
        CalcButtonAction.Scientific("tan", ScientificType.TAN),
        CalcButtonAction.Scientific("log", ScientificType.LOG)
    ),
    listOf(
        CalcButtonAction.Scientific("√", ScientificType.SQRT),
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Constant("e", Constants.E),
        CalcButtonAction.Symbol("^", " ^ ")
    )
)

val MultipleVariableGrid : List<List<CalcButtonAction>> = listOf(
    listOf(
        CalcButtonAction.Variable("x", Variables.X),
        CalcButtonAction.Variable("y", Variables.Y),
        CalcButtonAction.Symbol("("),
        CalcButtonAction.Symbol(")")
    ),
    listOf(
        CalcButtonAction.Scientific("sin", ScientificType.SIN),
        CalcButtonAction.Scientific("cos", ScientificType.COS),
        CalcButtonAction.Scientific("tan", ScientificType.TAN),
        CalcButtonAction.Scientific("log", ScientificType.LOG)
    ),
    listOf(
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Constant("e", Constants.E),
        CalcButtonAction.Symbol("^", " ^ "),
        CalcButtonAction.Symbol("÷", "/")
    ),
    listOf(
        CalcButtonAction.Symbol("7"),
        CalcButtonAction.Symbol("8"),
        CalcButtonAction.Symbol("9"),
        CalcButtonAction.Symbol("×", "*"),
    ),
    listOf(
        CalcButtonAction.Symbol("4"),
        CalcButtonAction.Symbol("5"),
        CalcButtonAction.Symbol("6"),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Symbol("1"),
        CalcButtonAction.Symbol("2"),
        CalcButtonAction.Symbol("3"),
        CalcButtonAction.Symbol("-")
    ),
    listOf(
        CalcButtonAction.Symbol("0"),
        CalcButtonAction.Symbol("."),
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Calculate
    )
)
val SingleVariableGrid : List<List<CalcButtonAction>> = listOf(
    listOf(
        CalcButtonAction.Variable("x", Variables.X),
        CalcButtonAction.Symbol("("),
        CalcButtonAction.Symbol(")"),
        CalcButtonAction.Clear
    ),
    listOf(
        CalcButtonAction.Scientific("sin", ScientificType.SIN),
        CalcButtonAction.Scientific("cos", ScientificType.COS),
        CalcButtonAction.Scientific("tan", ScientificType.TAN),
        CalcButtonAction.Scientific("log", ScientificType.LOG)
    ),
    listOf(
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Constant("e", Constants.E),
        CalcButtonAction.Symbol("^", " ^ "),
        CalcButtonAction.Symbol("÷", "/")
    ),
    listOf(
        CalcButtonAction.Symbol("7"),
        CalcButtonAction.Symbol("8"),
        CalcButtonAction.Symbol("9"),
        CalcButtonAction.Symbol("×", "*"),
    ),
    listOf(
        CalcButtonAction.Symbol("4"),
        CalcButtonAction.Symbol("5"),
        CalcButtonAction.Symbol("6"),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Symbol("1"),
        CalcButtonAction.Symbol("2"),
        CalcButtonAction.Symbol("3"),
        CalcButtonAction.Symbol("-")
    ),
    listOf(
        CalcButtonAction.Symbol("0"),
        CalcButtonAction.Symbol("."),
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Calculate
    )
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
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f, // Slightly less aggressive
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f), // Snappier
        label = "ButtonScale"
    )

    // Values based on isExpanded from Theme Dimens
    val targetCornerRadius =
        if (isExpanded) Dimens.ButtonCornerRadiusExpanded else Dimens.ButtonCornerRadiusStandard
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        label = "CornerRadius"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        label = "ContainerColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        label = "ContentColor"
    )

    val textStyle =
        if (isExpanded) MaterialTheme.typography.labelLarge else MaterialTheme.typography.displaySmall
    val targetIconSize = if (isExpanded) Dimens.ButtonIconSizeExpanded else Dimens.ButtonIconSizeStandard
    val animatedIconSize by animateDpAsState(
        targetValue = targetIconSize,
        label = "IconSize"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(animatedCornerRadius))
            .background(animatedContainerColor)
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
                    modifier = Modifier.size(animatedIconSize),
                    tint = animatedContentColor
                )
            }

            is CalcButtonAction.Symbol -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Scientific -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Constant -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Integrals -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Differentiate -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.DifferentiateSingle -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Variable -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            is CalcButtonAction.Limits -> {
                Text(
                    text = action.text,
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            CalcButtonAction.Calculate -> {
                Text(
                    text = "=",
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            CalcButtonAction.Done -> {
                Text(
                    text = "✔",
                    style = textStyle,
                    color = animatedContentColor
                )
            }

            CalcButtonAction.Graph -> {
                Text(
                    text = "Graph",
                    style = textStyle.copy(
                        fontSize = if (isExpanded) Dimens.GraphButtonTextSizeExpanded else Dimens.GraphButtonTextSizeStandard
                    ),
                    color = animatedContentColor
                )
            }

            CalcButtonAction.Clear -> {
                Text(
                    text = "C",
                    style = textStyle,
                    color = animatedContentColor
                )
            }
        }
    }
}


@Composable
fun AdditionalButtons(
    buttons: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    if (buttons.isEmpty()) return

    val pageCount = (buttons.size + 2) / 3
    val pagerState = rememberPagerState(pageCount = { pageCount })

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) { page ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            val startIndex = page * 3
            val endIndex = minOf(startIndex + 3, buttons.size)
            for (i in startIndex until endIndex) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    buttons[i]()
                }
            }
            repeat(3 - (endIndex - startIndex)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}