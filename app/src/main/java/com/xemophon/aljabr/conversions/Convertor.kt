package com.xemophon.aljabr.conversions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.components.Constants
import com.xemophon.aljabr.ui.components.ConverterField
import com.xemophon.aljabr.ui.components.ShortCalcButtons
import com.xemophon.aljabr.ui.theme.Dimens
import com.xemophon.aljabr.ui.theme.HorizontalSeparator

@Composable
fun ConvertorPage(
    onOpenDrawer: () -> Unit,
    viewModel: ConvertorViewModel = viewModel(),
) {
    val labels = viewModel.getLabels()
    val pagerState = rememberPagerState { ConversionMode.entries.size }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onModeChanged(ConversionMode.entries[pagerState.currentPage])
    }

    CalculatorScaffold(
        title = { Text("Convertor") },
        onOpenDrawer = onOpenDrawer)
    { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalSeparator(
                text = when (viewModel.mode) {
                    ConversionMode.ANGLE -> "Angle Convertor"
                    ConversionMode.COMPLEX_CART_POLAR -> "Complex Convertor"
                    ConversionMode.COMPLEX_CART_EXP -> "Exponential Convertor"
                }
            )
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingNormal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConverterField(
                        label = labels.first,
                        value = viewModel.primaryValue,
                        isFocused = viewModel.selectedField == SelectedField.PRIMARY,
                        cursorIndex = viewModel.primaryCursor,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedField = SelectedField.PRIMARY }
                    )
                    
                    IconButton(
                        onClick = { viewModel.swapFields() },
                        modifier = Modifier.padding(top = 24.dp) // Align with box centers roughly
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = "Swap")
                    }
                    
                    ConverterField(
                        label = labels.second,
                        value = viewModel.secondaryValue,
                        isFocused = viewModel.selectedField == SelectedField.SECONDARY,
                        cursorIndex = viewModel.secondaryCursor,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedField = SelectedField.SECONDARY }
                    )
                }
            }
            
            // Dots indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.PaddingNormal),
                horizontalArrangement = Arrangement.Center
            ) {
                ConversionMode.entries.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            val letterMode = if (viewModel.mode == ConversionMode.ANGLE) {
                CalcButtonAction.Constant("φ", Constants.PHI)
            } else {
                CalcButtonAction.Constant("i", Constants.I)
            }

            ShortCalcButtons(
                letterNeeded = letterMode,
                onAction = { viewModel.handleAction(it) }
            )
        }
    }
}
