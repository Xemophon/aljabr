package com.xemophon.aljabr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xemophon.aljabr.data.AppTheme
import com.xemophon.aljabr.modules.calculus.differentiate.DiffFunc
import com.xemophon.aljabr.modules.calculus.integrate.IntegFunc
import com.xemophon.aljabr.modules.misc.SettingsViewModel
import com.xemophon.aljabr.navigation.Algebra
import com.xemophon.aljabr.navigation.BasicCalcRoute
import com.xemophon.aljabr.navigation.CalculatorVariant
import com.xemophon.aljabr.navigation.Calculus
import com.xemophon.aljabr.navigation.Misc
import com.xemophon.aljabr.navigation.ReferenceSheets
import com.xemophon.aljabr.navigation.Series
import com.xemophon.aljabr.ui.components.buttons.HorizontalSeparator
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Warm up the math engines in the background to improve first-use performance
        lifecycleScope.launch(Dispatchers.Default) {
            IntegFunc.warmUp()
            DiffFunc.warmUp()
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appTheme by settingsViewModel.theme.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()
            val colorSchemeType by settingsViewModel.colorScheme.collectAsState()

            val isDarkTheme = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.AUTO -> isSystemInDarkTheme()
            }

            AlJabrTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColor,
                colorSchemeType = colorSchemeType
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val onNavigate: (CalculatorVariant<*>) -> Unit = { variant ->
                    navController.navigate(variant.route) {
                        if (variant.route is BasicCalcRoute) {
                            popUpTo(BasicCalcRoute::class) { inclusive = true }
                        } else {
                            popUpTo(BasicCalcRoute::class) { saveState = true }
                        }
                    }
                    scope.launch { drawerState.close() }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(12.dp))
                            DrawerSection(items = Misc, currentDestination = currentDestination, onNavigate = onNavigate)
                            DrawerSection(title = "Calculus", items = Calculus, currentDestination = currentDestination, onNavigate = onNavigate)
                            DrawerSection(title = "Algebra", items = Algebra, currentDestination = currentDestination, onNavigate = onNavigate)
                            DrawerSection(title = "Series Expansion", items = Series, currentDestination = currentDestination, onNavigate = onNavigate)
                            DrawerSection(title = "Utilities", items = ReferenceSheets, currentDestination = currentDestination, onNavigate = onNavigate)
                        }
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = BasicCalcRoute::class,
                        enterTransition = {
                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.95f,
                                        animationSpec = tween(220, easing = LinearOutSlowInEasing)
                                    )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.95f,
                                        animationSpec = tween(220, easing = LinearOutSlowInEasing)
                                    )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing))
                        },
                    ) {
                        Calculus.forEach { variant ->
                            composable(variant.routeClass) {
                                variant.content { scope.launch { drawerState.open() } }
                            }
                        }
                        Algebra.forEach { variant ->
                            composable(variant.routeClass) {
                                variant.content { scope.launch { drawerState.open() } }
                            }
                        }
                        Series.forEach { variant ->
                            composable(variant.routeClass) {
                                variant.content { scope.launch { drawerState.open() } }
                            }
                        }
                        Misc.forEach { variant ->
                            composable(variant.routeClass) {
                                variant.content { scope.launch { drawerState.open() } }
                            }
                        }
                        ReferenceSheets.forEach { variant ->
                            composable(variant.routeClass) {
                                variant.content { scope.launch { drawerState.open() } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSection(
    title: String? = null,
    items: List<CalculatorVariant<*>>,
    currentDestination: NavDestination?,
    onNavigate: (CalculatorVariant<*>) -> Unit
) {
    if (title != null) {
        HorizontalSeparator(text = title)
    }
    items.forEach { variant ->
        NavigationDrawerItem(
            label = { Text(variant.label) },
            icon = {
                when (val icon = variant.icon) {
                    is ImageVector -> Icon(icon, contentDescription = null)
                    is Int -> Icon(painterResource(icon), contentDescription = null)
                }
            },
            selected = currentDestination?.hasRoute(variant.routeClass) == true,
            onClick = { onNavigate(variant) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
