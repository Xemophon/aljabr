package com.xemophon.aljabr.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xemophon.aljabr.ui.theme.Dimens

@Composable
fun ConverterField(
    label: String,
    value: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    cursorIndex: Int = -1,
    onClick: () -> Unit,
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

    val scrollState = rememberScrollState()
    
    // Auto-scroll to end when value changes if focused
    LaunchedEffect(value) {
        if (isFocused) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier.padding(Dimens.PaddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isFocused -> MaterialTheme.colorScheme.primary
                isReadOnly -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp, max = 80.dp)
                .clip(RoundedCornerShape(Dimens.ButtonCornerRadiusStandard))
                .background(
                    when {
                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        isReadOnly -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    }
                )
                .border(
                    width = 2.dp,
                    color = when {
                        isFocused -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = RoundedCornerShape(Dimens.ButtonCornerRadiusStandard)
                )
                .then(if (!isReadOnly) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val annotatedText = buildAnnotatedString {
                if (!isReadOnly && isFocused && cursorIndex != -1 && (cursorIndex <= value.length)) {
                    append(value.substring(0, cursorIndex))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                        append("|")
                    }
                    append(value.substring(cursorIndex))
                } else {
                    append(value)
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                color = if (isReadOnly) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(scrollState)
            )
        }
    }
}
