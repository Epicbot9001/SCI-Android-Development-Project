//package edu.gatech.ce.allgather.mocks
//
//import android.location.Location
//import edu.gatech.ce.allgather.utils.livedata.LocationLiveData.LocationInfoBean
//import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData.SensorInfoBean
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import java.io.File
//import java.io.FileReader
//import java.text.SimpleDateFormat
//import java.util.*
//
//interface GPSSensor {
//    fun getLocation(): Flow<LocationInfoBean>
//}
//
//interface IMUSensor {
//    fun getSensorData(): Flow<SensorInfoBean>
//}
//
//class MockGPSSensor(private val csvFile: String) : GPSSensor {
//    override fun getLocation(): Flow<LocationInfoBean> = flow {
//        val reader = FileReader(File(csvFile))
//        reader.readLines().drop(1).forEach { line ->
//            val parts = line.split(",")
//            val location = Location("mock").apply {
//                latitude = parts[1].toDouble()
//                longitude = parts[2].toDouble()
//                speed = parts[3].toFloat()
//                time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse(parts[0])?.time ?: 0
//            }
//            emit(LocationInfoBean(true, location))
//            delay(1000) // Emit every second, adjust as needed
//        }
//    }
//}
//
//class MockIMUSensor(private val csvFile: String) : IMUSensor {
//    override fun getSensorData(): Flow<SensorInfoBean> = flow {
//        val reader = FileReader(File(csvFile))
//        reader.readLines().drop(1).forEach { line ->
//            val parts = line.split(",")
//            val sensorInfoMap = hashMapOf(
//                "KEY_ACCELERATE" to floatArrayOf(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat()),
//                "KEY_GYROSCOPE" to floatArrayOf(parts[4].toFloat(), parts[5].toFloat(), parts[6].toFloat())
//            )
//            emit(SensorInfoBean(sensorInfoMap))
//            delay(20) // Emit every 20ms, adjust based on your sensor frequency
//        }
//    }
//}
//
//class CSVReader(private val filePath: String) {
//    fun readCSV(): List<Map<String, String>> {
//        val reader = FileReader(File(filePath))
//        val lines = reader.readLines()
//        val headers = lines.first().split(",")
//        return lines.drop(1).map { line ->
//            val values = line.split(",")
//            headers.zip(values).toMap()
//        }
//    }
//}
//
//// Factory for creating real or mock sensors
//object SensorFactory {
//    fun createGPSSensor(isTestMode: Boolean, csvFile: String? = null): GPSSensor {
//        return if (isTestMode && csvFile != null) {
//            MockGPSSensor(csvFile)
//        } else {
//            RealGPSSensor()
//        }
//    }
//
//    fun createIMUSensor(isTestMode: Boolean, csvFile: String? = null): IMUSensor {
//        return if (isTestMode && csvFile != null) {
//            MockIMUSensor(csvFile)
//        } else {
//            RealIMUSensor()
//        }
//    }
//}
//
//// Placeholder for real sensor implementations
//class RealGPSSensor : GPSSensor {
//    override fun getLocation(): Flow<LocationInfoBean> {
//        // Implement real GPS sensor logic here
//        TODO("Not yet implemented")
//    }
//}
//
//class RealIMUSensor : IMUSensor {
//    override fun getSensorData(): Flow<SensorInfoBean> {
//        // Implement real IMU sensor logic here
//        TODO("Not yet implemented")
//    }
//}