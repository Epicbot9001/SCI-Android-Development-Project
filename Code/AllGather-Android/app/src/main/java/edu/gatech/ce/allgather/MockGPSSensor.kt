package edu.gatech.ce.allgather

import java.io.BufferedReader
import java.io.FileReader

data class GPSData(val latitude: Double, val longitude: Double, val timestamp: Long)

class MockGPSSensor(private val csvFilePath: String) {
    fun getMockGPSData(): List<GPSData> {
        val gpsData = mutableListOf<GPSData>()
        val reader = BufferedReader(FileReader(csvFilePath))
        reader.useLines { lines ->
            lines.forEach { line ->
                val tokens = line.split(",")
                val data = GPSData(tokens[0].toDouble(), tokens[1].toDouble(), tokens[2].toLong())
                gpsData.add(data)
            }
        }
        return gpsData
    }
}
