package edu.gatech.ce.allgather.helpers

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.utils.IRecord
import edu.gatech.ce.allgather.utils.livedata.LocationLiveData
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
import edu.gatech.ce.allgather.utils.recordLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import kotlin.collections.HashMap

/**
 * Recorder helper
 * @author Justin Lee
 * @date 2020/6/12
 */
class RecordHelper(val context: Context) : IRecord {
    companion object {
        private val TAG = RecordHelper::class.java.simpleName

        private const val LOCATION_FOLDER = "location/"
        private const val ACCELERATION_FOLDER = "acceleration/"
        const val CAMERA_FOLDER = "camera/"
        val CALIBRATION_FOLDER = "calibration/"
        val CALCULATION_FOLDER = "calculation/"

        const val RECORDING_INTERVAL = 100
    }

    //encryption
    private val key = context.resources.getString(R.string.encryption_key)
    private val dks = DESKeySpec(key.toByteArray(Charsets.UTF_8))
    private val skf = SecretKeyFactory.getInstance("DES")
    private val desKey = skf.generateSecret(dks)
    private val cipher = Cipher.getInstance("DES")

    private var locationFOS: FileOutputStream? = null
    private var locationCOS: CipherOutputStream? = null
    private var accelerationFOS: FileOutputStream? = null
    private var accelerationCOS: CipherOutputStream? = null

    private var calculationFOS: FileOutputStream? = null

    private var isRecording = false

    private val isEncrypt = AllGatherApplication.ENCRYPT_DATA

    private var timestamp = ""

    init {
        cipher.init(Cipher.ENCRYPT_MODE, desKey)
    }

    fun startRecord(): String {
        isRecording = true

        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowmanager.defaultDisplay.rotation

        //turn timestamp into a file name
        val dt = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        val formatString = sdf.format(dt)
        timestamp = formatString

        val locationFolder = File(StorageHelper.getSpecificStorageFolderPath(formatString) + LOCATION_FOLDER)
        val accelerationFolder = File(StorageHelper.getSpecificStorageFolderPath(formatString) + ACCELERATION_FOLDER)
        val calculationFolder = File(StorageHelper.getSpecificStorageFolderPath(formatString) + CALCULATION_FOLDER)

        if (!locationFolder.mkdirs() || !accelerationFolder.mkdirs() || !calculationFolder.mkdirs()) {
            Log.d("RecordHelper", "NO CREATED FOLDER")
        }

        if (isEncrypt) {
            locationFOS = FileOutputStream(File(locationFolder, formatString + "_loc.acx"))
            locationCOS = CipherOutputStream(locationFOS, cipher)
            accelerationFOS = FileOutputStream(File(locationFolder, formatString + "_acc.acx"))
            accelerationCOS = CipherOutputStream(accelerationFOS, cipher)

            locationCOS?.write("timestamp_utc_gps,timestamp_utc_local,latitude_dd,longitude_dd,altitude_m,bearing_deg,accuracy_m, speed_ms, speed_accuracy_ms\n".toByteArray())
            accelerationCOS?.write("orientation,${rotation}\n".toByteArray())
            accelerationCOS?.write("sensor_timestamp_milliseconds,local_timestamp_milliseconds,accel_x_mps2,accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2,rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps,angvelocity_y_radps,angvelocity_z_radps,yaw,pitch,roll,bbi\n".toByteArray())
        } else {
            locationFOS = FileOutputStream(File(locationFolder, formatString + "_loc.csv"))
            accelerationFOS = FileOutputStream(File(accelerationFolder, formatString + "_acc.csv"))
            calculationFOS = FileOutputStream(File(calculationFolder, formatString + "_calc.csv"))

            locationFOS?.write("timestamp_utc_gps,timestamp_utc_local,latitude_dd,longitude_dd,altitude_m,bearing_deg,accuracy_m, speed_ms, speed_accuracy_ms\n".toByteArray())
            accelerationFOS?.write("orientation,${rotation}\n".toByteArray())
            accelerationFOS?.write("sensor_timestamp_milliseconds,local_timestamp_milliseconds,accel_x_mps2,accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2,rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps,angvelocity_y_radps,angvelocity_z_radps,yaw,pitch,roll,bbi\n".toByteArray())

            calculationFOS?.write("local_timestamp_milliseconds,transformed_acc_x,transformed_acc_y,transformed_acc_z,transformed_ang_vel_x,transformed_ang_vel_y,transformed_ang_vel_z,bbi,bbi_filtered,superelevation_filtered\n".toByteArray())
        }
        return formatString
    }

