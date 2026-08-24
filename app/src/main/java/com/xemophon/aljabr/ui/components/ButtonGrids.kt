package com.xemophon.aljabr.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import com.xemophon.aljabr.ui.theme.Dimens

@Composable
private fun CalcButtonSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.inversePrimary,
        shape = AbsoluteRoundedCornerShape(
            topLeft = Dimens.LandingPageCornerRadius,
            topRight = Dimens.LandingPageCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
            content = content
        )
    }
}

sealed class ShortGridMode{
    data object Convertor : ShortGridMode()
    data object Polynomials : ShortGridMode()
    data object Functions : ShortGridMode()
    data object None : ShortGridMode()
}

@Composable
fun ShortCalcButtons(
    modifier: Modifier = Modifier,
    gridMode: ShortGridMode,
    letterNeeded: CalcButtonAction = CalcButtonAction.Constant("φ", Constants.PHI),
    onAction: (CalcButtonAction) -> Unit,
) {
    CalcButtonSheet(modifier.fillMaxHeight()) {
        ButtonGrid(
            gridData = ShortButtonGrid,
            modifier = Modifier.weight(1f),
            isExpanded = true,
            onAction = onAction,
            buttonModifier = Modifier.fillMaxHeight(),
            overrides = when(gridMode) {
                ShortGridMode.Convertor -> mapOf((4 to 3) to letterNeeded)
                ShortGridMode.Polynomials -> mapOf((4 to 3) to CalcButtonAction.Calculate, (4 to 2) to CalcButtonAction.Variable("x", Variables.X), (4 to 1) to CalcButtonAction.Symbol("^", " ^ "))
                ShortGridMode.Functions -> mapOf((4 to 1) to CalcButtonAction.Variable("x", Variables.X), (4 to 2) to CalcButtonAction.Done)
                else -> emptyMap()
            }
        )
    }
}

@Composable
fun CalcButtons(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isInverse: Boolean = false,
    useRadians: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleInverse: () -> Unit,
    onToggleAngleUnit: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) {
    val buttonAspectRatio by animateFloatAsState(
        targetValue = if (isExpanded) Dimens.ButtonAspectRatioExpanded else Dimens.ButtonAspectRatioStandard,
        animationSpec = spring(stiffness = StiffnessLow, dampingRatio = DampingRatioLowBouncy),
        label = "ButtonAspectRatio"
    )
    val transition = updateTransition(targetState = isExpanded, label = "ScientificTransition")
    val expandFraction by transition.animateFloat(
        transitionSpec = {
            spring(stiffness = StiffnessLow, dampingRatio = 0.8f) // Slightly less bouncy
        },
        label = "ExpandFraction"
    ) { state ->
        if (state) 1f else 0f
    }
    // We keep the old top button arrangement for the Basic Calculator button grid
    CalcButtonSheet(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                Dimens.SpacingSmall,
                Alignment.CenterHorizontally
            )
        ) {
            Button(
                onClick = onToggleExpand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                AnimatedContent(
                    targetState = if (isExpanded) "Standard" else "Scientific",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ToggleButtonText"
                ) { targetText ->
                    Text(
                        text = targetText,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = Dimens.TextSizeToggle
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    Button(
                        onClick = onToggleInverse,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInverse) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isInverse) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Inverse",
                            color = if (isInverse) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AngleUnitSwitch(
                        modifier = Modifier.padding(start = Dimens.SpacingSmall),
                        useRadians = useRadians,
                        onToggleAngleUnit = onToggleAngleUnit
                    )
                }
            }
        }

        if (expandFraction > 0f) {
            Layout(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .graphicsLayer { alpha = expandFraction },
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                        ButtonGrid(
                            gridData = ScientificButtonsGrid,
                            isExpanded = true,
                            isInverse = isInverse,
                            onAction = onAction,
                            buttonModifier = Modifier.aspectRatio(1.5f)
                        )
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val animatedHeight = (placeable.height * expandFraction).toInt()

                layout(placeable.width, animatedHeight) {
                    val yOffset = animatedHeight - placeable.height
                    placeable.placeRelative(0, yOffset)
                }
            }
        }

        ButtonGrid(
            gridData = StandardButtonsGrid,
            isExpanded = isExpanded,
            onAction = onAction,
            buttonModifier = Modifier.aspectRatio(buttonAspectRatio)
        )
    }
}

