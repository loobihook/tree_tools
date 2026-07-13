package me.rpgz.treetools.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.*
import me.rpgz.treetools.permissions.BlePermission

/**
 * BLE Manager that handles scanning, connecting, and communicating with BLE devices
 * Integrates permission management, scanning, and connection functionality
 */
@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context,
    private val activity: Activity
) {
    
    companion object {
        private const val TAG = "BleManager"
    }
    
    private val blePermission = BlePermission(context)
    private val bleScanner = BleScanner(context)
    private val bleConnection = BleConnection(context)
    
    // Combined state flows
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _status = MutableStateFlow("Initializing...")
    val status: StateFlow<String> = _status.asStateFlow()
    
    // Permission states
    val permissionStatus: StateFlow<BlePermission.BlePermissionStatus> = 
        flow { emit(blePermission.getPermissionStatus()) }
            .stateIn(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = BlePermission.BlePermissionStatus.PERMISSIONS_DENIED
            )
    
    // Scanner states
    val isScanning: StateFlow<Boolean> = bleScanner.isScanning
    val discoveredDevices: StateFlow<Set<BleDevice>> = bleScanner.discoveredDevices
    val scanError: StateFlow<String?> = bleScanner.scanError
    
    // Connection states
    val connectionState: StateFlow<BleConnectionState> = bleConnection.connectionState
    val dataReceived: StateFlow<BleDataReceived?> = bleConnection.dataReceived
    val notificationHistory: StateFlow<List<BleDataReceived>> = bleConnection.notificationHistory
    
    init {
        updateReadyState()
    }
    
    /**
     * Initialize BLE functionality
     * Checks all prerequisites and requests permissions if needed
     */
    fun initialize(): Boolean {
        _status.value = "Checking BLE support..."
        
        if (!blePermission.isBluetoothSupported()) {
            _status.value = "BLE not supported on this device"
            return false
        }
        
        when (blePermission.getPermissionStatus()) {
            BlePermission.BlePermissionStatus.ALL_READY -> {
                _status.value = "BLE ready"
                _isReady.value = true
                return true
            }
            
            BlePermission.BlePermissionStatus.PERMISSIONS_DENIED -> {
                _status.value = "Requesting BLE permissions..."
                blePermission.requestBlePermissions(activity)
                return false
            }
            
            BlePermission.BlePermissionStatus.BLUETOOTH_DISABLED -> {
                _status.value = "Requesting Bluetooth enable..."
                blePermission.requestEnableBluetooth(activity)
                return false
            }
            
            BlePermission.BlePermissionStatus.LOCATION_DISABLED -> {
                _status.value = "Location services required for BLE scanning"
                blePermission.requestEnableLocation()
                return false
            }
            
            BlePermission.BlePermissionStatus.BLUETOOTH_NOT_SUPPORTED -> {
                _status.value = "Bluetooth not supported on this device"
                return false
            }
        }
    }
    
    /**
     * Handle permission request results
     * Call this from Activity.onRequestPermissionsResult()
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        val granted = blePermission.handlePermissionResult(requestCode, permissions, grantResults)
        if (granted) {
            updateReadyState()
            _status.value = if (_isReady.value) "BLE ready" else "Checking BLE status..."
        } else {
            _status.value = "BLE permissions denied"
        }
        return granted
    }
    
    /**
     * Handle activity results (e.g., Bluetooth enable request)
     * Call this from Activity.onActivityResult()
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        // Handle Bluetooth enable result
        if (requestCode == 1001) { // Default Bluetooth enable request code
            updateReadyState()
            _status.value = if (_isReady.value) "BLE ready" else "Bluetooth required for BLE functionality"
        }
    }
    
    /**
     * Start scanning for BLE devices
     * @param scanPeriod How long to scan in milliseconds (default 10 seconds)
     * @param serviceUuids Optional list of service UUIDs to filter for
     * @param deviceName Optional device name to filter for
     */
    fun startScan(
        scanPeriod: Long = 10000L,
        serviceUuids: List<ParcelUuid>? = null,
        deviceName: String? = null
    ): Boolean {
        if (!_isReady.value) {
            Log.e(TAG, "BLE not ready for scanning")
            return false
        }
        
        if (!blePermission.canStartScanning()) {
            Log.e(TAG, "Cannot start scanning - missing permissions or Bluetooth/Location disabled")
            return false
        }
        
        _status.value = "Scanning for devices..."
        return bleScanner.startScan(scanPeriod, serviceUuids, deviceName)
    }
    
    /**
     * Stop BLE scanning
     */
    fun stopScan() {
        bleScanner.stopScan()
        _status.value = "Scan stopped"
    }
    
    /**
     * Clear scan results
     */
    fun clearScanResults() {
        bleScanner.clearResults()
    }
    
    /**
     * Connect to a BLE device
     * @param device The BLE device to connect to
     */
    fun connect(device: BluetoothDevice): Boolean {
        if (!_isReady.value) {
            Log.e(TAG, "BLE not ready for connection")
            return false
        }
        
        _status.value = "Connecting to ${device.name ?: device.address}..."
        return bleConnection.connect(device)
    }
    
    /**
     * Connect to a BLE device by address
     * @param address The MAC address of the device to connect to
     */
    fun connectByAddress(address: String): Boolean {
        val device = bleScanner.findDeviceByAddress(address)?.device
        if (device == null) {
            Log.e(TAG, "Device not found: $address")
            return false
        }
        return connect(device)
    }
    
    /**
     * Disconnect from the current BLE device
     */
    fun disconnect() {
        bleConnection.disconnect()
        _status.value = "Disconnected"
    }
    
    /**
     * Write data to a characteristic
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     * @param data The data to write
     */
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray): Boolean {
        return bleConnection.writeCharacteristic(serviceUuid, characteristicUuid, data)
    }
    
    /**
     * Write string command to a characteristic (ASCII encoded)
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     * @param command The string command to write
     */
    fun writeCommand(serviceUuid: String, characteristicUuid: String, command: String): Boolean {
        Log.d(TAG, "Sending command: '$command' (${command.length} chars)")
        val data = command.toByteArray(Charsets.UTF_8) // UTF-8 is ASCII compatible
        Log.d(TAG, "Command bytes: ${bytesToHex(data)}")
        return writeCharacteristic(serviceUuid, characteristicUuid, data)
    }
    
    /**
     * Write ASCII string command to a characteristic (explicitly ASCII)
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     * @param command The string command to write
     */
    fun writeAsciiCommand(serviceUuid: String, characteristicUuid: String, command: String): Boolean {
        Log.d(TAG, "Sending ASCII command: '$command' (${command.length} chars)")
        val data = command.toByteArray(Charsets.US_ASCII)
        Log.d(TAG, "ASCII command bytes: ${bytesToHex(data)}")
        return writeCharacteristic(serviceUuid, characteristicUuid, data)
    }
    
    /**
     * Read data from a characteristic
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     */
    fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Boolean {
        return bleConnection.readCharacteristic(serviceUuid, characteristicUuid)
    }
    
    /**
     * Enable notifications for a characteristic
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     */
    fun enableNotifications(serviceUuid: String, characteristicUuid: String): Boolean {
        return bleConnection.enableNotifications(serviceUuid, characteristicUuid)
    }
    
    /**
     * Disable notifications for a characteristic
     * @param serviceUuid The service UUID
     * @param characteristicUuid The characteristic UUID
     */
    fun disableNotifications(serviceUuid: String, characteristicUuid: String): Boolean {
        return bleConnection.disableNotifications(serviceUuid, characteristicUuid)
    }
    
    /**
     * Get the current connection state
     */
    fun isConnected(): Boolean {
        return connectionState.value.isConnected
    }
    
    /**
     * Get the connected device
     */
    fun getConnectedDevice(): BluetoothDevice? {
        return connectionState.value.device
    }
    
    /**
     * Get available characteristics of the connected device
     */
    fun getCharacteristics(): List<BleCharacteristic> {
        return connectionState.value.characteristics
    }
    
    /**
     * Find a device by name from scan results
     */
    fun findDevicesByName(name: String): List<BleDevice> {
        return bleScanner.findDevicesByName(name)
    }
    
    /**
     * Find a device by address from scan results
     */
    fun findDeviceByAddress(address: String): BleDevice? {
        return bleScanner.findDeviceByAddress(address)
    }
    
    /**
     * Get all discovered devices
     */
    fun getDiscoveredDevices(): Set<BleDevice> {
        return discoveredDevices.value
    }
    
    /**
     * Send a ping command (customize based on your device protocol)
     */
    fun sendPing(serviceUuid: String, characteristicUuid: String): Boolean {
        return writeCommand(serviceUuid, characteristicUuid, "PING")
    }
    
    /**
     * Request device information (customize based on your device protocol)
     */
    fun requestDeviceInfo(serviceUuid: String, characteristicUuid: String): Boolean {
        return writeCommand(serviceUuid, characteristicUuid, "INFO")
    }
    
    private fun updateReadyState() {
        val status = blePermission.getPermissionStatus()
        _isReady.value = status == BlePermission.BlePermissionStatus.ALL_READY
    }
    
    /**
     * Clean up resources
     * Call this when the activity is destroyed
     */
    fun cleanup() {
        bleScanner.cleanup()
        bleConnection.close()
        _status.value = "Cleaned up"
        _isReady.value = false
    }
    
    /**
     * Helper method to convert ByteArray to hex string for debugging
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02x".format(it) }
    }
    
    /**
     * Helper method to convert hex string to ByteArray
     */
    fun hexToBytes(hex: String): ByteArray {
        return hex.replace(" ", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
    
    /**
     * Clear notification history
     */
    fun clearNotificationHistory() {
        bleConnection.clearNotificationHistory()
    }
}