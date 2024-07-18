package edu.gatech.ce.allgather.ui.camera

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.Processor
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.api.ApiClient
import edu.gatech.ce.allgather.api.Post
import edu.gatech.ce.allgather.base.BaseFragment
import edu.gatech.ce.allgather.databinding.FragmentCameraBinding
import edu.gatech.ce.allgather.extend.gone
import edu.gatech.ce.allgather.extend.invisible
import edu.gatech.ce.allgather.extend.setOnClickDebounceListener
import edu.gatech.ce.allgather.extend.snackBar
import edu.gatech.ce.allgather.extend.toastLong
import edu.gatech.ce.allgather.extend.visible
import edu.gatech.ce.allgather.helpers.CameraXHelper
import edu.gatech.ce.allgather.helpers.NotificationHelper
import edu.gatech.ce.allgather.helpers.RecordHelper
import edu.gatech.ce.allgather.helpers.RecordLogHelper
import edu.gatech.ce.allgather.helpers.StorageHelper
import edu.gatech.ce.allgather.ui.calibration.CalibrationActivity
import edu.gatech.ce.allgather.ui.curves.CacheCurvesActivity
import edu.gatech.ce.allgather.ui.upload_data.UploadDataActivity
import edu.gatech.ce.allgather.utils.livedata.BatteryLiveData
import edu.gatech.ce.allgather.utils.livedata.LocationLiveData
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
import edu.gatech.ce.allgather.utils.livedata.StorageLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.compress.archivers.zip.ZipFile
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


/**
 *
 * @author Justin Lee
 * @date 2020/6/9
 */
class CameraFragment : BaseFragment() {
    private var _binding: FragmentCameraBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    companion object {
        private const val NOTIFICATION_ID = 0x1
    }

    /** Where the camera preview is displayed */
    private lateinit var preview: PreviewView

    private lateinit var rootLayout: View
    private lateinit var captureButton: FloatingActionButton
    private lateinit var recordingBorder: View
    private lateinit var crosshair: ImageView
    private lateinit var crosshair2: ImageView
    private lateinit var switchRec: Switch
    private lateinit var mCalibrationButton: Button
    private lateinit var uploadDataButton: Button
    private lateinit var cacheCurvesButton: CircularProgressButton

    /** Live data listener for changes in the location */
    private lateinit var locationLiveData: LocationLiveData

    /** Live data listener for sensors information */
    private lateinit var sensorInfoLiveData: SensorInfoLiveData

    /** Live data listener for battery information */
    private lateinit var batteryInfoLiveData: BatteryLiveData

    /** Live data listener for storage information */
    private lateinit var storageLiveData: StorageLiveData

    /** notification helper */
    private lateinit var notificationHelper: NotificationHelper

    /** camera helper */
    private lateinit var cameraHelper: CameraXHelper

    /** camera helper */
    private lateinit var recordHelper: RecordHelper

    /** Is recording */
    private var isRecording = false
    private var isCameraRecording = false

    /** handler */
    private var handler = Handler()

    private lateinit var processor: Processor

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationHelper = NotificationHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_camera, container, false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preview = view.findViewById(R.id.texture)
        rootLayout = view.findViewById(R.id.root_layout)
        recordingBorder = view.findViewById(R.id.recording_border)
        captureButton = view.findViewById(R.id.capture_button)
        crosshair = view.findViewById(R.id.crosshair)
        crosshair2 = view.findViewById(R.id.crosshair2)
        switchRec = view.findViewById(R.id.switch1)
        mCalibrationButton = view.findViewById(R.id.cam_but_cal)
        uploadDataButton = view.findViewById(R.id.upload_data_button)
        cacheCurvesButton = view.findViewById(R.id.cache_curves_button)

        val testingText = view.findViewById<TextView>(R.id.testingText)

        // show testing text
        if (AllGatherApplication.SHOW_FOR_TESTING_PURPOSES_ONLY_TEXT) {
            testingText.visible()
        } else {
            testingText.gone()
        }

