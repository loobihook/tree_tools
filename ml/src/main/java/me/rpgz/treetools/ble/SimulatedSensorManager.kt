package me.rpgz.treetools.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimulatedSensorManager : SensorManager {

    companion object {
        private const val TAG = "SimulatedSensorManager"
        private const val MESSAGE_UPDATE_INTERVAL = 2000L
    }

    private val sensorDataSimulator = SensorDataSimulator()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isListening = MutableStateFlow(true)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _deviceStatus = MutableStateFlow("Simulated - Listening...")
    override val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    override val receivedMessages: StateFlow<List<String>> = _receivedMessages.asStateFlow()

    override val sensorData: StateFlow<Hm10Manager.SensorData?> = sensorDataSimulator.simulatedData

    init {
        sensorDataSimulator.startSimulation()
        scope.launch {
            while (_isListening.value) {
                sensorDataSimulator.getCurrentData()?.let { data ->
                    val timestamp = timeFormatter.format(Date())
                    val message = "[$timestamp] " +
                            "土壤温度:${data.soilTemperature}°C " +
                            "土壤湿度:${data.soilMoisture}% " +
                            "电导率:${data.soilConductivity}μS/cm " +
                            "PH:${data.soilMoisturePH} " +
                            "环境温度:${data.temperature}°C " +
                            "环境湿度:${data.humidity}% " +
                            "大气压:${data.atmosphericPressure}kPa " +
                            "CO2:${data.ambientCarbonDioxideContent}ppm " +
                            "风速:${data.windSpeed}m/s"

                    val currentMessages = _receivedMessages.value.toMutableList()
                    currentMessages.add(0, message)
                    if (currentMessages.size > 50) {
                        currentMessages.removeAt(currentMessages.size - 1)
                    }
                    _receivedMessages.value = currentMessages
                }
                delay(MESSAGE_UPDATE_INTERVAL)
            }
        }
    }

    override fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        _deviceStatus.value = "Simulated - Listening..."
        sensorDataSimulator.startSimulation()

        scope.launch {
            while (_isListening.value) {
                sensorDataSimulator.getCurrentData()?.let { data ->
                    val timestamp = timeFormatter.format(Date())
                    val message = "[$timestamp] " +
                            "土壤温度:${data.soilTemperature}°C " +
                            "土壤湿度:${data.soilMoisture}% " +
                            "电导率:${data.soilConductivity}μS/cm " +
                            "PH:${data.soilMoisturePH} " +
                            "环境温度:${data.temperature}°C " +
                            "环境湿度:${data.humidity}% " +
                            "大气压:${data.atmosphericPressure}kPa " +
                            "CO2:${data.ambientCarbonDioxideContent}ppm " +
                            "风速:${data.windSpeed}m/s"

                    val currentMessages = _receivedMessages.value.toMutableList()
                    currentMessages.add(0, message)
                    if (currentMessages.size > 50) {
                        currentMessages.removeAt(currentMessages.size - 1)
                    }
                    _receivedMessages.value = currentMessages
                }
                delay(MESSAGE_UPDATE_INTERVAL)
            }
        }
    }

    override fun stopListening() {
        _isListening.value = false
        _deviceStatus.value = "Simulated - Connected"
        sensorDataSimulator.stopSimulation()
    }

    override fun disconnect() {
        _isConnected.value = false
        _isListening.value = false
        _deviceStatus.value = "Simulated - Disconnected"
        sensorDataSimulator.stopSimulation()
    }

    override fun clearMessages() {
        _receivedMessages.value = emptyList()
    }

    override fun reset() {
        stopListening()
        _isConnected.value = true
        _deviceStatus.value = "Simulated - Connected"
    }

    override fun cleanup() {
        sensorDataSimulator.cleanup()
        scope.coroutineContext[Job]?.cancel()
        _isConnected.value = false
        _isListening.value = false
        _receivedMessages.value = emptyList()
    }
}