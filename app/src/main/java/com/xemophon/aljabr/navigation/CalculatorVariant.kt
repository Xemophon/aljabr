package com.xemophon.aljabr.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

data class CalculatorVariant<T : Any>(
    val route: T,
    val routeClass: KClass<T>,
    val label: String,
    val icon: ImageVector,
    val content: @Composable (onOpenDrawer: () -> Unit) -> Unit
)