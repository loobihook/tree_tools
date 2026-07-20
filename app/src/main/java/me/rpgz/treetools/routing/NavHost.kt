package me.rpgz.treetools.routing

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import me.rpgz.treetools.pages.*

private const val ANIMATION_DURATION = 300

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.TreeList.route,
        enterTransition = {
            when (initialState.destination.route) {
                Routes.CreateTreeAnalysisRecord.route ->
                    slideInVertically(
                        initialOffsetY = { 300 },
                        animationSpec = tween(ANIMATION_DURATION)
                    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                else ->
                    slideInHorizontally(
                        initialOffsetX = { 300 },
                        animationSpec = tween(ANIMATION_DURATION)
                    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
            }
        },
        exitTransition = {
            when (targetState.destination.route) {
                Routes.CreateTreeAnalysisRecord.route ->
                    slideOutVertically(
                        targetOffsetY = { -300 },
                        animationSpec = tween(ANIMATION_DURATION)
                    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                else ->
                    slideOutHorizontally(
                        targetOffsetX = { -300 },
                        animationSpec = tween(ANIMATION_DURATION)
                    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
            }
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(ANIMATION_DURATION)
            ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(ANIMATION_DURATION)
            ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
        }
    ) {
        composable(Routes.TreeList.route) {
            TreeListPage(navController)
        }

        composable(Routes.CreateAnalysis.route) {
            CreateAnalysisPage(navController)
        }

        composable(Routes.DataOverview.route) {
            DataOverviewPage(navController)
        }

        composable(Routes.Toolbox.route) {
            ToolboxPage(navController)
        }

        composable(Routes.ModelTest.route) {
            ModelTestPage(navController)
        }

        composable(Routes.RealSenseTest.route) {
            RealSenseTestPage(navController)
        }

        composable(Routes.RealSenseMeasurement.route) {
            RealSenseMeasurementPage()
        }

        composable(Routes.Settings.route) {
            SettingsPage(navController)
        }

        composable(Routes.Benchmark.route) {
            BenchmarkPage()
        }

        composable(Routes.ApiConfig.route) {
            ApiConfigPage(navController)
        }

        composable(Routes.SensorMode.route) {
            SensorModePage(navController)
        }

        composable(
            route = "${Routes.CreateTreeAnalysisRecord.route}?recordId={recordId}",
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CreateTreeAnalysisPage(navController)
        }
    }
}