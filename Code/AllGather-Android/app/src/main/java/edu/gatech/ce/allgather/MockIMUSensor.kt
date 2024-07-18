package edu.gatech.ce.allgather

import java.io.BufferedReader
import java.io.FileReader

data class IMUData(val accelX: Double, val accelY: Double, val accelZ: Double, val gyroX: Double, val gyroY: Double, val gyroZ: Double, val timestamp: Long)

class MockIMUSensor(private val csvFilePath: String) {
    fun getMockIMUData(): List<IMUData> {
        val imuData = mutableListOf<IMUData>()
        val reader = BufferedReader(FileReader(csvFilePath))
        reader.useLines { lines ->
            lines.forEach { line ->
                val tokens = line.split(",")
                val data = IMUData(
                    tokens[0].toDouble(),
                    tokens[1].toDouble(),
                    tokens[2].toDouble(),
                    tokens[3].toDouble(),
                    tokens[4].toDouble(),
                    tokens[5].toDouble(),
                    tokens[6].toLong()
                )
                imuData.add(data)
            }
        }
        return imuData
    }
}
