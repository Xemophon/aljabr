package com.example.tuscalc.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import com.example.tuscalc.basicCalc.BasicCalc
import com.example.tuscalc.differentiate.DiffCalc
import com.example.tuscalc.graphMaker.GraphMaker
import com.example.tuscalc.integrate.IntegCalc
import com.example.tuscalc.limits.Limits
import kotlinx.serialization.Serializable

@Serializable object BasicCalcRoute
@Serializable object GraphMakerRoute
@Serializable object LimitsRoute
@Serializable object IntegrateRoute
@Serializable object DifferentiateRoute

val AllCalculatorVariants = listOf(
    CalculatorVariant(
        route = BasicCalcRoute,
        routeClass = BasicCalcRoute::class,
        label = "Basic Calculator",
        icon = Icons.Default.Calculate,
        content = { onOpenDrawer -> BasicCalc(onOpenDrawer = onOpenDrawer) }
    ),
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
