package me.rpgz.treetools.permissions

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.intel.realsense.librealsense.RsContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.rpgz.treetools.utils.ReceiverFlagContext


/**
 * After initialized, when a new usb device is attached, fist check whether the usb device is a target device.
 * if the usb device is a target device, first check whether we have camera permission
 * if we do not have camera permission, request the camera permission first
 *      - if the user grant the camera permission, go on and request the target device usb permission
 *      - else, toast camera permission denied
 */
class RealSensePermission(
    private val context: Context,
    private val activity: Activity,
    private val vendorId: Int,
    private val productId: Int
) {

    companion object {
        private const val ACTION_USB_PERMISSION = "me.zpgz.treetools.USB_PERMISSION"
    }
    
    // RsContext singleton initialization
    private object RsInitOnce {
        @Volatile private var done = false
        fun ensureInit(appCtx: Context) {
            if (!done) synchronized(this) {
                if (!done) {
                    val wrapped = ReceiverFlagContext(appCtx as Application)
                    RsContext.init(wrapped)
                    done = true
                }
            }
        }
    }
    
    private val cameraPermission = CameraPermission(context)
    
    private lateinit var usbManager: UsbManager
    @Volatile private var requesting = false
    private var onPermissionGranted: ((UsbDevice) -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    private var onDeviceAttached: ((UsbDevice) -> Unit)? = null
    private var onDeviceDetached: (() -> Unit)? = null
    private var pendingUsbDevice: UsbDevice? = null
    private var onRsContextReady: ((RsContext) -> Unit)? = null
    private var lastProcessedDevice: UsbDevice? = null
    private val _rsContextFlow = MutableStateFlow<RsContext?>(null)
    val rsContextFlow: StateFlow<RsContext?> = _rsContextFlow.asStateFlow()
    private var rsContext: RsContext?
        get() = _rsContextFlow.value
        set(value) {
            _rsContextFlow.value = value
        }
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("USB", "new intent $intent")
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (device != null && granted) {
                        initRsContextAsync {
                            onPermissionGranted?.invoke(device)
                        }
                    } else {
                        Toast.makeText(context, "USB 权限被拒绝", Toast.LENGTH_SHORT).show()
                        onPermissionDenied?.invoke()
                    }
                    requesting = false
                }
                
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        ?.let { device ->
                            Log.i("USB", "Device attached: ${device.deviceName}")
                            
                            // Prevent processing the same device multiple times
                            if (lastProcessedDevice?.deviceName == device.deviceName) {
                                Log.i("USB", "Device already processed, ignoring duplicate attachment")
                                return@let
                            }
                            
                            lastProcessedDevice = device
                            onDeviceAttached?.invoke(device)
                            
                            if (isTargetDevice(device)) {
                                Log.i("USB", "Target device detected: ${device.deviceName}")
                                if (cameraPermission.isCameraPermissionGranted()) {
                                    requestPermission(device)
                                } else {
                                    Log.i("USB", "Camera permission needed, storing pending device")
                                    pendingUsbDevice = device
                                    cameraPermission.requestCameraPermission(activity)
                                }
                            }
                        }
                }
                
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.i("USB", "Device detached")
                    lastProcessedDevice = null
                    pendingUsbDevice = null
                    requesting = false
                    onDeviceDetached?.invoke()
                }
            }
        }
    }
    
    fun initialize() {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbReceiver, filter)
        }
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("RealSensePermission", "Receiver not registered")
        }
        
        // Clean up RsContext
        rsContext?.let { ctx ->
            try {
                ctx.close()
            } catch (e: Exception) {
                Log.w("RealSensePermission", "Failed to close RsContext", e)
            }
        }
        rsContext = null
    }
    
    fun setCallbacks(
        onGranted: ((UsbDevice) -> Unit)? = null,
        onDenied: (() -> Unit)? = null,
        onAttached: ((UsbDevice) -> Unit)? = null,
        onDetached: (() -> Unit)? = null,
        onRsReady: ((RsContext) -> Unit)? = null
    ) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied
        onDeviceAttached = onAttached
        onDeviceDetached = onDetached
        onRsContextReady = onRsReady
    }
    
    fun requestPermission(device: UsbDevice) {
        Log.i("USB", "requestPermission ${device}, requesting=$requesting")
        
        // Prevent double requests
        if (requesting) {
            Log.w("USB", "Permission request already in progress, ignoring")
            return
        }
        
        if (!cameraPermission.isCameraPermissionGranted()) {
            Log.i("USB", "Camera permission not granted, requesting camera permission first")
            pendingUsbDevice = device
            cameraPermission.requestCameraPermission(activity)
            return
        }
        
        if (hasPermission(device)) {
            Log.i("USB", "USB permission already granted")
            Toast.makeText(context, "Realsense相机已连接", Toast.LENGTH_SHORT).show()
            initRsContextAsync {
                onPermissionGranted?.invoke(device)
            }
            return
        }

        Log.i("USB", "Requesting USB permission for device: ${device.deviceName}")
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        requesting = true
        usbManager.requestPermission(device, permissionIntent)
    }
    
    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }
    
    fun findTargetDevice(): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull {
            it.vendorId == vendorId && it.productId == productId
        }
    }
    
    fun getAllUsbDevices(): Collection<UsbDevice> {
        return usbManager.deviceList.values
    }
    
    fun isTargetDevice(device: UsbDevice): Boolean {
        return device.vendorId == vendorId && device.productId == productId
    }
    
    fun isRequesting(): Boolean = requesting
    
    fun isCameraPermissionGranted(): Boolean {
        return cameraPermission.isCameraPermissionGranted()
    }
    
    fun getCameraPermission(): CameraPermission {
        return cameraPermission
    }
    
    
    fun onCameraPermissionGranted() {
        Log.i("USB", "Camera permission granted, proceeding with USB permission")
        pendingUsbDevice?.let { device ->
            requestPermission(device)
            pendingUsbDevice = null
        }
    }
    
    fun onCameraPermissionDenied() {
        Log.i("USB", "Camera permission denied")
        pendingUsbDevice?.let {
            Toast.makeText(context, "相机权限被拒绝，无法使用USB设备", Toast.LENGTH_LONG).show()
            onPermissionDenied?.invoke()
            pendingUsbDevice = null
        }
        requesting = false // Reset requesting flag when denied
    }
    
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)) {
            onCameraPermissionGranted()
        } else {
            if (requestCode == CameraPermission.CAMERA_PERMISSION_REQUEST_CODE) {
                onCameraPermissionDenied()
            }
        }
    }
    
    private fun initRsContextAsync(onComplete: () -> Unit = {}) {
        if (rsContext != null) {
            onRsContextReady?.invoke(rsContext!!)
            onComplete()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize RsContext.init if not done
                RsInitOnce.ensureInit(context)
                
                // Create RsContext instance
                val newRsContext = RsContext()
                rsContext = newRsContext
                
                CoroutineScope(Dispatchers.Main).launch {
                    onRsContextReady?.invoke(newRsContext)
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("RealSensePermission", "Failed to initialize RsContext", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "RealSense 初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }
}
