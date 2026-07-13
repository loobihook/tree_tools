package me.rpgz.treetools.routing

import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.filled.Bluetooth

sealed class Routes(val route : String) {
    data object ModelTest : Routes("model-test")
    data object RealSenseTest : Routes("realsense-test")
    data object CreateTreeAnalysisRecord : Routes("create-tree-analysis-record")
    data object Home : Routes("home")
    data object Settings : Routes("settings")
    data object Sensors : Routes("sensors")

    data object RealSenseMeasurement: Routes("realsense-measurement")
}

fun bottomNavigationItems() : List<BottomNavigationItem> {
    return listOf(
        BottomNavigationItem("首页", Icons.Default.Home, route=Routes.Home.route),
        BottomNavigationItem("传感器", Icons.Filled.Bluetooth, route=Routes.Sensors.route),
//        BottomNavigationItem("模型", Icons.Default.Star, route=Routes.ModelTest.route),
//        BottomNavigationItem("相机", Icons.Default.Menu, route=Routes.RealSenseTest.route),
        BottomNavigationItem("设置", Icons.Default.Settings, route=Routes.Settings.route),
    )
}

data class BottomNavigationItem(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Home,
    val route : String = ""
)

@Composable
fun AppBottomNavigation(navController: NavController, show: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var navigationSelectedItem by remember { mutableIntStateOf(0) }
    if(show) {
        NavigationBar{
            bottomNavigationItems().forEachIndexed { index,navigationItem ->
                NavigationBarItem(
                    modifier = Modifier.fillMaxWidth(0.25f),
                    label = { Text(navigationItem.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == navigationItem.route } == true,
                    onClick = {
                        navigationSelectedItem = index
                        navController.navigate(navigationItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            navigationItem.icon,
                            contentDescription = navigationItem.label
                        )
                    }
                )
            }
        }
    }
}
