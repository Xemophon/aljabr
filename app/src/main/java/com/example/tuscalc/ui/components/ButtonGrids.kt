package com.example.tuscalc.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.layout.Layout
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.example.tuscalc.ui.theme.Dimens

@Composable
fun CalcButtons(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isInverse: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) {
    val buttonAspectRatio = if (isExpanded) Dimens.ButtonAspectRatioExpanded else Dimens.ButtonAspectRatioStandard
    val transition = updateTransition(targetState = isExpanded, label = "ScientificTransition")
    val expandFraction by transition.animateFloat(
        transitionSpec = {
            spring(stiffness = StiffnessLow, dampingRatio = DampingRatioLowBouncy)
        },
        label = "ExpandFraction"
    ) { state ->
        if (state) 1f else 0f
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryFixedDim,
        shape = AbsoluteRoundedCornerShape(topLeft = Dimens.LandingPageCornerRadius, topRight = Dimens.LandingPageCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal)
                .animateContentSize()
                .graphicsLayer(clip = true),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall, Alignment.CenterHorizontally)) {
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
                    Button(onClick = onToggleInverse,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInverse)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )) {
                        Text(
                            text = "Inverse",
                            color = if (isInverse)
                                MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.onTertiaryContainer
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
}

@Composable
fun CalcButtonsLimits(
    modifier: Modifier = Modifier,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
){
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryFixedDim,
        shape = AbsoluteRoundedCornerShape(
            topLeft = Dimens.LandingPageCornerRadius,
            topRight = Dimens.LandingPageCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onToggleInverse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInverse) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Inv",
                        color = if (isInverse) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onAction(CalcButtonAction.Limits("x → ∞", LimitType.INFINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "∞",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onAction(CalcButtonAction.Limits("x → a", LimitType.FINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "a",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            ButtonGrid(
                gridData = LimitsButtonsGrid,
                buttonAspectRatio = Dimens.ButtonAspectRatioExpanded,
                isExpanded = true,
                isInverse = isInverse,
                onAction = onAction
            )
        }
    }
}

@Composable
fun CalcButtonsIntegrate(
    modifier: Modifier = Modifier,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
){
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryFixedDim,
        shape = AbsoluteRoundedCornerShape(
            topLeft = Dimens.LandingPageCornerRadius,
            topRight = Dimens.LandingPageCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onToggleInverse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInverse) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Inv",
                        color = if (isInverse) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onAction(CalcButtonAction.Integrals("∫", IntegralType.INDEFINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "∫",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onAction(CalcButtonAction.Integrals("∫ab", IntegralType.DEFINITE)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "∫ ab",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            ButtonGrid(
                gridData = IntegralsButtonsGrid,
                buttonAspectRatio = Dimens.ButtonAspectRatioExpanded,
                isExpanded = true,
                isInverse = isInverse,
                onAction = onAction
            )
        }
    }
}

@Composable
fun CalcButtonsGraph(
    modifier: Modifier = Modifier,
    isInverse: Boolean = false,
    onToggleInverse: () -> Unit,
    onVisualize: () -> Unit,
    onAction: (CalcButtonAction) -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryFixedDim,
        shape = AbsoluteRoundedCornerShape(
            topLeft = Dimens.LandingPageCornerRadius,
            topRight = Dimens.LandingPageCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PaddingNormal, vertical = Dimens.PaddingNormal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onToggleInverse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInverse) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Inv",
                        color = if (isInverse) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
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
                    Text(
                        text = "Graph",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onAction(CalcButtonAction.Clear) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelLarge
                    )
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
    }
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
    return when (action) {
        is CalcButtonAction.Calculate -> ButtonColorPalette(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        is CalcButtonAction.Variable -> ButtonColorPalette(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        is CalcButtonAction.Scientific -> ButtonColorPalette(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        is CalcButtonAction.Symbol -> {
            if (action.isOperator) {
                ButtonColorPalette(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                ButtonColorPalette(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        is CalcButtonAction.Backspace -> ButtonColorPalette(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> ButtonColorPalette(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}