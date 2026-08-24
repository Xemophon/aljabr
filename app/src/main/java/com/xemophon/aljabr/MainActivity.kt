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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xemophon.aljabr.calculus.differentiate.DiffFunc
import com.xemophon.aljabr.calculus.graphMaker.GraphGenerator
import com.xemophon.aljabr.calculus.integrate.IntegFunc
import com.xemophon.aljabr.data.AppTheme
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.misc.SettingsViewModel
import com.xemophon.aljabr.navigation.Algebra
import com.xemophon.aljabr.navigation.BasicCalcRoute
import com.xemophon.aljabr.navigation.Calculus
import com.xemophon.aljabr.navigation.Misc
import com.xemophon.aljabr.navigation.ReferenceSheets
import com.xemophon.aljabr.navigation.Series
import com.xemophon.aljabr.ui.components.HorizontalSeparator
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(12.dp))
                            Misc.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute::class) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute::class) { saveState = true }
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                )
                            }
                            HorizontalSeparator(text = "Calculus")
                            Calculus.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute::class) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute::class) { saveState = true }
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                )
                            }
                            HorizontalSeparator(text = "Algebra")
                            Algebra.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute::class) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute::class) { saveState = true }
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                            HorizontalSeparator(text = "Series Expansion")
                            Series.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute::class) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute::class) { saveState = true }
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                )
                            }
                            HorizontalSeparator(text = "Utilities")
                            ReferenceSheets.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute::class) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute::class) { saveState = true }
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                )
                            }
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

    override fun onDestroy() {
        super.onDestroy()
        // Clear cache on exit if enabled
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = SettingsRepository(applicationContext)
            val autoClear = repository.autoClearCacheFlow.first()
            if (autoClear) {
                StorageUtils.clearAppCache(applicationContext)
                GraphGenerator.clearCache()
            }
        }
    }
}
