package com.xemophon.aljabr.modules.statistics.descriptives

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.AnalysisSectionHeader
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.FocusedInputOverlay
import com.xemophon.aljabr.ui.components.screens.MathDataMemberBox
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

    val isFocusedMode = selectedIndex != null && selectedIndex!! in members.indices

    BackHandler(enabled = resultState != null || isFocusedMode) {
        if (resultState != null) {
            resultState = null
        } else if (isFocusedMode) {
            selectedIndex = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .padding(padding)
                    .blur(if (isFocusedMode) 12.dp else 0.dp),
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Main Array Area Box
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Array Area (N = ${members.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(12.dp)
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
                                                    text = "Array Area is Empty",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tap '+' below to add members to the array",
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
                                                MathDataMemberBox(
                                                    indexPrefix = "x${index + 1}:",
                                                    value = value,
                                                    isSelected = selectedIndex == index,
                                                    onClick = { selectedIndex = index },
                                                    onRemove = {
                                                        members.removeAt(index)
                                                        selectedIndex = when {
                                                            members.isEmpty() -> null
                                                            selectedIndex == index -> null
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom Action Controls (Shown when no array member is focused for editing)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // + Button (Add member)
                            Button(
                                onClick = {
                                    members.add("")
                                    selectedIndex = members.size - 1
                                },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Member",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Solve / Compute Button
                            Button(
                                onClick = {
                                    resultState = DescriptivesFunc.calculate(members)
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Solve",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compute", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            // Clear Button
                            Button(
                                onClick = {
                                    members.clear()
                                    selectedIndex = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError )
                            }
                        }
                    }
                }
            }
        }

        // Focus Overlay for editing array elements
        AnimatedVisibility(
            visible = isFocusedMode,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            val activeIdx = selectedIndex ?: 0
            val currentVal = members.getOrNull(activeIdx) ?: ""

            FocusedInputOverlay(
                value = currentVal,
                title = "Editing Member x${activeIdx + 1}",
                onDismiss = { selectedIndex = null },
                onPrev = {
                    if (activeIdx > 0) {
                        selectedIndex = activeIdx - 1
                    }
                },
                onNext = {
                    if (activeIdx < members.size - 1) {
                        selectedIndex = activeIdx + 1
                    } else {
                        members.add("")
                        selectedIndex = members.size - 1
                    }
                },
                keypadContent = {
                    ShortCalcButtons(
                        modifier = Modifier.height(360.dp),
                        gridMode = ShortGridMode.Descriptives,
                        onAction = { action ->
                            val idx = selectedIndex ?: return@ShortCalcButtons
                            if (idx !in members.indices) return@ShortCalcButtons
                            val text = members[idx]

                            when (action) {
                                is CalcButtonAction.Symbol -> {
                                    when (action.text) {
                                        "." -> {
                                            if (!text.contains(".")) {
                                                members[idx] = "$text."
                                            }
                                        }
                                        "( )" -> {
                                            val openCount = text.count { it == '(' }
                                            val closeCount = text.count { it == ')' }
                                            members[idx] = if (openCount > closeCount && text.isNotEmpty() && text.last().isDigit()) {
                                                "$text)"
                                            } else {
                                                "$text("
                                            }
                                        }
                                        else -> {
                                            members[idx] = text + action.text
                                        }
                                    }
                                }

                                is CalcButtonAction.Backspace -> {
                                    if (text.isNotEmpty()) {
                                        members[idx] = text.dropLast(1)
                                    } else if (members.size > 1) {
                                        members.removeAt(idx)
                                        selectedIndex = (idx - 1).coerceAtLeast(0)
                                    }
                                }

                                is CalcButtonAction.Clear -> {
                                    if (text.isNotEmpty()) {
                                        members[idx] = ""
                                    } else {
                                        members.removeAt(idx)
                                        selectedIndex = if (members.isEmpty()) null else (idx - 1).coerceAtLeast(0)
                                    }
                                }

                                is CalcButtonAction.Calculate, is CalcButtonAction.Done -> {
                                    selectedIndex = null
                                    resultState = DescriptivesFunc.calculate(members)
                                }

                                is CalcButtonAction.Constant -> {
                                    members[idx] = text + action.text
                                }

                                is CalcButtonAction.Variable -> {
                                    members[idx] = text + action.text
                                }

                                else -> {}
                            }
                        }
                    )
                }
            )
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