    fun recordLocation(location: LocationLiveData.LocationInfoBean?) {
        if (isRecording) {
            val sysTime = System.currentTimeMillis()

            // location
            val locS = "${location?.location?.time},${sysTime},${location?.location?.latitude},${location?.location?.longitude}," +
                    "${location?.location?.altitude},${location?.location?.bearing},${location?.location?.accuracy}," +
                    "${if(location?.location?.hasSpeed() == true) location?.location?.speed else null}," +
                    "${if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location?.location?.hasSpeedAccuracy() == true)location?.location?.speedAccuracyMetersPerSecond else null}\n"

            if (isEncrypt) {
                locationCOS?.write(locS.toByteArray())
            } else {
                locationFOS?.write(locS.toByteArray())
            }
        }
    }

    fun recordSensor(sensorInfoBean: SensorInfoLiveData.SensorInfoBean?) {
        if (isRecording) {
            val sysTime = System.currentTimeMillis()

            val accS = "" +
                    (sensorInfoBean?.sensorUpdateTimestamp ?: 0) + "," +
                    sysTime.toString() + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(0)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(1)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(2)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ROTATION)?.get(0)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ROTATION)?.get(1)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ROTATION)?.get(2)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ROTATION)?.get(3)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(0)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(1)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(2)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(0)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(1)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(2)?.toDouble() ?: "") + "," +
                    (sensorInfoBean?.sensorInfoMap?.get(SensorInfoLiveData.KEY_BBI)?.get(0)?.toDouble() ?: "") +"\n"

            if (isEncrypt) {
                accelerationCOS?.write(accS.toByteArray())
            } else {
                accelerationFOS?.write(accS.toByteArray())
            }
        }
    }

    fun recordCalculations(transformedAccelData: DoubleArray, transformedAngVelocityData: DoubleArray, bbi: Double, bbi_filtered: Double, superelevation: Double) {
        if (isRecording) {
            val sysTime = System.currentTimeMillis()

            val csvEntry = "${sysTime},${transformedAccelData[0]},${transformedAccelData[1]},${transformedAccelData[2]}," +
                    "${transformedAngVelocityData[0]},${transformedAngVelocityData[1]},${transformedAngVelocityData[2]}," +
                    "${bbi},${bbi_filtered},${superelevation}\n"

            calculationFOS?.write(csvEntry.toByteArray())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopRecord() {
        isRecording = false

        val calFolder = File(StorageHelper.getSpecificStorageFolderPath(timestamp) + CALIBRATION_FOLDER)
        if (!calFolder.mkdirs()) {
            Log.d("RecordHelper", "NO CREATED FOLDER")
        }
        StorageHelper.copyCalibration(calFolder)

        try {
            if (isEncrypt) {
                locationCOS?.flush()
                locationCOS?.close()
                accelerationCOS?.flush()
                accelerationCOS?.close()
            } else {
                locationFOS?.close()
                accelerationFOS?.close()
                calculationFOS?.close()
            }
        } catch (ex: Exception) {
            recordLog(ex)
        } finally {
            locationFOS = null
            locationCOS = null
            accelerationCOS = null
            accelerationFOS = null
        }

    }

    private fun copyCalFIle(){

    }

}