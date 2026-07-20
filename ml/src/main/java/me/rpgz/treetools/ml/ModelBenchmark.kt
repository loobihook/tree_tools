package me.rpgz.treetools.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

private const val TAG = "ModelBenchmark"

class ModelBenchmark(private val context: Context) {
    private lateinit var inference: ModelInference
    private val inferenceTimes = mutableListOf<Long>()
    private var correctCount = 0
    private var totalCount = 0

    suspend fun runBenchmark(
        modelPath: String,
        modelType: ModelType,
        testSamples: List<TestSample>,
        warmUpRuns: Int = 5
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting benchmark for $modelType: $modelPath")

        inference = when (modelType) {
            ModelType.TFLITE -> TFLiteInference(context)
            ModelType.ONNX -> OnnxInference(context)
        }

        try {
            inference.initialize(modelPath)
            Log.d(TAG, "Model initialized")

            repeat(warmUpRuns) {
                val warmUpImage = testSamples.firstOrNull()?.image
                if (warmUpImage != null) {
                    inference.infer(warmUpImage)
                }
            }
            Log.d(TAG, "Warm-up completed")

            inferenceTimes.clear()
            correctCount = 0
            totalCount = 0

            testSamples.forEach { sample ->
                val result = inference.infer(sample.image)
                inferenceTimes.add(result.inferenceTimeMs)

                val predictedLabel = result.topK.firstOrNull()?.first
                if (predictedLabel == sample.expectedLabel) {
                    correctCount++
                }
                totalCount++

                Log.d(TAG, "Sample ${totalCount}: " +
                    "Predicted=${predictedLabel}, Expected=${sample.expectedLabel}, " +
                    "Time=${result.inferenceTimeMs}ms"
                )
            }

            val result = calculateResults(modelType, modelPath)
            Log.d(TAG, "Benchmark completed: $result")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Benchmark failed", e)
            throw e
        } finally {
            inference.close()
        }
    }

    private fun calculateResults(modelType: ModelType, modelPath: String): BenchmarkResult {
        val modelName = modelPath.substringAfterLast("/")

        val totalTime = inferenceTimes.sum()
        val avgTime = if (inferenceTimes.isNotEmpty()) {
            inferenceTimes.average()
        } else {
            0.0
        }
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

        val accuracy = if (totalCount > 0) {
            (correctCount.toDouble() / totalCount) * 100
        } else {
            0.0
        }

        val throughput = if (totalTime > 0) {
            (totalCount.toDouble() * 1000) / totalTime
        } else {
            0.0
        }

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

    fun generateTestSamples(count: Int): List<TestSample> {
        val samples = mutableListOf<TestSample>()
        val random = java.util.Random(System.currentTimeMillis())
        repeat(count) {
            val bitmap = generateRandomBitmap(random)
            val label = random.nextInt(OUTPUT_CLASSES)
            val labelName = id2label[label]?.chineseName ?: "Unknown"
            samples.add(TestSample(bitmap, label, labelName))
        }
        return samples
    }

    private fun generateRandomBitmap(random: java.util.Random): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        for (i in pixels.indices) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            pixels[i] = (0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
        }
        bitmap.setPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        return bitmap
    }

    private companion object {
        private const val IMAGE_SIZE = 224
        private const val OUTPUT_CLASSES = 50
    }
}