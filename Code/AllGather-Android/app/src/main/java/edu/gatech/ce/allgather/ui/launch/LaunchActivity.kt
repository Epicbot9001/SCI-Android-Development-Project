package edu.gatech.ce.allgather.ui.launch

//import kotlinx.android.synthetic.main.activity_launch.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.BuildConfig
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.*
import edu.gatech.ce.allgather.base.BaseActivity
import edu.gatech.ce.allgather.extend.setOnClickDebounceListener
import edu.gatech.ce.allgather.ui.camera.CameraActivity
import edu.gatech.ce.allgather.utils.VersionUtils
import edu.gatech.ce.allgather.utils.isInteger
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


class LaunchActivity : BaseActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO,
//            TODO: Why won't the below permission work?
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            )

        private const val RC_PERM = 0x111
    }
    private lateinit var sensorProvider: SensorProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        val mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        val buttonGotoMain = findViewById<Button>(R.id.button_goto_main)
        buttonGotoMain.setOnClickDebounceListener { saveIdAndProceedToMain() }

        // set SD card as default
        val storageUtil = (application as AllGatherApplication).storageUtil
        storageUtil.setStoragePrefDefault()

        //show previously stored driver id
        val editText_driver_id = findViewById<AppCompatEditText>(R.id.editText_driver_id)
        val driverID = mSharedPref.getString(getString(R.string.pref_driverid), null)
        if (driverID != null)
            editText_driver_id.setText(driverID)

        editText_driver_id.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveIdAndProceedToMain()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        val tv_version = findViewById<TextView>(R.id.tv_version)
        tv_version.text = "Version${VersionUtils.getVersionName(this)}"

        checkPermission()

        /*
            Tianqi Liu, 11/23/2019
            Fix App crash.
            Camera should not be opened twice, otherwise getCameraListID will throw error
         */

        //val cam = Camera.open()
        //val params = cam.parameters
        //val f = params.focalLength
        //Log.d("AC1","focal length is "+ f + " mm")

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermission() {
        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

        if (!Environment.isExternalStorageManager()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Permission needed!",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Settings") {
                    try {
                        val uri =
                            Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            uri
                        )
                        startActivity(intent)
                    } catch (ex: Exception) {
                        val intent = Intent()
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
                .show()
        }

        if (!EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
            EasyPermissions.requestPermissions(this, "We need permission", RC_PERM, *PERMISSIONS)
        }
    }

//    private fun saveIdAndProceedToMain() {
//        if (!EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
//            Log.d("LaunchActivity", "EasyPermissions doesn't see the permissions. Requesting permissions.")
//            EasyPermissions.requestPermissions(this, "We need permission", RC_PERM, *PERMISSIONS)
//            return
//        }
//
//
//        //validate driver id
//        val editText_driver_id = findViewById<AppCompatEditText>(R.id.editText_driver_id)
//        val id = editText_driver_id.text.toString()
//        if (isInteger(id) && id.length >= 6 && id.length <= 8) {
//            // done
//            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
//            val editor = sharedPref.edit()
//            editor.putString(getString(R.string.pref_driverid), editText_driver_id.text.toString())
//            editor.apply()
//            val i = Intent(this, CameraActivity::class.java)
//            startActivity(i)
//            finish()
//        } else {
//            // hint error msg
//            editText_driver_id.error = resources.getString(R.string.cannot_proceed)
//        }
//
//    }

    private fun saveIdAndProceedToMain() {
        if (!EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
            Log.d("LaunchActivity", "EasyPermissions doesn't see the permissions. Requesting permissions.")
            EasyPermissions.requestPermissions(this, "We need permission", RC_PERM, *PERMISSIONS)
            return
        }

        val editText_driver_id = findViewById<AppCompatEditText>(R.id.editText_driver_id)
        val id = editText_driver_id.text.toString()
        if (isInteger(id) && id.length >= 6 && id.length <= 8) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = sharedPref.edit()
            editor.putString(getString(R.string.pref_driverid), editText_driver_id.text.toString())
            editor.apply()

            val useMock = findViewById<Switch>(R.id.toggleTestingMode).isChecked
            // Update the file paths to point to the existing data
            val gpsCsvFilePath = "/storage/emulated/0/Download/2023_10_05_17_14_07_252_loc.csv"
            val imuCsvFilePath = "/storage/emulated/0/Download/2023_10_05_17_14_07_252_acc.csv"
//            val calibrationCsvFilePath = "/storage/emulated/0/Download/2023_10_05_17_11_10_478_calibration.csv"
            sensorProvider = SensorProvider(useMock, gpsCsvFilePath, imuCsvFilePath)

            if (useMock) {
                val gpsSensor = sensorProvider.getGPSSensor() as MockGPSSensor
                val mockGPSData = gpsSensor.getGPSData()

                val imuSensor = sensorProvider.getIMUSensor() as MockIMUSensor
                val mockIMUData = imuSensor.getIMUData()

                startProcessingMockData(mockGPSData, mockIMUData)
            } else {
                val i = Intent(this, CameraActivity::class.java)
                startActivity(i)
                finish()
            }
        } else {
            editText_driver_id.error = resources.getString(R.string.cannot_proceed)
        }
    }

    private fun startProcessingMockData(mockGPSData: List<GPSData>, mockIMUData: List<IMUData>) {
        // Simulate processing of GPS data
        mockGPSData.forEach { gpsData ->
            // Simulate processing each GPS data point
            Log.d("LaunchActivity", "Processing mock GPS data: Lat=${gpsData.latitude}, Lon=${gpsData.longitude}, Timestamp=${gpsData.timestamp}")
        }

        // Simulate processing of IMU data
        mockIMUData.forEach { imuData ->
            // Simulate processing each IMU data point
            Log.d("LaunchActivity", "Processing mock IMU data: AccelX=${imuData.accelX}, AccelY=${imuData.accelY}, AccelZ=${imuData.accelZ}, GyroX=${imuData.gyroX}, GyroY=${imuData.gyroY}, GyroZ=${imuData.gyroZ}, Timestamp=${imuData.timestamp}")
        }

        // Simulate saving processed data to local storage or any other required processing
        Log.d("LaunchActivity", "Finished processing mock data")
    }


    private fun isDriverIdValid(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val driverID = sharedPref.getString(getString(R.string.pref_driverid), "-1")
        //TODO: currently, the driver id just has to exist. SF Express conventions may require more stringent rules
        return driverID != "-1"
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // Log the denied permissions for debugging purposes
        Log.d("LaunchActivity", "Permissions denied: $perms")

        // Check if any of the permissions have been permanently denied
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // Show a dialog explaining permission essentiality and guiding to app settings
            AppSettingsDialog.Builder(this).build().show()
        } else {
            // For permissions not permanently denied, you might want to show a rationale or a custom dialog
            // Inform the user why these permissions are necessary
            Toast.makeText(this, "Permissions are essential for this app. Please allow access to continue.", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onRationaleDenied(requestCode: Int) {
    }

    override fun onRationaleAccepted(requestCode: Int) {
    }
}
