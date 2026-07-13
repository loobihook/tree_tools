package me.rpgz.treetools.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BleDevice(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ByteArray?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val name: String?
        @SuppressLint("MissingPermission")
        get() = device.name
    
    val address: String
        get() = device.address
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BleDevice
        return device.address == other.device.address
    }
    
    override fun hashCode(): Int {
        return device.address.hashCode()
    }
}

@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "BleScanner"
        private const val DEFAULT_SCAN_PERIOD = 10000L // 10 seconds
    }
    
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<Set<BleDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<BleDevice>> = _discoveredDevices.asStateFlow()
    
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()
    
    private var scanCallback: ScanCallback? = null
    private var stopScanningRunnable: Runnable? = null
    
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "Device found: ${result.device.address} - ${result.device.name}")
            
            val bleDevice = BleDevice(
                device = result.device,
                rssi = result.rssi,
                scanRecord = result.scanRecord?.bytes
            )
            
            val currentDevices = _discoveredDevices.value.toMutableSet()
            currentDevices.add(bleDevice)
            _discoveredDevices.value = currentDevices
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            Log.d(TAG, "Batch scan results: ${results.size} devices")
            
            val currentDevices = _discoveredDevices.value.toMutableSet()
            results.forEach { result ->
                val bleDevice = BleDevice(
                    device = result.device,
                    rssi = result.rssi,
                    scanRecord = result.scanRecord?.bytes
                )
                currentDevices.add(bleDevice)
            }
            _discoveredDevices.value = currentDevices
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "Scan failed: $errorMessage")
            _scanError.value = errorMessage
            _isScanning.value = false
        }
    }
    
    fun startScan(
        scanPeriod: Long = DEFAULT_SCAN_PERIOD,
        serviceUuids: List<ParcelUuid>? = null,
        deviceName: String? = null
    ): Boolean {
        if (_isScanning.value) {
            Log.w(TAG, "Scan already in progress")
            return false
        }
        
        val scanner = bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available")
            _scanError.value = "Bluetooth LE scanner not available"
            return false
        }
        
        Log.i(TAG, "Starting BLE scan for ${scanPeriod}ms")
        
        // Clear previous results and errors
        _discoveredDevices.value = emptySet()
        _scanError.value = null
        
        // Build scan filters
        val scanFilters = mutableListOf<ScanFilter>()
        
        serviceUuids?.forEach { uuid ->
            scanFilters.add(
                ScanFilter.Builder()
                    .setServiceUuid(uuid)
                    .build()
            )
        }
        
        deviceName?.let { name ->
            scanFilters.add(
                ScanFilter.Builder()
                    .setDeviceName(name)
                    .build()
            )
        }
        
        // Configure scan settings
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        
        try {
            scanner.startScan(scanFilters.ifEmpty { null }, scanSettings, leScanCallback)
            scanCallback = leScanCallback
            _isScanning.value = true
            
            // Stop scanning after specified period
            stopScanningRunnable = Runnable {
                stopScan()
            }
            handler.postDelayed(stopScanningRunnable!!, scanPeriod)
            
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing permissions", e)
            _scanError.value = "Missing Bluetooth permissions"
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            _scanError.value = "Failed to start scan: ${e.message}"
            return false
        }
    }
    
    fun stopScan() {
        if (!_isScanning.value) {
            Log.w(TAG, "Scan not in progress")
            return
        }
        
        Log.i(TAG, "Stopping BLE scan")
        
        try {
            bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while stopping scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan", e)
        }
        
        scanCallback = null
        _isScanning.value = false
        
        stopScanningRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            stopScanningRunnable = null
        }
    }
    
    fun clearResults() {
        _discoveredDevices.value = emptySet()
        _scanError.value = null
    }
    
    fun findDeviceByAddress(address: String): BleDevice? {
        return _discoveredDevices.value.find { it.address == address }
    }
    
    fun findDevicesByName(name: String): List<BleDevice> {
        return _discoveredDevices.value.filter { it.name == name }
    }
    
    fun cleanup() {
        stopScan()
        clearResults()
    }
}