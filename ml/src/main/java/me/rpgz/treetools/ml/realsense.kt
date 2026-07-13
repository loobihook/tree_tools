package me.rpgz.treetools.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.intel.realsense.librealsense.*
import java.io.File
import java.io.FileOutputStream


private const val TAG = "RealSenseUtil"

fun captureRgbAndPointCloud(context: Context, outDir: File) {
    // 1) 初始化 RealSense 上下文
    RsContext.init(context)

    val pipeline = Pipeline()
    val config = Config().apply {
        enableStream(StreamType.COLOR,StreamFormat.RGB8)
        enableStream(StreamType.DEPTH, StreamFormat.Z16)
    }

    val profile = pipeline.start(config)  // 打开相机流

    val align = Align(StreamType.COLOR)   // 如不需要逐像素对齐，可不使用
    val pc = Pointcloud()

    try {
        // 可选：预热丢掉几帧，避免首帧曝光不稳
        repeat(5) { pipeline.waitForFrames().close() }

        pipeline.waitForFrames().use { fs ->
            // 对齐到彩色（可选）
            val aligned: FrameSet = align.process(fs)
            val rgbFrame = aligned.first(StreamType.INFRARED)
        }
    } finally {
        // 资源释放
        pc.close()
        align.close()
        pipeline.stop()
        pipeline.close()
    }
}

/**
 * 捕获单张深度图并保存为灰度图像
 * @return 成功返回true，失败返回false
 */
fun captureDepthFrame(): Bitmap? {

    var tempPipeline: Pipeline? = null
    try {
        // 创建临时pipeline用于单次捕获
        tempPipeline = Pipeline()

        Config().use { cfg ->
            cfg.enableStream(StreamType.DEPTH, 640, 480)
            cfg.enableStream(StreamType.COLOR, 640, 480)
            tempPipeline.start(cfg).use { pp ->
                Log.d(TAG, "Pipeline started for depth capture")
                tempPipeline.waitForFrames(5000).use { frames ->  // 5秒超时
                    Log.i(TAG, "frames: ${frames}")
                    val depthFrame = frames.first(StreamType.DEPTH).`as`<DepthFrame>(Extension.DEPTH_FRAME)
                    Log.i(TAG, "depthFrame: ${depthFrame} ${depthFrame::class.java}")

                    if (depthFrame != null) {
                        val depth = depthFrame
                        val bmp = convertDepthToGrayscale(depthFrame)
                        depthFrame.close()
                        return bmp
                    } else {
                        Log.e(TAG, "No depth frame received")
                        return null
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error capturing depth frame: " + e.message)
        return null
    } finally {
        // 确保关闭临时pipeline
        if (tempPipeline != null) {
            try {
                tempPipeline.stop()
                Log.d(TAG, "Temporary pipeline stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping temporary pipeline: " + e.message)
            }
        }
    }
}


/**
 * 将深度帧转换为灰度图像并保存
 * @param depthFrame 深度帧
 * @return 成功返回true，失败返回false
 */
fun convertDepthToGrayscale(depthFrame: DepthFrame): Bitmap? {
    try {
        // 获取深度数据
        val width = depthFrame.getWidth()
        val height = depthFrame.getHeight()

        Log.d(TAG, "Depth frame size: " + width + "x" + height)


        // 获取深度数据字节数组
        val depthData = ByteArray(depthFrame.getDataSize())
        depthFrame.getData(depthData)

        return depthByteBufferToBitmap(depthData, width, height).first

    } catch (e: java.lang.Exception) {
        Log.e(TAG, "Error converting depth to grayscale: " + e.message)
        return null
    }
}


fun colorByteBufferToBitmap(colorData: ByteArray, width: Int, height: Int): Bitmap {

    // Create bitmap (assuming RGB8 format)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Convert RGB -> ARGB and copy into bitmap
    val pixels = IntArray(width * height)
    var i = 0
    var j = 0
    while (i < colorData.size) {
        val r = colorData[i].toInt() and 0xFF
        val g = colorData[i + 1].toInt() and 0xFF
        val b = colorData[i + 2].toInt() and 0xFF
        pixels[j] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        i += 3
        j++
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

fun depthByteBufferToBitmap(depthData: ByteArray, width: Int, height: Int): Pair<Bitmap, Array<IntArray>> {
    val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val depthMap = Array(height) { IntArray(width) }
    var maxDepth = 0
    for (i in 0..<width * height) {
        val depth =
            ((depthData[i * 2 + 1].toInt() and 0xFF) shl 8) or (depthData[i * 2].toInt() and 0xFF)
        if (depth > maxDepth) {
            maxDepth = depth
        }
    }

    for (y in 0..<height) { // (y+1)-th row
        for (x in 0..<width) { // (x+1)-th column
            val pixelIndex = y * width + x
            val depth =
                ((depthData[pixelIndex * 2 + 1].toInt() and 0xFF) shl 8) or (depthData[pixelIndex * 2].toInt() and 0xFF)
            var grayValue = 0
            if (maxDepth > 0 && depth > 0) {
                grayValue = ((depth / maxDepth.toFloat()) * 255).toInt()
            }
            val color = Color.rgb(grayValue, grayValue, grayValue)
            grayscaleBitmap.setPixel(x, y, color)
            depthMap[y][x] = depth
        }
    }
    return Pair(grayscaleBitmap, depthMap)
}


// 将 VideoFrame(RGB8) 保存为 JPEG
private fun saveColorAsJpeg(color: VideoFrame, outFile: File) {
    val w = color.width
    val h = color.height
    val stride = color.stride   // 每行字节数
    val rowBytes = w * 3        // RGB8 每像素3字节

    val raw = ByteArray(color.dataSize)
    color.getData(raw)

    // 转成 ARGB_8888 Bitmap（按行处理以适配 stride）
    val pixels = IntArray(w * h)
    var src = 0
    var dst = 0
    for (y in 0 until h) {
        val lineStart = y * stride
        for (x in 0 until w) {
            val i = lineStart + x * 3
            val r = raw[i].toInt() and 0xFF
            val g = raw[i + 1].toInt() and 0xFF
            val b = raw[i + 2].toInt() and 0xFF
            pixels[dst++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        src += rowBytes
    }

    val bmp = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    FileOutputStream(outFile).use { fos ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos)
    }
}