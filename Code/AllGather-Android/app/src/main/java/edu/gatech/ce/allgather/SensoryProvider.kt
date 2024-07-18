package edu.gatech.ce.allgather

class SensorProvider(private val useMock: Boolean, private val gpsCsvFilePath: String, private val imuCsvFilePath: String) {

    fun getGPSSensor(): Any {
        return if (useMock) {
            MockGPSSensor(gpsCsvFilePath)
        } else {
            RealGPSSensor()
        }
    }

    fun getIMUSensor(): Any {
        return if (useMock) {
            MockIMUSensor(imuCsvFilePath)
        } else {
            RealIMUSensor()
        }
    }
}
