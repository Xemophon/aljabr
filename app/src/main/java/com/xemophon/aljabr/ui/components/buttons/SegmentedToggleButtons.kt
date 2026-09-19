package com.xemophon.aljabr.ui.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable single mode toggle button.
 */
@Composable
fun ModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Reusable Segmented Toggle Buttons Row for switching between modes or options.
 */
@Composable
fun <T> SegmentedToggleButtons(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (option, label) ->
            ModeButton(
                text = label,
                isSelected = option == selectedOption,
                onClick = { onOptionSelected(option) },
                modifier = if (options.size <= 3) Modifier.weight(1f) else Modifier
            )
        }
    }
}
