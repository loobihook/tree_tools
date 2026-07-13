package me.rpgz.treetools.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity
import me.rpgz.treetools.models.TreeAnalysisRecordExtra
import me.rpgz.treetools.repositories.TreeAnalysisReportRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


private val TAG = "CreateTreeSensePageViewModel"

data class TreeAnalysisFormState(
    val name: String = "",
    val note: String? = null,
    val imageDir: String? = null,
    val report: String? = null
) 

@HiltViewModel
class CreateTreeSensePageViewModel @Inject constructor(
    private val repository: TreeAnalysisReportRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val xlog = XLog.tag("CreateTreeSensePageViewModel")
    private val recordId: Long? = savedStateHandle.get<String>("recordId")?.toLongOrNull()
    private var isEditMode: Boolean = recordId != null

    private val _formState = MutableStateFlow(TreeAnalysisFormState())
    val formState: StateFlow<TreeAnalysisFormState> = _formState.asStateFlow()

    private val _extraState = MutableStateFlow(TreeAnalysisRecordExtra(null, null, null))
    val extraState: StateFlow<TreeAnalysisRecordExtra> = _extraState.asStateFlow()

    private val _treeHeight = MutableStateFlow("")
    val treeHeightState = _treeHeight.asStateFlow()

    private val _treeDiameter = MutableStateFlow("")
    val treeDiameterState = _treeDiameter.asStateFlow()

    /**
     * 规范化规则：
     * 1) 只允许 [0-9 .]
     * 2) 若存在多个 '.'，仅保留“最后输入”的那个
     * 3) 若以 '.' 开头，修正为 "0."
     */
    fun normalizeDecimal(input: String): String {
        if (input.isEmpty()) return ""

        // 仅保留数字和小数点
        val filtered = buildString {
            input.forEach { c -> if (c.isDigit() || c == '.') append(c) }
        }
        if (filtered.isEmpty()) return ""

        val lastDotIndex = filtered.lastIndexOf('.')
        val digitsOnly = filtered.filter { it != '.' }

        val result =
            if (lastDotIndex >= 0) {
                // 计算“最后一个点之前的数字数量”
                val digitsBeforeLastDot = filtered.take(lastDotIndex).count { it.isDigit() }
                buildString {
                    append(digitsOnly.substring(0, digitsBeforeLastDot))
                    append('.')
                    append(digitsOnly.substring(digitsBeforeLastDot))
                }
            } else {
                digitsOnly
            }

        // 以点开头 → “0.” 修正
        return if (result.startsWith(".")) "0$result" else result
    }


    private val _leafImageUri = MutableStateFlow<Uri?>(null)
    val leafImageUri: StateFlow<Uri?> = _leafImageUri.asStateFlow()

    private val _showInferModal = MutableStateFlow(false)
    val showInferModal: StateFlow<Boolean> = _showInferModal.asStateFlow()

    private val _leafBitmap = MutableStateFlow<Bitmap?>(null)
    val leafBitmap: StateFlow<Bitmap?> = _leafBitmap.asStateFlow()

    private val _showReportGenModal = MutableStateFlow(false)
    val showReportGenModal: StateFlow<Boolean> = _showReportGenModal.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var imageDirPath: String? = null
    private val gson = Gson()

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    init {
        if (isEditMode && recordId != null) {
            loadExistingRecord(recordId)
        } else {
            generateImageDir()
            generateRecordName()
        }
    }

    private fun generateImageDir() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        imageDirPath = "tree_analysis_$timestamp"
        _formState.value = _formState.value.copy(imageDir = imageDirPath)
    }

    private fun generateRecordName() {
        val timestamp = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
        _formState.value = _formState.value.copy(name = "树木分析 $timestamp")
    }

    private fun loadExistingRecord(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val record = repository.getRecordById(id)
                
                if (record != null) {
                    // Parse extra data
                    val extraData = try {
                        if (record.extra != null) {
                            gson.fromJson(record.extra, TreeAnalysisRecordExtra::class.java)
                        } else TreeAnalysisRecordExtra(null, null, null)
                    } catch (e: Exception) {
                        TreeAnalysisRecordExtra(null, null, null)
                    }
                    
                    // Update form state
                    _formState.value = _formState.value.copy(
                        name = record.name,
                        note = record.note,
                        imageDir = record.imageDir,
                        report = record.report
                    )
                    
                    // Update extra state
                    _extraState.value = extraData
                    
                    // Set the imageDirPath for image operations
                    imageDirPath = record.imageDir

                    loadExistingLeafImage(context)
                    
                } else {
                    _saveState.value = SaveState.Error("记录不存在")
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("加载记录失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun updateNote(note: String) {
        _formState.value = _formState.value.copy(note = note.takeIf { it.isNotBlank() })
    }

    fun updateTreeSpecies(species: String) {
        _extraState.value = _extraState.value.copy(treeSpecies = species.takeIf { it.isNotBlank() })
    }

    fun updateTreeHeight(height: String) {
        _treeHeight.value = normalizeDecimal(height)
        val heightFloat = height.toFloatOrNull()
        _extraState.value = _extraState.value.copy(treeHeight = heightFloat)
    }

    fun updateTreeDiameter(diameter: String) {
        _treeDiameter.value = normalizeDecimal(diameter)
        val diameterFloat = diameter.toFloatOrNull()
        _extraState.value = _extraState.value.copy(treeDiameter = diameterFloat)
    }

    fun updateReport(report: String) {
        _formState.value = _formState.value.copy(report = report.takeIf { it.isNotBlank() })
    }

    fun onImageSelected(uri: Uri) {
        _leafImageUri.value = uri
    }

    fun onInferSpeciesClick(context: Context) {
        val uri = _leafImageUri.value
        if (uri == null) {
            // Will be handled in UI with Toast
            return
        }
        
        viewModelScope.launch {
            try {
                val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                _leafBitmap.value = bitmap
                _showInferModal.value = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun dismissInferModal() {
        _showInferModal.value = false
        _leafBitmap.value = null
    }
    
    fun applyPredictedSpecies(latinName: String, chineseName: String) {
        val combinedName = chineseName
        updateTreeSpecies(combinedName)
        dismissInferModal()
    }

    fun onGenerateReportClick() {
        _showReportGenModal.value = true
    }

    fun dismissReportGenModal() {
        _showReportGenModal.value = false
    }

    fun saveRecord() {
        viewModelScope.launch {
            try {
                _saveState.value = SaveState.Saving
                
                val currentState = _formState.value
                val currentExtraState = _extraState.value
                val currentTime = System.currentTimeMillis()
                
                val extraJson = gson.toJson(currentExtraState)
                
                if (isEditMode && recordId != null) {
                    // Update existing record
                    val record = TreeAnalysisRecordEntity(
                        id = recordId,
                        name = currentState.name,
                        note = currentState.note,
                        imageDir = currentState.imageDir,
                        extra = extraJson,
                        report = currentState.report,
                        createdAt = currentTime, // This will be ignored in update
                        deletedAt = null
                    )
                    
                    repository.updateRecord(record)
                    _saveState.value = SaveState.Success
                } else {
                    // Create new record
                    val record = TreeAnalysisRecordEntity(
                        id = null,
                        name = currentState.name,
                        note = currentState.note,
                        imageDir = currentState.imageDir,
                        extra = extraJson,
                        report = currentState.report,
                        createdAt = currentTime,
                        deletedAt = null
                    )
                    
                    repository.insertRecord(record)
                    _saveState.value = SaveState.Success
                }
                
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("保存失败: ${e.message}")
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun isEditMode(): Boolean = isEditMode

    fun getEditingRecordId(): Long? = recordId

    fun loadExistingLeafImage(context: Context) {
        XLog.d("isEditMode: $isEditMode, imageDirPath: ${imageDirPath}")

        if (isEditMode && imageDirPath != null) {
            viewModelScope.launch {
                try {
                    val externalDir = File(context.getExternalFilesDir(null), imageDirPath!!)
                    val leafFile = File(externalDir, "leaf.png")
                    XLog.d("leafFile: $leafFile, leafFile.exists(): ${leafFile.exists()}")
                    if (leafFile.exists()) {
                        // Use content URI instead of file URI for better compatibility
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            leafFile
                        )
                        _leafImageUri.value = uri
                    }
                } catch (e: Exception) {
                    // Fallback to file URI if FileProvider fails
                    try {
                        val externalDir = File(context.getExternalFilesDir(null), imageDirPath!!)
                        val leafFile = File(externalDir, "leaf.png")
                        if (leafFile.exists()) {
                            val uri = Uri.fromFile(leafFile)
                            _leafImageUri.value = uri
                        }
                    } catch (fallbackE: Exception) {
                        // Ignore if image doesn't exist
                    }
                }
            }
        }
    }

    fun saveImageToExternalStorage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val externalDir = File(context.getExternalFilesDir(null), imageDirPath ?: "")
                if (!externalDir.exists()) {
                    externalDir.mkdirs()
                }
                
                val leafFile = File(externalDir, "leaf.png")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(leafFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getLeafImageFile(context: Context): File? {
        return imageDirPath?.let { dirPath ->
            val externalDir = File(context.getExternalFilesDir(null), dirPath)
            val leafFile = File(externalDir, "leaf.png")
            if (leafFile.exists()) leafFile else null
        }
    }
}