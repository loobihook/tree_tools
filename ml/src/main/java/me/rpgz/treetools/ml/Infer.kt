package me.rpgz.treetools.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.google.gson.annotations.SerializedName
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp


internal data class Result(
    var detectedIndices: List<Int> = emptyList(),
    var detectedScore: MutableList<Float> = mutableListOf<Float>(),
    var processTimeMs: Long = 0
)


data class PlantResult(
    @SerializedName("score")
    val score: Double,
    @SerializedName("name")
    val name: String
)

data class PlantIdentificationResponse(
    @SerializedName("result")
    val result: List<PlantResult>,
    @SerializedName("log_id")
    val logId: String
)

private const val TAG = "Infer"

class Infer(modelBytes: ByteBuffer) {
    private var interpreter: Interpreter = Interpreter(
        modelBytes, Interpreter.Options()
            .setNumThreads(1)
            .setUseNNAPI(false)
            .setCancellable(false)
    )

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp((it - maxLogit).toDouble()) }
        val sumExp = expValues.sum()
        return expValues.map { (it / sumExp).toFloat() }.toFloatArray()
    }

    fun analyze(image: Bitmap, context: Context): PlantIdentificationResponse {
        val softwareBitmap = if (image.config == Bitmap.Config.HARDWARE) {
            image.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            image
        }

        val bitmap = softwareBitmap.scale(224, 224, false)
        val preprocessed = bitmapToModelInput(bitmap, context)
        val outputArr = Array(1) { FloatArray(50) }
        interpreter.run(preprocessed, outputArr)
        Log.d(TAG, "outputArr: ${outputArr.size}  ${outputArr[0].contentToString()}")
        val predictions = outputArr[0]
        
        val probabilities = softmax(predictions)
        
        val indexedResults = probabilities.mapIndexed { index, score -> 
            Pair(index, score) 
        }.sortedByDescending { it.second }
        
        val top3Results = indexedResults.take(3).map { (index, score) ->
            val name = id2label[index]?.chineseName ?: "Unknown"
            PlantResult(score.toDouble(), name)
        }

        return PlantIdentificationResponse(
            result = top3Results,
            logId = System.currentTimeMillis().toString()
        )
    }


    private fun preProcess(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224
        val pixelSize = 3
        val byteSize = 4
        val inputBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * pixelSize * byteSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        return inputBuffer
    }

    private val IMAGE_SIZE = 224
    private val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    /**
     * Prepares a (1,224,224,3) input tensor from a color Bitmap using TFLite Support.
     *
     * @param bitmap RGB color bitmap
     * @param floatModel true if model expects FLOAT32 (e.g., MobileNet); false for UINT8 (quantized)
     * @param toMinusOneToOne pass true if your float model expects [-1,1] normalization
     * @return TensorBuffer shaped [1,224,224,3] with the image data
     */
    private fun bitmapToModelInput(bitmap: Bitmap, context: Context): ByteBuffer {
        val inputType = DataType.FLOAT32
        var tensorImage = TensorImage(inputType).apply { load(bitmap) }
        val processor = ImageProcessor.Builder()
            .add(ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))      // x := x / 255  -> [0,1]
//            .add(NormalizeOp(IMAGENET_MEAN, IMAGENET_STD)) // per-channel (x - mean)/std
            .build()
        tensorImage = processor.process(tensorImage)
        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, IMAGE_SIZE, IMAGE_SIZE, 3),
            inputType
        )

        val timestampSeconds = System.currentTimeMillis() / 1000L
        val file = File(context.filesDir, "preprocessed_${timestampSeconds}")
        saveTensorImageBuffer(tensorImage, file)
        inputBuffer.loadBuffer(tensorImage.buffer)
        Log.d(TAG, "preprocessing done $inputBuffer")
        return inputBuffer.buffer.order(ByteOrder.nativeOrder())
    }

    private fun saveTensorImageBuffer(tensorImage: TensorImage, file: File) {
        val fos = FileOutputStream(file)
        val channel = fos.channel
        val buffer: ByteBuffer = tensorImage.buffer.duplicate()
        buffer.order(ByteOrder.nativeOrder())
        channel.write(buffer)
        channel.close()
        fos.close()
        Log.d(TAG, "buffer saved to ${file.path}")
    }
}
