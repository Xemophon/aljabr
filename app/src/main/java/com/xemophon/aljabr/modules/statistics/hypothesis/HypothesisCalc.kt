package com.xemophon.aljabr.modules.statistics.hypothesis

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.AnalysisSectionHeader
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.FocusedInputOverlay
import com.xemophon.aljabr.ui.components.screens.InputFieldSmall
import com.xemophon.aljabr.ui.components.screens.ReportScreen
import com.xemophon.aljabr.ui.components.screens.ResultItemCard
import java.util.Locale

@Composable
fun HypothesisCalc(
    viewModel: HypothesisViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    BackHandler(enabled = viewModel.resultState != null || viewModel.isFocusedMode) {
        if (viewModel.isFocusedMode) {
            viewModel.dismissFocus()
        } else if (viewModel.resultState != null) {
            viewModel.clearResult()
        }
    }

    CalculatorScaffold(
        title = { Text(text = "Hypothesis Testing") },
        onOpenDrawer = onOpenDrawer,
        navigationIcon = Icons.Default.Menu,
        navigationIconAction = onOpenDrawer,
        navigationIconContentDescription = "Menu"
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val result = viewModel.resultState
                if (result != null) {
                    HypothesisReport(
                        result = result,
                        onClear = { viewModel.clearResult() }
                    )
                } else {
                    HypothesisScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = viewModel.isFocusedMode,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    HypothesisOverlay(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HypothesisOverlay(
    viewModel: HypothesisViewModel
) {
    val title = when (viewModel.currentFocus) {
        HypothesisFocus.SAMPLE_MEAN -> "Editing Sample Mean (x̄)"
        HypothesisFocus.STANDARD_DEVIATION -> "Editing Standard Deviation (σ)"
        HypothesisFocus.SAMPLE_SIZE -> "Editing Sample Size (n)"
        HypothesisFocus.POPULATION_MEAN -> "Editing Pop Mean (μ₀)"
        HypothesisFocus.SIGNIFICANCE_LEVEL -> "Editing Significance Level (α)"
    }

    val placeholder = when (viewModel.currentFocus) {
        HypothesisFocus.SAMPLE_MEAN -> "Sample Mean"
        HypothesisFocus.STANDARD_DEVIATION -> "Standard Deviation"
        HypothesisFocus.SAMPLE_SIZE -> "Sample Size"
        HypothesisFocus.POPULATION_MEAN -> "Pop Mean"
        HypothesisFocus.SIGNIFICANCE_LEVEL -> "e.g. 0.05"
    }

    FocusedInputOverlay(
        value = viewModel.focusValue,
        title = title,
        placeholder = placeholder,
        onDismiss = { viewModel.dismissFocus() },
        onPrev = { viewModel.prevFocus() },
        onNext = { viewModel.nextFocus() },
        keypadContent = {
            ShortCalcButtons(
                modifier = Modifier.height(380.dp),
                gridMode = ShortGridMode.Distributions,
                onAction = { viewModel.handleAction(it) }
            )
        }
    )
}

@Composable
fun HypothesisScreen(
    viewModel: HypothesisViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Core Parameters
                Text(
                    text = "Core Parameters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputFieldSmall(
                        label = "x̄",
                        value = viewModel.sampleMean,
                        isFocused = viewModel.currentFocus == HypothesisFocus.SAMPLE_MEAN && viewModel.isFocusedMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onFocusChange(HypothesisFocus.SAMPLE_MEAN) }
                    )
                    InputFieldSmall(
                        label = "σ",
                        value = viewModel.standardDeviation,
                        isFocused = viewModel.currentFocus == HypothesisFocus.STANDARD_DEVIATION && viewModel.isFocusedMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onFocusChange(HypothesisFocus.STANDARD_DEVIATION) }
                    )
                    InputFieldSmall(
                        label = "n",
                        value = viewModel.sampleSize,
                        isFocused = viewModel.currentFocus == HypothesisFocus.SAMPLE_SIZE && viewModel.isFocusedMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onFocusChange(HypothesisFocus.SAMPLE_SIZE) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section 2: Hypothesis Params
                Text(
                    text = "Hypothesis Parameters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputFieldSmall(
                        label = "μ₀",
                        value = viewModel.populationMean,
                        isFocused = viewModel.currentFocus == HypothesisFocus.POPULATION_MEAN && viewModel.isFocusedMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onFocusChange(HypothesisFocus.POPULATION_MEAN) }
                    )
                    InputFieldSmall(
                        label = "α",
                        value = viewModel.significanceLevel,
                        isFocused = viewModel.currentFocus == HypothesisFocus.SIGNIFICANCE_LEVEL && viewModel.isFocusedMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onFocusChange(HypothesisFocus.SIGNIFICANCE_LEVEL) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.compute() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Compute", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = { viewModel.clear() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Clear", style = MaterialTheme.typography.titleMedium,color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
fun HypothesisReport(
    result: HypothesisResult,
    onClear: () -> Unit
) {
    ReportScreen(
        title = "Hypothesis & Confidence Report",
        error = result.errorMessage,
        onClear = onClear
    ) {
        item {
            AnalysisSectionHeader("Core Sample Parameters")
        }
        item {
            ResultItemCard(
                label = "Sample Mean (x̄)",
                displayText = formatDouble(result.sampleMean)
            )
        }
        item {
            ResultItemCard(
                label = "Standard Deviation (s)",
                displayText = formatDouble(result.standardDeviation)
            )
        }
        item {
            ResultItemCard(
                label = "Sample Size (n)",
                displayText = result.sampleSize.toString()
            )
        }
        item {
            ResultItemCard(
                label = "Standard Error (SE = s / √n)",
                displayText = formatDouble(result.standardError)
            )
        }
        item {
            ResultItemCard(
                label = "Degrees of Freedom (df = n - 1)",
                displayText = result.degreesOfFreedom.toString()
            )
        }

        // Hypothesis Testing Section
        if (result.populationMean != null) {
            item {
                AnalysisSectionHeader("Hypothesis Testing")
            }
            item {
                ResultItemCard(
                    label = "Hypothesized Population Mean (μ₀)",
                    displayText = formatDouble(result.populationMean)
                )
            }
            result.testStatistic?.let { stat ->
                item {
                    ResultItemCard(
                        label = "Test Statistic (z or t score)",
                        displayText = formatDouble(stat)
                    )
                }
            }

            // Z-Test Outputs
            item {
                AnalysisSectionHeader("Z-Test Results")
            }
            result.pTwoTailedZ?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (Z-Test, Two-Tailed)",
                        displayText = formatDouble(p)
                    )
                }
            }
            result.pLeftTailedZ?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (Z-Test, Left-Tailed: H₁: μ < μ₀)",
                        displayText = formatDouble(p)
                    )
                }
            }
            result.pRightTailedZ?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (Z-Test, Right-Tailed: H₁: μ > μ₀)",
                        displayText = formatDouble(p)
                    )
                }
            }
            if (result.significanceLevel != null) {
                result.zCriticalHypo?.let { crit ->
                    item {
                        ResultItemCard(
                            label = "Z Critical Value (z*)",
                            displayText = formatDouble(crit)
                        )
                    }
                }
                result.rejectNullZ?.let { reject ->
                    item {
                        ResultItemCard(
                            label = "Statistical Decision (Z-Test, α = ${formatDouble(result.significanceLevel)})",
                            displayText = if (reject) "Reject H₀" else "Fail to Reject H₀"
                        )
                    }
                }
            }

            // T-Test Outputs
            item {
                AnalysisSectionHeader("t-Test Results (df = ${result.degreesOfFreedom})")
            }
            result.pTwoTailedT?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (t-Test, Two-Tailed)",
                        displayText = formatDouble(p)
                    )
                }
            }
            result.pLeftTailedT?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (t-Test, Left-Tailed: H₁: μ < μ₀)",
                        displayText = formatDouble(p)
                    )
                }
            }
            result.pRightTailedT?.let { p ->
                item {
                    ResultItemCard(
                        label = "p-value (t-Test, Right-Tailed: H₁: μ > μ₀)",
                        displayText = formatDouble(p)
                    )
                }
            }
            if (result.significanceLevel != null) {
                result.tCriticalHypo?.let { crit ->
                    item {
                        ResultItemCard(
                            label = "t Critical Value (t*)",
                            displayText = formatDouble(crit)
                        )
                    }
                }
                result.rejectNullT?.let { reject ->
                    item {
                        ResultItemCard(
                            label = "Statistical Decision (t-Test, α = ${formatDouble(result.significanceLevel)})",
                            displayText = if (reject) "Reject H₀" else "Fail to Reject H₀"
                        )
                    }
                }
            }
        }

        // Confidence Interval Section (C = 1 - alpha)
        if (result.confidenceLevel != null) {
            item {
                AnalysisSectionHeader("Confidence Interval (${formatDouble(result.confidenceLevel * 100)}%)")
            }

            // Z-Distribution Confidence Interval
            if (result.zCriticalConf != null && result.marginOfErrorZ != null && result.lowerBoundZ != null && result.upperBoundZ != null) {
                item {
                    AnalysisSectionHeader("Z-Distribution Confidence Interval")
                }
                item {
                    ResultItemCard(
                        label = "Critical Value (z*)",
                        displayText = formatDouble(result.zCriticalConf)
                    )
                }
                item {
                    ResultItemCard(
                        label = "Margin of Error (MoE, z)",
                        displayText = formatDouble(result.marginOfErrorZ)
                    )
                }
                item {
                    ResultItemCard(
                        label = "Confidence Interval (z)",
                        displayText = "[ ${formatDouble(result.lowerBoundZ)}, ${formatDouble(result.upperBoundZ)} ]"
                    )
                }
            }

            // T-Distribution Confidence Interval
            if (result.tCriticalConf != null && result.marginOfErrorT != null && result.lowerBoundT != null && result.upperBoundT != null) {
                item {
                    AnalysisSectionHeader("t-Distribution Confidence Interval (df = ${result.degreesOfFreedom})")
                }
                item {
                    ResultItemCard(
                        label = "Critical Value (t*)",
                        displayText = formatDouble(result.tCriticalConf)
                    )
                }
                item {
                    ResultItemCard(
                        label = "Margin of Error (MoE, t)",
                        displayText = formatDouble(result.marginOfErrorT)
                    )
                }
                item {
                    ResultItemCard(
                        label = "Confidence Interval (t)",
                        displayText = "[ ${formatDouble(result.lowerBoundT)}, ${formatDouble(result.upperBoundT)} ]"
                    )
                }
            }
        }
    }
}

private fun formatDouble(value: Double, precision: Int = 4): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val str = String.format(Locale.US, "%.${precision}f", value)
    return if (str.contains(".")) {
        str.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
    } else str
}
