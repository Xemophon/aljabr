package com.xemophon.aljabr.ui.components.buttons

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.xemophon.aljabr.R
import com.xemophon.aljabr.ui.theme.Dimens

sealed interface CalcButtonAction {
    data class Symbol(val text: String, val formula: String = text) : CalcButtonAction
    data class Scientific(val text: String, val type: ScientificType) : CalcButtonAction
    data class Constant(val text: String, val type: Constants) : CalcButtonAction
    data class Variable(val text: String, val type: Variables) : CalcButtonAction
    data class Parameter(val text: String, val type: com.xemophon.aljabr.ui.components.buttons.Parameter) : CalcButtonAction
    data class Misc(val text: String, val type: com.xemophon.aljabr.ui.components.buttons.Misc) : CalcButtonAction
    data class Limits(val text: String, val type: LimitType) : CalcButtonAction
    data class Integrals(val text: String, val type: IntegralType) : CalcButtonAction
    data class Differentiate(val text: String = "d/dx") : CalcButtonAction
    data class DifferentiateSingle(val text: String = "d/dx") : CalcButtonAction
    data class DifferentiateComplex(val text: String = "Complex") : CalcButtonAction
    data class Laplace(val text: String) : CalcButtonAction
    data object Clear : CalcButtonAction
    data object Calculate : CalcButtonAction
    data object Graph : CalcButtonAction
    data object Done : CalcButtonAction
    data class Backspace(@param:DrawableRes val iconRes: Int) : CalcButtonAction
}

fun CalcButtonAction.getAdditionalActions(): List<CalcButtonAction> {
    return when (this) {
        is CalcButtonAction.Scientific -> {
            when (type) {
                ScientificType.SIN -> listOf(
                    CalcButtonAction.Scientific("sin⁻¹", ScientificType.ASIN),
                    CalcButtonAction.Scientific("sinh", ScientificType.SINH),
                    CalcButtonAction.Scientific("sinh⁻¹", ScientificType.ASINH)
                )
                ScientificType.COS -> listOf(
                    CalcButtonAction.Scientific("cos⁻¹", ScientificType.ACOS),
                    CalcButtonAction.Scientific("cosh", ScientificType.COSH),
                    CalcButtonAction.Scientific("cosh⁻¹", ScientificType.ACOSH)
                )
                ScientificType.TAN -> listOf(
                    CalcButtonAction.Scientific("tan⁻¹", ScientificType.ATAN),
                    CalcButtonAction.Scientific("tanh", ScientificType.TANH),
                    CalcButtonAction.Scientific("tanh⁻¹", ScientificType.ATANH)
                )
                ScientificType.LOG -> listOf(
                    CalcButtonAction.Scientific("ln", ScientificType.LN),
                    CalcButtonAction.Symbol(",", " , ")
                )
                ScientificType.SQRT -> listOf(
                    CalcButtonAction.Symbol("x²", "^2"),
                    CalcButtonAction.Scientific("Abs", ScientificType.ABS)
                )
                else -> emptyList()
            }
        }

        is CalcButtonAction.Constant -> {
            when (type) {
                Constants.PI -> listOf(
                    CalcButtonAction.Constant("j", Constants.I),
                    CalcButtonAction.Constant("φ", Constants.PHI)
                )
                else -> emptyList()
            }
        }

        is CalcButtonAction.Symbol -> {
            when (text) {
                "^" -> listOf(
                    CalcButtonAction.Symbol("x²", "^2"),
                    CalcButtonAction.Symbol("x³", "^3")
                )
                else -> emptyList()
            }
        }

        is CalcButtonAction.Variable ->{
            when (type) {
                Variables.X -> listOf(CalcButtonAction.Constant("∞", Constants.INF))
                else -> emptyList()
            }
        }

        else -> emptyList()
    }
}

enum class ScientificType { SQRT, SIN, COS, TAN, LOG, ASIN, ACOS, ATAN, SINH, COSH, TANH, ASINH, ACOSH, ATANH, LN, FACTORIAL, ABS }

enum class Constants { PI, I, PHI, E, INF}
enum class Variables { X, Y, Z, ZC, T, S}
enum class Misc { PRIME }
enum class Parameter { N }
enum class LimitType { FINITE, INFINITE }
enum class IntegralType { DEFINITE, INDEFINITE, ARC, XSURF, YSURF, XVOL, YVOL, DOUBLE, NDOUBLE, CURVET1, CURVET2  }

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
        CalcButtonAction.Symbol("^"),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Clear,
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Symbol("( )")
    )
)

