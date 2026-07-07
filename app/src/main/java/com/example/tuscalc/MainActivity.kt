package com.example.tuscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
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
import com.example.tuscalc.graphMaker.GraphMaker
import com.example.tuscalc.basicCalc.BasicCalc
import com.example.tuscalc.ui.theme.TUsCalcTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object BasicCalcRoute
@Serializable object GraphMaker

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
                            NavigationDrawerItem(
                                label = { Text("Basic Calculator") },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                                selected = currentDestination?.hasRoute<BasicCalcRoute>() == true,
                                onClick = {
                                    navController.navigate(BasicCalcRoute) {
                                        popUpTo(BasicCalcRoute) { inclusive = true }
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Graph Maker") },
                                icon = { Icon(Icons.Default.Functions, contentDescription = null) },
                                selected = currentDestination?.hasRoute<GraphMaker>() == true,
                                onClick = {
                                    navController.navigate(GraphMaker) {
                                        popUpTo(BasicCalcRoute) { saveState = true }
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = BasicCalcRoute
                    ) {
                        composable<BasicCalcRoute> {
                            BasicCalc(onOpenDrawer = { scope.launch { drawerState.open() } })
                        }
                        composable<GraphMaker> {
                            GraphMaker(onOpenDrawer = { scope.launch { drawerState.open() } })
                        }
                    }
                }
            }
        }
    }
}
