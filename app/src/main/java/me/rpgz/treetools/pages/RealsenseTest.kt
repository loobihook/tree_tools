package me.rpgz.treetools.pages

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.intel.realsense.librealsense.GLRsSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rpgz.treetools.components.RealSenseDeviceStatus
import me.rpgz.treetools.components.ScreenTile
import me.rpgz.treetools.ml.captureDepthFrame
import me.rpgz.treetools.ml.depthByteBufferToBitmap
import me.rpgz.treetools.realsense.FrameStreamingThread
import me.rpgz.treetools.realsense.RealSenseSurfaceView

private val TAG = "RealSenseTestPage"

@Composable
fun RealSenseTestPage(navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        // State to manage FrameStreamingThread
        var frameStreamingThread by remember { mutableStateOf<FrameStreamingThread?>(null) }
        var isStreaming by remember { mutableStateOf(false) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var glView by remember { mutableStateOf<GLRsSurfaceView?>(null) }


        // Cleanup when component unmounts
        DisposableEffect(Unit) {
            onDispose {
                frameStreamingThread?.stopAsync {
                    Log.d("RealsenseTest", "Frame streaming thread cleanup completed")
                }
                frameStreamingThread = null
                isStreaming = false
            }
        }

        ScreenTile("相机调试")

        RealSenseDeviceStatus(
            onAttach = {
                // Device connected - ready to start streaming
            },
            onDetach = {
                // Device disconnected - stop streaming
                frameStreamingThread?.stopAsync()
                isStreaming = false
            }
        )

        Box(modifier = Modifier.height(12.dp))


        Button(onClick = {
            if (!isStreaming) {
                glView?.clear()
                frameStreamingThread?.start()
                isStreaming = true
            } else {
                frameStreamingThread?.stopAsync {
                    // Optional: Handle completion
                    Log.d("RealsenseTest", "Streaming stopped")
                }
                isStreaming = false
            }
        }) {
            Text(if (isStreaming) "停止流" else "开始流")
        }

        Box(modifier = Modifier.height(12.dp))

        Button(onClick = {
            scope.launch {
                // 切到非 UI 线程执行耗时任务
                bitmap = withContext(Dispatchers.IO) {
                    captureDepthFrame() // suspend 函数，或自己写的耗时逻辑
                }
            }
        }) {
            Text("抓取一张")
        }

        Box(modifier = Modifier.height(12.dp))

        // Pause/Resume controls when streaming
        if (isStreaming) {
            Button(onClick = {
                frameStreamingThread?.pause()
            }) {
                Text("暂停")
            }

            Box(modifier = Modifier.height(8.dp))

            Button(onClick = {
                frameStreamingThread?.resume()
            }) {
                Text("恢复")
            }

            Box(modifier = Modifier.height(12.dp))
        }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        RealSenseSurfaceView(
            modifier = Modifier.fillMaxSize(),
            onViewReady = { view ->
                glView = view
            }
        )


        // Initialize FrameStreamingThread
        LaunchedEffect(Unit) {
            frameStreamingThread = FrameStreamingThread { colorBuf, depthBuf, width, height ->
                try {
                    bitmap?.recycle()
                    val bmp = depthByteBufferToBitmap(depthBuf, width, height)
                    scope.launch(Dispatchers.Main.immediate) {
                        bitmap = bmp.first
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Frame processing error: ${e.message}")
                }
            }
        }
    }
}