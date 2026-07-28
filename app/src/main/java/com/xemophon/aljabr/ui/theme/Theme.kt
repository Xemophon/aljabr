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

// Yellow Color Scheme
//Light
private val YellowLightColorScheme = lightColorScheme(
    primary = YellowPrimaryLight,
    onPrimary = YellowOnPrimaryLight,
    primaryContainer = YellowPrimaryContainerLight,
    onPrimaryContainer = YellowOnPrimaryContainerLight,
    secondary = YellowSecondaryLight,
    onSecondary = YellowOnSecondaryLight,
    secondaryContainer = YellowSecondaryContainerLight,
    onSecondaryContainer = YellowOnSecondaryContainerLight,
    tertiary = YellowTertiaryLight,
    onTertiary = YellowOnTertiaryLight,
    tertiaryContainer = YellowTertiaryContainerLight,
    onTertiaryContainer = YellowOnTertiaryContainerLight,
    surface = YellowSurfaceLight,
    onSurface = YellowOnSurfaceLight,
    surfaceVariant = YellowSurfaceVariantLight,
    onSurfaceVariant = YellowOnSurfaceVariantLight,
    outline = YellowOutlineLight,
    inversePrimary = YellowInversePrimaryLight,
    inverseSurface = YellowInverseSurfaceLight,
    inverseOnSurface = YellowInverseOnSurfaceLight
)

private val YellowDarkColorScheme = darkColorScheme(
    primary = YellowPrimaryDark,
    onPrimary = YellowOnPrimaryDark,
    primaryContainer = YellowPrimaryContainerDark,
    onPrimaryContainer = YellowOnPrimaryContainerDark,
    secondary = YellowSecondaryDark,
    onSecondary = YellowOnSecondaryDark,
    secondaryContainer = YellowSecondaryContainerDark,
    onSecondaryContainer = YellowOnSecondaryContainerDark,
    tertiary = YellowTertiaryDark,
    onTertiary = YellowOnTertiaryDark,
    tertiaryContainer = YellowTertiaryContainerDark,
    onTertiaryContainer = YellowOnTertiaryContainerDark,
    surface = YellowSurfaceDark,
    onSurface = YellowOnSurfaceDark,
    surfaceVariant = YellowSurfaceVariantDark,
    onSurfaceVariant = YellowOnSurfaceVariantDark,
    outline = YellowOutlineDark,
    inversePrimary = YellowInversePrimaryDark,
    inverseSurface = YellowInverseSurfaceDark,
    inverseOnSurface = YellowInverseOnSurfaceDark
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    onPrimary = OrangeOnPrimaryLight,
    primaryContainer = OrangePrimaryContainerLight,
    onPrimaryContainer = OrangeOnPrimaryContainerLight,
    secondary = OrangeSecondaryLight,
    onSecondary = OrangeOnSecondaryLight,
    secondaryContainer = OrangeSecondaryContainerLight,
    onSecondaryContainer = OrangeOnSecondaryContainerLight,
    tertiary = OrangeTertiaryLight,
    onTertiary = OrangeOnTertiaryLight,
    tertiaryContainer = OrangeTertiaryContainerLight,
    onTertiaryContainer = OrangeOnTertiaryContainerLight,
    surface = OrangeSurfaceLight,
    onSurface = OrangeOnSurfaceLight,
    surfaceVariant = OrangeSurfaceVariantLight,
    onSurfaceVariant = OrangeOnSurfaceVariantLight,
    outline = OrangeOutlineLight,
    inversePrimary = OrangeInversePrimaryLight,
    inverseSurface = OrangeInverseSurfaceLight,
    inverseOnSurface = OrangeInverseOnSurfaceLight
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    primaryContainer = OrangePrimaryContainerDark,
    onPrimaryContainer = OrangeOnPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = OrangeOnSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OrangeOnSecondaryContainerDark,
    tertiary = OrangeTertiaryDark,
    onTertiary = OrangeOnTertiaryDark,
    tertiaryContainer = OrangeTertiaryContainerDark,
    onTertiaryContainer = OrangeOnTertiaryContainerDark,
    surface = OrangeSurfaceDark,
    onSurface = OrangeOnSurfaceDark,
    surfaceVariant = OrangeSurfaceVariantDark,
    onSurfaceVariant = OrangeOnSurfaceVariantDark,
    outline = OrangeOutlineDark,
    inversePrimary = OrangeInversePrimaryDark,
    inverseSurface = OrangeInverseSurfaceDark,
    inverseOnSurface = OrangeInverseOnSurfaceDark
)

private val TealLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = TealOnSecondaryLight,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = TealOnSecondaryContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = TealOnTertiaryLight,
    tertiaryContainer = TealTertiaryContainerLight,
    onTertiaryContainer = TealOnTertiaryContainerLight,
    surface = TealSurfaceLight,
    onSurface = TealOnSurfaceLight,
    surfaceVariant = TealSurfaceVariantLight,
    onSurfaceVariant = TealOnSurfaceVariantLight,
    outline = TealOutlineLight,
    inversePrimary = TealInversePrimaryLight,
    inverseSurface = TealInverseSurfaceLight,
    inverseOnSurface = TealInverseOnSurfaceLight
)

private val TealDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealTertiaryContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    surface = TealSurfaceDark,
    onSurface = TealOnSurfaceDark,
    surfaceVariant = TealSurfaceVariantDark,
    onSurfaceVariant = TealOnSurfaceVariantDark,
    outline = TealOutlineDark,
    inversePrimary = TealInversePrimaryDark,
    inverseSurface = TealInverseSurfaceDark,
    inverseOnSurface = TealInverseOnSurfaceDark
)

