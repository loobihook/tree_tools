package me.rpgz.treetools.routing

import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

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
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFf0fdf4),
                            Color(0xFFdcfce7)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            containerColor = Color.Transparent,
            tonalElevation = 8.dp
        ){
            bottomNavigationItems().forEachIndexed { index, navigationItem ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == navigationItem.route } == true
                
                NavigationBarItem(
                    modifier = Modifier.fillMaxWidth(0.33f),
                    label = { 
                        Text(
                            navigationItem.label,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                        ) 
                    },
                    selected = isSelected,
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
                            contentDescription = navigationItem.label,
                            modifier = Modifier
                        )
                    },
                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF065f46),
                        selectedTextColor = Color(0xFF065f46),
                        indicatorColor = Color(0xFF86efac).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF708090),
                        unselectedTextColor = Color(0xFF708090)
                    )
                )
            }
        }
    }
}