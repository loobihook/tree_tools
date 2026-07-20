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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import me.rpgz.treetools.routing.Routes
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import me.rpgz.treetools.AppContainer
import me.rpgz.treetools.components.LeafCategoryInfer
import me.rpgz.treetools.components.QrImage
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
    val sensorData = AppContainer.instance.sensorManager.sensorData.collectAsState()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

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

    var currentStep by remember { mutableStateOf(1) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showQrModal by remember { mutableStateOf(false) }
    var qrCodeUrl by remember { mutableStateOf<String?>(null) }

    val steps = listOf("拍照识别", "测量数据", "生成报告")

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
            else -> { }
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
                Text(
                    if (viewModel.isEditMode()) "编辑树木分析记录" else "新建分析记录",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                StepIndicator(steps = steps, currentStep = currentStep)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                when (currentStep) {
                    1 -> StepOne(
                        leafImageUri = leafImageUri,
                        onImageClick = { showImageSourceDialog = true },
                        treeSpecies = extraState.treeSpecies ?: "",
                        onUpdateSpecies = viewModel::updateTreeSpecies,
                        onInferClick = {
                            if (leafImageUri == null) {
                                Toast.makeText(context, "请先选择叶片图像", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onInferSpeciesClick(context)
                            }
                        }
                    )

                    2 -> StepTwo(
                        sensorData = sensorData.value,
                        treeHeight = treeHeightState,
                        onUpdateTreeHeight = viewModel::updateTreeHeight,
                        treeDiameter = treeDiameterState,
                        onUpdateTreeDiameter = viewModel::updateTreeDiameter,
                        onRealSenseClick = { navController.navigate(Routes.RealSenseMeasurement.route) }
                    )

                    3 -> StepThree(
                        name = formState.name,
                        onUpdateName = viewModel::updateName,
                        note = formState.note ?: "",
                        onUpdateNote = viewModel::updateNote,
                        report = formState.report ?: "",
                        onGenerateReport = {
                            if (leafImageUri == null) {
                                Toast.makeText(context, "请先选择叶片图像", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onGenerateReportClick()
                            }
                        },
                        onShareReport = {
                            coroutineScope.launch {
                                try {
                                    val report = formState.report
                                    if (report == null || report == "") {
                                        Toast.makeText(context, "报告为空，无法生成二维码", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val now = System.currentTimeMillis()
                                    val tempFile = File(context.filesDir, "tree_report_${now}.txt")
                                    tempFile.writeText(report, Charsets.UTF_8)
                                    val cosPath = "reports/tree_report_${now}.txt"

                                    val url = uploadFile(tempFile.absolutePath, cosPath, context)
                                    tempFile.delete()

                                    qrCodeUrl = url
                                    showQrModal = true
                                } catch (e: Exception) {
                                    Log.e("CreateTreeAnalysis", "Failed to upload report", e)
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 1) {
                        Button(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.ArrowLeft, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("上一步")
                        }
                    } else {
                        TextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> {
                                        if (leafImageUri == null) {
                                            Toast.makeText(context, "请先选择叶片图像", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        currentStep++
                                    }
                                    2 -> {
                                        currentStep++
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下一步")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowRight, contentDescription = null)
                        }
                    } else {
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
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (viewModel.isEditMode()) "更新" else "保存")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在加载记录...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

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

        if (showInferModal && leafBitmap != null) {
            ModalBottomSheet(onDismissRequest = { viewModel.dismissInferModal() }) {
                LeafCategoryInfer(
                    bitmap = leafBitmap!!,
                    onApplyResult = { name ->
                        viewModel.applyPredictedSpecies("", name)
                    },
                    modifier = Modifier.padding(bottom = 32.dp),
                    viewModel = settingPageViewModel
                )
            }
        }

        if (showReportGenModal && leafImageUri != null) {
            ModalBottomSheet(onDismissRequest = { viewModel.dismissReportGenModal() }) {
                val leafBitmapForReport = remember(leafImageUri) {
                    try {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, leafImageUri!!))
                    } catch (e: Exception) {
                        null
                    }
                }

                if (leafBitmapForReport != null) {
                    TreeReportGen(
                        modifier = Modifier.fillMaxWidth().height(screenHeight / 2),
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

        if (showQrModal && qrCodeUrl != null) {
            ModalBottomSheet(onDismissRequest = { showQrModal = false }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("分享分析报告", style = MaterialTheme.typography.titleLarge)
                    QrImage(data = qrCodeUrl!!, size = 300)
                    Text("扫描二维码查看报告", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StepIndicator(steps: List<String>, currentStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            steps.forEachIndexed { index, step ->
                val isActive = index + 1 == currentStep
                val isCompleted = index + 1 < currentStep

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, _ ->
                if (index > 0) {
                    val isCompleted = index < currentStep
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StepOne(
    leafImageUri: Uri?,
    onImageClick: () -> Unit,
    treeSpecies: String,
    onUpdateSpecies: (String) -> Unit,
    onInferClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("拍照与识别", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("拍摄叶片照片或从相册选择，系统将自动识别树种", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable(onClick = onImageClick)
                .clip(RoundedCornerShape(16.dp)),
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("点击拍照或上传叶片图片", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("支持 JPG、PNG 格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = treeSpecies,
            onValueChange = onUpdateSpecies,
            label = { Text("树种") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onInferClick) {
                    Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "AI识别树种",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                }
            }
        )

        if (treeSpecies.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Green
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("已识别树种: $treeSpecies", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun StepTwo(
    sensorData: me.rpgz.treetools.ble.Hm10Manager.SensorData?,
    treeHeight: String,
    onUpdateTreeHeight: (String) -> Unit,
    treeDiameter: String,
    onUpdateTreeDiameter: (String) -> Unit,
    onRealSenseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("测量数据", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("输入树木高度和直径，或使用 RealSense 进行精确测量", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = treeHeight,
            onValueChange = onUpdateTreeHeight,
            label = { Text("树高 (米)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                IconButton(onClick = onRealSenseClick) {
                    Icon(Icons.Default.DeviceHub, contentDescription = "RealSense测量", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        )

        OutlinedTextField(
            value = treeDiameter,
            onValueChange = onUpdateTreeDiameter,
            label = { Text("直径 (厘米)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("传感器数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (sensorData != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SensorDataRow("土壤温度", "${sensorData.soilTemperature}°C", Icons.Default.Thermostat)
                SensorDataRow("土壤湿度", "${sensorData.soilMoisture}%", Icons.Default.Water)
                SensorDataRow("土壤PH", "${sensorData.soilMoisturePH}", Icons.Default.Lightbulb)
                SensorDataRow("环境温度", "${sensorData.temperature}°C", Icons.Default.Cloud)
                SensorDataRow("环境湿度", "${sensorData.humidity}%", Icons.Default.Cloud)
                SensorDataRow("CO2浓度", "${sensorData.ambientCarbonDioxideContent}ppm", Icons.Default.GasMeter)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在读取传感器数据...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SensorDataRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StepThree(
    name: String,
    onUpdateName: (String) -> Unit,
    note: String,
    onUpdateNote: (String) -> Unit,
    report: String,
    onGenerateReport: () -> Unit,
    onShareReport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("备注与报告", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("添加备注信息并生成AI分析报告", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onUpdateName,
            label = { Text("记录名称") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：小区门口梧桐树 - 2024年5月") }
        )

        OutlinedTextField(
            value = note,
            onValueChange = onUpdateNote,
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("添加备注信息，如测量时间、天气情况等") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI分析报告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onGenerateReport) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "生成报告", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onShareReport) {
                            Icon(Icons.Default.Share, contentDescription = "分享报告", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (report.isNotBlank()) {
                    Text(
                        report,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("点击魔杖图标生成AI分析报告", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}