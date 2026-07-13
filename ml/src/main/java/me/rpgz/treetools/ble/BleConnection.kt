package me.rpgz.treetools.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class BleCharacteristic(
    val serviceUuid: String,
    val characteristicUuid: String,
    val properties: Int,
    val permissions: Int
) {
    val canRead: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
    
    val canWrite: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
    
    val canWriteNoResponse: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
    
    val canNotify: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    
    val canIndicate: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
}

data class BleConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isDiscoveringServices: Boolean = false,
    val device: BluetoothDevice? = null,
    val services: List<BluetoothGattService> = emptyList(),
    val characteristics: List<BleCharacteristic> = emptyList(),
    val error: String? = null
)

data class BleDataReceived(
    val characteristicUuid: String,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BleDataReceived
        if (characteristicUuid != other.characteristicUuid) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false
        return true
    }
    
    override fun hashCode(): Int {
        var result = characteristicUuid.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

@SuppressLint("MissingPermission")
class BleConnection(private val context: Context) {
    
    companion object {
        private const val TAG = "BleConnection"
        private const val CONNECTION_TIMEOUT = 30000L // 30 seconds
        private const val OPERATION_TIMEOUT = 5000L // 5 seconds
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    private var bluetoothGatt: BluetoothGatt? = null
    private val operationQueue = ConcurrentLinkedQueue<BleOperation>()
    private var isOperationInProgress = false
    
    private val _connectionState = MutableStateFlow(BleConnectionState())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _dataReceived = MutableStateFlow<BleDataReceived?>(null)
    val dataReceived: StateFlow<BleDataReceived?> = _dataReceived.asStateFlow()
    
    private val _notificationHistory = MutableStateFlow<List<BleDataReceived>>(emptyList())
    val notificationHistory: StateFlow<List<BleDataReceived>> = _notificationHistory.asStateFlow()
    
    private var connectionTimeoutRunnable: Runnable? = null
    private var operationTimeoutRunnable: Runnable? = null
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            connectionTimeoutRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                connectionTimeoutRunnable = null
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    updateConnectionState(
                        isConnected = true,
                        isConnecting = false,
                        isDiscoveringServices = true,
                        device = gatt.device,
                        error = null
                    )
                    
                    // Discover services
                    Log.i(TAG, "Discovering services...")
                    gatt.discoverServices()
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    updateConnectionState(
                        isConnected = false,
                        isConnecting = false,
                        isDiscoveringServices = false,
                        device = null,
                        services = emptyList(),
                        characteristics = emptyList(),
                        error = if (status != BluetoothGatt.GATT_SUCCESS) "Connection lost" else null
                    )
                    cleanup()
                }
                
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.i(TAG, "Connecting to GATT server...")
                    updateConnectionState(isConnecting = true)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered: ${gatt.services.size}")
                
                val characteristics = mutableListOf<BleCharacteristic>()
                gatt.services.forEach { service ->
                    Log.d(TAG, "Service found: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        Log.d(TAG, "Characteristic found: ${characteristic.uuid} (Properties: ${characteristic.properties})")
                        characteristics.add(
                            BleCharacteristic(
                                serviceUuid = service.uuid.toString(),
                                characteristicUuid = characteristic.uuid.toString(),
                                properties = characteristic.properties,
                                permissions = characteristic.permissions
                            )
                        )
                    }
                }
                
                Log.i(TAG, "Total characteristics found: ${characteristics.size}")
                updateConnectionState(
                    isDiscoveringServices = false,
                    services = gatt.services,
                    characteristics = characteristics
                )
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                updateConnectionState(
                    isDiscoveringServices = false,
                    error = "Service discovery failed with status: $status"
                )
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            
            operationTimeoutRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                operationTimeoutRunnable = null
            }
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read: ${characteristic.uuid}")
                val data = characteristic.value ?: byteArrayOf()
                _dataReceived.value = BleDataReceived(
                    characteristicUuid = characteristic.uuid.toString(),
                    data = data
                )
            } else {
                Log.e(TAG, "Characteristic read failed: $status")
            }
            
            processNextOperation()
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            
            operationTimeoutRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                operationTimeoutRunnable = null
            }
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful: ${characteristic.uuid}")
            } else {
                Log.e(TAG, "Characteristic write failed: $status")
            }
            
            processNextOperation()
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            
            Log.i(TAG, "📨 Notification received from: ${characteristic.uuid}")
            val data = characteristic.value ?: byteArrayOf()
            Log.d(TAG, "📨 Data length: ${data.size} bytes")
            Log.d(TAG, "📨 Data hex: ${data.joinToString(" ") { "%02x".format(it) }}")
            Log.d(TAG, "📨 Data string: '${String(data, Charsets.UTF_8)}'")
            Log.d(TAG, "📨 Data ASCII: '${String(data, Charsets.US_ASCII)}'")
            
            val notification = BleDataReceived(
                characteristicUuid = characteristic.uuid.toString(),
                data = data
            )
            
            _dataReceived.value = notification
            
            // Add to history (keep last 50 notifications)
            val currentHistory = _notificationHistory.value.toMutableList()
            currentHistory.add(0, notification) // Add to beginning
            if (currentHistory.size > 50) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            _notificationHistory.value = currentHistory
        }
        
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            
            operationTimeoutRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                operationTimeoutRunnable = null
            }
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful: ${descriptor.uuid}")
            } else {
                Log.e(TAG, "Descriptor write failed: $status")
            }
            
            processNextOperation()
        }
    }
    
    fun connect(device: BluetoothDevice): Boolean {
        if (_connectionState.value.isConnected || _connectionState.value.isConnecting) {
            Log.w(TAG, "Already connected or connecting")
            return false
        }
        
        Log.i(TAG, "Connecting to device: ${device.address}")
        
        updateConnectionState(
            isConnecting = true,
            device = device,
            error = null
        )
        
        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            
            // Set connection timeout
            connectionTimeoutRunnable = Runnable {
                Log.e(TAG, "Connection timeout")
                updateConnectionState(
                    isConnecting = false,
                    error = "Connection timeout"
                )
                disconnect()
            }
            handler.postDelayed(connectionTimeoutRunnable!!, CONNECTION_TIMEOUT)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            updateConnectionState(
                isConnecting = false,
                error = "Failed to connect: ${e.message}"
            )
            return false
        }
    }
    
    fun disconnect() {
        Log.i(TAG, "Disconnecting...")
        
        connectionTimeoutRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            connectionTimeoutRunnable = null
        }
        
        try {
            bluetoothGatt?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
    
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null || !_connectionState.value.isConnected) {
            Log.e(TAG, "Not connected to device")
            return false
        }
        
        val operation = BleOperation.Write(serviceUuid, characteristicUuid, data)
        operationQueue.offer(operation)
        processNextOperation()
        
        return true
    }
    
    fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null || !_connectionState.value.isConnected) {
            Log.e(TAG, "Not connected to device")
            return false
        }
        
        val operation = BleOperation.Read(serviceUuid, characteristicUuid)
        operationQueue.offer(operation)
        processNextOperation()
        
        return true
    }
    
    fun enableNotifications(serviceUuid: String, characteristicUuid: String): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null || !_connectionState.value.isConnected) {
            Log.e(TAG, "Not connected to device")
            return false
        }
        
        val operation = BleOperation.EnableNotification(serviceUuid, characteristicUuid)
        operationQueue.offer(operation)
        processNextOperation()
        
        return true
    }
    
    fun disableNotifications(serviceUuid: String, characteristicUuid: String): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null || !_connectionState.value.isConnected) {
            Log.e(TAG, "Not connected to device")
            return false
        }
        
        val operation = BleOperation.DisableNotification(serviceUuid, characteristicUuid)
        operationQueue.offer(operation)
        processNextOperation()
        
        return true
    }
    
    private fun processNextOperation() {
        if (isOperationInProgress || operationQueue.isEmpty()) {
            return
        }
        
        val operation = operationQueue.poll() ?: return
        isOperationInProgress = true
        
        // Set operation timeout
        operationTimeoutRunnable = Runnable {
            Log.e(TAG, "Operation timeout")
            isOperationInProgress = false
            processNextOperation()
        }
        handler.postDelayed(operationTimeoutRunnable!!, OPERATION_TIMEOUT)
        
        when (operation) {
            is BleOperation.Read -> executeRead(operation)
            is BleOperation.Write -> executeWrite(operation)
            is BleOperation.EnableNotification -> executeEnableNotification(operation)
            is BleOperation.DisableNotification -> executeDisableNotification(operation)
        }
    }
    
    private fun executeRead(operation: BleOperation.Read) {
        val gatt = bluetoothGatt ?: return
        val characteristic = findCharacteristic(operation.serviceUuid, operation.characteristicUuid)
        
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found: ${operation.characteristicUuid}")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        if (!gatt.readCharacteristic(characteristic)) {
            Log.e(TAG, "Failed to read characteristic")
            isOperationInProgress = false
            processNextOperation()
        }
    }
    
    private fun executeWrite(operation: BleOperation.Write) {
        val gatt = bluetoothGatt ?: return
        val characteristic = findCharacteristic(operation.serviceUuid, operation.characteristicUuid)
        
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found for write: ${operation.characteristicUuid}")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        Log.i(TAG, "📤 Writing to characteristic: ${characteristic.uuid}")
        Log.d(TAG, "📤 Data length: ${operation.data.size} bytes")
        Log.d(TAG, "📤 Data hex: ${operation.data.joinToString(" ") { "%02x".format(it) }}")
        Log.d(TAG, "📤 Data string: '${String(operation.data, Charsets.UTF_8)}'")
        Log.d(TAG, "📤 Characteristic properties: ${characteristic.properties}")
        
        characteristic.value = operation.data
        if (!gatt.writeCharacteristic(characteristic)) {
            Log.e(TAG, "Failed to initiate characteristic write")
            isOperationInProgress = false
            processNextOperation()
        } else {
            Log.d(TAG, "Write operation initiated successfully")
        }
    }
    
    private fun executeEnableNotification(operation: BleOperation.EnableNotification) {
        val gatt = bluetoothGatt ?: return
        val characteristic = findCharacteristic(operation.serviceUuid, operation.characteristicUuid)
        
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found for notifications: ${operation.characteristicUuid}")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        Log.i(TAG, "Enabling notifications for characteristic: ${characteristic.uuid}")
        Log.d(TAG, "Characteristic properties: ${characteristic.properties}")
        
        // Check if characteristic supports notifications or indications
        val supportsNotifications = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
        val supportsIndications = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        
        if (!supportsNotifications && !supportsIndications) {
            Log.e(TAG, "Characteristic does not support notifications or indications")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            Log.e(TAG, "Failed to enable local notifications")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        Log.d(TAG, "Local notifications enabled, writing to CCCD...")
        
        // Write to Client Characteristic Configuration Descriptor
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            // Choose notifications or indications based on what the characteristic supports
            val descriptorValue = if (supportsNotifications) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            
            descriptor.value = descriptorValue
            Log.d(TAG, "Writing CCCD with value: ${descriptorValue.contentToString()}")
            
            if (!gatt.writeDescriptor(descriptor)) {
                Log.e(TAG, "Failed to write CCCD descriptor")
                isOperationInProgress = false
                processNextOperation()
            }
        } else {
            Log.e(TAG, "CCCD descriptor not found")
            // Some characteristics might work without CCCD
            Log.w(TAG, "Attempting to continue without CCCD...")
            isOperationInProgress = false
            processNextOperation()
        }
    }
    
    private fun executeDisableNotification(operation: BleOperation.DisableNotification) {
        val gatt = bluetoothGatt ?: return
        val characteristic = findCharacteristic(operation.serviceUuid, operation.characteristicUuid)
        
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found: ${operation.characteristicUuid}")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        if (!gatt.setCharacteristicNotification(characteristic, false)) {
            Log.e(TAG, "Failed to disable notifications")
            isOperationInProgress = false
            processNextOperation()
            return
        }
        
        // Write to Client Characteristic Configuration Descriptor
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            if (!gatt.writeDescriptor(descriptor)) {
                Log.e(TAG, "Failed to write descriptor")
                isOperationInProgress = false
                processNextOperation()
            }
        } else {
            Log.e(TAG, "CCCD not found")
            isOperationInProgress = false
            processNextOperation()
        }
    }
    
    private fun findCharacteristic(serviceUuid: String, characteristicUuid: String): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: return null
        return service.getCharacteristic(UUID.fromString(characteristicUuid))
    }
    
    private fun updateConnectionState(
        isConnected: Boolean = _connectionState.value.isConnected,
        isConnecting: Boolean = _connectionState.value.isConnecting,
        isDiscoveringServices: Boolean = _connectionState.value.isDiscoveringServices,
        device: BluetoothDevice? = _connectionState.value.device,
        services: List<BluetoothGattService> = _connectionState.value.services,
        characteristics: List<BleCharacteristic> = _connectionState.value.characteristics,
        error: String? = _connectionState.value.error
    ) {
        _connectionState.value = BleConnectionState(
            isConnected = isConnected,
            isConnecting = isConnecting,
            isDiscoveringServices = isDiscoveringServices,
            device = device,
            services = services,
            characteristics = characteristics,
            error = error
        )
    }
    
    fun clearNotificationHistory() {
        _notificationHistory.value = emptyList()
        Log.d(TAG, "Notification history cleared")
    }
    
    private fun cleanup() {
        operationQueue.clear()
        isOperationInProgress = false
        
        operationTimeoutRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            operationTimeoutRunnable = null
        }
        
        try {
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT", e)
        }
        
        bluetoothGatt = null
    }
    
    fun close() {
        disconnect()
        cleanup()
    }
}

sealed class BleOperation {
    data class Read(val serviceUuid: String, val characteristicUuid: String) : BleOperation()
    data class Write(val serviceUuid: String, val characteristicUuid: String, val data: ByteArray) : BleOperation()
    data class EnableNotification(val serviceUuid: String, val characteristicUuid: String) : BleOperation()
    data class DisableNotification(val serviceUuid: String, val characteristicUuid: String) : BleOperation()
}