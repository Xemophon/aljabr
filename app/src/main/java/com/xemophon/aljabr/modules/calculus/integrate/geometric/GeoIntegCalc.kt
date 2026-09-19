package com.xemophon.aljabr.modules.calculus.integrate.geometric

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.buttons.ButtonGrid
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.CalcContext
import com.xemophon.aljabr.ui.components.buttons.MultipleVariableGrid
import com.xemophon.aljabr.ui.components.buttons.SegmentedToggleButtons
import com.xemophon.aljabr.ui.components.buttons.Variables
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.FocusedInputOverlay
import com.xemophon.aljabr.ui.components.screens.GeoIntegReport
import com.xemophon.aljabr.ui.components.screens.LoadingIndicator
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import com.xemophon.aljabr.ui.theme.Dimens

@Composable
fun GeoIntegCalc(
    onOpenDrawer: () -> Unit,
    viewModel: GeoIntegViewModel = viewModel()
) {
    BackHandler(enabled = viewModel.geoIntegResult != null || viewModel.isCalculating) {
        viewModel.clearResult()
    }

    CalculatorScaffold(
        title = { Text("Geometric Integration") },
        onOpenDrawer = onOpenDrawer
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (viewModel.isCalculating) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    message = "Calculating Geometric Integration..."
                )
            } else if (viewModel.geoIntegResult != null) {
                GeoIntegReport(
                    result = viewModel.geoIntegResult!!,
                    onClear = { viewModel.clearResult() }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Mode Selector Slider / Row
                        SegmentedToggleButtons(
                            options = GeoIntegMode.entries.map { it to it.title },
                            selectedOption = viewModel.mode,
                            onOptionSelected = { viewModel.selectMode(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        when (viewModel.mode) {
                            GeoIntegMode.SCALAR_LINE -> {
                                ScalarLineIntegralView(viewModel = viewModel)
                            }
                            GeoIntegMode.ARC_LINE -> {
                                ArcLineIntegralView(viewModel = viewModel)
                            }
                            GeoIntegMode.VECTOR_LINE -> {
                                VectorLineIntegralView(viewModel = viewModel)
                            }
                            GeoIntegMode.SURFACE -> {
                                SurfaceIntegralView(viewModel = viewModel)
                            }
                            GeoIntegMode.VOLUME -> {
                                VolumeIntegralView(viewModel = viewModel)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Compute & Clear Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.computeResult() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solve")
                            }

                            Button(
                                onClick = { viewModel.clearAllFields() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }
            }

            // Focused Input Overlay containing the keypad grid
            if (viewModel.currentFocus != GeoIntegFocus.NONE && viewModel.geoIntegResult == null && !viewModel.isCalculating) {
                FocusedInputOverlay(
                    value = viewModel.getActiveText(),
                    title = "Editing ${viewModel.getFocusedTitle()}",
                    placeholder = viewModel.getFocusedPlaceholder(),
                    onDismiss = { viewModel.dismissFocus() },
                    onPrev = { viewModel.prevFocus() },
                    onNext = { viewModel.nextFocus() },
                    keypadContent = {
                        ButtonGrid(
                            gridData = MultipleVariableGrid,
                            isExpanded = true,
                            onAction = { viewModel.handleAction(it) },
                            buttonModifier = Modifier.aspectRatio(Dimens.ButtonAspectRatioExpanded).graphicsLayer(
                                scaleX = 0.95f,
                                scaleY = 0.9f
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            overrides = when (viewModel.mode) {
                                GeoIntegMode.SCALAR_LINE, GeoIntegMode.ARC_LINE, GeoIntegMode.VECTOR_LINE -> {
                                    mapOf(
                                        (0 to 0) to CalcButtonAction.Variable("x", Variables.X),
                                        (0 to 1) to CalcButtonAction.Variable("y", Variables.Y),
                                        (0 to 2) to CalcButtonAction.Variable("t", Variables.T),
                                        (0 to 3) to CalcButtonAction.Symbol("( )")
                                    )
                                }
                                GeoIntegMode.SURFACE -> {
                                    mapOf(
                                        (0 to 0) to CalcButtonAction.Variable("x", Variables.X),
                                        (0 to 1) to CalcButtonAction.Variable("y", Variables.Y),
                                        (0 to 2) to CalcButtonAction.Symbol("("),
                                        (0 to 3) to CalcButtonAction.Symbol(")")
                                    )
                                }
                                GeoIntegMode.VOLUME -> {
                                    mapOf(
                                        (0 to 0) to CalcButtonAction.Variable("x", Variables.X),
                                        (0 to 1) to CalcButtonAction.Variable("y", Variables.Y),
                                        (0 to 2) to CalcButtonAction.Variable("z", Variables.Z),
                                        (0 to 3) to CalcButtonAction.Symbol("( )")
                                    )
                                }
                            },
                            calcContext = CalcContext.MULTI_VARIABLE_INTEGRATION
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ScalarLineIntegralView(
    viewModel: GeoIntegViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // f(x, y) box
        GeoIntegInputBox(
            label = "f(x, y)",
            value = viewModel.fXYText,
            placeholder = "f(x, y)",
            isFocused = viewModel.currentFocus == GeoIntegFocus.F_XY,
            cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.F_XY) viewModel.cursorIndex else -1,
            onClick = { viewModel.setFocus(GeoIntegFocus.F_XY) },
            prefixText = "∫꜀ "
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // , x(t),
            GeoIntegInputBox(
                label = "x(t)",
                value = viewModel.paramXText,
                placeholder = "x(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_X) },
                modifier = Modifier.weight(1f)
            )

            // upper t bound.
            GeoIntegInputBox(
                label = "Upper t bound",
                value = viewModel.upperTText,
                placeholder = "upper t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_T) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // y(t),
            GeoIntegInputBox(
                label = "y(t)",
                value = viewModel.paramYText,
                placeholder = "y(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_Y) },
                modifier = Modifier.weight(1f)
            )

            // lower t bound,
            GeoIntegInputBox(
                label = "Lower t bound",
                value = viewModel.lowerTText,
                placeholder = "lower t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_T) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ArcLineIntegralView(
    viewModel: GeoIntegViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // , x(t),
            GeoIntegInputBox(
                label = "x(t)",
                value = viewModel.paramXText,
                placeholder = "x(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_X) },
                modifier = Modifier.weight(1f)
            )

            // upper t bound.
            GeoIntegInputBox(
                label = "Upper t bound",
                value = viewModel.upperTText,
                placeholder = "upper t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_T) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // y(t),
            GeoIntegInputBox(
                label = "y(t)",
                value = viewModel.paramYText,
                placeholder = "y(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_Y) },
                modifier = Modifier.weight(1f)
            )

            // lower t bound,
            GeoIntegInputBox(
                label = "Lower t bound",
                value = viewModel.lowerTText,
                placeholder = "lower t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_T) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VectorLineIntegralView(
    viewModel: GeoIntegViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // P(x, y),
            GeoIntegInputBox(
                label = "P(x, y)",
                value = viewModel.pXYText,
                placeholder = "P(x, y)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.P_XY,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.P_XY) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.P_XY) },
                prefixText = "∮꜀ [ ",
                modifier = Modifier.weight(1f)
            )

            // , Q(x, y),
            GeoIntegInputBox(
                label = "Q(x, y)",
                value = viewModel.qXYText,
                placeholder = "Q(x, y)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.Q_XY,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.Q_XY) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.Q_XY) },
                suffixText = " ]",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // [ x(t), ]
            GeoIntegInputBox(
                label = "x(t)",
                value = viewModel.paramXText,
                placeholder = "x(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_X) },
                modifier = Modifier.weight(1f)
            )

            // upper t bound.
            GeoIntegInputBox(
                label = "Upper t bound",
                value = viewModel.upperTText,
                placeholder = "upper t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_T) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // [ y(t), ]
            GeoIntegInputBox(
                label = "y(t)",
                value = viewModel.paramYText,
                placeholder = "y(t)",
                isFocused = viewModel.currentFocus == GeoIntegFocus.PARAM_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.PARAM_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.PARAM_Y) },
                modifier = Modifier.weight(1f)
            )

            // lower t bound,
            GeoIntegInputBox(
                label = "Lower t bound",
                value = viewModel.lowerTText,
                placeholder = "lower t bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_T,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_T) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_T) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SurfaceIntegralView(
    viewModel: GeoIntegViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // f(x, y),
        GeoIntegInputBox(
            label = "f(x, y)",
            value = viewModel.fXYText,
            placeholder = "f(x, y)",
            isFocused = viewModel.currentFocus == GeoIntegFocus.F_XY,
            cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.F_XY) viewModel.cursorIndex else -1,
            onClick = { viewModel.setFocus(GeoIntegFocus.F_XY) },
            prefixText = "∫∫ₛ "
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // upper x bound,
            GeoIntegInputBox(
                label = "Upper x bound",
                value = viewModel.upperXText,
                placeholder = "upper x bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_X) },
                modifier = Modifier.weight(1f)
            )

            // upper y bound.
            GeoIntegInputBox(
                label = "Upper y bound",
                value = viewModel.upperYText,
                placeholder = "upper y bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_Y) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // lower x bound,
            GeoIntegInputBox(
                label = "Lower x bound",
                value = viewModel.lowerXText,
                placeholder = "lower x bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_X) },
                modifier = Modifier.weight(1f)
            )

            // lower y bound,
            GeoIntegInputBox(
                label = "Lower y bound",
                value = viewModel.lowerYText,
                placeholder = "lower y bound",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_Y) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VolumeIntegralView(
    viewModel: GeoIntegViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // f(x, y, z)
        GeoIntegInputBox(
            label = "f(x, y, z)",
            value = viewModel.fXYZText,
            placeholder = "f(x, y, z)",
            isFocused = viewModel.currentFocus == GeoIntegFocus.F_XYZ,
            cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.F_XYZ) viewModel.cursorIndex else -1,
            onClick = { viewModel.setFocus(GeoIntegFocus.F_XYZ) },
            prefixText = "∫∫∫ᵥ "
        )

        Text(
            text = "Bounds for x y z",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // Upper Bounds Row (x, y, z)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GeoIntegInputBox(
                label = "Upper x",
                value = viewModel.upperXText,
                placeholder = "upper x",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_X) },
                modifier = Modifier.weight(1f)
            )

            GeoIntegInputBox(
                label = "Upper y",
                value = viewModel.upperYText,
                placeholder = "upper y",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_Y) },
                modifier = Modifier.weight(1f)
            )

            GeoIntegInputBox(
                label = "Upper z",
                value = viewModel.upperZText,
                placeholder = "upper z",
                isFocused = viewModel.currentFocus == GeoIntegFocus.UPPER_Z,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.UPPER_Z) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.UPPER_Z) },
                modifier = Modifier.weight(1f)
            )
        }

        // Lower Bounds Row (x, y, z)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GeoIntegInputBox(
                label = "Lower x",
                value = viewModel.lowerXText,
                placeholder = "lower x",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_X,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_X) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_X) },
                modifier = Modifier.weight(1f)
            )

            GeoIntegInputBox(
                label = "Lower y",
                value = viewModel.lowerYText,
                placeholder = "lower y",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_Y,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_Y) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_Y) },
                modifier = Modifier.weight(1f)
            )

            GeoIntegInputBox(
                label = "Lower z",
                value = viewModel.lowerZText,
                placeholder = "lower z",
                isFocused = viewModel.currentFocus == GeoIntegFocus.LOWER_Z,
                cursorIndex = if (viewModel.currentFocus == GeoIntegFocus.LOWER_Z) viewModel.cursorIndex else -1,
                onClick = { viewModel.setFocus(GeoIntegFocus.LOWER_Z) },
                modifier = Modifier.weight(1f)
            )
        }

        // dxdydz order switch button
        Button(
            onClick = { viewModel.cycleOrder() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Switch Order"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Order: ${viewModel.currentOrder}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GeoIntegInputBox(
    label: String,
    value: String,
    placeholder: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    prefixText: String? = null,
    suffixText: String? = null
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

    val scrollState = rememberScrollState()

    LaunchedEffect(value, isFocused) {
        if (isFocused) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(Dimens.ButtonCornerRadiusStandard))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(Dimens.ButtonCornerRadiusStandard)
                )
                .clickable { onClick() },
            color = if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
            shape = RoundedCornerShape(Dimens.ButtonCornerRadiusStandard)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (prefixText != null) {
                    Text(
                        text = prefixText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                val annotatedText = buildAnnotatedString {
                    if (value.isEmpty() && !isFocused) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) {
                            append(placeholder)
                        }
                    } else if (isFocused) {
                        val safeCursor = if (cursorIndex == -1) value.length else cursorIndex.coerceIn(0, value.length)
                        append(value.substring(0, safeCursor))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                            append("|")
                        }
                        append(value.substring(safeCursor))
                    } else {
                        append(value)
                    }
                }

                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(scrollState),
                    textAlign = TextAlign.Start
                )

                if (suffixText != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = suffixText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeoIntegPreview() {
    AlJabrTheme {
        GeoIntegCalc(onOpenDrawer = {})
    }
}