//Pink
private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimaryLight,
    onPrimary = PinkOnPrimaryLight,
    primaryContainer = PinkPrimaryContainerLight,
    onPrimaryContainer = PinkOnPrimaryContainerLight,
    secondary = PinkSecondaryLight,
    onSecondary = PinkOnSecondaryLight,
    secondaryContainer = PinkSecondaryContainerLight,
    onSecondaryContainer = PinkOnSecondaryContainerLight,
    tertiary = PinkTertiaryLight,
    onTertiary = PinkOnTertiaryLight,
    tertiaryContainer = PinkTertiaryContainerLight,
    onTertiaryContainer = PinkOnTertiaryContainerLight,
    surface = PinkSurfaceLight,
    onSurface = PinkOnSurfaceLight,
    surfaceVariant = PinkSurfaceVariantLight,
    onSurfaceVariant = PinkOnSurfaceVariantLight,
    outline = PinkOutlineLight,
    inversePrimary = PinkInversePrimaryLight,
    inverseSurface = PinkInverseSurfaceLight,
    inverseOnSurface = PinkInverseOnSurfaceLight
)
private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = PinkOnPrimaryDark,
    primaryContainer = PinkPrimaryContainerDark,
    onPrimaryContainer = PinkOnPrimaryContainerDark,
    secondary = PinkSecondaryDark,
    onSecondary = PinkOnSecondaryDark,
    secondaryContainer = PinkSecondaryContainerDark,
    onSecondaryContainer = PinkOnSecondaryContainerDark,
    tertiary = PinkTertiaryDark,
    onTertiary = PinkOnTertiaryDark,
    tertiaryContainer = PinkTertiaryContainerDark,
    onTertiaryContainer = PinkOnTertiaryContainerDark,
    surface = PinkSurfaceDark,
    onSurface = PinkOnSurfaceDark,
    surfaceVariant = PinkSurfaceVariantDark,
    onSurfaceVariant = PinkOnSurfaceVariantDark,
    outline = PinkOutlineDark,
    inversePrimary = PinkInversePrimaryDark,
    inverseSurface = PinkInverseSurfaceDark,
    inverseOnSurface = PinkInverseOnSurfaceDark
)

//Brown
private val BrownLightColorScheme = lightColorScheme(
    primary = BrownPrimaryLight,
    onPrimary = BrownOnPrimaryLight,
    primaryContainer = BrownPrimaryContainerLight,
    onPrimaryContainer = BrownOnPrimaryContainerLight,
    secondary = BrownSecondaryLight,
    onSecondary = BrownOnSecondaryLight,
    secondaryContainer = BrownSecondaryContainerLight,
    onSecondaryContainer = BrownOnSecondaryContainerLight,
    tertiary = BrownTertiaryLight,
    onTertiary = BrownOnTertiaryLight,
    tertiaryContainer = BrownTertiaryContainerLight,
    onTertiaryContainer = BrownOnTertiaryContainerLight,
    surface = BrownSurfaceLight,
    onSurface = BrownOnSurfaceLight,
    surfaceVariant = BrownSurfaceVariantLight,
    onSurfaceVariant = BrownOnSurfaceVariantLight,
    outline = BrownOutlineLight,
    inversePrimary = BrownInversePrimaryLight,
    inverseSurface = BrownInverseSurfaceLight,
    inverseOnSurface = BrownInverseOnSurfaceLight
)
private val BrownDarkColorScheme = darkColorScheme(
    primary = BrownPrimaryDark,
    onPrimary = BrownOnPrimaryDark,
    primaryContainer = BrownPrimaryContainerDark,
    onPrimaryContainer = BrownOnPrimaryContainerDark,
    secondary = BrownSecondaryDark,
    onSecondary = BrownOnSecondaryDark,
    secondaryContainer = BrownSecondaryContainerDark,
    onSecondaryContainer = BrownOnSecondaryContainerDark,
    tertiary = BrownTertiaryDark,
    onTertiary = BrownOnTertiaryDark,
    tertiaryContainer = BrownTertiaryContainerDark,
    onTertiaryContainer = BrownOnTertiaryContainerDark,
    surface = BrownSurfaceDark,
    onSurface = BrownOnSurfaceDark,
    surfaceVariant = BrownSurfaceVariantDark,
    onSurfaceVariant = BrownOnSurfaceVariantDark,
    outline = BrownOutlineDark,
    inversePrimary = BrownInversePrimaryDark,
    inverseSurface = BrownInverseSurfaceDark,
    inverseOnSurface = BrownInverseOnSurfaceDark
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
            ColorSchemeType.YELLOW -> YellowDarkColorScheme
            ColorSchemeType.ORANGE -> OrangeDarkColorScheme
            ColorSchemeType.TEAL -> TealDarkColorScheme
            ColorSchemeType.PINK -> PinkDarkColorScheme
            ColorSchemeType.BROWN -> BrownDarkColorScheme
        }

        else -> when (colorSchemeType) {
            ColorSchemeType.DEFAULT -> LightColorScheme
            ColorSchemeType.BLUE -> BlueLightColorScheme
            ColorSchemeType.GREEN -> GreenLightColorScheme
            ColorSchemeType.RED -> RedLightColorScheme
            ColorSchemeType.YELLOW -> YellowLightColorScheme
            ColorSchemeType.ORANGE -> OrangeLightColorScheme
            ColorSchemeType.TEAL -> TealLightColorScheme
            ColorSchemeType.PINK -> PinkLightColorScheme
            ColorSchemeType.BROWN -> BrownLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
