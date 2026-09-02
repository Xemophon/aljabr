package com.xemophon.aljabr.modules.misc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.data.AppTheme
import com.xemophon.aljabr.data.ColorSchemeType
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.buttons.HorizontalSeparator
import com.xemophon.aljabr.ui.components.buttons.ThemeButton
import kotlinx.coroutines.launch
import com.xemophon.aljabr.ui.theme.BluePrimaryDark
import com.xemophon.aljabr.ui.theme.BluePrimaryLight
import com.xemophon.aljabr.ui.theme.BlueSecondaryDark
import com.xemophon.aljabr.ui.theme.BlueSecondaryLight
import com.xemophon.aljabr.ui.theme.BlueTertiaryDark
import com.xemophon.aljabr.ui.theme.BlueTertiaryLight
import com.xemophon.aljabr.ui.theme.BrownPrimaryDark
import com.xemophon.aljabr.ui.theme.BrownPrimaryLight
import com.xemophon.aljabr.ui.theme.BrownSecondaryDark
import com.xemophon.aljabr.ui.theme.BrownSecondaryLight
import com.xemophon.aljabr.ui.theme.BrownTertiaryDark
import com.xemophon.aljabr.ui.theme.BrownTertiaryLight
import com.xemophon.aljabr.ui.theme.Dimens
import com.xemophon.aljabr.ui.theme.GreenPrimaryDark
import com.xemophon.aljabr.ui.theme.GreenPrimaryLight
import com.xemophon.aljabr.ui.theme.GreenSecondaryDark
import com.xemophon.aljabr.ui.theme.GreenSecondaryLight
import com.xemophon.aljabr.ui.theme.GreenTertiaryDark
import com.xemophon.aljabr.ui.theme.GreenTertiaryLight
import com.xemophon.aljabr.ui.theme.OrangePrimaryDark
import com.xemophon.aljabr.ui.theme.OrangePrimaryLight
import com.xemophon.aljabr.ui.theme.OrangeSecondaryDark
import com.xemophon.aljabr.ui.theme.OrangeSecondaryLight
import com.xemophon.aljabr.ui.theme.OrangeTertiaryDark
import com.xemophon.aljabr.ui.theme.OrangeTertiaryLight
import com.xemophon.aljabr.ui.theme.Pink40
import com.xemophon.aljabr.ui.theme.Pink80
import com.xemophon.aljabr.ui.theme.PinkPrimaryDark
import com.xemophon.aljabr.ui.theme.PinkPrimaryLight
import com.xemophon.aljabr.ui.theme.PinkSecondaryDark
import com.xemophon.aljabr.ui.theme.PinkSecondaryLight
import com.xemophon.aljabr.ui.theme.PinkTertiaryDark
import com.xemophon.aljabr.ui.theme.PinkTertiaryLight
import com.xemophon.aljabr.ui.theme.Purple40
import com.xemophon.aljabr.ui.theme.Purple80
import com.xemophon.aljabr.ui.theme.PurpleGrey40
import com.xemophon.aljabr.ui.theme.PurpleGrey80
import com.xemophon.aljabr.ui.theme.RedPrimaryDark
import com.xemophon.aljabr.ui.theme.RedPrimaryLight
import com.xemophon.aljabr.ui.theme.RedSecondaryDark
import com.xemophon.aljabr.ui.theme.RedSecondaryLight
import com.xemophon.aljabr.ui.theme.RedTertiaryDark
import com.xemophon.aljabr.ui.theme.RedTertiaryLight
import com.xemophon.aljabr.ui.theme.TealPrimaryDark
import com.xemophon.aljabr.ui.theme.TealPrimaryLight
import com.xemophon.aljabr.ui.theme.TealSecondaryDark
import com.xemophon.aljabr.ui.theme.TealSecondaryLight
import com.xemophon.aljabr.ui.theme.TealTertiaryDark
import com.xemophon.aljabr.ui.theme.TealTertiaryLight
import com.xemophon.aljabr.ui.theme.YellowPrimaryDark
import com.xemophon.aljabr.ui.theme.YellowPrimaryLight
import com.xemophon.aljabr.ui.theme.YellowSecondaryDark
import com.xemophon.aljabr.ui.theme.YellowSecondaryLight
import com.xemophon.aljabr.ui.theme.YellowTertiaryDark
import com.xemophon.aljabr.ui.theme.YellowTertiaryLight

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
){
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val colorSchemeType by viewModel.colorScheme.collectAsState()
    val useRadians by viewModel.useRadians.collectAsState()
    val useRationalize by viewModel.useRationalize.collectAsState()
    val precision by viewModel.precision.collectAsState()
    val showSteps by viewModel.showSteps.collectAsState()
    val autoClearCache by viewModel.autoClearCache.collectAsState()

    val scope = rememberCoroutineScope()
    val isDarkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AUTO -> isSystemInDarkTheme()
    }

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalSeparator(text = "Appearance")

            // Theme (Auto, Light, Dark)
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

            // Dynamic Color (Always supported since minSdk 33)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Color",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Use system colors (Material You)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }

            // Static Color Schemes (Visible if dynamic is off)
            AnimatedVisibility(!dynamicColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Color Theme",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    ColorSchemeType.entries.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { option ->
                                val label =
                                    option.name.lowercase().replaceFirstChar { it.uppercase() }
                                val (primary, secondary, tertiary) = when (option) {
                                    ColorSchemeType.DEFAULT -> if (isDarkTheme) {
                                        Triple(Purple40, PurpleGrey40, Pink40)
                                    } else {
                                        Triple(Purple80, PurpleGrey80, Pink80)
                                    }

                                    ColorSchemeType.BLUE -> if (isDarkTheme) {
                                        Triple(
                                            BluePrimaryLight,
                                            BlueSecondaryLight,
                                            BlueTertiaryLight
                                        )
                                    } else {
                                        Triple(BluePrimaryDark, BlueSecondaryDark, BlueTertiaryDark)
                                    }

                                    ColorSchemeType.GREEN -> if (isDarkTheme) {
                                        Triple(
                                            GreenPrimaryLight,
                                            GreenSecondaryLight,
                                            GreenTertiaryLight
                                        )
                                    } else {
                                        Triple(
                                            GreenPrimaryDark,
                                            GreenSecondaryDark,
                                            GreenTertiaryDark
                                        )
                                    }

                                    ColorSchemeType.RED -> if (isDarkTheme) {
                                        Triple(RedPrimaryLight, RedSecondaryLight, RedTertiaryLight)
                                    } else {
                                        Triple(RedPrimaryDark, RedSecondaryDark, RedTertiaryDark)
                                    }

                                    ColorSchemeType.YELLOW -> if (isDarkTheme) {
                                        Triple(
                                            YellowPrimaryLight,
                                            YellowSecondaryLight,
                                            YellowTertiaryLight
                                        )
                                    } else {
                                        Triple(
                                            YellowPrimaryDark,
                                            YellowSecondaryDark,
                                            YellowTertiaryDark
                                        )
                                    }

                                    ColorSchemeType.ORANGE -> if (isDarkTheme) {
                                        Triple(
                                            OrangePrimaryLight,
                                            OrangeSecondaryLight,
                                            OrangeTertiaryLight
                                        )
                                    } else {
                                        Triple(
                                            OrangePrimaryDark,
                                            OrangeSecondaryDark,
                                            OrangeTertiaryDark
                                        )
                                    }

                                    ColorSchemeType.TEAL -> if (isDarkTheme) {
                                        Triple(
                                            TealPrimaryLight,
                                            TealSecondaryLight,
                                            TealTertiaryLight
                                        )
                                    } else {
                                        Triple(TealPrimaryDark, TealSecondaryDark, TealTertiaryDark)
                                    }

                                    ColorSchemeType.PINK -> if (isDarkTheme) {
                                        Triple(
                                            PinkPrimaryLight,
                                            PinkSecondaryLight,
                                            PinkTertiaryLight
                                        )
                                    } else {
                                        Triple(PinkPrimaryDark, PinkSecondaryDark, PinkTertiaryDark)
                                    }

                                    ColorSchemeType.BROWN -> if (isDarkTheme) {
                                        Triple(
                                            BrownPrimaryLight,
                                            BrownSecondaryLight,
                                            BrownTertiaryLight
                                        )
                                    } else {
                                        Triple(
                                            BrownPrimaryDark,
                                            BrownSecondaryDark,
                                            BrownTertiaryDark
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .selectable(
                                            selected = (option == colorSchemeType),
                                            onClick = { viewModel.setColorScheme(option) },
                                            role = Role.RadioButton
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ThemeButton(
                                        primaryColor = primary,
                                        secondaryColor = secondary,
                                        tertiaryColor = tertiary,
                                        selected = (option == colorSchemeType)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // Fill empty space if row has less than 3 items
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            HorizontalSeparator(text = "Calculation")

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step-by-Step Explanations",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Shows detailed logic for calculus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showSteps,
                    onCheckedChange = { viewModel.setShowSteps(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use Rational Numbers",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Favors fractions over decimals (e.g., 0.5 becomes 1/2)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useRationalize,
                    onCheckedChange = { viewModel.setUseRationalize(it) }
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Decimal Precision",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (useRationalize) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = precision.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (useRationalize) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = precision.toFloat(),
                    onValueChange = { viewModel.setPrecision(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                    enabled = !useRationalize
                )
                Text(
                    text = if (useRationalize) "Disabled when using rational numbers" else "Number of decimal places displayed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (useRationalize) 0.38f else 1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalSeparator(text = "Privacy & Storage")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Clear Cache",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Clear temp data on discarding a math problem",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoClearCache,
                    onCheckedChange = { viewModel.setAutoClearCache(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ElevatedButton(
                    onClick = {
                        scope.launch {
                            StorageUtils.clearAppData(viewModel.getApplication())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All App Data & Cache")
                }
                Text(
                    text = "Resets all temporary data. Settings are preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutContent(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.PaddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Branding Header
            Text(
                text = "AlJabr",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Description
            Text(
                text = "A modern calculator built with Jetpack Compose and Kotlin, designed to handle mathematical expressions with ease. Developed to provide a clean, local alternative for university-level calculations.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Libraries Section
            Text(
                text = "Built With",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )

            HorizontalSeparator(text = null)

            val libraries = listOf(
                "Jetpack Compose",
                "Hilt Dependency Injection",
                "MathEclipse",
                "Kotlin Multiplatform LaTeX Rendering Library",
                "Kotlin Coroutines",
                "Material 3"
            )

            libraries.forEach { library ->
                Text(
                    text = library,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class MiscTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    SETTINGS("Settings", Icons.Default.Settings, "App Settings"),
    ABOUT("About", Icons.Default.Info, "About AlJabr")
}

@Composable
fun MiscPage(
    viewModel: SettingsViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    CalculatorScaffold(
        title = { Text("App Info") },
        onOpenDrawer = onOpenDrawer
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                MiscTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.contentDescription
                            )
                        }
                    )
                }
            }

            when (MiscTab.entries[selectedTabIndex]) {
                MiscTab.SETTINGS -> SettingsContent(viewModel = viewModel)
                MiscTab.ABOUT -> AboutContent()
            }
        }
    }
}
