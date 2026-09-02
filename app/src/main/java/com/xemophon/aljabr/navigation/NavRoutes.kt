package com.xemophon.aljabr.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.JoinInner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.R
import com.xemophon.aljabr.algebra.matrices.MatrixScreen
import com.xemophon.aljabr.algebra.matrices.MatrixViewModel
import com.xemophon.aljabr.algebra.polynomials.PolyCalc
import com.xemophon.aljabr.basicCalc.BasicCalc
import com.xemophon.aljabr.calculus.differentiate.DiffCalc
import com.xemophon.aljabr.graphMaker.GraphMaker
import com.xemophon.aljabr.calculus.integrate.IntegCalc
import com.xemophon.aljabr.calculus.limits.Limits
import com.xemophon.aljabr.conversions.ConvertorPage
import com.xemophon.aljabr.conversions.UtilitiesScreen
import com.xemophon.aljabr.misc.MiscPage
import com.xemophon.aljabr.series.fourier.FourierCalc
import com.xemophon.aljabr.series.taylor.TaylorCalc
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
object MiscRoute
@Serializable
object ConvertorRoute
@Serializable
object SheetRoute
@Serializable
object PolyRoute
@Serializable
object MatrixRoute
@Serializable
object FourierRoute
@Serializable
object TaylorRoute
val Calculus = listOf(
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
        icon = R.drawable.function,
        content = { onOpenDrawer -> DiffCalc(onOpenDrawer = onOpenDrawer) }
    )
)

val Algebra = listOf(
    CalculatorVariant(
        route = PolyRoute,
        routeClass = PolyRoute::class,
        label = "Polynomials",
        icon = Icons.Default.JoinInner,
        content = { onOpenDrawer -> PolyCalc(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = MatrixRoute,
        routeClass = MatrixRoute::class,
        label = "Matrices",
        icon = Icons.Default.DataArray,
        content = { onOpenDrawer ->
            val viewModel: MatrixViewModel = viewModel()
            MatrixScreen(
                viewModel = viewModel,
                onOpenDrawer = onOpenDrawer
            )
        }
    )
)

val Series = listOf(
    CalculatorVariant(
        route = FourierRoute,
        routeClass = FourierRoute::class,
        label = "Fourier Series",
        icon = R.drawable.fourier,
        content = { onOpenDrawer -> FourierCalc(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = TaylorRoute,
        routeClass = TaylorRoute::class,
        label = "Taylor Series",
        icon = R.drawable.taylor,
        content = { onOpenDrawer -> TaylorCalc(onOpenDrawer = onOpenDrawer) }
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
        route = GraphMakerRoute,
        routeClass = GraphMakerRoute::class,
        label = "Graph Maker",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        content = { onOpenDrawer -> GraphMaker(onOpenDrawer = onOpenDrawer) }
    ),
    CalculatorVariant(
        route = MiscRoute,
        routeClass = MiscRoute::class,
        label = "Settings & About",
        icon = Icons.Default.Settings,
        content = { onOpenDrawer -> MiscPage(onOpenDrawer = onOpenDrawer) }
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
    CalculatorVariant(
        route = SheetRoute,
        routeClass = SheetRoute::class,
        label = "Reference Sheets",
        icon = Icons.AutoMirrored.Filled.Article,
        content = { onOpenDrawer -> UtilitiesScreen(onOpenDrawer = onOpenDrawer)}
    )
)