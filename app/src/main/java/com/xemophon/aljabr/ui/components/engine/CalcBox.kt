package com.xemophon.aljabr.ui.components.engine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalcBox(
    expression: String,
    result: String,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    onCursorIndexChange: (Int) -> Unit = {},
    showStepsButton: Boolean = false,
    onShowStepsClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (showStepsButton) {
            IconButton(
                onClick = onShowStepsClick,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Show Steps",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                val expressionFontSize by animateFloatAsState(
                    targetValue = if (expression.length > 12) 32f else 40f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "ExpressionFontSize"
                )

                val textStyle = MaterialTheme.typography.displayMedium.copy(
                    fontSize = expressionFontSize.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End
                )

                val annotatedExpression = buildAnnotatedString {
                    if (cursorIndex != -1 && cursorIndex <= expression.length) {
                        append(expression.substring(0, cursorIndex))
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = cursorAlpha
                                )
                            )
                        ) {
                            append("|")
                        }
                        append(expression.substring(cursorIndex))
                    } else {
                        append(expression)
                    }
                }

                Text(
                    text = annotatedExpression,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = expressionFontSize.sp * 1.1f,
                    modifier = Modifier.clickable { onCursorIndexChange(expression.length) }
                )
            }
        }

        AnimatedContent(
            targetState = result,
            transitionSpec = {
                val isAppearing = targetState.isNotEmpty() && initialState.isEmpty()
                val isDisappearing = targetState.isEmpty() && initialState.isNotEmpty()

                if (isAppearing) {
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } +
                            fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.92f))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                } else if (isDisappearing) {
                    fadeIn(animationSpec = tween(90))
                        .togetherWith(
                            slideOutVertically { it / 3 } +
                                    fadeOut(animationSpec = tween(180)) +
                                    scaleOut(targetScale = 0.92f)
                        )
                } else {
                    // Directional scroll based on numerical change
                    val isIncreasing = (targetState.toDoubleOrNull() ?: 0.0) >= (initialState.toDoubleOrNull() ?: 0.0)
                    val slideOffset = { height: Int -> if (isIncreasing) height else -height }

                    (slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = slideOffset
                    ) + fadeIn(animationSpec = tween(150)))
                        .togetherWith(
                            slideOutVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetY = { -slideOffset(it) }
                            ) + fadeOut(animationSpec = tween(150))
                        )
                } using SizeTransform(clip = false)
            },
            label = "ResultAnimation",
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) { targetResult ->
            if (targetResult.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val resultFontSize by animateFloatAsState(
                        targetValue = if (targetResult.length > 8) 48f else 64f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ResultFontSize"
                    )

                    Text(
                        text = targetResult,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = resultFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
