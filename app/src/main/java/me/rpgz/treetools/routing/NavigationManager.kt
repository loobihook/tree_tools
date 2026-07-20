package me.rpgz.treetools.routing

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

class NavigationManager(private val navController: NavController) {

    fun navigateToTreeList() {
        navigate(Routes.TreeList.route)
    }

    fun navigateToCreateAnalysis() {
        navigate(Routes.CreateAnalysis.route)
    }

    fun navigateToDataOverview() {
        navigate(Routes.DataOverview.route)
    }

    fun navigateToToolbox() {
        navigate(Routes.Toolbox.route)
    }

    fun navigateToCreateRecord(recordId: Long? = null) {
        val route = if (recordId != null) {
            "${Routes.CreateTreeAnalysisRecord.route}?recordId=$recordId"
        } else {
            Routes.CreateTreeAnalysisRecord.route
        }
        navController.navigate(route)
    }

    fun navigateToModelTest() {
        navController.navigate(Routes.ModelTest.route)
    }

    fun navigateToRealSenseTest() {
        navController.navigate(Routes.RealSenseTest.route)
    }

    fun navigateToRealSenseMeasurement() {
        navController.navigate(Routes.RealSenseMeasurement.route)
    }

    fun navigateToSettings() {
        navController.navigate(Routes.Settings.route)
    }

    fun navigateToBenchmark() {
        navController.navigate(Routes.Benchmark.route)
    }

    fun goBack() {
        navController.popBackStack()
    }

    fun popToRoot() {
        navController.popBackStack(navController.graph.findStartDestination().id, false)
    }

    private fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun getCurrentRoute(): String? {
        return navController.currentDestination?.route
    }

    fun isOnBottomNavRoute(): Boolean {
        val currentRoute = getCurrentRoute()
        return currentRoute in listOf(
            Routes.TreeList.route,
            Routes.CreateAnalysis.route,
            Routes.DataOverview.route,
            Routes.Toolbox.route
        )
    }
}