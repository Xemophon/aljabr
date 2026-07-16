package com.xemophon.aljabr.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
            content = content
        )
    }
}

@Composable
fun CalcButtons(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isInverse: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) {
    val buttonAspectRatio =
        if (isExpanded) Dimens.ButtonAspectRatioExpanded else Dimens.ButtonAspectRatioStandard
    val transition = updateTransition(targetState = isExpanded, label = "ScientificTransition")
    val expandFraction by transition.animateFloat(
        transitionSpec = {
            spring(stiffness = StiffnessLow, dampingRatio = DampingRatioLowBouncy)
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
                Text(
                    text = if (isExpanded) "Standard" else "Scientific",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 24.sp
                )
            }
            if (isExpanded) {
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

@Composable
fun AdvancedButtonsGrid(
    modifier: Modifier = Modifier,
    mode: String = "Limits",
    isInverse: Boolean = false,
    limitType: LimitType = LimitType.FINITE,
    integType: IntegralType = IntegralType.DEFINITE,
    diffGridMode: String = "Single",
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) = CalcButtonSheet(modifier) {
    val selectedGrid = if (diffGridMode == "Single") SingleVariableGrid else MultipleVariableGrid
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
        when (mode) {
            "Limits" -> {
                Button(
                    onClick = { onAction(CalcButtonAction.Limits("x → ∞", LimitType.INFINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (limitType == LimitType.INFINITE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (limitType == LimitType.INFINITE) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "∞", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { onAction(CalcButtonAction.Limits("x → a", LimitType.FINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (limitType == LimitType.FINITE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (limitType == LimitType.FINITE) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "a", style = MaterialTheme.typography.labelLarge)
                }
            }

            "Integration", "Integrate" -> {
                Button(
                    onClick = {
                        onAction(
                            CalcButtonAction.Integrals(
                                "∫",
                                IntegralType.INDEFINITE
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (integType == IntegralType.INDEFINITE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (integType == IntegralType.INDEFINITE) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "∫", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        onAction(
                            CalcButtonAction.Integrals(
                                "∫ab",
                                IntegralType.DEFINITE
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (integType == IntegralType.DEFINITE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (integType == IntegralType.DEFINITE) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "∫ ab", style = MaterialTheme.typography.labelLarge)
                }
            }

            "Differentiation", "Differentiate" -> {
                Button(
                    onClick = { onAction(CalcButtonAction.DifferentiateSingle("d/dx")) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (diffGridMode == "Single") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (diffGridMode == "Single") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "d/dx", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { onAction(CalcButtonAction.Differentiate("∇f")) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (diffGridMode == "Multiple") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (diffGridMode == "Multiple") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "∇f", style = MaterialTheme.typography.labelLarge)
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
