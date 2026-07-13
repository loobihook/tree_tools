package me.rpgz.treetools.realsense

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.intel.realsense.librealsense.GLRsSurfaceView

@Composable
fun RealSenseSurfaceView(
    modifier: Modifier = Modifier,
    onViewReady: (GLRsSurfaceView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GLRsSurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            // This runs on every recomposition
            onViewReady(view)
        }
    )
}
