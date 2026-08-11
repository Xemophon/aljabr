package com.xemophon.aljabr.conversions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.xemophon.aljabr.ui.components.HorizontalSeparator
import com.xemophon.aljabr.ui.components.ShortCalcButtons
import com.xemophon.aljabr.ui.theme.Dimens

@Composable
fun ConvertorPage(
    onOpenDrawer: () -> Unit,
    viewModel: ConvertorViewModel = viewModel(),
) {
    val labels = viewModel.getLabels()
    val subLabels = viewModel.getSubLabels()
    val pagerState = rememberPagerState { ConversionMode.entries.size }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onModeChanged(ConversionMode.entries[pagerState.currentPage])
    }

    CalculatorScaffold(
        title = { Text("Convertor") },
        onOpenDrawer = onOpenDrawer
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                HorizontalSeparator(
                    text = when (viewModel.mode) {
                        ConversionMode.ANGLE -> "Angle Convertor"
                        ConversionMode.COMPLEX -> "Complex Convertor"
                    }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(0.7f)
                ) { _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.PaddingNormal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Side
                        ConvertorSide(
                            label = labels.first,
                            subLabels = subLabels.first,
                            values = viewModel.primaryValue to viewModel.primaryValue2,
                            isSideFocused = viewModel.selectedField == SelectedField.PRIMARY,
                            selectedSubField = viewModel.selectedSubField,
                            cursors = viewModel.primaryCursor to viewModel.primaryCursor2,
                            onSideClick = { viewModel.selectedField = SelectedField.PRIMARY },
                            onSubFieldClick = { viewModel.selectedSubField = it },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.swapFields() },
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = "Swap")
                        }

                        // Secondary Side
                        ConvertorSide(
                            label = labels.second,
                            subLabels = subLabels.second,
                            values = viewModel.secondaryValue to viewModel.secondaryValue2,
                            isSideFocused = viewModel.selectedField == SelectedField.SECONDARY,
                            selectedSubField = viewModel.selectedSubField,
                            cursors = viewModel.secondaryCursor to viewModel.secondaryCursor2,
                            onSideClick = { viewModel.selectedField = SelectedField.SECONDARY },
                            onSubFieldClick = { viewModel.selectedSubField = it },
                            modifier = Modifier.weight(1f)
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
                    CalcButtonAction.Constant("e", Constants.E)
                } else {
                    CalcButtonAction.Constant("j", Constants.I)
                }

                ShortCalcButtons(
                    modifier = Modifier.weight(1.3f),
                    letterNeeded = letterMode,
                    onAction = { viewModel.handleAction(it) }
                )
            }
        }
    }
}

@Composable
fun ConvertorSide(
    label: String,
    subLabels: Pair<String, String>,
    values: Pair<String, String>,
    isSideFocused: Boolean,
    selectedSubField: SubField,
    cursors: Pair<Int, Int>,
    onSideClick: () -> Unit,
    onSubFieldClick: (SubField) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSideFocused) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ConverterField(
            label = subLabels.first,
            value = values.first,
            isFocused = isSideFocused && selectedSubField == SubField.MAIN,
            isReadOnly = !isSideFocused,
            cursorIndex = cursors.first,
            modifier = Modifier.weight(1f),
            onClick = {
                onSideClick()
                onSubFieldClick(SubField.MAIN)
            }
        )

        if (subLabels.second.isNotEmpty()) {
            ConverterField(
                label = subLabels.second,
                value = values.second,
                isFocused = isSideFocused && selectedSubField == SubField.EXTRA,
                isReadOnly = !isSideFocused,
                cursorIndex = cursors.second,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSideClick()
                    onSubFieldClick(SubField.EXTRA)
                }
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
