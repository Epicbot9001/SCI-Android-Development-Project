package edu.gatech.ce.allgather.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.camera2.*
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import edu.gatech.ce.allgather.utils.IRecord
import edu.gatech.ce.allgather.utils.recordLog
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CameraX Helper
 * @author Justin Lee
 * @date 2020/6/15
 */
class CameraXHelper(val context: Context, val texture: PreviewView, val lifecycleOwner: LifecycleOwner) :
    IRecord {

    companion object {
        private val TAG = CameraXHelper::class.java.simpleName

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    }

    init {
        texture.post { initCamera() }
    }

    private var videoCapture: VideoCapture? = null

    private var cameraControl: CameraControlInternal? = null
    private var cameraSelector: CameraSelector? = null
    private var camera2Interop: Camera2Interop.Extender<Preview>? = null

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun initCamera() {
        val metrics = DisplayMetrics().also { texture.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val previewBuilder = Preview.Builder()
            camera2Interop = Camera2Interop.Extender(previewBuilder)
            // close auto focus
            camera2Interop?.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
//            camera2Interop?.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)

            // fixed focus distance
            camera2Interop?.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
            val preview = previewBuilder.setTargetAspectRatio(screenAspectRatio)
                    .build()

            // Disable automatic image stabilization
            camera2Interop?.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF)

            // Video Capture
            videoCapture = VideoCaptureConfig.Builder()
                    .setTargetRotation(texture.display.rotation)
                    .setTargetAspectRatio(screenAspectRatio)
                    //.setTargetResolution(resolution)
                    .setVideoFrameRate(25)
                    .build()

            // Camera Selector
            cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()



            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector!!, preview, videoCapture)
                preview.setSurfaceProvider(texture.createSurfaceProvider())

                cameraControl = camera.cameraControl as CameraControlInternal
            } catch (ex: Exception) {
                recordLog(ex)
                Log.e(TAG, "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    fun startRecording(formatString: String, videoListener: VideoListener) {
        if (videoCapture != null) {
            val file = createFile("mp4", formatString)
            fixedFocus()
            videoCapture!!.startRecording(file, Executors.newSingleThreadExecutor(), object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(file: File) {
                    Log.d(TAG, "Save path ${file.absolutePath}")
                    videoListener.onVideoSaved(file.absolutePath)
                    videoListener.onFinal()
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    videoListener.onError(message)
                    videoListener.onFinal()
                }
            })
        }
    }

    @SuppressLint("RestrictedApi")
    fun stopRecording() {
        if (videoCapture != null) {
            cancelFixedFocus()
            videoCapture!!.stopRecording()
            Log.i(TAG, "Video File stopped")
        }
    }

    /**
     * tap to focus
     * only
     *
     * @param focus event
     * Created by Justin Lee
     */
    fun setUpTapToFocus(event: MotionEvent) {
        Log.d(TAG, "Focus x:${event.x} , y:${event.y}")

        if (cameraControl == null || cameraSelector == null)
            return
        val factory = texture.createMeteringPointFactory(cameraSelector!!)
        val point = factory.createPoint(event.x, event.y)
        val action = FocusMeteringAction.Builder(point).build()
        cameraControl!!.startFocusAndMetering(action)
    }

    @SuppressLint("RestrictedApi")
    fun fixedFocus() {
        if (cameraControl == null || cameraSelector == null)
            return
//        cameraControl!!.enableTorch(true)
        cameraControl!!.cancelAfAeTrigger(true, true)
    }

    @SuppressLint("RestrictedApi")
    fun cancelFixedFocus() {
        if (cameraControl == null || cameraSelector == null)
            return
//        cameraControl!!.enableTorch(false)
        cameraControl!!.cancelAfAeTrigger(false, false)
    }


    interface VideoListener {
        fun onVideoSaved(path: String)

        fun onError(errorMsg: String)

        fun onFinal()
    }

    /**
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun createFile(extension: String, formatString: String = ""): File {
        val folder = File(StorageHelper.getSpecificStorageFolderPath(formatString) + RecordHelper.CAMERA_FOLDER)
        if (!folder.mkdirs())
            Log.d(TAG, "COULD NOT CREATE CAMERA FOLDER")

        return File(folder, "${formatString}_cam_$formatString.$extension")
    }

}