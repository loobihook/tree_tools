package me.rpgz.treetools.permissions

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BlePermission(private val context: Context) {
    
    companion object {
        const val BLE_PERMISSION_REQUEST_CODE = 200
        
        // Android 12+ requires new permissions
        val BLE_PERMISSIONS_API_31_PLUS = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        // Pre Android 12 permissions
        val BLE_PERMISSIONS_PRE_API_31 = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLE_PERMISSIONS_API_31_PLUS
        } else {
            BLE_PERMISSIONS_PRE_API_31
        }
    }
    
    fun areAllPermissionsGranted(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun getMissingPermissions(): Array<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    fun isBluetoothSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            val mode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }
    
    fun requestBlePermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions,
                BLE_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    fun requestEnableBluetooth(activity: Activity, requestCode: Int = 1001) {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, requestCode)
        }
    }
    
    fun requestEnableLocation() {
        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return getRequiredPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    fun getPermissionStatus(): BlePermissionStatus {
        return when {
            !isBluetoothSupported() -> BlePermissionStatus.BLUETOOTH_NOT_SUPPORTED
            !areAllPermissionsGranted() -> BlePermissionStatus.PERMISSIONS_DENIED
            !isBluetoothEnabled() -> BlePermissionStatus.BLUETOOTH_DISABLED
            !isLocationEnabled() -> BlePermissionStatus.LOCATION_DISABLED
            else -> BlePermissionStatus.ALL_READY
        }
    }
    
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        return if (requestCode == BLE_PERMISSION_REQUEST_CODE) {
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        } else {
            false
        }
    }
    
    fun canStartScanning(): Boolean {
        return isBluetoothSupported() && 
               areAllPermissionsGranted() && 
               isBluetoothEnabled() && 
               isLocationEnabled()
    }
    
    enum class BlePermissionStatus {
        ALL_READY,
        BLUETOOTH_NOT_SUPPORTED,
        PERMISSIONS_DENIED,
        BLUETOOTH_DISABLED,
        LOCATION_DISABLED
    }
}