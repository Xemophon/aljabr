package com.xemophon.aljabr.modules.algebra.matrices

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.components.Constants
import com.xemophon.aljabr.ui.components.MatrixReport
import com.xemophon.aljabr.ui.components.ShortCalcButtons
import com.xemophon.aljabr.ui.components.ShortGridMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(
    viewModel: com.xemophon.aljabr.modules.algebra.matrices.MatrixViewModel,
    onOpenDrawer: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content (Blurred when focused)
        CalculatorScaffold(
            title = { Text("Matrix Calculator") },
            onOpenDrawer = onOpenDrawer
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .blur(if (viewModel.isFocusedMode) 12.dp else 0.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (viewModel.resultText.isEmpty()) {
                        // Top controls: Mode, Matrix Label, and Dimensions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.ModeSelector(
                                currentMode = viewModel.mode,
                                onModeSelected = { viewModel.onModeChange(it) }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isMatrixB = viewModel.activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.B
                                val rowsEnabled = !isMatrixB || viewModel.mode !in listOf(
                                    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.ADDITION,
                                    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.SUBTRACTION,
                                    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.MULTIPLICATION,
                                    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.LINEARSOLVE
                                )
                                val colsEnabled = (viewModel.activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A && viewModel.mode !in _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.SquareMatrixModes) ||
                                        (viewModel.activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.B && viewModel.mode !in listOf(
                                            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.ADDITION,
                                            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.SUBTRACTION,
                                            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.LINEARSOLVE
                                        ))

                                _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.DimensionControl(
                                    label = "R",
                                    value = viewModel.rows,
                                    enabled = rowsEnabled,
                                    onValueChange = { viewModel.updateRows(it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.DimensionControl(
                                    label = "C",
                                    value = viewModel.columns,
                                    enabled = colsEnabled,
                                    onValueChange = { viewModel.updateColumns(it) }
                                )
                            }
                        }

                        HorizontalDivider()
                    }

                    // Matrix Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Matrix indicator (A or B)
                            AnimatedVisibility(visible = viewModel.mode !in _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.SingleMatrixModes || viewModel.activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Matrix ${viewModel.activeMatrix.name}",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (viewModel.resultText.isEmpty()) {
                                _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixBoxField(
                                    rows = viewModel.rows,
                                    cols = viewModel.columns,
                                    data = viewModel.matrixData,
                                    selectedIndex = viewModel.selectedIndex,
                                    onElementClick = { viewModel.onElementClick(it) }
                                )
                            } else {
                                MatrixReport(
                                    title = "${
                                        viewModel.mode.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    } Result",
                                    result = viewModel.resultText,
                                    onLoadA = { viewModel.loadResultIntoMatrix(_root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) },
                                    onLoadB = { viewModel.loadResultIntoMatrix(_root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.B) },
                                    onClear = { viewModel.clearResult() }
                                )
                            }
                        }
                    }

                    // Bottom Control Bar
                    if (viewModel.resultText.isEmpty()) {
                        _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.ControlBar(
                            activeMatrix = viewModel.activeMatrix,
                            activeMode = viewModel.mode,
                            onClear = { viewModel.clearCurrentMatrix() },
                            onToggle = { viewModel.toggleMatrix() },
                            onCompute = { viewModel.calculateResult() }
                        )
                    }
                }
            }
        }

        // Focus Overlay
        AnimatedVisibility(
            visible = viewModel.isFocusedMode,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.FocusOverlay(
                value = viewModel.matrixData.getOrNull(viewModel.selectedIndex) ?: "",
                onDismiss = { viewModel.dismissFocus() },
                onAction = { viewModel.handleAction(it) },
                onPrev = { viewModel.prevElement() },
                onNext = { viewModel.nextElement() }
            )
        }
    }
}

@Composable
fun ControlBar(
    activeMatrix: com.xemophon.aljabr.modules.algebra.matrices.MatrixName,
    activeMode: com.xemophon.aljabr.modules.algebra.matrices.MatrixMode,
    onClear: () -> Unit,
    onToggle: () -> Unit,
    onCompute: () -> Unit
) {
    val visible = activeMode !in _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.SingleMatrixModes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.FooterButton(
                text = "Clear",
                icon = Icons.Default.Clear,
                onClick = onClear,
                color = MaterialTheme.colorScheme.error
            )

            AnimatedVisibility(visible) {
                Button(
                    onClick = onToggle,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("To Matrix ${if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) "B" else "A"}")
                }
            }

            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.FooterButton(
                text = "Compute",
                icon = Icons.Default.Calculate,
                onClick = onCompute,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FooterButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = text, tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun FocusOverlay(
    value: String,
    onDismiss: () -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
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
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ValueTransition"
                ) { targetValue ->
                    Text(
                        text = targetValue,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
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
                        IconButton(onClick = onPrev) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }

                        Text(
                            text = "Editing Element",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = onNext) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixKeypad(
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
fun MatrixKeypad(
    onAction: (CalcButtonAction) -> Unit
) {
    ShortCalcButtons(
        modifier = Modifier.height(400.dp),
        gridMode = ShortGridMode.Convertor,
        onAction = onAction,
        letterNeeded = CalcButtonAction.Constant("i", Constants.I)
    )
}

@Composable
fun ModeSelector(
    currentMode: com.xemophon.aljabr.modules.algebra.matrices.MatrixMode,
    onModeSelected: (com.xemophon.aljabr.modules.algebra.matrices.MatrixMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMode.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(
                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DimensionControl(
    label: String,
    value: Int,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange(value - 1) },
                enabled = enabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                text = value.toString(),
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                enabled = enabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MatrixBoxField(
    rows: Int,
    cols: Int,
    data: List<String>,
    selectedIndex: Int,
    onElementClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellSize = minOf(maxWidth / cols, maxHeight / rows)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(cols) { colIndex ->
                        val index = rowIndex * cols + colIndex
                        _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixElementBox(
                            modifier = Modifier.size(cellSize),
                            value = data.getOrNull(index) ?: "",
                            isSelected = index == selectedIndex,
                            onClick = { onElementClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixElementBox(
    modifier: Modifier = Modifier,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (value.length > 5) 14.sp else 18.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FocusOverlayPreview() {
    MaterialTheme {
        _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.FocusOverlay(
            value = "1.23",
            onDismiss = {},
            onAction = {},
            onPrev = {},
            onNext = {}
        )
    }
}

@Preview
@Composable
fun BoxPreview() {
    MaterialTheme {
        _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixElementBox(
            modifier = Modifier.size(64.dp),
            value = "1",
            isSelected = true,
            onClick = {}
        )
    }
}
