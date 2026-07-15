package me.rpgz.treetools.ble

import kotlinx.coroutines.flow.StateFlow

interface SensorManager {
    val sensorData: StateFlow<Hm10Manager.SensorData?>
    val isConnected: StateFlow<Boolean>
    val isListening: StateFlow<Boolean>
    val deviceStatus: StateFlow<String>
    val receivedMessages: StateFlow<List<String>>

    fun startListening()
    fun stopListening()
    fun disconnect()
    fun clearMessages()
    fun reset()
    fun cleanup()
}