package me.rpgz.treetools.pages

import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import me.rpgz.treetools.api.tencent.uploadFile
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import me.rpgz.treetools.routing.Routes
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.components.LeafCategoryInfer
import me.rpgz.treetools.components.QrImage
import me.rpgz.treetools.components.ScreenTile
import me.rpgz.treetools.components.TreeReportGen
import me.rpgz.treetools.preferences.UserPreferences
import me.rpgz.treetools.viewmodels.CreateTreeSensePageViewModel
import me.rpgz.treetools.viewmodels.SettingPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTreeAnalysisPage(
    navController: NavHostController,
    viewModel: CreateTreeSensePageViewModel = hiltViewModel(),
    settingPageViewModel: SettingPageViewModel = hiltViewModel()
) {
    val sensorData = AppContainer.instance.hm10Manager.sensorData.collectAsState()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val formState by viewModel.formState.collectAsState()
    val extraState by viewModel.extraState.collectAsState()
    val treeHeightState by viewModel.treeHeightState.collectAsState()
    val treeDiameterState by viewModel.treeDiameterState.collectAsState()
    val leafImageUri by viewModel.leafImageUri.collectAsState()
    val showInferModal by viewModel.showInferModal.collectAsState()
    val leafBitmap by viewModel.leafBitmap.collectAsState()
    val showReportGenModal by viewModel.showReportGenModal.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showQrModal by remember { mutableStateOf(false) }
    var qrCodeUrl by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.onImageSelected(it)
                viewModel.saveImageToExternalStorage(context, it)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                photoUri?.let { uri ->
                    viewModel.onImageSelected(uri)
                    viewModel.saveImageToExternalStorage(context, uri)
                }
            }
        }
    )

    // Function to create image file URI for camera
    fun createImageFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}.jpg"
        val storageDir = File(context.getExternalFilesDir(null), "tree_analysis_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val imageFile = File(storageDir, imageFileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // Handle save state changes
    LaunchedEffect(saveState) {
        when (saveState) {
            is CreateTreeSensePageViewModel.SaveState.Success -> {
                val message = if (viewModel.isEditMode()) "记录更新成功" else "记录保存成功"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.clearSaveState()
                navController.popBackStack()
            }
            is CreateTreeSensePageViewModel.SaveState.Error -> {
                Toast.makeText(context, (saveState as CreateTreeSensePageViewModel.SaveState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.clearSaveState()
            }
            else -> { /* No action needed */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            ScreenTile(if (viewModel.isEditMode()) "编辑树木分析记录" else "创建树木分析记录")

            Spacer(modifier = Modifier.height(24.dp))
            
            // Display sensor data
            sensorData.value?.let { data ->
                Text(
                    text = "传感器数据",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("土壤温度:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.soilTemperature}°C", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("土壤湿度:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.soilMoisture}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("土壤PH:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.soilMoisturePH}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("土壤电导率:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.soilConductivity}μS/cm", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("环境温度:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.temperature}°C", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("环境湿度:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.humidity}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("大气压力:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.atmosphericPressure}kPa", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("二氧化碳含量:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.ambientCarbonDioxideContent}ppm", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("风速:", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.windSpeed}m/s", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formState.name,
                onValueChange = viewModel::updateName,
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = formState.note ?: "",
                onValueChange = viewModel::updateNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "叶片图像",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (leafImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(leafImageUri),
                        contentDescription = "叶片图像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "添加图像",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "点击选择叶片图像",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = extraState.treeSpecies ?: "",
                onValueChange = viewModel::updateTreeSpecies,
                label = { Text("树种") },
                modifier = Modifier.fillMaxWidth(),
                suffix = {
                    IconButton(onClick = {
                        if (leafImageUri == null) {
                            Toast.makeText(context, "请先选择叶片图像", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.onInferSpeciesClick(context)
                        }
                    }) {
                        Icon(Icons.Outlined.Star, contentDescription = "识别树种")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = treeHeightState,
                onValueChange = viewModel::updateTreeHeight,
                label = { Text("树高 (米)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = {
                    IconButton(onClick = {
                        navController.navigate(Routes.RealSenseMeasurement.route)
                    }) {
                        Icon(Icons.Filled.Build, contentDescription = "测距")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = treeDiameterState,
                onValueChange = viewModel::updateTreeDiameter,
                label = { Text("直径 (厘米)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = formState.report ?: "",
                onValueChange = viewModel::updateReport,
                label = { Text("分析报告") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                suffix = {
                    Column {
                        IconButton(onClick = {
                            if (leafImageUri == null) {
                                Toast.makeText(context, "请先选择叶片图像", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onGenerateReportClick()
                            }
                        }) {
                            Icon(Icons.Filled.Star, contentDescription = "生成分析报告")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        IconButton(onClick = {
                            // Save report to temp file and upload
                            viewModel.viewModelScope.launch {
                                try {
                                    val report = formState.report
                                    if (report == null || report == "") {
                                        Toast.makeText(context, "报告为空，无法生成二维码", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val now = System.currentTimeMillis()
                                    val tempFile = File(context.filesDir, "tree_report_${now}.txt", )
                                    tempFile.writeText(report, Charsets.UTF_8)
                                    val cosPath = "reports/tree_report_${now}.txt"
                                    Log.d("CreateTreeAnalysis", "tempFile.absolutePath: ${tempFile.absolutePath}")

                                    val url = uploadFile(tempFile.absolutePath, cosPath, context)
                                    Log.d("CreateTreeAnalysis", "Report uploaded successfully: $url")
                                    tempFile.delete()

                                    qrCodeUrl = url
                                    showQrModal = true
                                } catch (e: Exception) {
                                    Log.e("CreateTreeAnalysis", "Failed to upload report", e)
                                }
                            }

                        }) {
                            Icon(Icons.Filled.QrCode, contentDescription = "分享分析报告")
                        }
                    }


                }
            )


            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = viewModel::saveRecord,
                    modifier = Modifier.weight(1f),
                    enabled = formState.name.isNotBlank() && saveState !is CreateTreeSensePageViewModel.SaveState.Saving
                ) {
                    if (saveState is CreateTreeSensePageViewModel.SaveState.Saving) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(if (viewModel.isEditMode()) "更新中..." else "保存中...")
                        }
                    } else {
                        Text(if (viewModel.isEditMode()) "更新" else "保存")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在加载记录...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Dialog for choosing image source
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("选择图像来源") },
            text = { Text("请选择获取叶片图像的方式") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        photoUri = createImageFileUri()
                        cameraLauncher.launch(photoUri!!)
                    }
                ) {
                    Text("拍照")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Text("从相册选择")
                }
            }
        )
    }

    // Modal for leaf species inference
    if (showInferModal && leafBitmap != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissInferModal() }
        ) {
            LeafCategoryInfer(
                bitmap = leafBitmap!!,
                onApplyResult = { name ->
                    viewModel.applyPredictedSpecies("" , name)
                },
                modifier = Modifier.padding(bottom = 32.dp),
                viewModel = settingPageViewModel
            )
        }
    }

    // Modal for tree report generation
    if (showReportGenModal && leafImageUri != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissReportGenModal() }
        ) {
            val leafBitmapForReport = remember(leafImageUri) {
                try {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, leafImageUri!!))
                } catch (e: Exception) {
                    null
                }
            }

            if (leafBitmapForReport != null) {
                TreeReportGen(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight / 2),
                    leafImage = leafBitmapForReport,
                    treeSpecies = extraState.treeSpecies,
                    treeHeight = extraState.treeHeight,
                    treeDiameter = extraState.treeDiameter,
                    soilTemperature = sensorData.value?.soilTemperature,
                    soilMoisture = sensorData.value?.soilMoisture,
                    airCO2 = sensorData.value?.ambientCarbonDioxideContent,
                    ambientTemperature = sensorData.value?.temperature,
                    ambientHumidity = sensorData.value?.humidity,
                    userPreferences = userPreferences,
                    onReportGenerated = { report ->
                        viewModel.updateReport(report)
                        viewModel.dismissReportGenModal()
                    },
                    onClose = { viewModel.dismissReportGenModal() }
                )
            }
        }
    }

    // Modal for QR code display
    if (showQrModal && qrCodeUrl != null) {
        ModalBottomSheet(
            onDismissRequest = { showQrModal = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "分享分析报告",
                    style = MaterialTheme.typography.titleLarge
                )

                QrImage(data = qrCodeUrl!!, size = 300)

                Text(
                    text = "扫描二维码查看报告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}