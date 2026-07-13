package me.rpgz.treetools.components

import me.rpgz.treetools.api.qwen.StreamingCallback
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.rpgz.treetools.api.qwen.genTreeReportStreaming
import kotlinx.coroutines.launch
import me.rpgz.treetools.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeReportGen(
    modifier: Modifier = Modifier,
    leafImage: Bitmap,
    treeSpecies: String?,
    treeHeight: Float?,
    treeDiameter: Float?,
    soilTemperature: Double? = null,
    soilMoisture: Double? = null,
    airCO2: Double? = null,
    ambientTemperature: Double? = null,
    ambientHumidity: Double? = null,
    userPreferences: UserPreferences,
    onReportGenerated: (String) -> Unit = {},
    onClose: () -> Unit = {},
) {
    var isGenerating by remember { mutableStateOf(false) }
    var isButtonLoading by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasStarted by remember { mutableStateOf(false) }
    
    val apiKey by userPreferences.dashscopeApiKey.collectAsStateWithLifecycle(initialValue = "")
    val coroutineScope = rememberCoroutineScope()
    
    val streamingCallback = remember {
        object : StreamingCallback {
            override fun onStart() {
                hasStarted = true
                isGenerating = true
                isButtonLoading = false
                streamingContent = ""
                errorMessage = null
            }
            
            override fun onChunk(chunk: String) {
                streamingContent += chunk
            }
            
            override fun onComplete(fullContent: String) {
                isGenerating = false
                isButtonLoading = false
                streamingContent = fullContent
                onReportGenerated(fullContent)
            }
            
            override fun onError(error: String) {
                isGenerating = false
                isButtonLoading = false
                errorMessage = error
            }
        }
    }
    
    fun generateReport() {
        if (apiKey.isBlank()) {
            errorMessage = "请先设置 DashScope API Key"
            return
        }
        
        isButtonLoading = true
        errorMessage = null
        
        coroutineScope.launch {
            genTreeReportStreaming(
                leafImage = leafImage,
                treeSpecies = treeSpecies,
                treeHeight = treeHeight,
                treeDiameter = treeDiameter,
                soilTemperature = soilTemperature,
                soilMoisture = soilMoisture,
                airCO2 = airCO2,
                ambientTemperature = ambientTemperature,
                ambientHumidity = ambientHumidity,
                apiKey = apiKey,
                callback = streamingCallback
            )
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "树木状态分析报告",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    if (!isGenerating && streamingContent.isNotBlank()) {
                        IconButton(
                            onClick = { generateReport() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新生成"
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status and content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    errorMessage != null -> {
                        // Error state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "生成报告时出错",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { 
                                    errorMessage = null
                                    generateReport()
                                },
                                enabled = !isButtonLoading
                            ) {
                                if (isButtonLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("重试中...")
                                    }
                                } else {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    
                    !hasStarted -> {
                        // Initial state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "点击下方按钮开始生成树木健康分析报告",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { generateReport() },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                enabled = !isButtonLoading
                            ) {
                                if (isButtonLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("生成中...")
                                    }
                                } else {
                                    Text("生成报告")
                                }
                            }
                        }
                    }
                    
                    isGenerating -> {
                        // Generating state with streaming content
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "正在生成报告...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            if (streamingContent.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxSize(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    val streamingScrollState = rememberScrollState()
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(streamingScrollState)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = streamingContent,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp
                                        )
                                        
                                        // Blinking cursor effect
                                        var showCursor by remember { mutableStateOf(true) }
                                        LaunchedEffect(Unit) {
                                            while (isGenerating) {
                                                delay(500)
                                                showCursor = !showCursor
                                            }
                                        }
                                        
                                        if (showCursor) {
                                            Text(
                                                text = "█",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    // Auto-scroll to bottom when new content is added
                                    LaunchedEffect(streamingContent) {
                                        if (streamingContent.isNotBlank()) {
                                            streamingScrollState.animateScrollTo(streamingScrollState.maxValue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {
                        // Completed state
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = "报告生成完成",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = streamingContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}