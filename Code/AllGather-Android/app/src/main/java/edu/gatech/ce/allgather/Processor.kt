package edu.gatech.ce.allgather

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import edu.gatech.ce.allgather.filters.LowPassFilter
import edu.gatech.ce.allgather.filters.SignalFilter
import edu.gatech.ce.allgather.old.utils.StorageUtil
import edu.gatech.ce.allgather.utils.Orientation
import edu.gatech.ce.allgather.utils.livedata.LocationLiveData
import edu.gatech.ce.allgather.utils.livedata.LocationLiveData.LocationInfoBean
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData.SensorInfoBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import edu.gatech.ce.allgather.utils.toMatrix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.ejml.simple.SimpleMatrix


class Processor(private val locationFlow: Flow<LocationInfoBean>, private val sensorFlow: Flow<SensorInfoBean>) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var latestLocation: Location? = null
    private lateinit var storageUtil: StorageUtil

    private val accelFilters: Array<SignalFilter> = Array(3) { LowPassFilter(0.05) }
    private val angVelocityFilters: Array<SignalFilter> = Array(3) { LowPassFilter(0.05) }

    private val _calculationsFlow = MutableSharedFlow<CalculationBean>(extraBufferCapacity = 100)
    val calculationsFlow = _calculationsFlow.asSharedFlow()

    private lateinit var orientation: Orientation
    private lateinit var rotationMatrix: SimpleMatrix

    companion object {
        private const val TAG = "Processor"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startProcessing() {
        storageUtil = StorageUtil(AllGatherApplication.instance)

        coroutineScope.launch {
            locationFlow.collect { locationInfoBean ->
                processLocation(locationInfoBean)
            }
        }

        coroutineScope.launch {
            sensorFlow.collect { sensorInfoBean ->
                processSensor(sensorInfoBean)
            }
        }

        coroutineScope.launch {
            while (true) {
                storageUtil.updateStorageSpace()
                Thread.sleep(1000)
            }
        }

        val calibrationFilePath: String = AllGatherApplication.instance.getSharedPreferences("calibration", 0).getString("calibrationFilePath", "")!!
        if (calibrationFilePath.isEmpty()) {
            Log.d(TAG, "Calibration file path is empty.")
            return
        }

        val res = processCalibrationData(calibrationFilePath)
        orientation = res.first
        rotationMatrix = res.second
    }

    private fun getLatestSpeed(): Double {
        val latestSpeed = latestLocation?.speed ?: 0f
        return latestSpeed.toDouble()
    }

    private fun processLocation(locationInfoBean: LocationInfoBean) {
        // Your logic to process location update
        val location = locationInfoBean.location
        if (locationInfoBean.isGpsWork && location != null) {
            latestLocation = location
            val speed = location.speed
            // Process the location data, e.g., logging, updating a database, etc.
            Log.d(TAG, "New location: Lat=${location.latitude}, Lon=${location.longitude}, Speed=${speed}")
        } else {
            Log.e(TAG, "GPS is not working.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processSensor(sensorInfoBean: SensorInfoBean) {
        val sensorInfoMap: HashMap<String, FloatArray> = sensorInfoBean.sensorInfoMap

        val acceleration = sensorInfoMap[SensorInfoLiveData.KEY_ACCELERATE]!!
        val gyroscope = sensorInfoMap[SensorInfoLiveData.KEY_GYROSCOPE]!!

        // Find the value of the key calibrationFilePath in the app context
        val calibrationFilePath: String = AllGatherApplication.instance.getSharedPreferences("calibration", 0).getString("calibrationFilePath", "")!!
        if (calibrationFilePath.isEmpty()) {
            Log.d(TAG, "Calibration file path is empty.")
            return
        }

        val accelData = toMatrix(acceleration)
        val angVelocityData = toMatrix(gyroscope)

        val (transformedAccelData, transformedAngVelocityData) = transformIMUData(accelData, angVelocityData, orientation, rotationMatrix)
        val (bbi, bbiFiltered, superelevation) = processCrossSlope(getLatestSpeed(), transformedAccelData[0], transformedAngVelocityData[0], accelFilters, angVelocityFilters)
        _calculationsFlow.tryEmit(CalculationBean(transformedAccelData[0].clone(), transformedAngVelocityData[0].clone(), bbi, bbiFiltered, superelevation))
    }

    fun stopProcessing() {
        // re-initialize all the filters
        accelFilters.forEach { it.reset() }
        angVelocityFilters.forEach { it.reset() }
    }

    data class CalculationBean(val transformedAccelData: DoubleArray, val transformedAngVelocityData: DoubleArray, val bbi: Double, val bbiFiltered: Double, val superelevation: Double) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CalculationBean

            if (!transformedAccelData.contentEquals(other.transformedAccelData)) return false
            if (!transformedAngVelocityData.contentEquals(other.transformedAngVelocityData)) return false
            if (bbi != other.bbi) return false
            if (bbiFiltered != other.bbiFiltered) return false
            if (superelevation != other.superelevation) return false

            return true
        }

        override fun hashCode(): Int {package edu.gatech.ce.allgather

            import android.location.Location
                    import android.os.Build
                    import android.util.Log
                    import androidx.annotation.RequiresApi
                    import edu.gatech.ce.allgather.filters.LowPassFilter
                    import edu.gatech.ce.allgather.filters.SignalFilter
                    import edu.gatech.ce.allgather.old.utils.StorageUtil
                    import edu.gatech.ce.allgather.utils.Orientation
                    import edu.gatech.ce.allgather.utils.livedata.LocationLiveData
                    import edu.gatech.ce.allgather.utils.livedata.LocationLiveData.LocationInfoBean
                    import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
                    import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData.SensorInfoBean
                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Dispatchers
                    import kotlinx.coroutines.launch
                    import edu.gatech.ce.allgather.utils.toMatrix
                    import kotlinx.coroutines.flow.Flow
                    import kotlinx.coroutines.flow.MutableSharedFlow
                    import kotlinx.coroutines.flow.asSharedFlow
                    import org.ejml.simple.SimpleMatrix


            class Processor(private val locationFlow: Flow<LocationInfoBean>, private val sensorFlow: Flow<SensorInfoBean>) {

                private val coroutineScope = CoroutineScope(Dispatchers.Default)
                private var latestLocation: Location? = null
                private lateinit var storageUtil: StorageUtil

                private val accelFilters: Array<SignalFilter> = Array(3) { LowPassFilter(0.05) }
                private val angVelocityFilters: Array<SignalFilter> = Array(3) { LowPassFilter(0.05) }

                private val _calculationsFlow = MutableSharedFlow<CalculationBean>(extraBufferCapacity = 100)
                val calculationsFlow = _calculationsFlow.asSharedFlow()

                private lateinit var orientation: Orientation
                private lateinit var rotationMatrix: SimpleMatrix

                companion object {
                    private const val TAG = "Processor"
                }

                @RequiresApi(Build.VERSION_CODES.O)
                fun startProcessing() {
                    storageUtil = StorageUtil(AllGatherApplication.instance)

                    coroutineScope.launch {
                        locationFlow.collect { locationInfoBean ->
                            processLocation(locationInfoBean)
                        }
                    }

                    coroutineScope.launch {
                        sensorFlow.collect { sensorInfoBean ->
                            processSensor(sensorInfoBean)
                        }
                    }

                    coroutineScope.launch {
                        while (true) {
                            storageUtil.updateStorageSpace()
                            Thread.sleep(1000)
                        }
                    }

                    val calibrationFilePath: String = AllGatherApplication.instance.getSharedPreferences("calibration", 0).getString("calibrationFilePath", "")!!
                    if (calibrationFilePath.isEmpty()) {
                        Log.d(TAG, "Calibration file path is empty.")
                        return
                    }

                    val res = processCalibrationData(calibrationFilePath)
                    orientation = res.first
                    rotationMatrix = res.second
                }

                private fun getLatestSpeed(): Double {
                    val latestSpeed = latestLocation?.speed ?: 0f
                    return latestSpeed.toDouble()
                }

                private fun processLocation(locationInfoBean: LocationInfoBean) {
                    // Your logic to process location update
                    val location = locationInfoBean.location
                    if (locationInfoBean.isGpsWork && location != null) {
                        latestLocation = location
                        val speed = location.speed
                        // Process the location data, e.g., logging, updating a database, etc.
                        Log.d(TAG, "New location: Lat=${location.latitude}, Lon=${location.longitude}, Speed=${speed}")
                    } else {
                        Log.e(TAG, "GPS is not working.")
                    }
                }

                @RequiresApi(Build.VERSION_CODES.O)
                private fun processSensor(sensorInfoBean: SensorInfoBean) {
                    val sensorInfoMap: HashMap<String, FloatArray> = sensorInfoBean.sensorInfoMap

                    val acceleration = sensorInfoMap[SensorInfoLiveData.KEY_ACCELERATE]!!
                    val gyroscope = sensorInfoMap[SensorInfoLiveData.KEY_GYROSCOPE]!!

                    // Find the value of the key calibrationFilePath in the app context
                    val calibrationFilePath: String = AllGatherApplication.instance.getSharedPreferences("calibration", 0).getString("calibrationFilePath", "")!!
                    if (calibrationFilePath.isEmpty()) {
                        Log.d(TAG, "Calibration file path is empty.")
                        return
                    }

                    val accelData = toMatrix(acceleration)
                    val angVelocityData = toMatrix(gyroscope)

                    val (transformedAccelData, transformedAngVelocityData) = transformIMUData(accelData, angVelocityData, orientation, rotationMatrix)
                    val (bbi, bbiFiltered, superelevation) = processCrossSlope(getLatestSpeed(), transformedAccelData[0], transformedAngVelocityData[0], accelFilters, angVelocityFilters)
                    _calculationsFlow.tryEmit(CalculationBean(transformedAccelData[0].clone(), transformedAngVelocityData[0].clone(), bbi, bbiFiltered, superelevation))
                }

                fun stopProcessing() {
                    // re-initialize all the filters
                    accelFilters.forEach { it.reset() }
                    angVelocityFilters.forEach { it.reset() }
                }

                data class CalculationBean(val transformedAccelData: DoubleArray, val transformedAngVelocityData: DoubleArray, val bbi: Double, val bbiFiltered: Double, val superelevation: Double) {
                    override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (javaClass != other?.javaClass) return false

                        other as CalculationBean

                        if (!transformedAccelData.contentEquals(other.transformedAccelData)) return false
                        if (!transformedAngVelocityData.contentEquals(other.transformedAngVelocityData)) return false
                        if (bbi != other.bbi) return false
                        if (bbiFiltered != other.bbiFiltered) return false
                        if (superelevation != other.superelevation) return false

                        return true
                    }

                    override fun hashCode(): Int {
                        var result = transformedAccelData.contentHashCode()
                        result = 31 * result + transformedAngVelocityData.contentHashCode()
                        result = 31 * result + bbi.hashCode()
                        result = 31 * result + bbiFiltered.hashCode()
                        result = 31 * result + superelevation.hashCode()
                        return result
                    }
                }
            }

            var result = transformedAccelData.contentHashCode()
            result = 31 * result + transformedAngVelocityData.contentHashCode()
            result = 31 * result + bbi.hashCode()
            result = 31 * result + bbiFiltered.hashCode()
            result = 31 * result + superelevation.hashCode()
            return result
        }
    }
}
