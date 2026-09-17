package com.xemophon.aljabr.modules.statistics.descriptives

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.AnalysisSectionHeader
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.ReportScreen
import com.xemophon.aljabr.ui.components.screens.ResultItemCard
import java.util.Locale
import kotlin.math.abs

@Composable
fun DescriptivesScreen(
    onOpenDrawer: () -> Unit
) {
    val members = remember { mutableStateListOf<String>() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var resultState by remember { mutableStateOf<DescriptivesResult?>(null) }

    BackHandler(enabled = resultState != null) {
        resultState = null
    }

    CalculatorScaffold(
        title = { Text(text = "Descriptive Statistics") },
        onOpenDrawer = onOpenDrawer,
        navigationIcon = Icons.Default.Menu,
        navigationIconAction = onOpenDrawer,
        navigationIconContentDescription = "Menu"
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (resultState != null) {
                DescriptivesReport(
                    result = resultState!!,
                    onClear = { resultState = null }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    // Upper Box: Interactive Array Fields & Action Controls
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Control Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Data Array (N = ${members.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (members.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                members.clear()
                                                selectedIndex = null
                                            },
                                            shape = MaterialTheme.shapes.medium,
                                            contentPadding = ButtonDefaults.ContentPadding,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear All",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            members.add("")
                                            selectedIndex = members.size - 1
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        contentPadding = ButtonDefaults.ContentPadding,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Member",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Member", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            resultState = DescriptivesFunc.calculate(members)
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "Compute",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Compute", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scrollable Focus Box Area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(8.dp)
                            ) {
                                if (members.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Array is empty",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Tap '+ Member' to add data elements to analyze",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        members.forEachIndexed { index, value ->
                                            DataMemberBox(
                                                index = index,
                                                value = value,
                                                isSelected = selectedIndex == index,
                                                onClick = { selectedIndex = index },
                                                onRemove = {
                                                    members.removeAt(index)
                                                    selectedIndex = when {
                                                        members.isEmpty() -> null
                                                        selectedIndex == index -> (index - 1).coerceAtLeast(0)
                                                        selectedIndex != null && selectedIndex!! > index -> selectedIndex!! - 1
                                                        else -> selectedIndex
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Keypad for input into focused member box
                    ShortCalcButtons(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth(),
                        gridMode = ShortGridMode.Descriptives,
                        onAction = { action ->
                            // Ensure there is an active focused box
                            if (selectedIndex == null || selectedIndex!! >= members.size) {
                                if (members.isEmpty()) {
                                    members.add("")
                                    selectedIndex = 0
                                } else {
                                    selectedIndex = members.size - 1
                                }
                            }

                            val activeIdx = selectedIndex ?: return@ShortCalcButtons
                            val currentText = members[activeIdx]

                            when (action) {
                                is CalcButtonAction.Symbol -> {
                                    when (action.text) {
                                        "." -> {
                                            if (!currentText.contains(".")) {
                                                members[activeIdx] = currentText + "."
                                            }
                                        }
                                        "( )" -> {
                                            val openCount = currentText.count { it == '(' }
                                            val closeCount = currentText.count { it == ')' }
                                            members[activeIdx] = if (openCount > closeCount && currentText.isNotEmpty() && currentText.last().isDigit()) {
                                                "$currentText)"
                                            } else {
                                                "$currentText("
                                            }
                                        }
                                        else -> {
                                            members[activeIdx] = currentText + action.text
                                        }
                                    }
                                }

                                is CalcButtonAction.Backspace -> {
                                    if (currentText.isNotEmpty()) {
                                        members[activeIdx] = currentText.dropLast(1)
                                    } else if (members.size > 1) {
                                        members.removeAt(activeIdx)
                                        selectedIndex = (activeIdx - 1).coerceAtLeast(0)
                                    }
                                }

                                is CalcButtonAction.Clear -> {
                                    if (currentText.isNotEmpty()) {
                                        members[activeIdx] = ""
                                    } else {
                                        members.clear()
                                        selectedIndex = null
                                    }
                                }

                                is CalcButtonAction.Calculate, is CalcButtonAction.Done -> {
                                    resultState = DescriptivesFunc.calculate(members)
                                }

                                is CalcButtonAction.Constant -> {
                                    members[activeIdx] = currentText + action.text
                                }

                                is CalcButtonAction.Variable -> {
                                    members[activeIdx] = currentText + action.text
                                }

                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DataMemberBox(
    index: Int,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
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

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "x${index + 1}:",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            val displayText = if (isSelected) {
                buildAnnotatedString {
                    append(value)
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                        append("|")
                    }
                }
            } else {
                buildAnnotatedString {
                    if (value.isEmpty()) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) {
                            append("0")
                        }
                    } else {
                        append(value)
                    }
                }
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DescriptivesReport(
    result: DescriptivesResult,
    onClear: () -> Unit
) {
    ReportScreen(
        title = "Descriptive Statistics Report",
        error = result.error,
        onClear = onClear
    ) {
        item {
            AnalysisSectionHeader("Measures of Central Tendency")
        }
        item {
            ResultItemCard(
                label = "Mean (x̄)",
                displayText = formatDouble(result.mean),
                rawValue = "x̄ = ${formatDouble(result.mean)}"
            )
        }
        item {
            ResultItemCard(
                label = "Median (x̃)",
                displayText = formatDouble(result.median),
                rawValue = "x̃ = ${formatDouble(result.median)}"
            )
        }
        item {
            val modeStr = if (result.mode.isEmpty()) "No Mode (All values unique)"
            else result.mode.joinToString(", ") { formatDouble(it) }
            ResultItemCard(
                label = "Mode",
                displayText = modeStr
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            AnalysisSectionHeader("Measures of Dispersion")
        }
        item {
            ResultItemCard(
                label = "Sample Variance (s²)",
                displayText = formatDouble(result.varianceSample),
                rawValue = "s^2 = ${formatDouble(result.varianceSample)}"
            )
        }
        item {
            ResultItemCard(
                label = "Sample Standard Deviation (s)",
                displayText = formatDouble(result.stdDevSample),
                rawValue = "s = ${formatDouble(result.stdDevSample)}"
            )
        }
        item {
            ResultItemCard(
                label = "Population Variance (σ²)",
                displayText = formatDouble(result.variancePopulation),
                rawValue = "\\sigma^2 = ${formatDouble(result.variancePopulation)}"
            )
        }
        item {
            ResultItemCard(
                label = "Population Standard Deviation (σ)",
                displayText = formatDouble(result.stdDevPopulation),
                rawValue = "\\sigma = ${formatDouble(result.stdDevPopulation)}"
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            AnalysisSectionHeader("Summary Statistics")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResultItemCard(label = "Count (N)", displayText = result.count.toString())
                }
                Box(modifier = Modifier.weight(1f)) {
                    ResultItemCard(label = "Sum (Σx)", displayText = formatDouble(result.sum))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResultItemCard(label = "Min", displayText = formatDouble(result.min))
                }
                Box(modifier = Modifier.weight(1f)) {
                    ResultItemCard(label = "Max", displayText = formatDouble(result.max))
                }
                Box(modifier = Modifier.weight(1f)) {
                    ResultItemCard(label = "Range", displayText = formatDouble(result.range))
                }
            }
        }
        item {
            ResultItemCard(
                label = "Sorted Data Array",
                displayText = "[ " + result.sortedValues.joinToString(", ") { formatDouble(it) } + " ]"
            )
        }

        result.regression?.let { reg ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AnalysisSectionHeader("Linear Regression & Trend Analysis")
            }

            val slope = reg.slope
            val intercept = reg.intercept
            val rSquare = reg.rSquare
            val r = reg.r

            if (!r.isNaN()) {
                item {
                    ResultItemCard(
                        label = "Pearson Correlation Coefficient (r)",
                        displayText = formatDouble(r),
                        rawValue = "r = ${formatDouble(r)}"
                    )
                }
            }

            if (!slope.isNaN() && !intercept.isNaN()) {
                val sign = if (intercept >= 0) "+" else "-"
                val absIntercept = abs(intercept)
                val eqText = "y = ${formatDouble(slope)}x $sign ${formatDouble(absIntercept)}"
                item {
                    ResultItemCard(
                        label = "Linear Regression Equation",
                        displayText = eqText,
                        rawValue = eqText
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ResultItemCard(
                                label = "Slope (m)",
                                displayText = formatDouble(slope),
                                rawValue = "m = ${formatDouble(slope)}"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ResultItemCard(
                                label = "Y-Intercept (b)",
                                displayText = formatDouble(intercept),
                                rawValue = "b = ${formatDouble(intercept)}"
                            )
                        }
                    }
                }
            }

            if (!rSquare.isNaN()) {
                item {
                    ResultItemCard(
                        label = "Coefficient of Determination (R²)",
                        displayText = formatDouble(rSquare),
                        rawValue = "R^2 = ${formatDouble(rSquare)}"
                    )
                }
            }
        }
    }
}

private fun formatDouble(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val formatted = String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    return if (formatted == "-0") "0" else formatted
}
