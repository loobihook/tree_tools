package me.rpgz.treetools.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.ble.SensorMode
import me.rpgz.treetools.preferences.UserPreferences

@Composable
fun SensorModeSettings() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val currentMode = remember {
        if (AppContainer.instance.sensorManager is me.rpgz.treetools.ble.SimulatedSensorManager) {
            SensorMode.SIMULATED
        } else {
            SensorMode.REAL
        }
    }
    
    var selectedMode by remember { mutableStateOf(currentMode) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "传感器模式",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedMode != SensorMode.REAL) {
                            selectedMode = SensorMode.REAL
                            coroutineScope.launch {
                                userPreferences.setSensorMode(SensorMode.REAL.name)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == SensorMode.REAL) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedMode == SensorMode.REAL)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selectedMode == SensorMode.REAL) 4.dp else 0.dp
                    )
                ) {
                    Text("真实传感器")
                }
                
                Button(
                    onClick = {
                        if (selectedMode != SensorMode.SIMULATED) {
                            selectedMode = SensorMode.SIMULATED
                            coroutineScope.launch {
                                userPreferences.setSensorMode(SensorMode.SIMULATED.name)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == SensorMode.SIMULATED) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedMode == SensorMode.SIMULATED)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selectedMode == SensorMode.SIMULATED) 4.dp else 0.dp
                    )
                ) {
                    Text("模拟传感器")
                }
            }
            
            Text(
                text = "当前模式: ${if (selectedMode == SensorMode.SIMULATED) "模拟传感器" else "真实传感器"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "提示：切换传感器模式需要重新启动应用才能生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}