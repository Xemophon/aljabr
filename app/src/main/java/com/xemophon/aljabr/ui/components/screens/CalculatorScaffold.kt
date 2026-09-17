package com.xemophon.aljabr.ui.components.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScaffold(
    title: @Composable () -> Unit,
    onOpenDrawer: () -> Unit,
    navigationIcon: ImageVector = Icons.Default.Menu,
    navigationIconAction: () -> Unit = onOpenDrawer,
    navigationIconContentDescription: String = "Open Drawer",
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = navigationIconAction) {
                        Icon(navigationIcon, contentDescription = navigationIconContentDescription)
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
        content(padding)
    }
}

/**
 * High-level layout template for calculator screens.
 * Manages scaffold, back button handling, loading state, display vs report view switching, and bottom keypad.
 */
@Suppress("unused")
@Composable
fun CalculatorScreenLayout(
    titleText: String,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    isCalculating: Boolean = false,
    loadingMessage: String = "Calculating...",
    isReportView: Boolean = false,
    onClearReport: (() -> Unit)? = null,
    reportContent: (@Composable () -> Unit)? = null,
    keypadContent: (@Composable () -> Unit)? = null,
    displayContent: @Composable () -> Unit
) {
    if (isReportView && onClearReport != null) {
        BackHandler {
            onClearReport()
        }
    }

    CalculatorScaffold(
        title = { Text(titleText) },
        onOpenDrawer = onOpenDrawer
    ) { padding ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isCalculating -> {
                            LoadingIndicator(
                                modifier = Modifier.fillMaxSize(),
                                message = loadingMessage
                            )
                        }
                        isReportView && reportContent != null -> {
                            reportContent()
                        }
                        else -> {
                            displayContent()
                        }
                    }
                }

                if (!isCalculating && !isReportView && keypadContent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    keypadContent()
                }
            }
        }
    }
}
