package com.xemophon.aljabr.modules.statistics.distributions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.modules.graphMaker.Point
import com.xemophon.aljabr.ui.components.buttons.HorizontalSeparator
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.FocusedInputOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun DistCalc(
    viewModel: DistributionsViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    CalculatorScaffold(
        title = { Text(text = "Distributions Calculator") },
        onOpenDrawer = onOpenDrawer,
        navigationIcon = Icons.Default.Menu,
        navigationIconAction = onOpenDrawer,
        navigationIconContentDescription = "Menu"
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DistributionsScreen(viewModel = viewModel)

                AnimatedVisibility(
                    visible = viewModel.isFocusedMode,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    DistFocusOverlay(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DistFocusOverlay(
    viewModel: DistributionsViewModel
) {
    val placeholder = when (viewModel.currentFocus) {
        DistributionsFocus.PARAM1 -> viewModel.activeParam1Label
        DistributionsFocus.PARAM2 -> viewModel.activeParam2Label ?: "Parameter 2"
        DistributionsFocus.PARAM3 -> viewModel.activeParam3Label ?: "Parameter 3"
        DistributionsFocus.X_VAL -> viewModel.xLabel
        DistributionsFocus.X2_VAL -> viewModel.x2Label
    }

    FocusedInputOverlay(
        value = viewModel.focusValue,
        title = "Editing ${viewModel.currentFocus.name}",
        placeholder = placeholder,
        onDismiss = { viewModel.dismissFocus() },
        onPrev = { viewModel.prevFocus() },
        onNext = { viewModel.nextFocus() },
        keypadContent = {
            ShortCalcButtons(
                modifier = Modifier.height(400.dp),
                gridMode = ShortGridMode.Distributions,
                onAction = { viewModel.handleAction(it) },
            )
        }
    )
}

@Composable
fun DistributionsScreen(
    viewModel: DistributionsViewModel,
) {
    val isExpanded = viewModel.isGraphExpanded

    val graphWeight by animateFloatAsState(
        targetValue = if (isExpanded) 0.78f else 0.38f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GraphWeightAnimation"
    )

    val isDiscrete = viewModel.type in listOf(
        DistributionsType.BINOMIAL,
        DistributionsType.POISSON,
        DistributionsType.HYPERGEOMETRIC,
        DistributionsType.GEOMETRIC
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top: Dynamic graph area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(graphWeight)
        ) {
            DistributionsGraph(
                modifier = Modifier.fillMaxSize(),
                type = viewModel.type,
                param1Val = viewModel.activeParam1Value,
                param2Val = viewModel.activeParam2Value ?: "",
                param3Val = viewModel.activeParam3Value ?: "",
                minX = viewModel.graphMinX,
                maxX = viewModel.graphMaxX,
                calcMode = viewModel.calcMode,
                shadeMinX = viewModel.shadeMinX,
                shadeMaxX = viewModel.shadeMaxX,
                isDiscrete = isDiscrete
            )

            // Top-Right: Floating Expand/Collapse Button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.toggleGraphExpanded() },
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isExpanded) "Collapse Graph" else "Expand Graph",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isExpanded) "Collapse" else "Expand Graph",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Bottom: Card with Horizontal Pager, Inputs & Results
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - graphWeight),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        .clickable { viewModel.toggleGraphExpanded() }
                )

                // Horizontal Pager with Distributions
                DistributionsPager(viewModel = viewModel)

                // Calculation Mode Selector
                DistributionsModeSelector(viewModel = viewModel)

                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalSeparator("Input")
                        DistributionsParams(viewModel = viewModel)

                        HorizontalSeparator("Results")
                        DistributionsResults(result = viewModel.distributionResult)
                    }
                }

                if (isExpanded) {
                    DistributionsQuickResultSummary(result = viewModel.distributionResult)
                }
            }
        }
    }
}

