package me.rpgz.treetools.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

private const val TAG = "OnnxInference"
private const val IMAGE_SIZE = 224
private const val OUTPUT_CLASSES = 50

class OnnxInference(private val context: Context) : ModelInference {
    private var modelName: String = ""

    override fun initialize(modelPath: String) {
        modelName = modelPath.substringAfterLast("/")
        Log.d(TAG, "ONNX model initialization skipped - requires ONNX Runtime library")
        throw UnsupportedOperationException("ONNX Runtime not available")
    }

    override fun infer(image: Bitmap): InferenceResult {
        throw UnsupportedOperationException("ONNX Runtime not available")
    }

    override fun close() {
        Log.d(TAG, "ONNX session closed")
    }

    override fun getModelType(): ModelType {
        return ModelType.ONNX
    }

    fun getModelName(): String {
        return modelName
    }
}