sealed class AdvancedGridMode {
    data class Limits(val currentType: LimitType) : AdvancedGridMode()
    data class Integration(val currentType: IntegralType, val axis: String) : AdvancedGridMode()
    data class Differentiation(val currentMode: String) : AdvancedGridMode()
    data object Graph : AdvancedGridMode()
}

@Composable
fun AdvancedButtonsGrid(
    modifier: Modifier = Modifier,
    gridMode: AdvancedGridMode,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    onAction: (CalcButtonAction) -> Unit
) = CalcButtonSheet(modifier) {
    val selectedGrid = when (gridMode) {
        is AdvancedGridMode.Differentiation -> if (gridMode.currentMode == "Single") SingleVariableGrid else MultipleVariableGrid
        is AdvancedGridMode.Graph -> MultipleVariableGrid
        else -> SingleVariableGrid
    }

    val buttons = mutableListOf<@Composable () -> Unit>()
    buttons.add {
        ModeToggleButton(
            label = "Inv",
            isSelected = isInverse,
            onClick = onToggleInverse,
            modifier = Modifier.fillMaxWidth()
        )
    }

    when (gridMode) {
        is AdvancedGridMode.Graph -> {
            buttons.add {
                Button(
                    onClick = { onSecondaryAction?.invoke() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Graph", style = MaterialTheme.typography.labelLarge)
                }
            }
            buttons.add {
                Button(
                    onClick = { onAction(CalcButtonAction.Clear) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        is AdvancedGridMode.Limits -> {
            buttons.add {
                ModeToggleButton(
                    label = "x→∞",
                    isSelected = gridMode.currentType == LimitType.INFINITE,
                    onClick = { onAction(CalcButtonAction.Limits("x → ∞", LimitType.INFINITE)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                ModeToggleButton(
                    label = "x→a",
                    isSelected = gridMode.currentType == LimitType.FINITE,
                    onClick = { onAction(CalcButtonAction.Limits("x → a", LimitType.FINITE)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is AdvancedGridMode.Integration -> {
            buttons.add {
                ModeToggleButton(
                    label = "∫",
                    isSelected = gridMode.currentType == IntegralType.INDEFINITE,
                    onClick = { onAction(CalcButtonAction.Integrals("∫", IntegralType.INDEFINITE)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                ModeToggleButton(
                    label = "∫ ab",
                    isSelected = gridMode.currentType == IntegralType.DEFINITE,
                    onClick = { onAction(CalcButtonAction.Integrals("∫ab", IntegralType.DEFINITE)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                Button(
                    onClick = { onAction(CalcButtonAction.Constant("∞", Constants.INF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = "∞", style = MaterialTheme.typography.labelLarge)
                }
            }
            buttons.add {
                ModeToggleButton(
                    label = "L",
                    isSelected = gridMode.currentType == IntegralType.ARC,
                    onClick = { onAction(CalcButtonAction.Integrals("Arc", IntegralType.ARC)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                ModeToggleButton(
                    label = "V${if (gridMode.axis == "X") "ₓ" else "ᵧ"}",
                    isSelected = gridMode.currentType == IntegralType.XVOL || gridMode.currentType == IntegralType.YVOL,
                    onClick = { 
                        val type = if (gridMode.axis == "X") IntegralType.XVOL else IntegralType.YVOL
                        onAction(CalcButtonAction.Integrals("Vol ${gridMode.axis}", type)) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                ModeToggleButton(
                    label = "S${if (gridMode.axis == "X") "ₓ" else "ᵧ"}",
                    isSelected = gridMode.currentType == IntegralType.XSURF || gridMode.currentType == IntegralType.YSURF,
                    onClick = { 
                        val type = if (gridMode.axis == "X") IntegralType.XSURF else IntegralType.YSURF
                        onAction(CalcButtonAction.Integrals("Surf ${gridMode.axis}", type)) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is AdvancedGridMode.Differentiation -> {
            buttons.add {
                ModeToggleButton(
                    label = "d/dx",
                    isSelected = gridMode.currentMode == "Single",
                    onClick = { onAction(CalcButtonAction.DifferentiateSingle("d/dx")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            buttons.add {
                ModeToggleButton(
                    label = "∇f",
                    isSelected = gridMode.currentMode == "Multiple",
                    onClick = { onAction(CalcButtonAction.Differentiate("∇f")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        else -> {}
    }

    AdditionalButtons(buttons = buttons)
    ButtonGrid(
        gridData = selectedGrid,
        isExpanded = true,
        isInverse = isInverse,
        onAction = onAction,
        buttonModifier = Modifier.aspectRatio(Dimens.ButtonAspectRatioExpanded),
        overrides = when (gridMode) {
            is AdvancedGridMode.Graph -> {
                mapOf((6 to 3) to CalcButtonAction.Symbol("="))
            }
            else -> emptyMap()
        }
    )
}

@Composable
private fun ModeToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}


@Composable
private fun ButtonGrid(
    gridData: List<List<CalcButtonAction>>,
    modifier: Modifier = Modifier,
    isInverse: Boolean = false,
    isExpanded: Boolean,
    onAction: (CalcButtonAction) -> Unit,
    buttonModifier: Modifier = Modifier,
    overrides: Map<Pair<Int, Int>, CalcButtonAction> = emptyMap()
) {
    val colorScheme = MaterialTheme.colorScheme
    gridData.forEachIndexed { rowIndex, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            row.forEachIndexed { colIndex, action ->
                val baseAction = overrides[rowIndex to colIndex] ?: action
                val displayAction = if (isInverse) baseAction.toInverse() else baseAction
                val colors = remember(displayAction, colorScheme) { getButtonColors(displayAction, colorScheme) }

                CalcButton(
                    action = displayAction,
                    isExpanded = isExpanded,
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .then(buttonModifier),
                    onClick = { onAction(displayAction) }
                )
            }
        }
    }
}

@Composable
private fun AngleUnitSwitch(
    useRadians: Boolean,
    onToggleAngleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
    ) {
        Switch(
            checked = useRadians,
            onCheckedChange = { onToggleAngleUnit() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.inversePrimary,
                checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSecondary,
            ),
            thumbContent = {
                Text(
                    text = if (useRadians) "Rad" else "Deg",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = Dimens.TextSizeSwitchLabel
                )
            }
        )
    }
}

private data class ButtonColorPalette(val containerColor: Color, val contentColor: Color)

private fun getButtonColors(action: CalcButtonAction, colorScheme: ColorScheme): ButtonColorPalette {
    return when (action) {
        is CalcButtonAction.Calculate -> ButtonColorPalette(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary
        )

        is CalcButtonAction.Clear, is CalcButtonAction.Backspace -> ButtonColorPalette(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )

        is CalcButtonAction.Symbol -> {
            val text = action.text
            when {
                text == "=" -> ButtonColorPalette(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )

                text in listOf("÷", "×", "-", "+") -> ButtonColorPalette(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )

                text in listOf("(", ")", "( )", "^", "π", "e", "%") -> ButtonColorPalette(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                )

                // Numbers 0-9 and .
                text.all { it.isDigit() || it == '.' } -> ButtonColorPalette(
                    containerColor = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurface
                )

                else -> ButtonColorPalette(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                )
            }
        }

        is CalcButtonAction.Scientific -> ButtonColorPalette(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )

        is CalcButtonAction.Constant -> ButtonColorPalette(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )

        is CalcButtonAction.Variable -> ButtonColorPalette(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )

        else -> ButtonColorPalette(
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface
        )
    }
}