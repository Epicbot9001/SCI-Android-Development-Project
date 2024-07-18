package edu.gatech.ce.allgather.utils.livedata

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2

/**
 * Acceleration Live Data
 * @author Justin Lee
 * @date 2020/6/10
 */
class SensorInfoLiveData(val context: Context) : LiveData<HashMap<String, FloatArray>>(), SensorEventListener2 {
    companion object {
        const val KEY_ACCELERATE = "key_accelerate"
        const val KEY_MAGNETIC_FORCE = "key_magnetic_force"
        const val KEY_ROTATION = "key_rotation"
        const val KEY_GYROSCOPE = "key_gyroscope"
        const val KEY_BBI = "key_bbi"
        //get by calculation
        const val KEY_ORIENTATION_ANGLE = "key_orientation_angle"

        private val ACCELERATION_FOLDER = "acceleration/"
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var mLinearAcceleration: Sensor
    private lateinit var mMagneticForce: Sensor
    private lateinit var mRotation: Sensor
    private lateinit var mGyroscope: Sensor
    private lateinit var mZeroVectorXY: FloatArray

    //sensor information
    private val sensorInfoMap = HashMap<String, FloatArray>()

    private val _sensorUpdates = MutableSharedFlow<SensorInfoBean>(extraBufferCapacity = 1000)
    val sensorUpdates = _sensorUpdates.asSharedFlow()

    var shouldPublishSensorUpdates = false
    private val PUBLISH_INTERVAL = 100L // Corresponding to 10Hz frequency, 1000ms/10 = 100ms
    private var lastPublishTime = 0L

    init {
        if (context is Activity) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            mLinearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
            mMagneticForce = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
            mRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!
            mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
            regListener()
        }
    }

    /**
     * register each sensor
     *
     * Created by Justin Lee
     */
    private fun regListener() {
        // linear acceleration
        sensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_UI)
        // magnetic force
        sensorManager.registerListener(this, mMagneticForce, SensorManager.SENSOR_DELAY_UI)
        // rotation
        sensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_UI)
        // gyroscope
        sensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI)

    }

    override fun onActive() {
        super.onActive()
        regListener()
    }

    override fun onInactive() {
        super.onInactive()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // accuracy changing
    }

    override fun onFlushCompleted(sensor: Sensor) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                sensorInfoMap[KEY_ACCELERATE] = event.values
            }
            Sensor.TYPE_GYROSCOPE -> {
                sensorInfoMap[KEY_GYROSCOPE] = event.values
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                sensorInfoMap[KEY_ROTATION] = event.values
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                sensorInfoMap[KEY_MAGNETIC_FORCE] = event.values
            }
        }
        sensorInfoMap[KEY_ORIENTATION_ANGLE] = getOrientationAngles()
//        sensorInfoMap[KEY_BBI] = getBBI()
        postValue(sensorInfoMap)

        if ((currentTime - lastPublishTime >= PUBLISH_INTERVAL) && (shouldPublishSensorUpdates)) {
            // Deep copy the sensorInfoMap
            val sensorInfoMapCopy = HashMap<String, FloatArray>()
            for ((key, value) in sensorInfoMap) {
                sensorInfoMapCopy[key] = value.copyOf()
            }

            // Emit to the shared flow and update the last update time
            _sensorUpdates.tryEmit(
                SensorInfoBean(
                    sensorInfoMapCopy.toMap() as HashMap<String, FloatArray>,
                    currentTime
                )
            )
            lastPublishTime = currentTime
        }
    }

    /**
     * get orientation angles
     * depends on [sensorInfoMap] ([KEY_ACCELERATE],[KEY_MAGNETIC_FORCE])
     * @param
     * @return
     * Created by Justin Lee
     */
    private fun getOrientationAngles(): FloatArray {
        val accelerationInfo = if (sensorInfoMap.containsKey(KEY_ACCELERATE)) {
            sensorInfoMap[KEY_ACCELERATE]
        } else {
            floatArrayOf(0f, 0f, 0f)
        }

        val magneticForceInfo = if (sensorInfoMap.containsKey(KEY_MAGNETIC_FORCE)) {
            sensorInfoMap[KEY_MAGNETIC_FORCE]
        } else {
            floatArrayOf(0f, 0f, 0f)
        }

        //get roll, pitch, azimuth
        var rotMatrix = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        SensorManager.getRotationMatrix(rotMatrix, null, accelerationInfo, magneticForceInfo)
        var orientationAngles = floatArrayOf(0f, 0f, 0f)
        SensorManager.getOrientation(rotMatrix, orientationAngles)
        return orientationAngles
    }

    data class SensorInfoBean(val sensorInfoMap: HashMap<String, FloatArray>, val sensorUpdateTimestamp: Long)

//    /**
//     * get orientation angles
//     * depends on [sensorInfoMap] ([KEY_ACCELERATE],[KEY_MAGNETIC_FORCE])
//     * @param
//     * @return
//     * Created by Tianqi Liu
//     */
//    private fun getBBI(): FloatArray {
//
//        fun dot(a: FloatArray, b: FloatArray): Float = ((a zip b).map{ it.first * it.second}).sum()
//        val accVectorXY = if (sensorInfoMap.containsKey(KEY_ACCELERATE)) {
//            sensorInfoMap[KEY_ACCELERATE]
//        } else {
//            floatArrayOf(0f, 0f)
//        }
//        val ax = accVectorXY!!.get(0); val ay = accVectorXY!!.get(1)
//        val gx = mZeroVectorXY!!.get(0); val gy = mZeroVectorXY!!.get(1)
//
//        val bbi = floatArrayOf(atan2((ax*gy-ay*gx), (ax*gx+ay*gy)) * PI.toFloat() / 180)
//
//        return bbi
//    }
}