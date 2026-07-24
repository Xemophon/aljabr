package com.xemophon.aljabr.graphMaker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.CalcBox
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalcButtonsGraph
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun GraphMaker(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.calculationEnabled = false
    }

    var isInverse by remember { mutableStateOf(false) }
    var showGraph by remember { mutableStateOf(false) }

    BackHandler(enabled = showGraph) {
        showGraph = false
    }

    CalculatorScaffold(
        title = { Text(if (showGraph) "Graph Analysis" else "Graph Equation") },
        onOpenDrawer = onOpenDrawer,
        navigationIcon = if (showGraph) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
        navigationIconAction = if (showGraph) {
            { showGraph = false }
        } else onOpenDrawer,
        navigationIconContentDescription = if (showGraph) "Back" else "Menu"
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = if (showGraph) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (showGraph) {
                InteractiveGraphView(expression = viewModel.displayText)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalcBox(
                        expression = viewModel.displayText,
                        result = viewModel.resultText,
                        cursorIndex = viewModel.cursorIndex,
                        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalcButtonsGraph(
                        isInverse = isInverse,
                        onToggleInverse = { isInverse = !isInverse },
                        onVisualize = { showGraph = true },
                        modifier = Modifier.fillMaxWidth(),
                        onAction = { action ->
                            if (action is CalcButtonAction.Graph) {
                                showGraph = true
                            } else {
                                viewModel.handleAction(action)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveGraphView(expression: String) {
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportScale by remember { mutableFloatStateOf(1f) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val aspectRatio = configuration.screenHeightDp.toFloat() / configuration.screenWidthDp.toFloat()

    val rangeX = 20f / viewportScale
    val rangeY = rangeX * aspectRatio

    val minX = (-rangeX / 2 + viewportOffset.x).toDouble()
    val maxX = (rangeX / 2 + viewportOffset.x).toDouble()
    val minY = (-rangeY / 2 + viewportOffset.y).toDouble()
    val maxY = (rangeY / 2 + viewportOffset.y).toDouble()

    val result by produceState<Pair<List<List<Point>>, GraphAnalysis>>(
        initialValue = emptyList<List<Point>>() to GraphAnalysis(),
        expression, minX, maxX, minY, maxY
    ) {
        value = withContext(Dispatchers.Default) {
            GraphGenerator.generateAndAnalyze(expression, minX, maxX, minY, maxY, 1000)
        }
    }
    val points = result.first
    val analysis = result.second

    // Material You Colors
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline
    val graphColor = MaterialTheme.colorScheme.primary
    val asymptoteColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    // Material You Colors for elements
    val critPointMax = MaterialTheme.colorScheme.primary
    val critPointMin = MaterialTheme.colorScheme.secondary
    val inflectPoint = MaterialTheme.colorScheme.tertiary
    val dotSize = 10f
    //Draw animation
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(expression) {
        animProgress.snapTo(0f) // Reset only on expression change
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewportScale *= zoom
                        // Adjust pan based on scale to keep it intuitive
                        viewportOffset = Offset(
                            viewportOffset.x - pan.x / (size.width / rangeX),
                            viewportOffset.y + pan.y / (size.height / rangeY)
                        )
                    }
                }
                .background(surfaceColor)
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2 - (viewportOffset.x * (width / rangeX))
            val centerY = height / 2 + (viewportOffset.y * (height / rangeY))

            val scaleX = width / rangeX
            val scaleY = height / rangeY

            // Draw Grid
            val gridStep = if (viewportScale > 2f) 1f else if (viewportScale < 0.5f) 5f else 2f

            // Vertical grid lines
            var startX = ((minX / gridStep).roundToInt() * gridStep).toFloat()
            while (startX <= maxX) {
                val x = centerX + startX * scaleX
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.2f
                )
                startX += gridStep.toFloat()
            }

            // Horizontal grid lines
            val minY = (viewportOffset.y - rangeY / 2)
            val maxY = (viewportOffset.y + rangeY / 2)
            var startY = ((minY / gridStep).roundToInt() * gridStep).toFloat()
            while (startY <= maxY) {
                val y = centerY - startY * scaleY
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.2f
                )
                startY += gridStep.toFloat()
            }

            // Draw Axes
            drawLine(
                color = axisColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = axisColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 2f
            )

            // Draw Graph
            val progress = animProgress.value

            points.forEach { segment ->
                if (segment.isNotEmpty()) {
                    val path = Path()
                    var first = true
                    segment.forEach { point ->
                        val x = centerX + point.x * scaleX
                        val y = centerY - point.y * scaleY

                        if (y in -height..height * 2) {
                            if (first) {
                                path.moveTo(x, y)
                                first = false
                            } else {
                                path.lineTo(x, y)
                            }
                        } else {
                            first = true
                        }
                    }
                    if (progress < 1f) {
                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
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

            // Draw Critical Points
            analysis.localMaxima.forEach { p ->
                drawCircle(
                    color = critPointMax,
                    radius = dotSize,
                    center = Offset(centerX + p.x * scaleX, centerY - p.y * scaleY)
                )
            }
            analysis.localMinima.forEach { p ->
                drawCircle(
                    color = critPointMin,
                    radius = dotSize,
                    center = Offset(centerX + p.x * scaleX, centerY - p.y * scaleY)
                )
            }
            analysis.inflectionPoints.forEach { p ->
                drawCircle(
                    color = inflectPoint,
                    radius = dotSize,
                    center = Offset(centerX + p.x * scaleX, centerY - p.y * scaleY)
                )
            }

            // Draw Asymptotes
            analysis.verticalAsymptotes.forEach { vX ->
                val x = centerX + vX * scaleX
                drawLine(
                    color = asymptoteColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
            }
            analysis.horizontalAsymptotes.forEach { hY ->
                val y = centerY - hY * scaleY
                drawLine(
                    color = asymptoteColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
            }
        }

        // Analysis Widget
        AnalysisWidget(
            expression = expression,
            analysis = analysis,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun AnalysisWidget(
    expression: String,
    analysis: GraphAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 240.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if ("y" in expression) {
                    expression
                } else {
                    "f(x) = $expression"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            if (analysis.localMaxima.isNotEmpty()) {
                AnalysisSection("Maxima", analysis.localMaxima)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (analysis.localMinima.isNotEmpty()) {
                AnalysisSection("Minima", analysis.localMinima)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (analysis.inflectionPoints.isNotEmpty()) {
                AnalysisSection("Inflections", analysis.inflectionPoints)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (analysis.verticalAsymptotes.isNotEmpty() || analysis.horizontalAsymptotes.isNotEmpty()) {
                val vTexts = analysis.verticalAsymptotes.map { "x ≈ %.1f".format(it) }
                val hTexts = analysis.horizontalAsymptotes.map { "y ≈ %.1f".format(it) }
                val allTexts = vTexts + hTexts

                Text(
                    text = "Asymptotes: ${allTexts.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }

            if (analysis.localMaxima.isEmpty() && analysis.localMinima.isEmpty() && analysis.inflectionPoints.isEmpty() && analysis.verticalAsymptotes.isEmpty() && analysis.horizontalAsymptotes.isEmpty()) {
                Text(
                    text = "No critical points detected in view",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AnalysisSection(title: String, points: List<Point>) {
    Text(text = "$title:", style = MaterialTheme.typography.labelSmall)
    points.take(2).forEach { p ->
        Text(
            text = " • (%.2f, %.2f)".format(p.x, p.y),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
    if (points.size > 2) Text(text = " • ...", fontSize = 10.sp)
}

@Preview(showBackground = true)
@Composable
fun GraphMakerPreview() {
    AlJabrTheme {
        GraphMaker(onOpenDrawer = {})
    }
}
