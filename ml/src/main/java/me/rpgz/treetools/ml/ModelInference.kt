package me.rpgz.treetools.ml

import android.graphics.Bitmap

interface ModelInference {
    fun initialize(modelPath: String)
    fun infer(image: Bitmap): InferenceResult
    fun close()
    fun getModelType(): ModelType
}

enum class ModelType {
    TFLITE,
    ONNX
}

data class InferenceResult(
    val predictions: FloatArray,
    val topK: List<Pair<Int, Float>>,
    val inferenceTimeMs: Long,
    val modelType: ModelType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InferenceResult
        if (!predictions.contentEquals(other.predictions)) return false
        if (topK != other.topK) return false
        if (inferenceTimeMs != other.inferenceTimeMs) return false
        if (modelType != other.modelType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = predictions.contentHashCode()
        result = 31 * result + topK.hashCode()
        result = 31 * result + inferenceTimeMs.hashCode()
        result = 31 * result + modelType.hashCode()
        return result
    }
}

data class BenchmarkResult(
    val modelType: ModelType,
    val modelName: String,
    val totalInferenceTimeMs: Long,
    val averageTimeMs: Double,
    val minTimeMs: Long,
    val maxTimeMs: Long,
    val medianTimeMs: Double,
    val accuracy: Double,
    val sampleCount: Int,
    val throughput: Double
)

data class TestSample(
    val image: Bitmap,
    val expectedLabel: Int,
    val labelName: String
)