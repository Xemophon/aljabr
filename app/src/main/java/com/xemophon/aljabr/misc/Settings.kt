package com.xemophon.aljabr.misc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.data.AppTheme
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.HorizontalSeparator

@Composable
fun SettingsPage(
    viewModel: SettingsViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    val theme by viewModel.theme.collectAsState()
    val useRadians by viewModel.useRadians.collectAsState()
    val precision by viewModel.precision.collectAsState()

    CalculatorScaffold(
        title = { Text("Settings") },
        onOpenDrawer = onOpenDrawer,
    ) { padding ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding)) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { option ->
                        val label = option.name.lowercase().replaceFirstChar { it.uppercase() }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (option == theme),
                                    onClick = { viewModel.setTheme(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == theme),
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                HorizontalSeparator(text = null)

                Text(
                    text = "Calculation",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Radians",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Affects trigonometric calculations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useRadians,
                        onCheckedChange = { viewModel.setUseRadians(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Decimal Precision",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = precision.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = precision.toFloat(),
                        onValueChange = { viewModel.setPrecision(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                    Text(
                        text = "Number of decimal places displayed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
