package me.rpgz.treetools.components

import android.R
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.api.baidu.PlantIdentifier
import me.rpgz.treetools.ml.id2label
import me.rpgz.treetools.ml.PlantResult
import me.rpgz.treetools.models.PlanetInferModel
import me.rpgz.treetools.viewmodels.SettingPageViewModel

@Composable
fun LeafCategoryInfer(
    bitmap: Bitmap,
    viewModel: SettingPageViewModel,
    onApplyResult: (name: String) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    var isInferring by remember { mutableStateOf(false) }
    var plantResults by remember { mutableStateOf<List<PlantResult>>(emptyList()) }
    var selectedResult by remember { mutableStateOf<PlantResult?>(null) }
    val context = LocalContext.current
    val selectedModel by viewModel.planetInferModel.collectAsState()

    LaunchedEffect(bitmap, selectedModel) {
        isInferring = true
        try {
            val response = when (selectedModel) {
                PlanetInferModel.THIRD_PARTY_API -> {
                    PlantIdentifier.identifyPlant(bitmap)
                }
                PlanetInferModel.PRIVATE_MODEL -> {
                    AppContainer.instance.infer.analyze(bitmap, context = context)
                }
            }
            if(response == null) {
                throw RuntimeException("Result is Unavailable");
            }
            Log.d("Infer", response.result.joinToString { it.name })
            plantResults = response.result
            selectedResult = response?.result?.firstOrNull()
        } catch (e: Exception) {
            Log.e("Infer", e.toString())
            plantResults = emptyList()
            selectedResult = null
        } finally {
            isInferring = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(

            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display the leaf image
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Leaf image",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display inference result
            if (isInferring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analyzing...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (plantResults.isNotEmpty()) {
                Text(
                    text = "识别结果 (Top 3):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display top 3 results as selectable cards
                plantResults.forEachIndexed { index, result ->
                    val isSelected = selectedResult == result
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface
                        ),
                        onClick = { selectedResult = result }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${result.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "置信度: ${String.format("%.1f", result.score * 100)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                selectedResult?.let { result ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onApplyResult(result.name)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用选中结果")
                    }
                }
            } else {
                Text(
                    text = "无法识别植物",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}