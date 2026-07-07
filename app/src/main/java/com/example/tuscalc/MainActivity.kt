package com.example.tuscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tuscalc.navigation.AllCalculatorVariants
import com.example.tuscalc.navigation.BasicCalcRoute
import com.example.tuscalc.ui.theme.TUsCalcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TUsCalcTheme {
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
                            AllCalculatorVariants.forEach { variant ->
                                NavigationDrawerItem(
                                    label = { Text(variant.label) },
                                    icon = { Icon(variant.icon, contentDescription = null) },
                                    selected = currentDestination?.hasRoute(variant.routeClass) == true,
                                    onClick = {
                                        navController.navigate(variant.route) {
                                            if (variant.route is BasicCalcRoute) {
                                                popUpTo(BasicCalcRoute) { inclusive = true }
                                            } else {
                                                popUpTo(BasicCalcRoute) { saveState = true }
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
                        startDestination = BasicCalcRoute
                    ) {
                        AllCalculatorVariants.forEach { variant ->
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
