package com.xemophon.aljabr.modules.series.fourier

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.screens.AnalysisSectionHeader
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.buttons.Constants
import com.xemophon.aljabr.ui.components.screens.FourierReport
import com.xemophon.aljabr.ui.components.screens.ResultItemCard
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode

@Composable
fun FourierCalc(
    onOpenDrawer: () -> Unit
){
    val viewModel: FourierViewModel = viewModel()
    
    Box(modifier = Modifier.fillMaxSize()) {
        CalculatorScaffold(
            title = { Text("Fourier Series") },
            onOpenDrawer = onOpenDrawer
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .blur(if (viewModel.isFocusedMode) 12.dp else 0.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (viewModel.fourierResult == null && !viewModel.isCalculating) {
                    FourierContent(viewModel)
                } else if (viewModel.isCalculating) {
                    FourierLoadingReport(viewModel)
                } else {
                    FourierReport(
                        result = viewModel.fourierResult!!,
                        onClear = { viewModel.clearResult() }
                    )
                }
            }
        }

        // Focus Overlay
        AnimatedVisibility(
            visible = viewModel.isFocusedMode,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            FourierFocusOverlay(
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun FourierContent(
    viewModel: FourierViewModel
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton(
                text = "Single Function",
                isSelected = !viewModel.isTwoBranch,
                onClick = { viewModel.isTwoBranch = false },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                text = "Two Branch",
                isSelected = viewModel.isTwoBranch,
                onClick = { viewModel.isTwoBranch = true },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Branch 1 / Single Function
        BranchArea(
            label = if (viewModel.isTwoBranch) "Branch 1" else "Function",
            functionValue = viewModel.f1,
            limitA = viewModel.a,
            limitB = if (viewModel.isTwoBranch) viewModel.b else viewModel.c,
            onFunctionClick = { viewModel.onFocusChange(FourierFocus.BRANCH1) },
            onLimitAClick = { viewModel.onFocusChange(FourierFocus.LIMIT_A) },
            onLimitBClick = { 
                if (viewModel.isTwoBranch) viewModel.onFocusChange(FourierFocus.LIMIT_B)
                else viewModel.onFocusChange(FourierFocus.LIMIT_C)
            }
        )
        
        // Branch 2
        AnimatedVisibility(visible = viewModel.isTwoBranch) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                BranchArea(
                    label = "Branch 2",
                    functionValue = viewModel.f2,
                    limitA = viewModel.b,
                    limitB = viewModel.c,
                    onFunctionClick = { viewModel.onFocusChange(FourierFocus.BRANCH2) },
                    onLimitAClick = { viewModel.onFocusChange(FourierFocus.LIMIT_B) },
                    onLimitBClick = { viewModel.onFocusChange(FourierFocus.LIMIT_C) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Compute Button
        Button(
            onClick = { viewModel.calculateResult() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Compute")
        }
    }
}

@Composable
fun BranchArea(
    label: String,
    functionValue: String,
    limitA: String,
    limitB: String,
    onFunctionClick: () -> Unit,
    onLimitAClick: () -> Unit,
    onLimitBClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Function Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onFunctionClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = functionValue.ifEmpty { "f(x)" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (functionValue.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Limits Row: a < x < b
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LimitBox(value = limitA, onClick = onLimitAClick)
                Text(
                    text = " < x < ",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                LimitBox(value = limitB, onClick = onLimitBClick)
            }
        }
    }
}

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
        modifier = modifier.border(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LimitBox(
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FourierLoadingReport(viewModel: FourierViewModel) {
    val result = viewModel.fourierResult ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Calculating Fourier Series...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = Color.Transparent
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (result.l.isNotEmpty()) {
                    AnalysisSectionHeader("Parameters")
                    ResultItemCard("L (Half-period)", result.l)
                }
                
                if (result.a0.isNotEmpty()) {
                    ResultItemCard("a₀ (DC Component)", result.a0)
                }

                if (result.anGeneral != null || result.bnGeneral != null) {
                    AnalysisSectionHeader("General Coefficients")
                    result.anGeneral?.let { ResultItemCard("aₙ (Symbolic)", it) }
                    result.bnGeneral?.let { ResultItemCard("bₙ (Symbolic)", it) }
                }
            }
        }
    }
}
@Composable
fun FourierFocusOverlay(
    viewModel: FourierViewModel
) {
    val focusValue = when (viewModel.currentFocus) {
        FourierFocus.BRANCH1 -> viewModel.f1
        FourierFocus.BRANCH2 -> viewModel.f2
        FourierFocus.LIMIT_A -> viewModel.a
        FourierFocus.LIMIT_B -> viewModel.b
        FourierFocus.LIMIT_C -> viewModel.c
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.dismissFocus() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Large Focused Element Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = focusValue,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ValueTransition"
                ) { targetValue ->
                    Text(
                        text = targetValue.ifEmpty { 
                            when(viewModel.currentFocus) {
                                FourierFocus.BRANCH1 -> if (viewModel.isTwoBranch) "f1(x)" else "f(x)"
                                FourierFocus.BRANCH2 -> "f2(x)"
                                FourierFocus.LIMIT_A -> "a"
                                FourierFocus.LIMIT_B -> "b"
                                FourierFocus.LIMIT_C -> "c"
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (targetValue.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                color = MaterialTheme.colorScheme.inversePrimary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.prevFocus() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }

                        Text(
                            text = "Editing ${viewModel.currentFocus.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = { viewModel.nextFocus() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    val gridMode = if (viewModel.currentFocus in listOf(FourierFocus.BRANCH1, FourierFocus.BRANCH2)) {
                        ShortGridMode.Functions
                    } else {
                        ShortGridMode.Convertor
                    }

                    ShortCalcButtons(
                        modifier = Modifier.height(400.dp),
                        gridMode = gridMode,
                        onAction = { viewModel.handleAction(it) },
                        letterNeeded = if (gridMode == ShortGridMode.Convertor) CalcButtonAction.Done else CalcButtonAction.Constant("π", Constants.PI)
                    )
                }
            }
        }
    }
}