        cameraHelper = CameraXHelper(requireContext(), preview, viewLifecycleOwner)
        recordHelper = RecordHelper(requireContext())

        // Used for observe location change
        locationLiveData = LocationLiveData(requireContext()).apply {
            observe(viewLifecycleOwner, androidx.lifecycle.Observer { locationInfoBean ->
                val location = locationInfoBean.location

                val sysTime = System.currentTimeMillis()
                var locationInfo =
                    "${location?.time},${sysTime},${location?.latitude},${location?.longitude}," +
                            "${location?.altitude},${location?.bearing},${location?.accuracy}\n"

                val gpsSignalTextView = view.findViewById<TextView>(R.id.gpsSignalTextView)
                if (locationInfoBean.isGpsWork && location != null) {
                    gpsSignalTextView.text = resources.getString(R.string.gps_signal_connected)
                } else {
                    gpsSignalTextView.text = resources.getString(R.string.gps_signal_disconnected)
                }
                doUpdateGlow(gpsSignalTextView)
            })
        }

        // Used for observe sensor information
        sensorInfoLiveData = SensorInfoLiveData(requireContext()).apply {
            observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                // crosshair position, only current state isn't recording need to calibration
                if (it.containsKey(SensorInfoLiveData.KEY_ORIENTATION_ANGLE) && !isRecording) {
                    val orientationAngleInfo = it[SensorInfoLiveData.KEY_ORIENTATION_ANGLE]!!
                    updateCalibrationUI(orientationAngleInfo[2], orientationAngleInfo[1])
                }
            })
        }

        // Used for observe battery information
        batteryInfoLiveData = BatteryLiveData(requireContext()).apply {
            observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                updateBatteryUI(it)
            })
        }
        // Used for observe storage information
        storageLiveData = StorageLiveData().apply {
            observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                val storageTextView = view.findViewById<TextView>(R.id.tv_storage)
                storageTextView.text = resources.getString(R.string.storage, StorageHelper.storageType.name)
                updateStorageUI(it.availableSpace, it.totalSpace)
            })
        }

        // Launch recording coroutine for locations
        coroutineScope.launch {
            locationLiveData.locationUpdatesFlow.collect { locationInfoBean ->
                recordHelper.recordLocation(locationInfoBean)
            }
        }

        // Launch recording coroutine for sensors
        coroutineScope.launch {
            sensorInfoLiveData.sensorUpdates.collect { sensorInfoBean ->
                recordHelper.recordSensor(sensorInfoBean)
            }
        }

        processor = Processor(locationLiveData.locationUpdatesFlow, sensorInfoLiveData.sensorUpdates)
        processor.startProcessing()

        // Launch recording coroutine for calculations
        coroutineScope.launch {
            processor.calculationsFlow.collect { calculationBean ->
                recordHelper.recordCalculations(
                    calculationBean.transformedAccelData,
                    calculationBean.transformedAngVelocityData,
                    calculationBean.bbi,
                    calculationBean.bbiFiltered,
                    calculationBean.superelevation
                )
            }
        }

        initEvent()
    }

    /**
     * init event
     *
     * Created by Justin Lee
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    private fun initEvent() {
        captureButton.setOnClickDebounceListener {
            // expiry hint

            val currTime = System.currentTimeMillis()

            if (AllGatherApplication.EXPIRY_DATE.before(Date(currTime))) {
                toastLong(R.string.expiry_date_passed)
                return@setOnClickDebounceListener
            }
            if (AllGatherApplication.EXPIRY_WARNING_DATE.before(Date(currTime))) {
                toastLong(R.string.expiry_date_passed)
            }

            lifecycleScope.launch(Dispatchers.Main) {
                if (isRecording) {

                    //enable calibration
                    mCalibrationButton.isEnabled = true
                    //stop recording
                    recordHelper.stopRecord()
                    // stop publishing location updates
                    locationLiveData.shouldPublishLocationUpdates = false
                    // stop publishing sensor updates
                    sensorInfoLiveData.shouldPublishSensorUpdates = false

                    // stop processing
                    processor.stopProcessing()

                    if (isCameraRecording) {
                        cameraHelper.stopRecording()
                    }

                    toggleRecording(false)
                    handler.removeCallbacksAndMessages(null)
                    // finished log record
                    RecordLogHelper.endLog()


                } else {

                    //disable calibration
                    mCalibrationButton.isEnabled = false

                    // start recording
                    var formatString = async { recordHelper.startRecord() }
                    // exception log record
                    RecordLogHelper.prepareLog(formatString.await())

                    if (switchRec.isChecked) {

                        // Prevents screen rotation during the video recording
                        this@CameraFragment.requireActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LOCKED

                        isCameraRecording = true
                        cameraHelper.startRecording(
                            formatString.await(),
                            object : CameraXHelper.VideoListener {
                                override fun onVideoSaved(path: String) {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        captureButton.snackBar("Video was saved: $path")
                                    }
                                }

                                override fun onError(errorMsg: String) {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        captureButton.snackBar("Something wrong: $errorMsg")
                                    }
                                }

                                override fun onFinal() {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        // Unlocks screen rotation after recording finished
                                        this@CameraFragment.requireActivity().requestedOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                                    }
                                    isCameraRecording = false
                                }
                            })

                    }

                    locationLiveData.shouldPublishLocationUpdates = true
                    sensorInfoLiveData.shouldPublishSensorUpdates = true

                    toggleRecording(true)

                    //delay 3s to calculate zeroVectorXY(average of vectors)
                    delay(3000)
                    //for sure there is not other message in Handler's loop
                    handler.removeCallbacksAndMessages(null)
                    Log.e("RecordHelper", "Start Recording")
                }
                isRecording = !isRecording
            }
        }

        // set tap to focus
        preview.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && !isRecording) {
                cameraHelper.setUpTapToFocus(event)
            }
            return@setOnTouchListener true
        }

        mCalibrationButton.setOnClickDebounceListener {
            val calIntent = Intent(activity, CalibrationActivity::class.java)
            startActivity(calIntent)
        }

        // Make dummy API call to json placeholder website using retrofit
        uploadDataButton.setOnClickDebounceListener {
            startActivity(Intent(activity, UploadDataActivity::class.java))
        }

        cacheCurvesButton.setOnClickDebounceListener {
            // TODO: Organization ID is currently hardcoded, make it as input later on.
            fetchCurveInventory(2)
        }
    }

    private fun fetchCurveInventory(organizationID: Int) {
        cacheCurvesButton.startAnimation()

        // Save this content to shared preference
        val mSharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        // Check if the curve data is already cached
        val curveData = mSharedPref.getString("curve_data", null)
        if (curveData != null) {
            Toast.makeText(context, "Curve data has already been cached", Toast.LENGTH_SHORT).show()
            cacheCurvesButton.revertAnimation()
            return
        }

        val call = ApiClient.apiService.fetchCurveInventory(organizationID)
        call.enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Server contacted and has file");

                    CoroutineScope(Dispatchers.IO).launch {
                        // Save the zip content to temporary file as data.zip
                        val tempFile = File.createTempFile("curve_data", "zip")
                        response.body()?.byteStream()?.copyTo(tempFile.outputStream())
                        Log.d(TAG, "file download was successful")

                        // Load the curves from the temporary file
                        val zipFile = ZipFile.builder().setFile(tempFile).get()
                        // Load the file data.json
                        val dataFile = zipFile.getEntry("data.json")
                        val data = zipFile.getInputStream(dataFile).bufferedReader().use { it.readText() }
                        Log.d(TAG, "Unzipped file: $data")
                        // Now delete the zip file
                        tempFile.delete()

                        // Save this content to shared preference
                        with (mSharedPref.edit()) {
                            putString("curve_data", data)
                            apply()
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Curve data has been cached successfully", Toast.LENGTH_SHORT).show()
                            // Revert the animation now
                            cacheCurvesButton.revertAnimation()
                        }

                        // Log the curve information
                        Log.d(TAG, "Curve data: $data")
                    }
                } else {
                    Log.d(TAG, "Server contact failed: ${response.message()}");
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d(TAG, "Server contact failed: ${t.message}");
            }
        })
    }

    private fun updateCalibrationUI(pitch: Float, roll: Float) {
        //move crosshairs
        val windowmanager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowmanager.defaultDisplay.rotation
        if (rotation == 0) {
            val rollAngle = roll + Math.PI / 2
            val alpha: Float = 0.5f
            crosshair2.translationY += alpha * (rollAngle.toFloat() * 100 - crosshair2.translationY)
        } else if (rotation == 1) {
            val pitchAngle = pitch + Math.PI / 2
            val alpha: Float = 0.5f
            crosshair2.translationY += alpha * (-pitchAngle.toFloat() * 100 - crosshair2.translationY)
        } else if (rotation == 3) {
            val pitchAngle = pitch - Math.PI / 2
            val alpha: Float = 0.5f
            crosshair2.translationY += alpha * (pitchAngle.toFloat() * 100 - crosshair2.translationY)
        }
    }

    private fun updateBatteryUI(p: Int) {
        val batteryTextView = view?.findViewById<TextView>(R.id.batteryTextView)
        batteryTextView?.text = p.toString() + "%"

        val batteryProgressBar = view?.findViewById<ProgressBar>(R.id.batteryProgressBar)
        batteryProgressBar?.progress = p
    }

    private fun updateStorageUI(freeBytes: Long, totalBytes: Long) {
        var value = freeBytes.toFloat() / (1024 * 1024)
        var unit = "MB"
        if (value > 1024) {
            value /= 1024f
            unit = "GB"
        }
        if (value > 1024) {
            value /= 1024f
            unit = "TB"
        }
        var totalValue = totalBytes.toFloat() / (1024 * 1024)
        var totalUnit = "MB"
        if (totalValue > 1024) {
            totalValue /= 1024f
            totalUnit = "GB"
        }
        if (totalValue > 1024) {
            totalValue /= 1024f
            totalUnit = "TB"
        }

        val storageTextView = view?.findViewById<TextView>(R.id.tv_storage)
        storageTextView?.text = String.format("%.1f %s free of %.0f %s", value, unit, totalValue, totalUnit)
        val storageProgressBar = view?.findViewById<ProgressBar>(R.id.storageProgressBar)
        storageProgressBar?.progress = (freeBytes * 100 / totalBytes).toInt()
    }

    /**
     * change basic UI
     *
     * @param isRecording
     * Created by Justin Lee
     */
    private suspend fun toggleRecording(isRecording: Boolean) {
        withContext(Dispatchers.Main) {
            if (isRecording) {
                notificationHelper.notify(
                    NOTIFICATION_ID,
                    notificationHelper.getOngoingNotification(resources.getString(R.string.recording_in_progress))
                )
                captureButton.setImageResource(R.drawable.ic_media_stop)
                recordingBorder.visible()
                crosshair.invisible()
                crosshair2.invisible()
                captureButton.snackBar(R.string.started_recording)
            } else {
                //notification
                notificationHelper.cancel(NOTIFICATION_ID)
                captureButton.setImageResource(R.drawable.ic_media_play)
                recordingBorder.gone()
                crosshair.visible()
                crosshair2.visible()
            }
        }
    }

    /**
     * GPS blips
     *
     * @param v blink view
     * Created by Justin Lee
     */
    private fun doUpdateGlow(v: TextView) {
        val colorAnim = ObjectAnimator.ofInt(v, "textColor", Color.WHITE, Color.YELLOW)
        colorAnim.duration = LocationLiveData.UPDATE_FREQUENCY
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.repeatCount = 0
        colorAnim.repeatMode = ValueAnimator.RESTART
        colorAnim.start()
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            lifecycleScope.launch(Dispatchers.Main) {
                cameraHelper.stopRecording()
                isRecording = false
                toggleRecording(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationHelper.cancel(NOTIFICATION_ID)
        handler.removeCallbacksAndMessages(null)
    }
}