package com.xemophon.aljabr.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import com.xemophon.aljabr.basicCalc.BasicCalc
import com.xemophon.aljabr.calculus.differentiate.DiffCalc
import com.xemophon.aljabr.calculus.graphMaker.GraphMaker
import com.xemophon.aljabr.calculus.integrate.IntegCalc
import com.xemophon.aljabr.calculus.limits.Limits
import com.xemophon.aljabr.conversions.ConvertorPage
import com.xemophon.aljabr.misc.AboutScreen
import com.xemophon.aljabr.misc.SettingsPage
import kotlinx.serialization.Serializable

@Serializable
object BasicCalcRoute
@Serializable
object GraphMakerRoute
@Serializable
object LimitsRoute
@Serializable
object IntegrateRoute
@Serializable
object DifferentiateRoute
@Serializable
object AboutRoute
@Serializable
object SettingsRoute
@Serializable
object ConvertorRoute
val Calculus = listOf(

    CalculatorVariant(
        route = GraphMakerRoute,
        routeClass = GraphMakerRoute::class,
        label = "Graph Maker",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        content = { onOpenDrawer -> GraphMaker(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = LimitsRoute,
        routeClass = LimitsRoute::class,
        label = "Limits",
        icon = Icons.AutoMirrored.Filled.ArrowRightAlt,
        content = { onOpenDrawer -> Limits(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = IntegrateRoute,
        routeClass = IntegrateRoute::class,
        label = "Integrate",
        icon = Icons.Default.Functions,
        content = { onOpenDrawer -> IntegCalc(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = DifferentiateRoute,
        routeClass = DifferentiateRoute::class,
        label = "Differentiate",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        content = { onOpenDrawer -> DiffCalc(onOpenDrawer = onOpenDrawer) }
    )
)

val Misc = listOf(
    CalculatorVariant(
        route = BasicCalcRoute,
        routeClass = BasicCalcRoute::class,
        label = "Basic Calculator",
        icon = Icons.Default.Calculate,
        content = { onOpenDrawer -> BasicCalc(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = AboutRoute,
        routeClass = AboutRoute::class,
        label = "About",
        icon = Icons.Default.Info,
        content = { onOpenDrawer -> AboutScreen(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
        label = "Settings",
        icon = Icons.Default.Settings,
        content = { onOpenDrawer -> SettingsPage(onOpenDrawer = onOpenDrawer) }
    )
)

val ReferenceSheets = listOf(
    CalculatorVariant(
        route = ConvertorRoute,
        routeClass = ConvertorRoute::class,
        label = "Convertors",
        icon = Icons.Default.Repeat,
        content = { onOpenDrawer -> ConvertorPage(onOpenDrawer = onOpenDrawer) }
    ),
)