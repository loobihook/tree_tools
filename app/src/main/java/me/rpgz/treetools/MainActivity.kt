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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.rpgz.treetools.routing.AppBottomNavigation
import me.rpgz.treetools.routing.Routes
import dagger.hilt.android.AndroidEntryPoint
import me.rpgz.treetools.routing.AppNavHost
import me.rpgz.treetools.ui.theme.TreeToolsTheme


fun Context.decodeBitmapFromUri(
    uri: Uri,
    maxSizePx: Int = 200000 // longest side target to avoid OOM
): Bitmap {
    val cr: ContentResolver = contentResolver
    // Use ImageDecoder on Android 9+ (API 28+), with downsampling
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
        appContainer.hm10Manager.handlePermissionResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        appContainer.bleManager.handleActivityResult(requestCode, resultCode)
        appContainer.hm10Manager.handleActivityResult(requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer.init(application, this)

        enableEdgeToEdge()
        setContent {
            TreeToolsTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                var bottomBarState by rememberSaveable { (mutableStateOf(true)) }
                bottomBarState = when (navBackStackEntry?.destination?.route) {
                    Routes.Home.route, Routes.Settings.route, Routes.Sensors.route -> true
                    else -> false
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AppBottomNavigation(
                            navController = navController,
                            show = bottomBarState
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(paddingValues = innerPadding)) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }

//    private val pickImage =
//        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
//            if (uri != null) {
//                // Decode off the UI thread
//                lifecycleScope.launch {
//                    val bmp = withContext(Dispatchers.IO) {
//                        decodeBitmapFromUri(uri, maxSizePx = 2048)  // see function below
//                    }
//                    // TODO: use your bitmap (e.g., imageView.setImageBitmap(bmp))
//                    appContainer.infer.analyze(bmp)
//                }
//            }
//        }

}

