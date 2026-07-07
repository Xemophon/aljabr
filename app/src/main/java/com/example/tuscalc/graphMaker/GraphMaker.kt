package com.example.tuscalc.graphMaker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuscalc.ui.components.CalcBox
import com.example.tuscalc.ui.components.CalcBoxViewModel
import com.example.tuscalc.ui.components.CalcButtonsGraph
import com.example.tuscalc.ui.theme.TUsCalcTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showGraph) "Graph Analysis" else "Graph Equation") },
                navigationIcon = {
                    if (showGraph) {
                        IconButton(onClick = { showGraph = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
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
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalcButtonsGraph(
                        isInverse = isInverse,
                        onToggleInverse = { isInverse = !isInverse },
                        onVisualize = { showGraph = true },
                        modifier = Modifier.fillMaxWidth(),
                        onAction = { action ->
                            viewModel.handleAction(action)
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

    val rangeX = 20f / viewportScale
    val rangeY = 20f / viewportScale
    
    val minX = (-rangeX / 2 + viewportOffset.x).toDouble()
    val maxX = (rangeX / 2 + viewportOffset.x).toDouble()

    val result = remember(expression, minX, maxX) {
        GraphGenerator.generateAndAnalyze(expression, minX, maxX, 1000)
    }
    val points = result.first
    val analysis = result.second

    // Material You Colors
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val axisColor = MaterialTheme.colorScheme.outline
    val graphColor = MaterialTheme.colorScheme.primary
    val asymptoteColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

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
                    strokeWidth = 1f
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
                    strokeWidth = 1f
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
                    drawPath(
                        path = path,
                        color = graphColor,
                        style = Stroke(width = 4f)
                    )
                }
            }
            
            // Draw Critical Points
            analysis.localMaxima.forEach { p ->
                drawCircle(
                    color = Color.Red,
                    radius = 6f,
                    center = Offset(centerX + p.x * scaleX, centerY - p.y * scaleY)
                )
            }
            analysis.localMinima.forEach { p ->
                drawCircle(
                    color = Color.Blue,
                    radius = 6f,
                    center = Offset(centerX + p.x * scaleX, centerY - p.y * scaleY)
                )
            }
            analysis.inflectionPoints.forEach { p ->
                drawCircle(
                    color = Color(0xFF4CAF50), // Material Green
                    radius = 6f,
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
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
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
                text = "f(x) = $expression",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            
            if (analysis.localMaxima.isNotEmpty()) {
                AnalysisSection("Maxima", analysis.localMaxima)
            }
            if (analysis.localMinima.isNotEmpty()) {
                AnalysisSection("Minima", analysis.localMinima)
            }
            if (analysis.inflectionPoints.isNotEmpty()) {
                AnalysisSection("Inflections", analysis.inflectionPoints)
            }
            if (analysis.verticalAsymptotes.isNotEmpty()) {
                Text(
                    text = "Asymptotes: x ≈ ${analysis.verticalAsymptotes.joinToString { "%.1f".format(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }
            
            if (analysis.localMaxima.isEmpty() && analysis.localMinima.isEmpty() && analysis.inflectionPoints.isEmpty() && analysis.verticalAsymptotes.isEmpty()) {
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
    TUsCalcTheme {
        GraphMaker(onOpenDrawer = {})
    }
}
