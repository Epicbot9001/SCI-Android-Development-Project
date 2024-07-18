package edu.gatech.ce.allgather.utils

import android.os.Build
import androidx.annotation.RequiresApi
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.ejml.simple.SimpleMatrix
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.tan

enum class Orientation {
    SMARTPHONE_LANDSCAPE_LEFT,
    SMARTPHONE_LANDSCAPE_RIGHT,
    SMARTPHONE_FACE_UP,
    GOPRO_LANDSCAPE_BUTTON_UP,
    GOPRO_LANDSCAPE_BUTTON_DOWN
}

fun transformSensorToVehicleCoordinates(
    sensorData: Array<DoubleArray>,
    orientation: Orientation
): Array<DoubleArray> {
    // Define transformation matrices
    val smartphoneLandscapeLeftMatrix = SimpleMatrix(
        arrayOf(
            doubleArrayOf(0.0, 0.0, 1.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(-1.0, 0.0, 0.0)
        )
    )
    val smartphoneLandscapeRightMatrix = SimpleMatrix(
        arrayOf(
            doubleArrayOf(0.0, 0.0, -1.0),
            doubleArrayOf(0.0, -1.0, 0.0),
            doubleArrayOf(-1.0, 0.0, 0.0)
        )
    )
    val smartphoneFaceUpMatrix = SimpleMatrix(
        arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
    )
    val goproLandscapeButtonUpMatrix = SimpleMatrix(
        arrayOf(
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(-1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
    )
    val goproLandscapeButtonDownMatrix = SimpleMatrix(
        arrayOf(
            doubleArrayOf(0.0, -1.0, 0.0),
            doubleArrayOf(-1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, -1.0)
        )
    )

    val transformationMatrix: SimpleMatrix
    transformationMatrix =
        when (orientation) {
            Orientation.SMARTPHONE_LANDSCAPE_LEFT -> smartphoneLandscapeLeftMatrix
            Orientation.SMARTPHONE_LANDSCAPE_RIGHT -> smartphoneLandscapeRightMatrix
            Orientation.SMARTPHONE_FACE_UP -> smartphoneFaceUpMatrix
            Orientation.GOPRO_LANDSCAPE_BUTTON_UP -> goproLandscapeButtonUpMatrix
            Orientation.GOPRO_LANDSCAPE_BUTTON_DOWN -> goproLandscapeButtonDownMatrix
            else -> throw IllegalArgumentException("Invalid orientation")
        }

    val sensorMatrix = SimpleMatrix(sensorData)
    val transformedSensorMatrix = sensorMatrix.mult(transformationMatrix)
    val transformedSensorData = Array(transformedSensorMatrix.numRows()) {
        DoubleArray(
            transformedSensorMatrix.numCols()
        )
    }
    for (i in 0 until transformedSensorMatrix.numRows()) {
        for (j in 0 until transformedSensorMatrix.numCols()) {
            transformedSensorData[i][j] = transformedSensorMatrix[i, j]
        }
    }
    return transformedSensorData
}

fun determineSmartphoneOrientation(accelColumns: Array<DoubleArray>): Orientation {
    // Calculate the average acceleration values for the first 10 readings
    var sumX = 0.0
    var sumY = 0.0
    var sumZ = 0.0
    val readings = min(accelColumns.size.toDouble(), 10.0).toInt()
    for (i in 0 until readings) {
        sumX += accelColumns[i][0]
        sumY += accelColumns[i][1]
        sumZ += accelColumns[i][2]
    }
    val avgX = sumX / readings
    val avgY = sumY / readings
    val avgZ = sumZ / readings

    // Determine the orientation based on the average acceleration values
    if (abs(avgX) > abs(avgY) && abs(avgX) > abs(avgZ)) {
        return if (avgX > 0) {
            Orientation.SMARTPHONE_LANDSCAPE_LEFT
        } else {
            Orientation.SMARTPHONE_LANDSCAPE_RIGHT
        }
    } else if (abs(avgY) > abs(avgX) && abs(avgY) > abs(avgZ)) {
        // Add more orientation cases for portrait mode if needed
    } else if (abs(avgZ) > abs(avgX) && abs(avgZ) > abs(avgY)) {
        // Add more orientation cases for flat mode if needed
        if (avgZ > 0) {
            return Orientation.SMARTPHONE_FACE_UP
        }
    }

    throw IllegalArgumentException("Unable to determine the smartphone orientation based on the calibration data.")
}

fun calculateBBI(yAcc: Double, zAcc: Double): Double {
//    dummy function
    return 0.0
}

fun getSuperelevation(
    speed: Double,
    gyro: Double,
    bbi: Double,
    k: Double,
    speedUnit: String,
    gyroUnit: String
): Double {
//    dummy function
    return 0.0
}

@RequiresApi(Build.VERSION_CODES.O)
fun loadAccCsvData(filePath: String): List<HashMap<String, String>> {
    val path = Paths.get(filePath)
    try {
        CSVParser.parse(path, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())
            .use { csvParser ->
                return csvParser.getRecords().stream()
                    .map { csvRecord: CSVRecord ->
                        val dataMap =
                            HashMap<String, String>()
                        csvRecord.toMap()
                            .forEach { (key: String, value: String) ->
                                dataMap[key.trim { it <= ' ' }] = value
                            } // Trim the key before putting it in the map
                        dataMap
                    }
                    .collect(
                        Collectors.toList()
                    )
            }
    } catch (e: java.lang.Exception) {
        throw java.lang.Exception("Error loading CSV data from $filePath", e)
    }
}
