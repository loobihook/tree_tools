package me.rpgz.treetools.routing

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppBottomNavigation(
    navController: NavController,
    show: Boolean
) {
    if (!show) return

    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavItems = listOf(
            Routes.TreeList,
            Routes.CreateAnalysis,
            Routes.DataOverview,
            Routes.Toolbox
        )

        bottomNavItems.forEach { route ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.title
                    )
                },
                label = { Text(route.title) },
                selected = currentRoute == route.route,
                onClick = {
                    if (currentRoute != route.route) {
                        navController.navigate(route.route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )
        }
    }
}