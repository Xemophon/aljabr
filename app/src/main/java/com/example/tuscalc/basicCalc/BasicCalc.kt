package com.example.tuscalc.basicCalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuscalc.ui.components.CalcBox
import com.example.tuscalc.ui.components.CalcBoxViewModel
import com.example.tuscalc.ui.components.CalcButtons
import com.example.tuscalc.ui.theme.TUsCalcTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isInverse by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Basic Calculator") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalcButtons(
                    isExpanded = isExpanded,
                    isInverse = isInverse,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onToggleInverse = { isInverse = !isInverse },
                    modifier = Modifier.fillMaxWidth(),
                    onAction = { action ->
                        viewModel.handleAction(action)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BasicCalcPreview() {
    TUsCalcTheme {
        BasicCalc(onOpenDrawer = {})
    }
}
