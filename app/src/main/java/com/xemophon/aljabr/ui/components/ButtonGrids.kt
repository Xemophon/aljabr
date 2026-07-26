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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.sp
import com.xemophon.aljabr.R
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

@Composable
fun ShortCalcButtons(
    modifier: Modifier = Modifier,
    letterNeeded: CalcButtonAction = CalcButtonAction.Constant("φ", Constants.PHI),
    onAction: (CalcButtonAction) -> Unit,
) {
    val shortButtonGrid = listOf(
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
            letterNeeded,
        )
    )
    CalcButtonSheet(modifier.fillMaxHeight()) {
        shortButtonGrid.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                row.forEach { action ->
                    val colors = getButtonColors(action)
                    CalcButton(
                        action = action,
                        isExpanded = true,
                        containerColor = colors.containerColor,
                        contentColor = colors.contentColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onAction(action) }
                    )
                }
            }
        }
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
                        fontSize = 24.sp
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
                            buttonAspectRatio = 1.5f,
                            isExpanded = true,
                            isInverse = isInverse,
                            onAction = onAction
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
            buttonAspectRatio = buttonAspectRatio,
            isExpanded = isExpanded,
            onAction = onAction
        )
    }
}

sealed class AdvancedGridMode {
    data class Limits(val currentType: LimitType) : AdvancedGridMode()
    data class Integration(val currentType: IntegralType) : AdvancedGridMode()
    data class Differentiation(val currentMode: String) : AdvancedGridMode()
}

@Composable
fun AdvancedButtonsGrid(
    modifier: Modifier = Modifier,
    gridMode: AdvancedGridMode,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) = CalcButtonSheet(modifier) {
    val selectedGrid = when (gridMode) {
        is AdvancedGridMode.Differentiation -> if (gridMode.currentMode == "Single") SingleVariableGrid else MultipleVariableGrid
        else -> SingleVariableGrid // Default for others if not specified, though Limits/Integ usually use SingleVariableGrid
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall, Alignment.CenterHorizontally)
    ) {
        Button(
            onClick = onToggleInverse,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInverse) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isInverse) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Inv", style = MaterialTheme.typography.labelLarge)
        }

        AnimatedContent(
            targetState = gridMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(2f),
            label = "AdvancedModeButtons"
        ) { mode ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                when (mode) {
                    is AdvancedGridMode.Limits -> {
                        ModeToggleButton(
                            label = "∞",
                            isSelected = mode.currentType == LimitType.INFINITE,
                            onClick = { onAction(CalcButtonAction.Limits("x → ∞", LimitType.INFINITE)) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeToggleButton(
                            label = "a",
                            isSelected = mode.currentType == LimitType.FINITE,
                            onClick = { onAction(CalcButtonAction.Limits("x → a", LimitType.FINITE)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is AdvancedGridMode.Integration -> {
                        ModeToggleButton(
                            label = "∫",
                            isSelected = mode.currentType == IntegralType.INDEFINITE,
                            onClick = { onAction(CalcButtonAction.Integrals("∫", IntegralType.INDEFINITE)) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeToggleButton(
                            label = "∫ ab",
                            isSelected = mode.currentType == IntegralType.DEFINITE,
                            onClick = { onAction(CalcButtonAction.Integrals("∫ab", IntegralType.DEFINITE)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is AdvancedGridMode.Differentiation -> {
                        ModeToggleButton(
                            label = "d/dx",
                            isSelected = mode.currentMode == "Single",
                            onClick = { onAction(CalcButtonAction.DifferentiateSingle("d/dx")) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeToggleButton(
                            label = "∇f",
                            isSelected = mode.currentMode == "Multiple",
                            onClick = { onAction(CalcButtonAction.Differentiate("∇f")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    ButtonGrid(
        gridData = selectedGrid,
        buttonAspectRatio = Dimens.ButtonAspectRatioExpanded,
        isExpanded = true,
        isInverse = isInverse,
        onAction = onAction
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
fun CalcButtonsGraph(
    modifier: Modifier = Modifier,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onVisualize: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) = CalcButtonSheet(modifier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.SpacingSmall,
            Alignment.CenterHorizontally
        )
    ) {
        Button(
            onClick = onToggleInverse,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInverse) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isInverse) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Inv",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = onVisualize,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Graph", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = { onAction(CalcButtonAction.Clear) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Clear", style = MaterialTheme.typography.labelLarge)
        }
    }
    ButtonGrid(
        gridData = GraphButtonsGrid,
        buttonAspectRatio = Dimens.ButtonAspectRatioExpanded,
        isExpanded = true,
        isInverse = isInverse,
        onAction = onAction
    )
}

@Composable
private fun ButtonGrid(
    gridData: List<List<CalcButtonAction>>,
    buttonAspectRatio: Float,
    isInverse: Boolean = false,
    isExpanded: Boolean,
    onAction: (CalcButtonAction) -> Unit
) {
    gridData.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            row.forEach { action ->
                val colors = getButtonColors(action)
                val displayAction = if (isInverse) action.toInverse() else action

                CalcButton(
                    action = displayAction,
                    isExpanded = isExpanded,
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(buttonAspectRatio),
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
                checkedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                checkedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSecondary,
            ),
            thumbContent = {
                Text(
                    text = if (useRadians) "Rad" else "Deg",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp
                )
            }
        )
    }
}

private data class ButtonColorPalette(val containerColor: Color, val contentColor: Color)

@Composable
private fun getButtonColors(action: CalcButtonAction): ButtonColorPalette {
    val colorScheme = MaterialTheme.colorScheme
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
