package me.rpgz.treetools.pages

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.components.BitmapLineSegmentPicker
import me.rpgz.treetools.components.LineSegmentPoints
import me.rpgz.treetools.components.ScreenTile
import me.rpgz.treetools.ml.colorByteBufferToBitmap
import me.rpgz.treetools.ml.depthByteBufferToBitmap
import me.rpgz.treetools.realsense.FrameStreamingThread
import me.rpgz.treetools.realsense.RealSenseIntrinsic
import me.rpgz.treetools.viewmodels.RealSenseManagementPageViewModel
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "RealSenseMeasurementPage"

fun deproject(u: Float, v: Float, zMeters: Float, K: RealSenseIntrinsic): FloatArray {
    // 返回 [X, Y, Z]，单位米
    val x = (u - K.ppx) / K.fx * zMeters
    val y = (v - K.ppy) / K.fy * zMeters
    return floatArrayOf(x, y, zMeters)
}

@Composable
fun RealSenseMeasurementPage(viewModel: RealSenseManagementPageViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        val scope = rememberCoroutineScope()
        var frameStreamingThread by remember { mutableStateOf<FrameStreamingThread?>(null) }
        var isStreaming by remember { mutableStateOf(false) }
        var isPickingLine by remember { mutableStateOf(false) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var depthMap by remember { mutableStateOf<Array<IntArray>?>(null) }
        var colorBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var realSenseIntrinsic by remember { mutableStateOf<RealSenseIntrinsic?>(null) } // 相机内参
        var paused by remember { mutableStateOf(false) } // 相机内参
        val rsContext by AppContainer.instance.realSensePermission.rsContextFlow.collectAsStateWithLifecycle()
        var lineSegmentPoints by remember { mutableStateOf<LineSegmentPoints?>(null) } // 相机内参
        var distance by remember { mutableStateOf<Double?>(null) } // 相机内参
        if (rsContext == null) {
            Text("未检测到RealSense相机接入")
            return
        }

        DisposableEffect(Unit) {
            onDispose {
                frameStreamingThread?.stopAsync {
                    Log.d("RealsenseTest", "Frame streaming thread cleanup completed")
                }
                frameStreamingThread = null
                isStreaming = false
            }
        }

        ScreenTile("测距")

        LaunchedEffect(Unit) {
            frameStreamingThread = FrameStreamingThread { colorBuf, depthBuf, width, height ->
                try {
                    val colorBmp = colorByteBufferToBitmap(colorBuf, width, height)
                    val depthPair = depthByteBufferToBitmap(depthBuf, width, height)
                    scope.launch(Dispatchers.Main.immediate) {
                        bitmap = depthPair.first
                        depthMap = depthPair.second
                        colorBitmap = colorBmp
                        if (realSenseIntrinsic == null) {
                            realSenseIntrinsic = frameStreamingThread?.getRealSenseIntrinsic()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Frame processing error: ${e.message}")
                }
            }
        }

        Box(modifier = Modifier.height(12.dp))

        if (!isStreaming) {
            Button(onClick = {
                frameStreamingThread?.start()
                isStreaming = true
                paused = false
            }) {
                Text("开始取流")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(0.5F)
                )
            }
            colorBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(1F)
                )
            }
        }

        Box(modifier = Modifier.height(12.dp))

        if (isStreaming) {

            Button(onClick = {
                if (paused) {
                    frameStreamingThread?.resume()
                    isPickingLine = false
                    paused = false
                } else {
                    frameStreamingThread?.pause()
                    paused = true
                }
            }) {
                Text(if (!paused) "暂停" else "恢复")
            }

            Box(modifier = Modifier.height(12.dp))
        }

        if (colorBitmap != null && depthMap != null && !isPickingLine) {
            Button(onClick = {
                frameStreamingThread?.pause()
                paused = true
                isPickingLine = true
            }) {
                Text("开始选点")
            }
        }

        if (isPickingLine && colorBitmap != null) {
            BitmapLineSegmentPicker(
                colorBitmap!!, {
                    lineSegmentPoints = it
                }, modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("(${lineSegmentPoints?.startPoint?.x?.toInt()}, ${lineSegmentPoints?.startPoint?.y?.toInt()})  (${lineSegmentPoints?.endPoint?.x?.toInt()}, ${lineSegmentPoints?.endPoint?.y?.toInt()})")
                Button(onClick = {
                    val x1 = lineSegmentPoints?.startPoint?.x?.toInt()
                    val y1 = lineSegmentPoints?.startPoint?.y?.toInt()

                    val x2 = lineSegmentPoints?.endPoint?.x?.toInt()
                    val y2 = lineSegmentPoints?.endPoint?.y?.toInt()

                    if (x1 != null && x2 != null && y1 != null && y2 != null && realSenseIntrinsic != null && depthMap != null) {
                        val p1 = deproject(
                            x1.toFloat(),
                            y1.toFloat(),
                            depthMap!![y1][x1].toFloat() / 1000,
                            realSenseIntrinsic!!
                        )
                        val p2 = deproject(
                            x2.toFloat(),
                            y2.toFloat(),
                            depthMap!![y2][x2].toFloat() / 1000,
                            realSenseIntrinsic!!
                        )

                        distance = sqrt(
                            (p1[0] - p2[0]).toDouble().pow(2) +
                                    (p1[1] - p2[1]).toDouble().pow(2) +
                                    (p1[2] - p2[2]).toDouble().pow(2)
                        )
                    }
                }) {
                    Text("计算距离")
                }
                if (distance != null) {
                    Text(
                        text = "${DecimalFormat("#.##").format(distance)}米", style = TextStyle(
                            fontSize = 24.sp
                        )
                    )
                }
            }


        }
    }
}
