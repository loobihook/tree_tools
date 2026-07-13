package me.rpgz.treetools.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intel.realsense.librealsense.DeviceListener
import me.rpgz.treetools.AppContainer



@Composable
fun RealSenseDeviceStatus(
    modifier: Modifier = Modifier,
    onAttach: () -> Unit = {},
    onDetach: () -> Unit = {}
) {
    val onAttachState by rememberUpdatedState(onAttach)
    val onDetachState by rememberUpdatedState(onDetach)

    var status by remember { mutableStateOf("Waiting for permissions…") }
    val rsContext by AppContainer.instance.realSensePermission.rsContextFlow.collectAsStateWithLifecycle()


    // Update status based on RsContext availability
    LaunchedEffect(rsContext) {
        status = if (rsContext != null) {
            "RsContext ready"
        } else {
            "Waiting for permissions…"
        }
    }

    // Register device listener
    DisposableEffect(rsContext) {
        val rs = rsContext
        if (rs == null) {
            onDispose { }
        } else {
            val listener = object : DeviceListener {
                override fun onDeviceAttach() {
                    status = "Device attached"
                    onAttachState()
                }

                override fun onDeviceDetach() {
                    status = "Device detached"
                    onDetachState()
                }
            }
            rs.setDevicesChangedCallback(listener)

            onDispose {
                rs.removeDevicesChangedCallback()
            }
        }
    }

    // UI
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "RealSense Status", style = MaterialTheme.typography.titleMedium)
        Text(text = status, style = MaterialTheme.typography.bodyMedium)
        
        if (rsContext == null) {
            Text(
                text = "RsContext will be initialized after camera and USB permissions are granted",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
