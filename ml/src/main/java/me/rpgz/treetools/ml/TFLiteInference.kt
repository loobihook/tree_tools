package me.rpgz.treetools.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import kotlin.math.exp

private const val TAG = "TFLiteInference"
private const val IMAGE_SIZE = 224
private const val OUTPUT_CLASSES = 50

class TFLiteInference(private val context: Context) : ModelInference {
    private var interpreter: Interpreter? = null
    private var modelName: String = ""

    override fun initialize(modelPath: String) {
        modelName = modelPath.substringAfterLast("/")
        Log.d(TAG, "Initializing TFLite model: $modelName")

        val buffer = loadModelFile(modelPath)
        interpreter = Interpreter(
            buffer, Interpreter.Options()
                .setNumThreads(1)
                .setUseNNAPI(false)
                .setCancellable(false)
        )
        Log.d(TAG, "TFLite model initialized successfully")
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        return if (modelPath.startsWith("assets://")) {
            val assetName = modelPath.removePrefix("assets://")
            context.assets.open(assetName).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteBuffer.allocateDirect(size)
                val channel = Channels.newChannel(inputStream)
                while (buffer.hasRemaining()) {
                    channel.read(buffer)
                }
                buffer.flip()
                buffer
            }
        } else {
            FileInputStream(modelPath).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteBuffer.allocateDirect(size)
                val channel = Channels.newChannel(inputStream)
                while (buffer.hasRemaining()) {
                    channel.read(buffer)
                }
                buffer.flip()
                buffer
            }
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp((it - maxLogit).toDouble()) }
        val sumExp = expValues.sum()
        return expValues.map { (it / sumExp).toFloat() }.toFloatArray()
    }

    override fun infer(image: Bitmap): InferenceResult {
        val startTime = System.currentTimeMillis()

        val softwareBitmap = if (image.config == Bitmap.Config.HARDWARE) {
            image.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            image
        }

        val inputBuffer = bitmapToModelInput(softwareBitmap)
        val outputArr = Array(1) { FloatArray(OUTPUT_CLASSES) }

        interpreter?.run(inputBuffer, outputArr)

        val inferenceTime = System.currentTimeMillis() - startTime
        val predictions = softmax(outputArr[0])
        val topK = predictions.mapIndexed { index, score -> Pair(index, score) }
            .sortedByDescending { it.second }
            .take(3)

        return InferenceResult(
            predictions = predictions,
            topK = topK,
            inferenceTimeMs = inferenceTime,
            modelType = ModelType.TFLITE
        )
    }

    private fun bitmapToModelInput(bitmap: Bitmap): ByteBuffer {
        val inputType = DataType.FLOAT32
        var tensorImage = TensorImage(inputType).apply { load(bitmap) }
        val processor = ImageProcessor.Builder()
            .add(ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
        tensorImage = processor.process(tensorImage)
        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, IMAGE_SIZE, IMAGE_SIZE, 3),
            inputType
        )
        inputBuffer.loadBuffer(tensorImage.buffer)
        return inputBuffer.buffer.order(ByteOrder.nativeOrder())
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "TFLite interpreter closed")
    }

    override fun getModelType(): ModelType {
        return ModelType.TFLITE
    }

    fun getModelName(): String {
        return modelName
    }
}