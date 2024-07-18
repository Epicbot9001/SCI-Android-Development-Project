package edu.gatech.ce.allgather.old.helpers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import edu.gatech.ce.allgather.AllGatherApplication

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.extend.logMessage
import edu.gatech.ce.allgather.old.utils.AutoFitTextureView
import edu.gatech.ce.allgather.old.utils.StopVehicleFilter
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Created by Anirban Chatterjee on 15-Dec-17.
 */

class CameraHelper(context: Context,
                   /**
                     * An [AutoFitTextureView] for camera preview.
                     */
                    private val mTextureView: AutoFitTextureView?) : HelperClass(context) {
    val CAMERA_FOLDER = "camera/"
    /**
     * A reference to the opened [android.hardware.camera2.CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * A reference to the current [android.hardware.camera2.CameraCaptureSession] for
     * preview.
     */
    private var mPreviewSession: CameraCaptureSession? = null

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture,
                                               width: Int, height: Int) {
            logMessage("surface available")
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture,
                                                 width: Int, height: Int) {
            logMessage("surface size changed")
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            logMessage("surface destroyed")
            if (mCameraDevice != null) {
                closeCamera()
            }
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        }

    }

    private var mTexture: SurfaceTexture? = null
    private var mPreviewSurface: Surface? = null
    private var mRecordedSurface: Surface? = null
    private var mSurfaces: ArrayList<Surface>? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * The [android.util.Size] of video recording.
     */
    private var mVideoSize: Size? = null

    /**
     * MediaRecorder
     */
    private var mMediaRecorder: MediaRecorder? = null

    /**
     * Whether the app is recording video now
     */
    private var mIsRecordingVideo: Boolean = false

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            logMessage("camera open")
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            if (null != mTextureView) {
                configureTransform(mTextureView.width, mTextureView.height)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            logMessage("camera disconnected")
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            logMessage("camera error")
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            val activity = context as Activity
            activity?.finish()
        }

    }
    private var mSensorOrientation: Int? = null
    private var mNextVideoAbsolutePath: String? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null

    private val mSharedPref: SharedPreferences

    // if this is the first time we hit record
    var isFirstRecording = true
    // if this is the first timestamp, use the original timestamp when we hit record
    private var useFirstTimestamp = false
    // the original timestamp of when we hit record in ms
    private var recordingTimeStamp: Long = 0
    // the previously used timestamp
    private var lastTimeStamp: Long = 0
    // the original date of when we hit record, derived from [recordingTimeStamp]
    private var recordingDate: String = ""

    // used for stopping recording of video while stopped
    private var isMovingSubscription: Disposable? = null
    private var mIsActivelyRecording = false

    init {
        mTextureView?.surfaceTextureListener = mSurfaceTextureListener
        mMediaRecorder = MediaRecorder()
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override val isReady: Boolean
        get() = true

    fun onResume() {
        startBackgroundThread()
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    fun onPause() {
        closeCamera()
        stopBackgroundThread()
    }


    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by `mStateCallback`.
     */
    private fun openCamera(width: Int, height: Int) {

        val activity = context as Activity
        if (null == activity || activity.isFinishing) {
            return
        }
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            Log.d(TAG, "tryAcquire")

            mMediaRecorder = MediaRecorder()

            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }


            val cameraIds = manager.cameraIdList
            val cameraId = getRearFacingCameraId(manager, cameraIds)

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            if (map == null) {
                throw RuntimeException("Cannot get available preview/video sizes")
            }

            // choose resolution from preferences, default: choose highest
            val prefResolution = mSharedPref.getString(context.resources.getString(R.string.pref_resolution),
                    chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java)).toString())
            mVideoSize = Size.parseSize(prefResolution)
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    width, height, mVideoSize)

            val orientation = context.resources.configuration.orientation


            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                mTextureView!!.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
            } else {
                mTextureView!!.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
            }

            configureTransform(width, height)

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
                return
            }

            manager.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            mainActivity.logException(e)
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity.finish()

        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            //            ErrorDialog.newInstance("Camera error")
            //                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            mainActivity.logException(e)
            Log.e("AC1", "camera2 API not supported by this device")
        } catch (e: InterruptedException) {
            mainActivity.logException(e)
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }

    }



    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mMediaRecorder) {
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
        } catch (e: InterruptedException) {
            mainActivity.logException(e)
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            mTexture = mTextureView.surfaceTexture!!
            mTexture?.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            mPreviewSurface = Surface(mTexture)
            mPreviewBuilder!!.addTarget(mPreviewSurface!!)

            mCameraDevice!!.createCaptureSession(listOf(mPreviewSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(session: CameraCaptureSession) {
                            mPreviewSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            val activity = context as Activity
                            if (null != activity) {
                                Toast.makeText(activity, "Save Complete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

        getAvailableFpsTargetRanges(context)

    }

    /**
     * Update the camera preview. [.startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (null == mCameraDevice) {
            Log.e("AC1", "updatePreview error, return")
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder)
//            val thread = HandlerThread("CameraPreview")
//            thread.start()
            mPreviewSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val whiteBalance = mSharedPref.getBoolean(context.resources.getString(R.string.pref_white_balance), false)
        val awbMode = if (whiteBalance) CameraMetadata.CONTROL_AWB_MODE_AUTO else
            CameraMetadata.CONTROL_AWB_MODE_OFF
        builder!!.set(CaptureRequest.CONTROL_AWB_MODE, awbMode)
        val autofocus = mSharedPref.getBoolean(context.resources.getString(R.string.pref_autofocus), false)
        val autoFocusMode = if (autofocus) CameraMetadata.CONTROL_AF_MODE_AUTO else
            CameraMetadata.CONTROL_AF_MODE_OFF
        builder!!.set(CaptureRequest.CONTROL_AF_MODE, autoFocusMode)
        builder!!.set(CaptureRequest.LENS_FOCUS_DISTANCE,0f)


        val antibanding = mSharedPref.getBoolean(context.resources.getString(R.string.pref_antibanding), true)
        val antibandingMode = if (antibanding) CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO else
            CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF
        builder!!.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antibandingMode)
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = context as Activity
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    viewHeight.toFloat() / mPreviewSize!!.height,
                    viewWidth.toFloat() / mPreviewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
        mTextureView.setTransform(matrix)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder(savePath: File) {
        //val activity = context as Activity ?: return
        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath!!.isEmpty()) {
            mNextVideoAbsolutePath = savePath.absolutePath//getVideoFilePath(getActivity());
        }
        mMediaRecorder!!.setOutputFile(mNextVideoAbsolutePath)
        val bitrate = mSharedPref.getString(context.resources.getString(R.string.pref_bitrate), "10000000")
        Log.d(TAG, "Bitrate: $bitrate")
        mMediaRecorder!!.setVideoEncodingBitRate(bitrate!!.toInt())
        val framerate = mSharedPref.getString(context.resources.getString(R.string.pref_framerate), "10")
        Log.d(TAG, "Framerate: $framerate")
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setVideoSize(mVideoSize!!.width, mVideoSize!!.height)
        mMediaRecorder!!.setVideoFrameRate(framerate!!.toInt())
        mMediaRecorder!!.setCaptureRate(framerate.toDouble())

        // set the max file size
        mMediaRecorder!!.setMaxFileSize(MAX_FILE_SIZE)

        mMediaRecorder!!.setOnInfoListener { _, info, _ ->
            when (info) {
                MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ->
                        recordAgain()
            }
        }

        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //val rotation = activity.windowManager.defaultDisplay.rotation
        val rotation = windowmanager.defaultDisplay.rotation

        when (mSensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> mMediaRecorder!!.setOrientationHint(
                DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES -> mMediaRecorder!!.setOrientationHint(
                INVERSE_ORIENTATIONS.get(rotation))
        }

        mMediaRecorder!!.prepare()
    }

    private fun getVideoFilePath(context: Context): String {
        val dir = context.getExternalFilesDir(null)
        return ((if (dir == null) "" else dir.absolutePath + "/")
                + System.currentTimeMillis() + ".mp4")
    }

    private fun startRecordingVideo(path: File) {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            // pass file name to StopVehicleFilter
            if (AllGatherApplication.USE_MINI_VIDEOS) {
                videoFilesSubject.onNext(path)
            }

            closePreviewSession()
            setUpMediaRecorder(path)
            mTexture = mTextureView.surfaceTexture!!
            mTexture?.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            mSurfaces = ArrayList<Surface>()

            // Set up Surface for the camera preview
            mPreviewSurface = Surface(mTexture)
            mSurfaces?.add(mPreviewSurface!!)
            mPreviewBuilder!!.addTarget(mPreviewSurface!!)

            // Set up Surface for the MediaRecorder
            mRecordedSurface = mMediaRecorder!!.surface
            mSurfaces?.add(mRecordedSurface!!)
            mPreviewBuilder!!.addTarget(mRecordedSurface!!)

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice!!.createCaptureSession(mSurfaces!!, object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession
                    updatePreview()
                    (context as Activity).runOnUiThread {
                        // UI
                        mIsRecordingVideo = true

                        // Start recording
                        mMediaRecorder!!.start()
                        Log.d("PREF_CHECK", "AWB: ${mPreviewBuilder!![CaptureRequest.CONTROL_AWB_MODE]}")
                        Log.d("PREF_CHECK", "AF: ${mPreviewBuilder!![CaptureRequest.CONTROL_AF_MODE]}")
                        Log.d("PREF_CHECK", "ANTIBANDING: ${mPreviewBuilder!![CaptureRequest.CONTROL_AE_ANTIBANDING_MODE]}")
                        if (AllGatherApplication.USE_MINI_VIDEOS) {
                            reRecord()
                        }
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    val activity = context as Activity
                    if (null != activity) {
                        Toast.makeText(activity, "Couldn't start recording video", Toast.LENGTH_SHORT).show()
                    }
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            mainActivity.logException(e)
            e.printStackTrace()
        } catch (e: IOException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

    }

    private fun reRecord() {
        if (AllGatherApplication.USE_MINI_VIDEOS) {
            val handler = Handler()
            handler.postDelayed({
                recordAgain()
            }, (AllGatherApplication.MINI_VIDEOS_LENGTH_SECONDS * 1000).toLong())
        }

    }

    private fun recordAgain() {
        if (isRecording) {
            stopRecording()
            Log.d("AC1", "starting rerecording")
            startRecording(recordingTimeStamp)
        }
    }

    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    private fun stopRecordingVideo() {
        try {
            mPreviewSession?.stopRepeating()
            mPreviewSession?.abortCaptures()
        } catch (ex: Exception) {
            mainActivity.logException(ex)
            Log.d(TAG, "Stop Record exception: ${ex.message}")
        }
        // UI
        mIsRecordingVideo = false

        mIsActivelyRecording = false

        // Stop recording
        // https://github.com/googlesamples/android-Camera2Video/issues/37
        // Delay for XiaoMI
        val timer = Timer()
        val timerTask = timerTask {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
        }
        timer.schedule(timerTask, 30)

        val activity = context as Activity
        if (null != activity) {
            //Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath!!,
            //        Toast.LENGTH_SHORT).show()
            Toast.makeText(activity, "File saved", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath!!)
        }
        mNextVideoAbsolutePath = null
        startPreview()
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {

        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                    .setMessage(arguments.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i -> activity.finish() }
                    .create()
        }

        companion object {

            private val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }

    }

    @Throws(IOException::class, FileNotFoundException::class)
    override fun record(f: File) {
        if (!mIsRecordingVideo) {
            startRecordingVideo(f)
            mIsActivelyRecording = true
        }
    }

    override fun stopRecord() {
        if (mIsRecordingVideo) {
            stopRecordingVideo()
            // dispose of the subscription
            isMovingSubscription?.dispose()
        }

    }

    override fun name2File(fileName: String): File {
        val folder = File(getMyStrUtil().getSpecificStorageFolderPath(fileName.substring(0,10)) + CAMERA_FOLDER)
        if (!folder.mkdirs()) {
            Log.d("AC1", "Cam Directory not created")
        }
        var curTimestamp = System.currentTimeMillis()
        // use the same timestamp as the first recording time if it is the first recording
        if (useFirstTimestamp) {
            curTimestamp = recordingTimeStamp
            useFirstTimestamp = false
        }
        lastTimeStamp = curTimestamp
        return File(folder, "${recordingDate}_cam_$curTimestamp.mp4")
    }

    override fun startRecording(timeStamp: Long) {
        if (mIsRecordingVideo || !getMyStrUtil().isExternalStorageWritable()) {
            throw IllegalStateException()
        } else {
            //turn timestamp into a file name
            // use the original timestamp from first time we hit play
            setIsRecording(true)
            if (isFirstRecording) {
                recordingTimeStamp = timeStamp
                val dt = Date(recordingTimeStamp)
                val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
                recordingDate = sdf.format(dt)
                useFirstTimestamp = true
                isFirstRecording = false
            }
            // if using Stopped Vehicle Filter, subscribe to subject
            if (AllGatherApplication.STOPPED_VEHICLE_FILTER) {
                // subscribe to isMovingSubject, only change recording when boolean value changes
                isMovingSubscription = StopVehicleFilter.isMovingSubject.subscribe { isMoving ->
                    val curTimestamp = System.currentTimeMillis()
                    if (isMoving && !mIsActivelyRecording) {
                        // only record when the vehicle is moving
                        try {
                            record(name2File(recordingDate))
                        } catch (ex: IOException) {
                            mainActivity.logException(ex)
                            ex.printStackTrace()
                        }
                    } else if (!isMoving && mIsActivelyRecording &&
                            curTimestamp - lastTimeStamp >= MIN_VIDEO_LENGTH
                    ) {
                        // only stop recording if the current video is greater than the min length
                        stopRecordingVideo()
                    }
                }
            } else { // if not using stopped vehicle filter, just record
                try {
                    record(name2File(recordingDate))
                } catch (ex: IOException) {
                    mainActivity.logException(ex)
                    ex.printStackTrace()
                }
            }

//            // record
//            else if (!AllGatherApplication.STOPPED_VEHICLE_FILTER) {
//                // only record when the vehicle is moving
//                try {
//                    record(name2File(recordingDate))
//                } catch (ex: IOException) {
//                    ex.printStackTrace()
//                }
//            }


        }
    }

    companion object {
        val videoFilesSubject: PublishSubject<File> = PublishSubject.create()

        private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
        private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
        private val DEFAULT_ORIENTATIONS = SparseIntArray()
        private val INVERSE_ORIENTATIONS = SparseIntArray()

        private val TAG = "CameraHelper2"
        private val REQUEST_VIDEO_PERMISSIONS = 1
        private val FRAGMENT_DIALOG = "dialog"

        private val REQUEST_CAMERA_PERMISSION = 200

        private val VIDEO_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        private val MAX_FILE_SIZE: Long = 1000000000 // 1 GB

        private val MIN_VIDEO_LENGTH: Long = 300000 // 5 min = 300000 ms

        init {
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        init {
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
        }

        fun getSupportedSizes(context: Context): List<Size> {

            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // Choose the sizes for camera preview and video recording

            val cameraIds = manager.cameraIdList
            val cameraId = getRearFacingCameraId(manager, cameraIds)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            map ?: throw RuntimeException("Cannot get available preview/video sizes")

            val sizes = map.getOutputSizes(MediaRecorder::class.java)
            // filter out any sizes with greater than 1080p and only 4:3/16:9 ratio?
            return sizes.filter { (it.width == it.height * 4 / 3 || it.width == it.height * 16 / 9)
                    && it.height <= 1080 }
        }

        private fun getAvailableFpsTargetRanges(context: Context): List<Range<Int>> {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // Choose the sizes for camera preview and video recording

            val cameraIds = manager.getCameraIdList()
            val cameraId = getRearFacingCameraId(manager, cameraIds)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            Log.d("AC1","FPS:")
            for(rng in fpsRanges!!.toList())
            {
                Log.d("AC1","fps: " + rng.toString())
            }
            return fpsRanges.toList()
        }

        private fun getRearFacingCameraId(manager: CameraManager, ids: Array<String>): String {
            for (id in ids) {
                try {
                    val characteristics = manager.getCameraCharacteristics(id)

                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        return id
                    }
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }

            }
            return "";
        }

        /**
         * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
         * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
         *
         * @param choices The list of available sizes
         * @return The video size
         */
        private fun chooseVideoSize(choices: Array<Size>): Size {
            for (size in choices) {
                if ((size.width == size.height * 4 / 3 || size.width == size.height * 16 / 9)
                        && size.height <= 1080) {
                    return size
                }
            }
            Log.e(TAG, "Couldn't find any suitable video size")
            return choices[choices.size - 1]
        }

        /**
         * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
         * width and height are at least as large as the respective requested values, and whose aspect
         * ratio matches with the specified value.
         *
         * @param choices     The list of sizes that the camera supports for the intended output class
         * @param width       The minimum desired width
         * @param height      The minimum desired height
         * @param aspectRatio The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size?): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            val w = aspectRatio!!.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.height == option.width * h / w &&
                        option.width >= width && option.height >= height) {
                    bigEnough.add(option)
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

    }
}