@Composable
fun DistributionsPager(
    viewModel: DistributionsViewModel,
    modifier: Modifier = Modifier
) {
    val distributions = DistributionsType.entries
    val pagerState = rememberPagerState(
        initialPage = distributions.indexOf(viewModel.type).coerceAtLeast(0),
        pageCount = { distributions.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val dist = distributions.getOrNull(page) ?: return@collect
            if (dist != viewModel.type) {
                viewModel.onTypeChange(dist)
            }
        }
    }

    LaunchedEffect(viewModel.type) {
        val page = distributions.indexOf(viewModel.type)
        if ((page >= 0) && (page != pagerState.currentPage)) {
            pagerState.animateScrollToPage(page)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 12.dp
    ) { page ->
        val dist = distributions[page]
        val isSelected = dist == viewModel.type

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.onTypeChange(dist) },
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = dist.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun DistributionsModeSelector(
    viewModel: DistributionsViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        val isDiscrete = viewModel.type in listOf(
            DistributionsType.BINOMIAL,
            DistributionsType.POISSON,
            DistributionsType.HYPERGEOMETRIC,
            DistributionsType.GEOMETRIC
        )
        val modes = listOf(
            DistCalcMode.PDF_PMF to if (isDiscrete) "PMF P(X=k)" else "PDF f(x)",
            DistCalcMode.CDF to "CDF P(X≤x)",
            DistCalcMode.UPPER_CDF to "P(X≥x)",
            DistCalcMode.RANGE to "Range"
        )

        modes.forEach { (mode, label) ->
            val isSelected = viewModel.calcMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.onCalcModeChange(mode) },
                label = { Text(text = label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
fun DistributionsParams(
    viewModel: DistributionsViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Param 1
        ParamBox(
            label = viewModel.activeParam1Label,
            value = viewModel.activeParam1Value,
            isSelected = viewModel.currentFocus == DistributionsFocus.PARAM1,
            onClick = { viewModel.onFocusChange(DistributionsFocus.PARAM1) },
            modifier = Modifier.weight(1f)
        )

        // Param 2 (if present)
        if (viewModel.hasParam2) {
            ParamBox(
                label = viewModel.activeParam2Label ?: "",
                value = viewModel.activeParam2Value ?: "",
                isSelected = viewModel.currentFocus == DistributionsFocus.PARAM2,
                onClick = { viewModel.onFocusChange(DistributionsFocus.PARAM2) },
                modifier = Modifier.weight(1f)
            )
        }

        // Param 3 (if present)
        if (viewModel.hasParam3) {
            ParamBox(
                label = viewModel.activeParam3Label ?: "",
                value = viewModel.activeParam3Value ?: "",
                isSelected = viewModel.currentFocus == DistributionsFocus.PARAM3,
                onClick = { viewModel.onFocusChange(DistributionsFocus.PARAM3) },
                modifier = Modifier.weight(1f)
            )
        }

        // Value X
        ParamBox(
            label = viewModel.xLabel,
            value = viewModel.xVal,
            isSelected = viewModel.currentFocus == DistributionsFocus.X_VAL,
            onClick = { viewModel.onFocusChange(DistributionsFocus.X_VAL) },
            modifier = Modifier.weight(1f)
        )

        // Value X2 (if range mode)
        if (viewModel.isRangeMode) {
            ParamBox(
                label = viewModel.x2Label,
                value = viewModel.x2Val,
                isSelected = viewModel.currentFocus == DistributionsFocus.X2_VAL,
                onClick = { viewModel.onFocusChange(DistributionsFocus.X2_VAL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ParamBox(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifEmpty { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DistributionsQuickResultSummary(
    result: DistributionsResult?,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = result.primaryResultLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "μ=${result.mean}, σ=${result.stdDev}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = result.primaryResultValue,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DistributionsResults(
    result: DistributionsResult?,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Highlighted Result
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.primaryResultLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = result.primaryResultValue,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Main Statistical Properties
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem(label = "Mean (μ)", value = result.mean)
                ResultItem(label = "Variance (σ²)", value = result.variance)
                ResultItem(label = "Standard Deviation (σ)", value = result.stdDev)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem(label = result.pdfOrPmfLabel, value = result.pdfOrPmfValue)
                ResultItem(label = result.cdfLabel, value = result.cdfValue)
                ResultItem(label = result.upperCdfLabel, value = result.upperCdfValue)
            }

            // Extra Info (e.g. Z-Score, Two-Tailed p-value, P(X > k))
            result.extraInfo.forEach { (key, valStr) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = valStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ResultItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DistributionsGraph(
    modifier: Modifier = Modifier,
    type: DistributionsType = DistributionsType.BINOMIAL,
    param1Val: String = "10",
    param2Val: String = "0.5",
    param3Val: String = "",
    minX: Double = -5.0,
    maxX: Double = 5.0,
    calcMode: DistCalcMode = DistCalcMode.PDF_PMF,
    shadeMinX: Double? = null,
    shadeMaxX: Double? = null,
    isDiscrete: Boolean = false,
    shadeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
) {
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportScale by remember { mutableFloatStateOf(1f) }

    val effectiveMinX = minX / viewportScale + viewportOffset.x
    val effectiveMaxX = maxX / viewportScale + viewportOffset.x

    val points by produceState<List<List<Point>>>(
        initialValue = emptyList(),
        type, param1Val, param2Val, param3Val, effectiveMinX, effectiveMaxX, calcMode
    ) {
        value = withContext(Dispatchers.Default) {
            DistributionsFunc.generateGraphPoints(
                type = type,
                param1Str = param1Val,
                param2Str = param2Val,
                param3Str = param3Val,
                minX = effectiveMinX,
                maxX = effectiveMaxX,
                calcMode = calcMode
            )
        }
    }

    val maxYVal = remember(points) {
        val maxPointY = points.flatten().maxOfOrNull { it.y } ?: 1f
        if (maxPointY <= 0f) 1f else maxPointY
    }

    val minY = if (calcMode == DistCalcMode.CDF) -0.15f else -0.12f * maxYVal
    val maxY = if (calcMode == DistCalcMode.CDF) 1.15f else 1.30f * maxYVal

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val axisColor = MaterialTheme.colorScheme.outline
    val graphColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val animProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LaunchedEffect(type, param1Val, param2Val, param3Val, calcMode) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewportScale = (viewportScale * zoom).coerceIn(0.5f, 5f)
                    val rangeX = (effectiveMaxX - effectiveMinX).toFloat()
                    viewportOffset = Offset(
                        viewportOffset.x - pan.x * (rangeX / size.width),
                        0f
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor)
        ) {
            val width = size.width
            val height = size.height

            val rangeX = (effectiveMaxX - effectiveMinX).toFloat()
            val rangeY = maxY - minY

            if (rangeX <= 0f || rangeY <= 0f) return@Canvas

            fun toPx(x: Float): Float = ((x - effectiveMinX.toFloat()) / rangeX) * width
            fun toPy(y: Float): Float = height - ((y - minY) / rangeY) * height

            val yZeroPx = toPy(0f).coerceIn(0f, height)
            val xZeroPx = if (0f in effectiveMinX.toFloat()..effectiveMaxX.toFloat()) toPx(0f) else toPx(effectiveMinX.toFloat())

            // Draw X and Y Axes
            drawLine(
                color = axisColor,
                start = Offset(0f, yZeroPx),
                end = Offset(width, yZeroPx),
                strokeWidth = 2f
            )
            drawLine(
                color = axisColor.copy(alpha = 0.5f),
                start = Offset(xZeroPx, 0f),
                end = Offset(xZeroPx, height),
                strokeWidth = 1.5f
            )

            // Draw Dynamic X Ticks & Grid Lines
            val rawStep = rangeX / 6f
            val xStep = calculateNiceStep(rawStep)

            var xTick = (floor(effectiveMinX / xStep) * xStep).toFloat()
            while (xTick <= effectiveMaxX + xStep) {
                val px = toPx(xTick)
                if (px in 0f..width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(px, 0f),
                        end = Offset(px, height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = axisColor,
                        start = Offset(px, yZeroPx - 4f),
                        end = Offset(px, yZeroPx + 4f),
                        strokeWidth = 1.5f
                    )
                    val labelStr = formatTick(xTick)
                    val textLayout = textMeasurer.measure(labelStr, style = textStyle)
                    val labelY = if (yZeroPx > height - 25f) yZeroPx - 18f else yZeroPx + 6f
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(px - textLayout.size.width / 2f, labelY)
                    )
                }
                xTick += xStep
            }

            // Draw Y Ticks & Labels
            val yStep = (rangeY / 4f)
            for (i in 0..4) {
                val yVal = minY + i * yStep
                val py = toPy(yVal)
                if (py in 0f..height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, py),
                        end = Offset(width, py),
                        strokeWidth = 1f
                    )
                    val labelStr = formatTick(yVal)
                    val textLayout = textMeasurer.measure(labelStr, style = textStyle)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(8f, py - textLayout.size.height / 2f)
                    )
                }
            }

            // Draw Shaded Probability Area under curve
            if (shadeMinX != null && shadeMaxX != null && shadeMinX < shadeMaxX && shadeMinX < effectiveMaxX && shadeMaxX > effectiveMinX) {
                val clampedShadeMin = shadeMinX.coerceIn(effectiveMinX, effectiveMaxX).toFloat()
                val clampedShadeMax = shadeMaxX.coerceIn(effectiveMinX, effectiveMaxX).toFloat()

                points.forEach { segment ->
                    val inRangePoints = segment.filter { pt ->
                        pt.x in clampedShadeMin..clampedShadeMax
                    }
                    if (inRangePoints.isNotEmpty()) {
                        val firstPt = inRangePoints.first()
                        val lastPt = inRangePoints.last()

                        val xStartPx = toPx(firstPt.x)
                        val xEndPx = toPx(lastPt.x)

                        val fillPath = Path()
                        fillPath.moveTo(xStartPx, yZeroPx)
                        inRangePoints.forEach { pt ->
                            fillPath.lineTo(toPx(pt.x), toPy(pt.y))
                        }
                        fillPath.lineTo(xEndPx, yZeroPx)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            color = shadeColor
                        )

                        // Boundary indicator vertical lines
                        val firstPy = toPy(firstPt.y)
                        val lastPy = toPy(lastPt.y)
                        if (shadeMinX >= effectiveMinX) {
                            drawLine(
                                color = graphColor,
                                start = Offset(xStartPx, yZeroPx),
                                end = Offset(xStartPx, firstPy),
                                strokeWidth = 2.5f
                            )
                        }
                        if (shadeMaxX <= effectiveMaxX) {
                            drawLine(
                                color = graphColor,
                                start = Offset(xEndPx, yZeroPx),
                                end = Offset(xEndPx, lastPy),
                                strokeWidth = 2.5f
                            )
                        }
                    }
                }
            }

            // Draw Discrete PMF Stems or Continuous Smooth Curve
            val progress = animProgress.value

            if (isDiscrete && calcMode == DistCalcMode.PDF_PMF) {
                // Discrete Stems
                points.flatten().forEach { pt ->
                    val k = pt.x.roundToInt()
                    if (abs(pt.x - k) < 0.05f) {
                        val px = toPx(pt.x)
                        val py = toPy(pt.y)
                        if (px in 0f..width) {
                            val inShadeRange = shadeMinX != null && shadeMaxX != null && pt.x >= shadeMinX.toFloat() && pt.x <= shadeMaxX.toFloat()
                            val stemColor = if (inShadeRange) shadeColor.copy(alpha = 1f) else graphColor.copy(alpha = 0.5f)

                            val animatedPy = yZeroPx - (yZeroPx - py) * progress
                            drawLine(
                                color = stemColor,
                                start = Offset(px, yZeroPx),
                                end = Offset(px, animatedPy),
                                strokeWidth = 3.5f
                            )
                            drawCircle(
                                color = stemColor,
                                radius = 5.5f,
                                center = Offset(px, animatedPy)
                            )
                        }
                    }
                }
            } else {
                // Continuous Smooth Line
                points.forEach { segment ->
                    if (segment.isNotEmpty()) {
                        val path = Path()
                        var first = true
                        segment.forEach { point ->
                            val px = toPx(point.x)
                            val py = toPy(point.y)

                            if (py in -height..height * 2) {
                                if (first) {
                                    path.moveTo(px, py)
                                    first = false
                                } else {
                                    path.lineTo(px, py)
                                }
                            } else {
                                first = true
                            }
                        }
                        if (progress < 1f) {
                            val pathMeasure = PathMeasure()
                            pathMeasure.setPath(path, forceClosed = false)
                            val animatedPath = Path()
                            pathMeasure.getSegment(
                                startDistance = 0f,
                                stopDistance = pathMeasure.length * progress,
                                destination = animatedPath
                            )
                            drawPath(
                                path = animatedPath,
                                color = graphColor,
                                style = Stroke(width = 4.5f)
                            )
                        } else {
                            drawPath(
                                path = path,
                                color = graphColor,
                                style = Stroke(width = 4.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateNiceStep(rawStep: Float): Float {
    if (rawStep <= 0f) return 1f
    val exponent = floor(log10(rawStep.toDouble()))
    val fraction = rawStep / 10.0.pow(exponent).toFloat()
    val niceFraction = when {
        fraction < 1.5f -> 1f
        fraction < 3f -> 2f
        fraction < 7f -> 5f
        else -> 10f
    }
    return (niceFraction * 10.0.pow(exponent)).toFloat()
}

private fun formatTick(value: Float): String {
    val absVal = abs(value)
    return when {
        absVal == 0f -> "0"
        absVal >= 100f -> value.roundToInt().toString()
        absVal >= 10f -> String.format(Locale.US, "%.1f", value)
        absVal >= 0.1f -> String.format(Locale.US, "%.2f", value)
        else -> String.format(Locale.US, "%.3f", value)
    }
}
