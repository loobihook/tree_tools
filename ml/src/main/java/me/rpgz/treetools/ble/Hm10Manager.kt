package me.rpgz.treetools.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import me.rpgz.treetools.permissions.BlePermission
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * HM-10 BLE管理器，具有自动发现和连接功能
 * 基于提供的 HM-10 配置
 */
@SuppressLint("MissingPermission")
class Hm10Manager(
    private val context: Context,
    private val activity: Activity
) : SensorManager {

    companion object {
        private const val TAG = "Hm10Manager"

        // HM-10 Configuration
        private const val STANDARD_SERVICE = "FFE0"
        private const val STANDARD_CHARACTERISTIC = "FFE1"
        private const val STANDARD_SERVICE_128 = "0000ffe0-0000-1000-8000-00805f9b34fb"
        private const val STANDARD_CHARACTERISTIC_128 = "0000ffe1-0000-1000-8000-00805f9b34fb"

        const val SCAN_TIMEOUT = 15000L

        // Device name patterns
        private val DEVICE_NAME_PATTERNS = listOf(
            Regex("HM", RegexOption.IGNORE_CASE),
            Regex("HM-10", RegexOption.IGNORE_CASE),
            Regex("HM10", RegexOption.IGNORE_CASE),
            Regex("fHMSot", RegexOption.IGNORE_CASE)
        )

        // Start/Stop listening commands
        private const val START_LISTENING_COMMAND = "a"
        private const val STOP_LISTENING_COMMAND = "b"
    }

    private val blePermission = BlePermission(context)
    private lateinit var bleScanner: BleScanner
    private lateinit var bleConnection: BleConnection
    private lateinit var scope: CoroutineScope
    // State flows
    private val _deviceStatus = MutableStateFlow("Searching...")
    override val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    override val receivedMessages: StateFlow<List<String>> = _receivedMessages.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    override val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    private var currentDevice: BleDevice? = null
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    data class SensorData(
        val soilTemperature: Double, // 土壤温度
        val soilMoisture: Double,    // 土壤湿度
        val soilMoisturePH: Double,  // 土壤 PH
        val soilConductivity: Double,// 土壤电导率
        val humidity: Double,        // 环境湿度
        val temperature: Double,     // 环境温度
        val atmosphericPressure: Double, // ⼤⽓压⼒
        val ambientCarbonDioxideContent: Double, //环境⼆氧化碳含量
        val windSpeed: Double,       // 风速
    )

    init {
        initializeManager()
    }

    fun initializeManager() {
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        bleScanner = BleScanner(context)
        bleConnection = BleConnection(context)
        observeConnectionState()
        observeDataReceived()
        startAutoDiscovery()
    }

    private fun observeConnectionState() {
        scope.launch {
            bleConnection.connectionState.collect { state ->
                _isConnected.value = state.isConnected
                when {
                    state.isConnecting -> _deviceStatus.value = "Connecting..."
                    state.isConnected -> {
                        _deviceStatus.value = "Connected to ${state.device?.name ?: "HM-10"}"
                        // Auto-enable notifications and start listening
                        scope.launch {
                            delay(1500) // Wait longer for service discovery
                            enableNotifications()
                            delay(1000) // Wait for notifications to be enabled
                            autoStartListening()
                        }
                    }

                    state.error != null -> _deviceStatus.value = "Error: ${state.error}"
                    !state.isConnected -> _deviceStatus.value = "Searching..."
                }
            }
        }
    }

    private fun observeDataReceived() {
        scope.launch {
            bleConnection.dataReceived.collect { data ->
                data?.let {
                    Log.i(TAG, "🔥 Hm10Manager: Data received from BleConnection")
                    val timestamp = timeFormatter.format(Date(it.timestamp))
                    val dataString = String(it.data).trim()
                    // 改为16进制解码
                    val hexString = it.data.joinToString("") { byte -> "%02x".format(byte) }
                    Log.i(TAG, "🔥 Raw hex data: $hexString")
                    // 解析16进制数据
                    val parsedData = parseHexData(hexString)

                    // Update structured sensor data StateFlow
                    _sensorData.value = parsedData

                    val message = if (parsedData != null) {
                        "[$timestamp] " +
                                "土壤温度:${parsedData.soilTemperature}°C " +
                                "土壤湿度:${parsedData.soilMoisture}% " +
                                "电导率:${parsedData.soilConductivity}μS/cm " +
                                "PH:${parsedData.soilMoisturePH} " +
                                "环境温度:${parsedData.temperature}°C " +
                                "环境湿度:${parsedData.humidity}% " +
                                "大气压:${parsedData.atmosphericPressure}kPa " +
                                "CO2:${parsedData.ambientCarbonDioxideContent}ppm " +
                                "风速:${parsedData.windSpeed}m/s"
                    } else {
                        "[$timestamp] 接收(HEX): $hexString"
                    }

                    Log.i(TAG, "🔥 Adding message to UI: $message")

                    val currentMessages = _receivedMessages.value.toMutableList()
                    currentMessages.add(0, message)
                    if (currentMessages.size > 50) {
                        currentMessages.removeAt(currentMessages.size - 1)
                    }
                    _receivedMessages.value = currentMessages

                    Log.i(
                        TAG,
                        "🔥 Updated receivedMessages StateFlow, total: ${currentMessages.size}"
                    )
                }
            }
        }
    }

    private fun parseHexData(hexString: String): SensorData? {
        return try {
            // 数据格式：每个值占2个字节（4个16进制字符）
            // 顺序：土壤温度、土壤湿度、电导率、PH值、环境温度、环境湿度、大气压、二氧化碳含量、风速
            if (hexString.length >= 36) { // 需要36个字符（18字节）
                val soilTemperature = hexString.substring(0, 4).toInt(16) / 10.0
                val soilMoisture = hexString.substring(4, 8).toInt(16) / 10.0
                val soilConductivity = hexString.substring(8,12).toInt(16) /1.0
                val soilMoisturePH = hexString.substring(12,16).toInt(16)/10.0
                val temperature = hexString.substring(16, 20).toInt(16) / 10.0
                val humidity = (hexString.substring(20, 24).toInt(16) / 10.0) + 40
                val atmosphericPressure = hexString.substring(24, 28).toInt(16) / 10.0
                val ambientCarbonDioxideContent = hexString.substring(28, 32).toInt(16) / 1.0
                val windSpeed = hexString.substring(32,36).toInt(16) / 1.0
                SensorData(
                    soilTemperature = soilTemperature,
                    soilMoisture = soilMoisture,
                    soilConductivity = soilConductivity,
                    soilMoisturePH =soilMoisturePH,
                    humidity = humidity.coerceAtMost(95.0),
                    temperature = temperature,
                    atmosphericPressure = atmosphericPressure,
                    ambientCarbonDioxideContent = ambientCarbonDioxideContent,
                    windSpeed = windSpeed,
                )
            } else {
                Log.w(TAG, "Hex data too short: $hexString")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse hex data: $hexString", e)
            null
        }
    }

    private fun startAutoDiscovery() {
        if (!initialize()) {
            Log.e(TAG, "Failed to initialize BLE")
            return
        }

        _deviceStatus.value = "Initializing BLE..."

        // Wait for BLE to be ready, then start auto-discovery
        scope.launch {
            delay(1000) // Give time for initialization
            if (blePermission.getPermissionStatus() == BlePermission.BlePermissionStatus.ALL_READY) {
                autoDiscoverHm10()
            } else {
                _deviceStatus.value = "BLE permissions required"
            }
        }
    }

    private fun initialize(): Boolean {
        if (!blePermission.isBluetoothSupported()) {
            _deviceStatus.value = "BLE not supported"
            return false
        }

        when (blePermission.getPermissionStatus()) {
            BlePermission.BlePermissionStatus.ALL_READY -> {
                return true
            }

            BlePermission.BlePermissionStatus.PERMISSIONS_DENIED -> {
                blePermission.requestBlePermissions(activity)
                return false
            }

            BlePermission.BlePermissionStatus.BLUETOOTH_DISABLED -> {
                blePermission.requestEnableBluetooth(activity)
                return false
            }

            BlePermission.BlePermissionStatus.LOCATION_DISABLED -> {
                blePermission.requestEnableLocation()
                return false
            }

            else -> {
                _deviceStatus.value = "BLE not available"
                return false
            }
        }
    }

    private fun autoDiscoverHm10() {
        // 检查权限
        if (!blePermission.canStartScanning()) {
            _deviceStatus.value = "Cannot scan - check permissions"
            return
        }

        _deviceStatus.value = "Scanning for HM-10..."
        Log.d(TAG, "Starting scan for HM-10 devices")

        // 扫描所有 BLE 设备，不使用过滤
        val scanStarted = bleScanner.startScan(
            scanPeriod = SCAN_TIMEOUT,
            serviceUuids = null,
            deviceName = null
        )

        if (!scanStarted) {
            _deviceStatus.value = "Failed to start scanning"
            return
        }
        Log.i(TAG, "scanStarted")
        // 监控扫描结果并自动连接
        scope.launch {
            var deviceFound = false

            bleScanner.discoveredDevices
                .takeWhile { !deviceFound && !_isConnected.value }
                .collect { devices ->

                    Log.i(TAG, "devices: ${devices.joinToString(", ") { it.name.toString() }}")
                    // 查找 HM-10 设备（通过名称模式匹配）
                    val hm10Device = devices.find { device ->
                        device.name?.let { name ->
                            // 检查设备名称是否匹配 HM-10 模式
                            name.contains("HM", ignoreCase = true) ||
                                    name.contains("HMSoft", ignoreCase = true)
//                                    ||  name.contains("fHMSot", ignoreCase = true)
//                                    || name == "BT05" ||  // 一些 HM-10 克隆版本
//                                    name == "MLT-BT05"  // 另一种常见变体
                        } ?: false
                    }

                    // 找到设备，停止扫描并连接
                    if (hm10Device != null) {
                        deviceFound = true
                        currentDevice = hm10Device
                        bleScanner.stopScan()

                        Log.d(TAG, "Found HM-10: ${hm10Device.name} (${hm10Device.address})")
                        _deviceStatus.value = "Connecting to ${hm10Device.name}..."

                        // 连接设备
                        val connected = bleConnection.connect(hm10Device.device)

                        if (connected) {
                            // 连接成功，等待服务发现后启用通知
                            delay(2000) // 等待服务发现完成

                            // 启用通知
                            val notifyEnabled = bleConnection.enableNotifications(
                                STANDARD_SERVICE_128,
                                STANDARD_CHARACTERISTIC_128
                            )

                            if (notifyEnabled) {
                                Log.d(TAG, "HM-10 ready for communication")
                                _deviceStatus.value = "Connected to ${hm10Device.name}"

                                // 自动开始监听数据
                                delay(500)
                                startListening()
                            } else {
                                Log.e(TAG, "Failed to enable notifications")
                                _deviceStatus.value = "Connected but notifications failed"
                            }
                        } else {
                            _deviceStatus.value = "Connection failed"
                            deviceFound = false // 允许继续扫描
                        }
                    }
                }

            // 扫描超时后自动重试
            if (!deviceFound && !_isConnected.value) {
                delay(2000)
                Log.d(TAG, "No HM-10 found, retrying...")
                autoDiscoverHm10() // 递归重试
            }
        }
    }

    private fun enableNotifications() {
        scope.launch {
            // Wait for service discovery
            delay(1000)

            Log.d(TAG, "Attempting to enable notifications for HM-10...")
            val success =
                bleConnection.enableNotifications(STANDARD_SERVICE_128, STANDARD_CHARACTERISTIC_128)

            if (success) {
                Log.d(TAG, "Successfully enabled notifications for HM-10")
            } else {
                Log.e(TAG, "Failed to enable notifications for HM-10")
            }
        }
    }

    /**
     * Automatically start listening after connection established
     */
    private fun autoStartListening() {
        Log.d(TAG, "Auto-starting data listening...")
        startListening()
    }

    /**
     * Start listening for data by sending 'a' command
     */
    override fun startListening() {
        if (!_isConnected.value) {
            Log.w(TAG, "Not connected to device")
            return
        }

        scope.launch {
            try {
                _isListening.value = true

                // Small delay before sending command
                delay(500)

                var retryCount = 0
                val maxRetries = 3

                while (retryCount < maxRetries) {
                    val success = bleConnection.writeCharacteristic(
                        STANDARD_SERVICE_128,
                        STANDARD_CHARACTERISTIC_128,
                        START_LISTENING_COMMAND.toByteArray(Charsets.US_ASCII)
                    )

                    if (success) {
                        Log.d(TAG, "Start listening command sent")
                        break
                    } else {
                        retryCount++
                        if (retryCount < maxRetries) {
                            delay(1000)
                        }
                    }
                }

                if (retryCount >= maxRetries) {
                    Log.e(TAG, "Failed to send start listening command after $maxRetries attempts")
                    _isListening.value = false
                } else {
                    Log.i(TAG, "Start listening command sent successfully - waiting for data...")
                    _deviceStatus.value = "Listening for data..."
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error starting listening", e)
                _isListening.value = false
            }
        }
    }

    /**
     * Stop listening for data by sending 'b' command
     */
    override fun stopListening() {
        if (!_isConnected.value) {
            Log.w(TAG, "Not connected to device")
            return
        }

        scope.launch {
            try {
                _isListening.value = false

                // Small delay before sending command
                delay(500)

                var retryCount = 0
                val maxRetries = 3

                while (retryCount < maxRetries) {
                    val success = bleConnection.writeCharacteristic(
                        STANDARD_SERVICE_128,
                        STANDARD_CHARACTERISTIC_128,
                        STOP_LISTENING_COMMAND.toByteArray(Charsets.US_ASCII)
                    )

                    if (success) {
                        Log.d(TAG, "Stop listening command sent")
                        break
                    } else {
                        retryCount++
                        if (retryCount < maxRetries) {
                            delay(1000)
                        }
                    }
                }

                if (retryCount >= maxRetries) {
                    Log.e(TAG, "Failed to send stop listening command after $maxRetries attempts")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listening", e)
            }
        }
    }

    /**
     * Disconnect from current device
     */
    override fun disconnect() {
        bleConnection.disconnect()
        currentDevice = null
        _isListening.value = false
        _receivedMessages.value = emptyList()
    }

    /**
     * Clear received messages
     */
    override fun clearMessages() {
        _receivedMessages.value = emptyList()
    }

    /**
     * Handle permission results
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        val granted = blePermission.handlePermissionResult(requestCode, permissions, grantResults)
        if (granted) {
            scope.launch {
                delay(500)
                autoDiscoverHm10()
            }
        }
        return granted
    }

    /**
     * Handle activity results
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == 1001) { // Bluetooth enable request
            scope.launch {
                delay(500)
                autoDiscoverHm10()
            }
        }
    }

    /**
     * Clean up resources
     */
    override fun cleanup() {
        scope.cancel()
        bleScanner.cleanup()
        bleConnection.close()
        currentDevice = null
        _receivedMessages.value = emptyList()
        _isListening.value = false
        _isConnected.value = false
        _deviceStatus.value = "Cleaned up"
    }

    override fun reset() {
        cleanup()
        initializeManager()
    }
}