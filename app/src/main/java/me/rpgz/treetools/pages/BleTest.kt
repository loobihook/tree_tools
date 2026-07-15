package me.rpgz.treetools.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.components.ScreenTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleTest() {


    val sensorManager = remember {
        AppContainer.instance.sensorManager
    }
    
    val deviceStatus by sensorManager.deviceStatus.collectAsStateWithLifecycle()
    val isConnected by sensorManager.isConnected.collectAsStateWithLifecycle()
    val isListening by sensorManager.isListening.collectAsStateWithLifecycle()
    val receivedMessages by sensorManager.receivedMessages.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        ScreenTile("传感器日志")
        // Device Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "传感器设备状态",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = deviceStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row {
            Button(onClick = { AppContainer.instance.sensorManager.reset() }) {
                Text("重置传感器连接")
            }
        }


        // Control Buttons
        if (isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "数据监听控制",
                        style = MaterialTheme.typography.titleMedium
                    )

                    
                    if (isListening) {
                        Text(
                            text = "正在监听数据...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { sensorManager.stopListening() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("停止监听")
                        }
                    } else {
                        Text(
                            text = "已停止监听",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { sensorManager.startListening() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("开始监听")
                        }
                    }
                }
            }
        }
        
        // Received Data - Always show the card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "接收到的数据",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = { sensorManager.clearMessages() },
                        enabled = receivedMessages.isNotEmpty()
                    ) {
                        Text("清除")
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (receivedMessages.isEmpty()) {
                            if (isConnected && isListening) {
                                Text(
                                    text = "等待数据接收...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (isConnected) {
                                Text(
                                    text = "等待数据...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "等待设备连接...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            receivedMessages.forEach { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}