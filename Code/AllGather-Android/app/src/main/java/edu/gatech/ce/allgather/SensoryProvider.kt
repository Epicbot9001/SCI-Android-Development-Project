package edu.gatech.ce.allgather

class SensorProvider(private val useMock: Boolean, private val gpsCsvFilePath: String, private val imuCsvFilePath: String) {

    fun getGPSSensor(): GPSSensor {
        return if (useMock) {
            MockGPSSensor(gpsCsvFilePath)
        } else {
            RealGPSSensor() // Use the real GPS sensor class
        }
    }

    fun getIMUSensor(): IMUSensor {
        return if (useMock) {
            MockIMUSensor(imuCsvFilePath)
        } else {
            RealIMUSensor() // Use the real IMU sensor class
        }
    }
}
