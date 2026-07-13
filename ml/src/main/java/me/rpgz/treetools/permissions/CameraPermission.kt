package me.rpgz.treetools.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraPermission(private val context: Context) {
    
    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
    
    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun requestCameraPermission(activity: Activity) {
        if (!isCameraPermissionGranted()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
    }
    
    fun canRequestPermission(): Boolean {
        return !isCameraPermissionGranted()
    }
    
    fun getPermissionStatus(): PermissionStatus {
        return when {
            isCameraPermissionGranted() -> PermissionStatus.GRANTED
            else -> PermissionStatus.DENIED
        }
    }
    
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        return if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                false
            }
        } else {
            false
        }
    }
    
    enum class PermissionStatus {
        GRANTED,
        DENIED
    }
}