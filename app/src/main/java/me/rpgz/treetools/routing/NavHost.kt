package me.rpgz.treetools.routing

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import me.rpgz.treetools.pages.BleTest
import me.rpgz.treetools.pages.CreateTreeAnalysisPage
import me.rpgz.treetools.pages.HomePage
import me.rpgz.treetools.pages.ModelTestPage
import me.rpgz.treetools.pages.RealSenseMeasurementPage
import me.rpgz.treetools.pages.RealSenseTestPage
import me.rpgz.treetools.pages.SettingsPage

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination= Routes.Home.route
    ) {
        composable(
            route= Routes.ModelTest.route,
            enterTransition = null,
            exitTransition = null
        ) {
            ModelTestPage(navController)
        }

        composable(
            route= Routes.RealSenseTest.route,
            enterTransition = null,
            exitTransition = null
        ) {
            RealSenseTestPage(navController)
        }

        composable(
            route = "${Routes.CreateTreeAnalysisRecord.route}?recordId={recordId}",
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = null,
            exitTransition = null
        ) {
            CreateTreeAnalysisPage(navController)
        }

        composable(
            route= Routes.Home.route,
            enterTransition = null,
            exitTransition = null
        ) {
            HomePage(navController)
        }


        composable(
            route= Routes.Settings.route,
            enterTransition = null,
            exitTransition = null
        ) {
            SettingsPage(navController)
        }

        composable(
            route= Routes.Sensors.route,
            enterTransition = null,
            exitTransition = null
        ) {
//            SensorsPage(navController)
            BleTest()
        }

        composable(
            route= Routes.RealSenseMeasurement.route,

            ){
            RealSenseMeasurementPage()
        }
    }
}