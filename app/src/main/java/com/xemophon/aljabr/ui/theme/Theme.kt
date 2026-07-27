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
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight, // Overridden for calc buttons
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight, // Overridden for calc buttons
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    tertiary = BlueTertiaryLight,
    onTertiary = BlueOnTertiaryLight,
    tertiaryContainer = BlueTertiaryContainerLight, // Overridden for calc buttons
    onTertiaryContainer = BlueOnTertiaryContainerLight,
    surface = BlueSurfaceLight,
    onSurface = BlueOnSurfaceLight,
    surfaceVariant = BlueSurfaceVariantLight, // Overridden for calc background
    onSurfaceVariant = BlueOnSurfaceVariantLight,
    outline = BlueOutlineLight,
    inversePrimary = BlueInversePrimaryLight,
    inverseSurface = BlueInverseSurfaceLight,
    inverseOnSurface = BlueInverseOnSurfaceLight
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark, // Overridden for calc buttons
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark, // Overridden for calc buttons
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    tertiary = BlueTertiaryDark,
    onTertiary = BlueOnTertiaryDark,
    tertiaryContainer = BlueTertiaryContainerDark, // Overridden for calc buttons
    onTertiaryContainer = BlueOnTertiaryContainerDark,
    surface = BlueSurfaceDark,
    onSurface = BlueOnSurfaceDark,
    surfaceVariant = BlueSurfaceVariantDark, // Overridden for calc background
    onSurfaceVariant = BlueOnSurfaceVariantDark,
    outline = BlueOutlineDark,
    inversePrimary = BlueInversePrimaryDark,
    inverseSurface = BlueInverseSurfaceDark,
    inverseOnSurface = BlueInverseOnSurfaceDark
)

// Green Color Schemes
private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = GreenOnPrimaryContainerLight,
    secondary = GreenSecondaryLight,
    onSecondary = GreenOnSecondaryLight,
    secondaryContainer = GreenSecondaryContainerLight,
    onSecondaryContainer = GreenOnSecondaryContainerLight,
    tertiary = GreenTertiaryLight,
    onTertiary = GreenOnTertiaryLight,
    tertiaryContainer = GreenTertiaryContainerLight,
    onTertiaryContainer = GreenOnTertiaryContainerLight,
    surface = GreenSurfaceLight,
    onSurface = GreenOnSurfaceLight,
    surfaceVariant = GreenSurfaceVariantLight,
    onSurfaceVariant = GreenOnSurfaceVariantLight,
    outline = GreenOutlineLight,
    inversePrimary = GreenInversePrimaryLight,
    inverseSurface = GreenInverseSurfaceLight,
    inverseOnSurface = GreenInverseOnSurfaceLight
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = GreenOnSecondaryDark,
    secondaryContainer = GreenSecondaryContainerDark,
    onSecondaryContainer = GreenOnSecondaryContainerDark,
    tertiary = GreenTertiaryDark,
    onTertiary = GreenOnTertiaryDark,
    tertiaryContainer = GreenTertiaryContainerDark,
    onTertiaryContainer = GreenOnTertiaryContainerDark,
    surface = GreenSurfaceDark,
    onSurface = GreenOnSurfaceDark,
    surfaceVariant = GreenSurfaceVariantDark,
    onSurfaceVariant = GreenOnSurfaceVariantDark,
    outline = GreenOutlineDark,
    inversePrimary = GreenInversePrimaryDark,
    inverseSurface = GreenInverseSurfaceDark,
    inverseOnSurface = GreenInverseOnSurfaceDark
)

// Red Color Schemes
private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    onPrimary = RedOnPrimaryLight,
    primaryContainer = RedPrimaryContainerLight,
    onPrimaryContainer = RedOnPrimaryContainerLight,
    secondary = RedSecondaryLight,
    onSecondary = RedOnSecondaryLight,
    secondaryContainer = RedSecondaryContainerLight,
    onSecondaryContainer = RedOnSecondaryContainerLight,
    tertiary = RedTertiaryLight,
    onTertiary = RedOnTertiaryLight,
    tertiaryContainer = RedTertiaryContainerLight,
    onTertiaryContainer = RedOnTertiaryContainerLight,
    surface = RedSurfaceLight,
    onSurface = RedOnSurfaceLight,
    surfaceVariant = RedSurfaceVariantLight,
    onSurfaceVariant = RedOnSurfaceVariantLight,
    outline = RedOutlineLight,
    inversePrimary = RedInversePrimaryLight,
    inverseSurface = RedInverseSurfaceLight,
    inverseOnSurface = RedInverseOnSurfaceLight
)

private val RedDarkColorScheme = darkColorScheme(
    primary = RedPrimaryDark,
    onPrimary = RedOnPrimaryDark,
    primaryContainer = RedPrimaryContainerDark,
    onPrimaryContainer = RedOnPrimaryContainerDark,
    secondary = RedSecondaryDark,
    onSecondary = RedOnSecondaryDark,
    secondaryContainer = RedSecondaryContainerDark,
    onSecondaryContainer = RedOnSecondaryContainerDark,
    tertiary = RedTertiaryDark,
    onTertiary = RedOnTertiaryDark,
    tertiaryContainer = RedTertiaryContainerDark,
    onTertiaryContainer = RedOnTertiaryContainerDark,
    surface = RedSurfaceDark,
    onSurface = RedOnSurfaceDark,
    surfaceVariant = RedSurfaceVariantDark,
    onSurfaceVariant = RedOnSurfaceVariantDark,
    outline = RedOutlineDark,
    inversePrimary = RedInversePrimaryDark,
    inverseSurface = RedInverseSurfaceDark,
    inverseOnSurface = RedInverseOnSurfaceDark
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