val FunctionsButtonGrid : List<List<CalcButtonAction>> = listOf(
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
        CalcButtonAction.Constant("π", Constants.PI),
        CalcButtonAction.Symbol("+")
    ),
    listOf(
        CalcButtonAction.Variable("x", Variables.X),
        CalcButtonAction.Symbol("("),
        CalcButtonAction.Symbol(")"),
        CalcButtonAction.Symbol("^"),
    ),
    listOf(
        CalcButtonAction.Backspace(R.drawable.backspace),
        CalcButtonAction.Clear,
        CalcButtonAction.Done
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
        CalcButtonAction.Symbol("^")
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
        CalcButtonAction.Symbol("^"),
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
        CalcButtonAction.Symbol("^"),
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
fun CalcButtonContent(
    action: CalcButtonAction,
    textStyle: TextStyle,
    contentColor: Color,
    iconSize: Dp = 24.dp,
    isExpanded: Boolean = false
) {
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

        is CalcButtonAction.Misc -> {
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

        is CalcButtonAction.Constant -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.Integrals -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.Differentiate -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.DifferentiateSingle -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.DifferentiateComplex -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.Laplace -> {
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

        is CalcButtonAction.Parameter -> {
            Text(
                text = action.text,
                style = textStyle,
                color = contentColor
            )
        }

        is CalcButtonAction.Limits -> {
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

        CalcButtonAction.Done -> {
            Text(
                text = "✔",
                style = textStyle,
                color = contentColor
            )
        }

        CalcButtonAction.Graph -> {
            Text(
                text = "Graph",
                style = textStyle.copy(
                    fontSize = if (isExpanded) Dimens.GraphButtonTextSizeExpanded else Dimens.GraphButtonTextSizeStandard
                ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalcButton(
    action: CalcButtonAction,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    isExpanded: Boolean = false,
    onActionSelected: ((CalcButtonAction) -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showPopup by remember { mutableStateOf(false) }

    val extraActions = remember(action) { action.getAdditionalActions() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "ButtonScale"
    )

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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = {
                    if (extraActions.isNotEmpty()) {
                        showPopup = true
                    }
                }
            )
    ) {
        if (extraActions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(6.dp)
                    .background(
                        color = animatedContentColor.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }

        CalcButtonContent(
            action = action,
            textStyle = textStyle,
            contentColor = animatedContentColor,
            iconSize = animatedIconSize,
            isExpanded = isExpanded
        )

        if (showPopup && extraActions.isNotEmpty()) {
            Popup(
                popupPositionProvider = remember {
                    object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset {
                            val margin = 16 // Margin in pixels from screen edges

                            // Try positioning above anchor
                            var y = anchorBounds.top - popupContentSize.height - margin
                            if (y < margin) {
                                // If too close to top edge, flip to below anchor
                                y = anchorBounds.bottom + margin
                            }

                            // Center horizontally over anchor
                            val preferredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                            val maxX = windowSize.width - popupContentSize.width - margin
                            val x = preferredX.coerceIn(margin, maxOf(margin, maxX))

                            return IntOffset(x, y)
                        }
                    }
                },
                onDismissRequest = { showPopup = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        extraActions.forEach { extraAction ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = 74.dp, height = 64.dp) //Controls the button size
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable {
                                        showPopup = false
                                        (onActionSelected ?: { _ -> onClick() }).invoke(extraAction)
                                    }
                            ) {
                                CalcButtonContent(
                                    action = extraAction,
                                    textStyle = MaterialTheme.typography.labelLarge,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    iconSize = 40.dp,
                                    isExpanded = true
                                )
                            }
                        }
                    }
                }
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
        verticalAlignment = Alignment.CenterVertically,
        pageSize = PageSize.Fill,
        snapPosition = SnapPosition.Center
    ) { page ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            val startIndex = page * 3
            val endIndex = minOf(startIndex + 3, buttons.size)
            val itemCount = endIndex - startIndex
            val missingItems = 3 - itemCount

            if (missingItems > 0) {
                Spacer(modifier = Modifier.weight(missingItems / 2f))
            }

            for (i in startIndex until endIndex) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    buttons[i]()
                }
            }

            if (missingItems > 0) {
                Spacer(modifier = Modifier.weight(missingItems / 2f))
            }
        }
    }
}