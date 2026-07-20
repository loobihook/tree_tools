package me.rpgz.treetools.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rpgz.treetools.ml.BenchmarkResult
import me.rpgz.treetools.ml.ModelType
import me.rpgz.treetools.ml.TestSample
import javax.inject.Inject

private const val TAG = "BenchmarkViewModel"

data class BenchmarkUiState(
    val selectedModel: ModelType = ModelType.TFLITE,
    val sampleCount: Int = 50,
    val isRunning: Boolean = false,
    val currentProgress: Int = 0,
    val results: List<BenchmarkResult> = emptyList(),
    val error: String? = null,
    val modelExists: Boolean = true,
    val testSetLoaded: Boolean = false
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    private var realTestSamples: List<TestSample> = emptyList()

    init {
        checkModelExists()
        loadRealTestSamples()
    }

    private fun checkModelExists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tfliteExists = context.assets.list("")?.contains("resnet50d_1_224_224_3_20250911.tflite") == true
                val onnxExists = context.assets.list("")?.contains("resnet50d.onnx") == true
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(modelExists = tfliteExists || onnxExists) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking model existence", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(modelExists = false) }
                }
            }
        }
    }

    private fun loadRealTestSamples() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val samples = mutableListOf<TestSample>()
                val testImagesDir = "test_images"
                val imageFiles = context.assets.list(testImagesDir) ?: emptyArray()
                
                if (imageFiles.isEmpty()) {
                    Log.w(TAG, "No test images found in assets/test_images")
                    return@launch
                }

                for (file in imageFiles) {
                    if (!file.lowercase().endsWith(".jpg") && !file.lowercase().endsWith(".png")) {
                        continue
                    }

                    val parts = file.split("_")
                    if (parts.size >= 2) {
                        val labelStr = parts[0]
                        val label = try {
                            labelStr.toInt()
                        } catch (e: NumberFormatException) {
                            0
                        }
                        
                        val inputStream = context.assets.open("$testImagesDir/$file")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            val labelName = me.rpgz.treetools.ml.id2label[label]?.chineseName ?: "Unknown"
                            samples.add(TestSample(bitmap, label, labelName))
                        }
                    }
                }

                realTestSamples = samples
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(testSetLoaded = samples.isNotEmpty()) }
                }
                Log.d(TAG, "Loaded ${samples.size} real test samples")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading real test samples", e)
            }
        }
    }

    fun selectModel(modelType: ModelType) {
        _uiState.update { it.copy(selectedModel = modelType) }
    }

    fun setSampleCount(count: Int) {
        _uiState.update { it.copy(sampleCount = count) }
    }

    fun runBenchmark() {
        Log.d(TAG, "runBenchmark called")
        if (_uiState.value.isRunning) {
            Log.d(TAG, "Already running, ignoring")
            return
        }

        val modelType = _uiState.value.selectedModel
        val sampleCount = _uiState.value.sampleCount

        Log.d(TAG, "Starting benchmark: modelType=$modelType, sampleCount=$sampleCount")

        _uiState.update { it.copy(
            isRunning = true,
            currentProgress = 0,
            error = null
        ) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating test samples")
                val testSamples = if (realTestSamples.isNotEmpty()) {
                    realTestSamples.take(sampleCount)
                } else {
                    generateTestSamples(sampleCount)
                }

                if (testSamples.isEmpty()) {
                    throw Exception("没有可用的测试样本")
                }

                Log.d(TAG, "Generated ${testSamples.size} test samples, starting benchmark")

                val result = performBenchmark(modelType, testSamples)
                
                Log.d(TAG, "Benchmark completed successfully")

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(
                        isRunning = false,
                        currentProgress = sampleCount,
                        results = listOf(result) + it.results.take(4)
                    ) }
                }

                testSamples.forEach { it.image.recycle() }
                Log.d(TAG, "Bitmap resources recycled")

            } catch (e: Exception) {
                Log.e(TAG, "Benchmark failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(
                        isRunning = false,
                        error = "测试失败: ${e.message}"
                    ) }
                }
            }
        }
    }

    private suspend fun performBenchmark(modelType: ModelType, testSamples: List<TestSample>): BenchmarkResult {
        Log.d(TAG, "performBenchmark: modelType=$modelType")

        val inference = when (modelType) {
            ModelType.TFLITE -> me.rpgz.treetools.ml.TFLiteInference(context)
            ModelType.ONNX -> me.rpgz.treetools.ml.OnnxInference(context)
        }

        val modelPath = when (modelType) {
            ModelType.TFLITE -> "assets://resnet50d_1_224_224_3_20250911.tflite"
            ModelType.ONNX -> "assets://resnet50d.onnx"
        }

        try {
            Log.d(TAG, "Initializing model: $modelPath")
            inference.initialize(modelPath)
            Log.d(TAG, "Model initialized successfully")

            repeat(3) {
                val warmUpImage = testSamples.firstOrNull()?.image
                if (warmUpImage != null) {
                    Log.d(TAG, "Warm-up run $it")
                    inference.infer(warmUpImage)
                }
            }
            Log.d(TAG, "Warm-up completed")

            val inferenceTimes = mutableListOf<Long>()
            var correctCount = 0
            var totalCount = 0

            for ((index, sample) in testSamples.withIndex()) {
                Log.d(TAG, "Processing sample ${index + 1}/${testSamples.size}")
                val result = inference.infer(sample.image)
                inferenceTimes.add(result.inferenceTimeMs)

                val predictedLabel = result.topK.firstOrNull()?.first
                if (predictedLabel == sample.expectedLabel) {
                    correctCount++
                }
                totalCount++

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(currentProgress = index + 1) }
                }
            }

            Log.d(TAG, "All samples processed, calculating results")
            return calculateResults(modelType, modelPath, inferenceTimes, correctCount, totalCount)

        } finally {
            Log.d(TAG, "Closing inference")
            inference.close()
            Log.d(TAG, "Inference closed")
        }
    }

    private fun calculateResults(
        modelType: ModelType,
        modelPath: String,
        inferenceTimes: List<Long>,
        correctCount: Int,
        totalCount: Int
    ): BenchmarkResult {
        val modelName = modelPath.substringAfterLast("/")

        val totalTime = inferenceTimes.sum()
        val avgTime = if (inferenceTimes.isNotEmpty()) inferenceTimes.average() else 0.0
        val minTime = inferenceTimes.minOrNull() ?: 0
        val maxTime = inferenceTimes.maxOrNull() ?: 0
        val medianTime = if (inferenceTimes.isNotEmpty()) {
            val sorted = inferenceTimes.sorted()
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
        } else {
            0.0
        }

        val accuracy = if (totalCount > 0) (correctCount.toDouble() / totalCount) * 100 else 0.0
        val throughput = if (totalTime > 0) (totalCount.toDouble() * 1000) / totalTime else 0.0

        return BenchmarkResult(
            modelType = modelType,
            modelName = modelName,
            totalInferenceTimeMs = totalTime,
            averageTimeMs = avgTime,
            minTimeMs = minTime,
            maxTimeMs = maxTime,
            medianTimeMs = medianTime,
            accuracy = accuracy,
            sampleCount = totalCount,
            throughput = throughput
        )
    }

    private fun generateTestSamples(count: Int): List<TestSample> {
        val samples = mutableListOf<TestSample>()
        val random = java.util.Random(System.currentTimeMillis())
        repeat(count) {
            val bitmap = generateRandomBitmap(random)
            val label = random.nextInt(50)
            val labelName = me.rpgz.treetools.ml.id2label[label]?.chineseName ?: "Unknown"
            samples.add(TestSample(bitmap, label, labelName))
        }
        return samples
    }

    private fun generateRandomBitmap(random: java.util.Random): Bitmap {
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(224 * 224)
        for (i in pixels.indices) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            pixels[i] = (0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
        }
        bitmap.setPixels(pixels, 0, 224, 0, 0, 224, 224)
        return bitmap
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}