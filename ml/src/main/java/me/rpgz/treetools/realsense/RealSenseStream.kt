package me.rpgz.treetools.realsense

import android.util.Log
import com.intel.realsense.librealsense.Config
import com.intel.realsense.librealsense.Extension
import com.intel.realsense.librealsense.Pipeline
import com.intel.realsense.librealsense.StreamType
import com.intel.realsense.librealsense.VideoStreamProfile
import kotlinx.coroutines.*
import kotlin.use

private val TAG = "FrameStreamingThread"
data class RealSenseIntrinsic(
    val width: Int,
    val height: Int,
    val ppx: Float,
    val ppy: Float,
    val fx: Float,
    val fy: Float,
)

class FrameStreamingThread(
    private val task: (color: ByteArray, depth: ByteArray, width: Int, height: Int) -> Unit   // 外部回调
) {
    private var streamJob: Job? = null
    private var pipeline: Pipeline? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default) // 背景线程

    private var realSenseIntrinsic: RealSenseIntrinsic? = null

    fun getRealSenseIntrinsic(): RealSenseIntrinsic? {
        return realSenseIntrinsic
    }

    fun start() {
        // 避免重复开启
        if (streamJob?.isActive == true) return

        streamJob = scope.launch {
            try {
                // Initialize pipeline once
                pipeline = Pipeline()
                Config().use { cfg ->
                    cfg.enableStream(StreamType.DEPTH, 640, 480)
                    cfg.enableStream(StreamType.COLOR, 640, 480)
                    pipeline!!.start(cfg).use { pp -> }
                }

                Log.d(TAG, "Pipeline started for depth capture")

                while (isActive) {
                    try {
                        // 阻塞取帧放到 IO/Default 线程
                        val frames = withContext(Dispatchers.IO) {
                            pipeline!!.waitForFrames(1000) // 1秒超时
                        }
                        frames.use { frameSet ->
                            val color = frameSet.first(StreamType.COLOR)
                            val depth = frameSet.first(StreamType.DEPTH)
                            if (realSenseIntrinsic == null) {
                                val p =
                                    depth.profile.`as`<VideoStreamProfile>(Extension.VIDEO_PROFILE)
                                realSenseIntrinsic = RealSenseIntrinsic(
                                    width = p.intrinsic.width,
                                    height = p.intrinsic.height,
                                    ppx = p.intrinsic::class.java.getDeclaredField("mPpx").let {
                                        it.isAccessible = true
                                        it.get(p.intrinsic) as Float
                                    },
                                    ppy = p.intrinsic::class.java.getDeclaredField("mPpy").let {
                                        it.isAccessible = true
                                        it.get(p.intrinsic) as Float
                                    },
                                    fx = p.intrinsic::class.java.getDeclaredField("mFx").let {
                                        it.isAccessible = true
                                        it.get(p.intrinsic) as Float
                                    },
                                    fy = p.intrinsic::class.java.getDeclaredField("mFy").let {
                                        it.isAccessible = true
                                        it.get(p.intrinsic) as Float
                                    }
                                )
                            }

                            val colorBuf = ByteArray(640 * 480 * 3)
                            val depthBuf = ByteArray(640 * 480 * 2)

                            color.use { it.getData(colorBuf) }
                            depth.use { it.getData(depthBuf) }
                            task(colorBuf, depthBuf, 640, 480)
                        }
                        // 可选：限帧或给消息队列让步
                        delay(16) // ~60fps
                        yield() // 让步给其他协程
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e // 重新抛出取消异常
                        Log.w(TAG, "Frame capture failed: ${e.message}")
                        if (isActive) {
                            delay(100) // Brief pause before retry
                        }
                    }
                }
            } catch (ce: CancellationException) {
                // 正常取消，忽略
                Log.d(TAG, "Frame streaming cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Pipeline initialization failed", e)
            } finally {
                // Cleanup pipeline
                pipeline?.let { p ->
                    try {
                        p.stop()
                        p.close()
                        Log.d(TAG, "Pipeline resources released")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing pipeline resources", e)
                    } finally {
                        pipeline = null
                    }
                }
                Log.d(TAG, "Frame streaming ended")
            }
        }
    }

    fun pause() {
        streamJob?.cancel()
    }

    fun resume() {
        start() // Restart streaming
    }

    fun stop() {
        streamJob?.cancel()   // 立即可取消
        streamJob = null
    }

    fun stopAsync(onStopped: (() -> Unit)? = null) {
        streamJob?.cancel()   // 立即可取消
        streamJob = null
        onStopped?.invoke()
    }

    fun isActive(): Boolean = streamJob?.isActive == true
}

