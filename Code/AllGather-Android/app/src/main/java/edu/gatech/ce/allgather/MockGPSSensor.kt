package edu.gatech.ce.allgather

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileNotFoundException

data class GPSData(val latitude: Double, val longitude: Double, val timestamp: Long)

class MockGPSSensor(private val csvFilePath: String) : GPSSensor {
    override fun getGPSData(): List<GPSData> {
        val gpsData = mutableListOf<GPSData>()
        try {
            val file = File(csvFilePath)
            if (!file.exists()) {
                throw FileNotFoundException("File not found: $csvFilePath")
            }
            val reader = BufferedReader(FileReader(file))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val tokens = line.split(",")
                    val data = GPSData(tokens[0].toDouble(), tokens[1].toDouble(), tokens[2].toLong())
                    gpsData.add(data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return gpsData
    }
}
