package com.xemophon.aljabr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.xemophon.aljabr.data.ColorSchemeType

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    primaryContainer = CalcButtonStandardDark,
    secondaryContainer = CalcButtonOperationDark,
    tertiaryContainer = CalcButtonEqualDark,
    surfaceVariant = CalcContainerBackgroundDark,
    primaryFixedDim = CalcPrimaryFixedDimDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    primaryContainer = CalcButtonStandardLight,
    secondaryContainer = CalcButtonOperationLight,
    tertiaryContainer = CalcButtonEqualLight,
    surfaceVariant = CalcContainerBackgroundLight,
    primaryFixedDim = CalcPrimaryFixedDimLight
)

// Blue Color Schemes
private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueTertiaryLight,
    primaryContainer = CalcButtonStandardLight,
    secondaryContainer = CalcButtonOperationLight,
    tertiaryContainer = CalcButtonEqualLight,
    surfaceVariant = CalcContainerBackgroundLight
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = BlueSecondaryDark,
    tertiary = BlueTertiaryDark,
    primaryContainer = CalcButtonStandardDark,
    secondaryContainer = CalcButtonOperationDark,
    tertiaryContainer = CalcButtonEqualDark,
    surfaceVariant = CalcContainerBackgroundDark
)

// Green Color Schemes
private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    secondary = GreenSecondaryLight,
    tertiary = GreenTertiaryLight,
    primaryContainer = CalcButtonStandardLight,
    secondaryContainer = CalcButtonOperationLight,
    tertiaryContainer = CalcButtonEqualLight,
    surfaceVariant = CalcContainerBackgroundLight
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = GreenSecondaryDark,
    tertiary = GreenTertiaryDark,
    primaryContainer = CalcButtonStandardDark,
    secondaryContainer = CalcButtonOperationDark,
    tertiaryContainer = CalcButtonEqualDark,
    surfaceVariant = CalcContainerBackgroundDark
)

// Red Color Schemes
private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    secondary = RedSecondaryLight,
    tertiary = RedTertiaryLight,
    primaryContainer = CalcButtonStandardLight,
    secondaryContainer = CalcButtonOperationLight,
    tertiaryContainer = CalcButtonEqualLight,
    surfaceVariant = CalcContainerBackgroundLight
)

private val RedDarkColorScheme = darkColorScheme(
    primary = RedPrimaryDark,
    secondary = RedSecondaryDark,
    tertiary = RedTertiaryDark,
    primaryContainer = CalcButtonStandardDark,
    secondaryContainer = CalcButtonOperationDark,
    tertiaryContainer = CalcButtonEqualDark,
    surfaceVariant = CalcContainerBackgroundDark
)

@Composable
fun AlJabrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorSchemeType: ColorSchemeType = ColorSchemeType.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> when (colorSchemeType) {
            ColorSchemeType.DEFAULT -> DarkColorScheme
            ColorSchemeType.BLUE -> BlueDarkColorScheme
            ColorSchemeType.GREEN -> GreenDarkColorScheme
            ColorSchemeType.RED -> RedDarkColorScheme
        }

        else -> when (colorSchemeType) {
            ColorSchemeType.DEFAULT -> LightColorScheme
            ColorSchemeType.BLUE -> BlueLightColorScheme
            ColorSchemeType.GREEN -> GreenLightColorScheme
            ColorSchemeType.RED -> RedLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
