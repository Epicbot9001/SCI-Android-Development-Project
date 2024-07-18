package edu.gatech.ce.allgather

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileNotFoundException

data class IMUData(val accelX: Double, val accelY: Double, val accelZ: Double, val gyroX: Double, val gyroY: Double, val gyroZ: Double, val timestamp: Long)

class MockIMUSensor(private val csvFilePath: String) : IMUSensor {
    override fun getIMUData(): List<IMUData> {
        val imuData = mutableListOf<IMUData>()
        try {
            val file = File(csvFilePath)
            if (!file.exists()) {
                throw FileNotFoundException("File not found: $csvFilePath")
            }
            val reader = BufferedReader(FileReader(file))
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imuData
    }
}
