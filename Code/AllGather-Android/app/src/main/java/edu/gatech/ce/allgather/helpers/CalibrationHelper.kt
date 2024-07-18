package edu.gatech.ce.allgather.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.processCalibrationData
import edu.gatech.ce.allgather.utils.IRecord
import edu.gatech.ce.allgather.utils.calculateBBI
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
import edu.gatech.ce.allgather.utils.recordLog
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class CalibrationHelper(val context: Context) : IRecord {

    companion object {
        private val TAG = CalibrationHelper::class.java.simpleName

        private val RAW_DATA_FOLDER = "raw_data/"
        private val CALIBRATION_RESULT_FOLDER = "calibration_results/"
        const val RECORDING_INTERVAL = 100
    }


    private var accelerationFOS: FileOutputStream? = null

    private var isRecording = false

    private var timestamp: String = ""
    private var NB_FIELDS = 13

    fun startRecord(calibrationID: Int): String {
        // Calibration ID 0 is for the first calibration step, 1 is for the second calibration step.
        // 2 is for the third calibration step.

        isRecording = true

        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowmanager.defaultDisplay.rotation

        if (timestamp == "") {
            //turn timestamp into a file name
            val dt = Date(System.currentTimeMillis())
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            timestamp = sdf.format(dt)
        }

        val accelerationFolder =
            File(StorageHelper.getSpecificCalibrationFolderPath(timestamp) + RAW_DATA_FOLDER)

        if (!accelerationFolder.mkdirs()) {
            Log.d("RecordHelper", "FOLDER NOT CREATED")
        }

        accelerationFOS =
            FileOutputStream(File(accelerationFolder, "acceleration_" + calibrationID + "_deg.csv"))

        accelerationFOS?.write("orientation,${rotation}\n".toByteArray())
        accelerationFOS?.write(
            ("timestamp_nanosecond,local_timestamp_milliseconds,accel_x_mps2," +
                    "accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2," +
                    "rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps," +
                    "angvelocity_y_radps,angvelocity_z_radps,yaw,pitch,roll,bbi\n").toByteArray()
        )

        return timestamp
    }

    fun record(sensorInfo: HashMap<String, FloatArray>?) {
        if (isRecording) {
            val sysTime = System.currentTimeMillis()

            val accS = sysTime.toString() + "," +
                    System.currentTimeMillis() + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(0) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(1) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ACCELERATE)?.get(2) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ROTATION)?.get(0) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ROTATION)?.get(1) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ROTATION)?.get(2) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ROTATION)?.get(3) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(0) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(1) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_GYROSCOPE)?.get(2) ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(0)
                        ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(1)
                        ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_ORIENTATION_ANGLE)?.get(2)
                        ?: "") + "," +
                    (sensorInfo?.get(SensorInfoLiveData.KEY_BBI)?.get(0) ?: "") + "\n"

            accelerationFOS?.write(accS.toByteArray())

            Log.d(TAG, "calibration sensors:$accS")
        }
    }

    fun stopRecord() {
        isRecording = false
        try {
            accelerationFOS?.close()

        } catch (ex: Exception) {
            recordLog(ex)
        } finally {
            accelerationFOS = null
        }
    }

    fun startEndCalibration() {
        val accelerationFolder = File(StorageHelper.getSpecificCalibrationFolderPath(timestamp) + CALIBRATION_RESULT_FOLDER)

        if (!accelerationFolder.mkdirs()) {
            Log.d("RecordHelper", "FOLDER NO CREATED")
        }

        accelerationFOS = FileOutputStream(File(accelerationFolder, timestamp + "_calibration.csv"))
        accelerationFOS?.write(
            ("recording_time_second,orientation_deg,accel_x_mps2," +
                    "accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2," +
                    "rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps," +
                    "angvelocity_y_radps,angvelocity_z_radps,yaw,pitch,roll\n").toByteArray()
        )
    }

    @SuppressLint("NewApi")
    fun getTransformedBBI(calibrationIDs: List<Int>): List<Double> {
        val accS: MutableList<String> = mutableListOf()
        for (calibrationID in calibrationIDs) {
            accS.add(averageVector(calibrationID))
        }

        // Convert accS to List<HashMap<String, String>>
        val calibrationData: List<HashMap<String, String>> = accS.map { line ->
            val row = line.split(",")
            hashMapOf(
                "recording_time_second" to row[0],
                "orientation_deg" to row[1],
                "accel_x_mps2" to row[2],
                "accel_y_mps2" to row[3],
                "accel_z_mps2" to row[4],
                "rotation_x_sin_theta_by_2" to row[5],
                "rotation_y_sin_theta_by_2" to row[6],
                "rotation_z_sin_theta_by_2" to row[7],
                "rotation_cos_theta_by_2" to row[8],
                "angvelocity_x_radps" to row[9],
                "angvelocity_y_radps" to row[10],
                "angvelocity_z_radps" to row[11],
                "yaw" to row[12],
                "pitch" to row[13],
                "roll" to row[14]
            )
        }

        val (_, _, transformedCalibrationData) = processCalibrationData(calibrationData)

        val result = mutableListOf<Double>()
        for (i in 0 until transformedCalibrationData.size) {
            val transformedBBI = calculateBBI(transformedCalibrationData[i]["accel_y_mps2"]!!.toDouble(), transformedCalibrationData[i]["accel_z_mps2"]!!.toDouble())
            result.add(transformedBBI)
        }

        return result.toList()
    }

    @SuppressLint("NewApi")
    fun endCalibraton() {
        val firstAccS = averageVector(0)
        accelerationFOS?.write(firstAccS.toByteArray())

        val secondAccS = averageVector(1)
        accelerationFOS?.write(secondAccS.toByteArray())

//        // This is the third calibration step which is used only for validation.
//        val thirdAccS = averageVector(2)
//        accelerationFOS?.write(thirdAccS.toByteArray())
//
//        val firstBBI = calculateBBI(firstAccS.split(",")[2].toDouble(), firstAccS.split(",")[3].toDouble())
//        val secondBBI = calculateBBI(secondAccS.split(",")[2].toDouble(), secondAccS.split(",")[3].toDouble())
//        val thirdBBI = calculateBBI(thirdAccS.split(",")[2].toDouble(), secondAccS.split(",")[3].toDouble())
//
        val calibrationFilePath = StorageHelper.getSpecificCalibrationFolderPath(timestamp) + CALIBRATION_RESULT_FOLDER + timestamp + "_calibration.csv"
//        val (_, _, transformedCalibrationData) = processCalibrationData(calibrationFilePath)
//
//        val firstTransformedBBI = calculateBBI(transformedCalibrationData[0]["accel_y_mps2"]!!.toDouble(), transformedCalibrationData[0]["accel_z_mps2"]!!.toDouble())
//        val secondTransformedBBI = calculateBBI(transformedCalibrationData[1]["accel_y_mps2"]!!.toDouble(), transformedCalibrationData[1]["accel_z_mps2"]!!.toDouble())
//        val thirdTransformedBBI = calculateBBI(transformedCalibrationData[2]["accel_y_mps2"]!!.toDouble(), transformedCalibrationData[2]["accel_z_mps2"]!!.toDouble())
//
//        Log.d("Calibration BBI", "firstBBI: $firstBBI, secondBBI: $secondBBI")
//        Log.d("Calibration BBI", "firstTransformedBBI: $firstTransformedBBI, secondTransformedBBI: $secondTransformedBBI")
//        Log.d("Calibration BBI", "thirdBBI: $thirdBBI, thirdTransformedBBI: $thirdTransformedBBI")

        stopRecord()

        // Add the calibration file path to the context
        context.getSharedPreferences("calibration", Context.MODE_PRIVATE).edit().putString("calibrationFilePath", calibrationFilePath).apply()
    }

    fun averageVector(calibrationID: Int): String {
        val fName =
            StorageHelper.getSpecificCalibrationFolderPath(timestamp) + RAW_DATA_FOLDER + "acceleration_" + calibrationID + "_deg.csv"

        // Calibration ID 0 is for the first calibration step, 1 is for the second calibration step.
        // 2 is for the third calibration step.
        var orientation = "0"
        if (calibrationID == 1) {
            orientation = "180"
        }

        var count = 0
        var line: Array<String>
        val vector = DoubleArray(NB_FIELDS)

        var lines = File(fName).readLines()
        lines = lines.subList(2, lines.size)
        lines.forEach { l ->
            line = l.split(",").toTypedArray()
            for (j in 2 until NB_FIELDS + 2) {
                vector[j-2] += line[j].toDouble()
            }
            count += 1
        }

        val totTime = (count + 1) * RECORDING_INTERVAL / 1000
        var accS = "$totTime,$orientation,"
        for (j in 0 until NB_FIELDS) {
            accS += (vector[j] / count).toString() + ","
        }
        accS = accS.dropLast(1) + "\n"

        return accS
    }
}
