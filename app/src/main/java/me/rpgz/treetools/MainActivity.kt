package me.rpgz.treetools

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.rpgz.treetools.ble.SensorMode
import me.rpgz.treetools.preferences.UserPreferences
import me.rpgz.treetools.routing.AppLayout
import me.rpgz.treetools.routing.AppNavHost
import me.rpgz.treetools.ui.theme.TreeToolsTheme

fun Context.decodeBitmapFromUri(
    uri: Uri,
    maxSizePx: Int = 200000
): Bitmap {
    val cr: ContentResolver = contentResolver
    val src = ImageDecoder.createSource(cr, uri)
    return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
        val w = info.size.width
        val h = info.size.height
        val sample = maxOf(1, maxOf(w, h) / maxSizePx)
        if (sample > 1) decoder.setTargetSampleSize(sample)
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onDestroy() {
        super.onDestroy()
        appContainer.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        appContainer.realSensePermission.handlePermissionResult(requestCode, permissions, grantResults)
        appContainer.bleManager.handlePermissionResult(requestCode, permissions, grantResults)
        appContainer.handlePermissionResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        appContainer.bleManager.handleActivityResult(requestCode, resultCode)
        appContainer.handleActivityResult(requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            val userPreferences = UserPreferences(applicationContext)
            val savedMode = userPreferences.sensorMode.first()
            val sensorMode = try {
                SensorMode.valueOf(savedMode)
            } catch (e: IllegalArgumentException) {
                SensorMode.REAL
            }
            
            appContainer = AppContainer.init(
                application, 
                this@MainActivity, 
                AppConfig(sensorMode = sensorMode)
            )

            enableEdgeToEdge()
            setContent {
                TreeToolsApp()
            }
        }
    }
}

@Composable
fun TreeToolsApp() {
    TreeToolsTheme {
        val navController = rememberNavController()
        
        AppLayout(navController = navController) {
            AppNavHost(navController = navController)
        }
    }
}