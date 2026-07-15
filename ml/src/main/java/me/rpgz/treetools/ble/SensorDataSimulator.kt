package me.rpgz.treetools.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.math.sin
import java.util.Random

class SensorDataSimulator {

    companion object {
        private const val TAG = "SensorDataSimulator"
        private const val UPDATE_INTERVAL = 1000L
    }

    private val random = Random()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var isSimulating = false

    private val _simulatedData = MutableStateFlow<Hm10Manager.SensorData?>(null)
    val simulatedData: StateFlow<Hm10Manager.SensorData?> = _simulatedData.asStateFlow()

    private var timeOffset = 0L

    fun startSimulation() {
        if (isSimulating) return
        isSimulating = true
        timeOffset = System.currentTimeMillis()
        scope.launch {
            simulateDataLoop()
        }
    }

    fun stopSimulation() {
        isSimulating = false
    }

    fun isRunning(): Boolean = isSimulating

    private suspend fun simulateDataLoop() {
        while (isSimulating) {
            val elapsedSeconds = (System.currentTimeMillis() - timeOffset) / 1000.0
            _simulatedData.value = generateSimulatedData(elapsedSeconds)
            delay(UPDATE_INTERVAL)
        }
    }

    private fun generateSimulatedData(elapsedSeconds: Double): Hm10Manager.SensorData {
        return Hm10Manager.SensorData(
            soilTemperature = simulateSoilTemperature(elapsedSeconds),
            soilMoisture = simulateSoilMoisture(elapsedSeconds),
            soilMoisturePH = simulateSoilPH(elapsedSeconds),
            soilConductivity = simulateSoilConductivity(elapsedSeconds),
            humidity = simulateHumidity(elapsedSeconds),
            temperature = simulateTemperature(elapsedSeconds),
            atmosphericPressure = simulateAtmosphericPressure(elapsedSeconds),
            ambientCarbonDioxideContent = simulateCO2(elapsedSeconds),
            windSpeed = simulateWindSpeed(elapsedSeconds)
        )
    }

    private fun simulateSoilTemperature(elapsedSeconds: Double): Double {
        val base = 22.0
        val dailyVariation = sin(elapsedSeconds / 3600.0 * 2 * Math.PI) * 3.0
        val noise = (random.nextDouble() - 0.5) * 0.5
        return (base + dailyVariation + noise).coerceIn(15.0, 35.0)
    }

    private fun simulateSoilMoisture(elapsedSeconds: Double): Double {
        val base = 65.0
        val trend = if (elapsedSeconds < 7200) 0.0 else -0.001 * (elapsedSeconds - 7200)
        val noise = (random.nextDouble() - 0.5) * 2.0
        return (base + trend + noise).coerceIn(30.0, 90.0)
    }

    private fun simulateSoilPH(elapsedSeconds: Double): Double {
        val base = 6.8
        val noise = (random.nextDouble() - 0.5) * 0.2
        return (base + noise).coerceIn(5.5, 8.5)
    }

    private fun simulateSoilConductivity(elapsedSeconds: Double): Double {
        val base = 250.0
        val moistureFactor = simulateSoilMoisture(elapsedSeconds) / 100.0
        val noise = (random.nextDouble() - 0.5) * 20.0
        return (base * moistureFactor * 1.5 + noise).coerceIn(50.0, 500.0)
    }

    private fun simulateHumidity(elapsedSeconds: Double): Double {
        val base = 60.0
        val dailyVariation = sin(elapsedSeconds / 3600.0 * 2 * Math.PI + Math.PI) * 15.0
        val noise = (random.nextDouble() - 0.5) * 3.0
        return (base + dailyVariation + noise).coerceIn(30.0, 95.0)
    }

    private fun simulateTemperature(elapsedSeconds: Double): Double {
        val base = 25.0
        val dailyVariation = sin(elapsedSeconds / 3600.0 * 2 * Math.PI) * 5.0
        val noise = (random.nextDouble() - 0.5) * 1.0
        return (base + dailyVariation + noise).coerceIn(10.0, 40.0)
    }

    private fun simulateAtmosphericPressure(elapsedSeconds: Double): Double {
        val base = 101.3
        val dailyVariation = sin(elapsedSeconds / 7200.0 * 2 * Math.PI) * 0.5
        val noise = (random.nextDouble() - 0.5) * 0.2
        return (base + dailyVariation + noise).coerceIn(98.0, 105.0)
    }

    private fun simulateCO2(elapsedSeconds: Double): Double {
        val base = 450.0
        val dailyPeak = sin(elapsedSeconds / 3600.0 * 2 * Math.PI + Math.PI / 4) * 50.0
        val noise = (random.nextDouble() - 0.5) * 20.0
        return (base + dailyPeak + noise).coerceIn(350.0, 600.0)
    }

    private fun simulateWindSpeed(elapsedSeconds: Double): Double {
        val base = 2.0
        val gustFactor = sin(elapsedSeconds / 1800.0 * 2 * Math.PI) * 3.0
        val noise = random.nextDouble() * 1.0
        return (base + gustFactor + noise).coerceIn(0.0, 15.0)
    }

    fun getCurrentData(): Hm10Manager.SensorData? = _simulatedData.value

    fun cleanup() {
        stopSimulation()
        scope.coroutineContext[Job]?.cancel()
    }
}