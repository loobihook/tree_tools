package me.rpgz.treetools

import android.app.Activity
import android.app.Application
import androidx.annotation.MainThread
import me.rpgz.treetools.ble.BleManager
import me.rpgz.treetools.ble.Hm10Manager
import me.rpgz.treetools.ble.SensorManager
import me.rpgz.treetools.ble.SensorMode
import me.rpgz.treetools.ble.SimulatedSensorManager
import me.rpgz.treetools.permissions.RealSensePermission
import me.rpgz.treetools.ml.Infer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels

data class AppConfig(
    val modelBytes: ByteBuffer? = null,
    val sensorMode: SensorMode = SensorMode.REAL,
)

class AppContainer private constructor(
    val app: Application,
    activity: Activity,
    val config: AppConfig
) {
    val realSensePermission = RealSensePermission(
        context = app,
        activity = activity,
        vendorId = 32902,
        productId = 2908
    ).apply {
        initialize()
    }

    // 这个只用于处理蓝牙权限
    val bleManager = BleManager(
        context = app,
        activity = activity
    ).apply {
        initialize()
    }

    val sensorManager: SensorManager

    private val realHm10Manager: Hm10Manager?

    init {
        if (config.sensorMode == SensorMode.REAL) {
            val manager = Hm10Manager(context = app, activity = activity)
            realHm10Manager = manager
            sensorManager = manager
        } else {
            realHm10Manager = null
            sensorManager = SimulatedSensorManager()
        }
    }

    val hm10Manager: Hm10Manager
        get() = realHm10Manager ?: error("Hm10Manager not available in simulated mode")

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        realHm10Manager?.handlePermissionResult(requestCode, permissions, grantResults)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        realHm10Manager?.handleActivityResult(requestCode, resultCode)
    }

    val infer: Infer by lazy {
        val modelBytes = config.modelBytes ?: loadModelFromAssets()
        Infer(modelBytes)
    }

    private fun loadModelFromAssets(): ByteBuffer {
        val inputStream: InputStream = app.assets.open("resnet50d_1_224_224_3_20250911.tflite")

        val size = inputStream.available()
        val buffer = ByteBuffer.allocateDirect(size)
        val channel = Channels.newChannel(inputStream)
        while (buffer.hasRemaining()) {
            channel.read(buffer)
        }
        buffer.flip()
        inputStream.close()
        return buffer
    }

    companion object {
        @Volatile private var _instance: AppContainer? = null

        val instance: AppContainer
            get() = _instance ?: error("AppContainer is not initialized. Call AppContainer.init(...) first.")

        /** 线程安全的一次性初始化 */
        @MainThread
        fun init(app: Application,activity: Activity,  config: AppConfig = AppConfig()): AppContainer {
            _instance?.let { return it }
            return synchronized(this) {
                _instance ?: AppContainer(app,activity, config).also { _instance = it }
            }
        }
    }

    fun onDestroy() {
        realSensePermission.unregister()
        bleManager.cleanup()
        sensorManager.cleanup()
    }
}