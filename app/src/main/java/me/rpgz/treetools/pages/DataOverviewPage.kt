package me.rpgz.treetools.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.rpgz.treetools.ble.Hm10Manager
import me.rpgz.treetools.ble.SensorDataSimulator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataOverviewPage(navController: NavHostController) {
    val simulator = remember { SensorDataSimulator() }
    var sensorData by remember { mutableStateOf<Hm10Manager.SensorData?>(null) }
    var isSimulating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        simulator.startSimulation()
        simulator.simulatedData.collect { data ->
            sensorData = data
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text("数据总览", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("实时传感器数据与历史趋势", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (sensorData == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在初始化传感器...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SensorStatusCard(connected = isSimulating)
                    }

                    item {
                        Text(
                            "实时数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        SensorDataGrid(sensorData = sensorData)
                    }

                    item {
                        Text(
                            "操作面板",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionButton(
                                icon = Icons.Default.PlayArrow,
                                label = if (isSimulating) "暂停" else "开始",
                                onClick = {
                                    isSimulating = !isSimulating
                                    if (isSimulating) {
                                        simulator.startSimulation()
                                    } else {
                                        simulator.stopSimulation()
                                    }
                                },
                                color = if (isSimulating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            ActionButton(
                                icon = Icons.Default.Refresh,
                                label = "刷新",
                                onClick = { },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )

                            ActionButton(
                                icon = Icons.Default.Bluetooth,
                                label = "连接",
                                onClick = { },
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStatusCard(connected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (connected) Color.Green else Color.Gray
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (connected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (connected) "传感器已连接" else "传感器未连接",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (connected) "正在实时传输数据" else "点击开始按钮启动模拟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SensorDataGrid(sensorData: Hm10Manager.SensorData?) {
    if (sensorData == null) return

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorDataCard(
                title = "土壤湿度",
                value = String.format("%.1f", sensorData.soilMoisture),
                unit = "%",
                icon = Icons.Default.Water,
                color = getMoistureColor(sensorData.soilMoisture),
                modifier = Modifier.weight(1f)
            )

            SensorDataCard(
                title = "土壤温度",
                value = String.format("%.1f", sensorData.soilTemperature),
                unit = "°C",
                icon = Icons.Default.Thermostat,
                color = getTemperatureColor(sensorData.soilTemperature),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorDataCard(
                title = "环境温度",
                value = String.format("%.1f", sensorData.temperature),
                unit = "°C",
                icon = Icons.Default.Cloud,
                color = getTemperatureColor(sensorData.temperature),
                modifier = Modifier.weight(1f)
            )

            SensorDataCard(
                title = "环境湿度",
                value = String.format("%.1f", sensorData.humidity),
                unit = "%",
                icon = Icons.Default.Cloud,
                color = getMoistureColor(sensorData.humidity),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorDataCard(
                title = "CO2浓度",
                value = String.format("%.0f", sensorData.ambientCarbonDioxideContent),
                unit = "ppm",
                icon = Icons.Default.GasMeter,
                color = getCo2Color(sensorData.ambientCarbonDioxideContent),
                modifier = Modifier.weight(1f)
            )

            SensorDataCard(
                title = "土壤PH",
                value = String.format("%.2f", sensorData.soilMoisturePH),
                unit = "",
                icon = Icons.Default.Lightbulb,
                color = getPhColor(sensorData.soilMoisturePH),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorDataCard(
                title = "大气压",
                value = String.format("%.2f", sensorData.atmosphericPressure),
                unit = "kPa",
                icon = Icons.Default.Air,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )

            SensorDataCard(
                title = "风速",
                value = String.format("%.1f", sensorData.windSpeed),
                unit = "m/s",
                icon = Icons.Default.Waves,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SensorDataCard(
                title = "电导率",
                value = String.format("%.0f", sensorData.soilConductivity),
                unit = "μS/cm",
                icon = Icons.Default.Bolt,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDataCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            }

            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

fun getMoistureColor(value: Double): Color {
    return when {
        value < 30 -> Color.Red
        value < 60 -> Color.Yellow
        else -> Color.Green
    }
}

fun getTemperatureColor(value: Double): Color {
    return when {
        value < 10 -> Color.Blue
        value < 30 -> Color.Green
        else -> Color.Red
    }
}

fun getCo2Color(value: Double): Color {
    return when {
        value < 800 -> Color.Green
        value < 1500 -> Color.Yellow
        else -> Color.Red
    }
}

fun getPhColor(value: Double): Color {
    return when {
        value < 5.5 -> Color.Red
        value < 6.0 -> Color.Yellow
        value < 7.5 -> Color.Green
        value < 8.0 -> Color.Yellow
        else -> Color.Red
    }
}