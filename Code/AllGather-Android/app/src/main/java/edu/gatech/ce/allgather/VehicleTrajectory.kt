package edu.gatech.ce.allgather

import android.os.Build
import androidx.annotation.RequiresApi
import edu.gatech.ce.allgather.utils.Orientation
import edu.gatech.ce.allgather.utils.determineSmartphoneOrientation
import edu.gatech.ce.allgather.utils.getZTransformationMatrix
import edu.gatech.ce.allgather.utils.getZVectFromCalib
import edu.gatech.ce.allgather.utils.transformSensorToVehicleCoordinates
import org.ejml.simple.SimpleMatrix
import edu.gatech.ce.allgather.utils.loadAccCsvData

fun getAccelColumns(data: List<HashMap<String, String>>): Array<DoubleArray> {
    // Return the accel data as a 2D array where the columns are accel_x_mps2, accel_y_mps2, accel_z_mps2
    val accelColumns = Array(data.size) { DoubleArray(3) }
    for (i in data.indices) {
        val row = data[i]
        val accelData = DoubleArray(3)
        accelData[0] = row["accel_x_mps2"]!!.toDouble()
        accelData[1] = row["accel_y_mps2"]!!.toDouble()
        accelData[2] = row["accel_z_mps2"]!!.toDouble()
        accelColumns[i] = accelData
    }
    return accelColumns
}

fun getAngVelocityColumns(data: List<HashMap<String, String>>): Array<DoubleArray> {
    // Return the angular velocity data as a 2D array where the columns are
    // angvelocity_x_radps, angvelocity_y_radps, angvelocity_z_radps
    val angVelocityColumns = Array(data.size) { DoubleArray(3) }
    for (i in data.indices) {
        val row = data[i]
        val angVelocityData = DoubleArray(3)
        angVelocityData[0] = row["angvelocity_x_radps"]!!.toDouble()
        angVelocityData[1] = row["angvelocity_y_radps"]!!.toDouble()
        angVelocityData[2] = row["angvelocity_z_radps"]!!.toDouble()
        angVelocityColumns[i] = angVelocityData
    }
    return angVelocityColumns
}

fun roughTransformAccelData(accelerationData: Array<DoubleArray>, orientation: Orientation): Array<DoubleArray> {
    return transformSensorToVehicleCoordinates(accelerationData, orientation)
}

private fun roughTransformAngVelocityData(angVelocityData: Array<DoubleArray>, orientation: Orientation): Array<DoubleArray> {
    return transformSensorToVehicleCoordinates(angVelocityData, orientation)
}

private fun applyRotationSensorData(rotationMatrix: SimpleMatrix, data: Array<DoubleArray>): Array<DoubleArray> {
    val matrix = SimpleMatrix(data)
    val rotatedMatrix = matrix.mult(rotationMatrix.transpose())
    // Convert the above SimpleMatrix object to a 2D array of doubles and return it
    return Array(rotatedMatrix.numRows()) { i ->
        DoubleArray(rotatedMatrix.numCols()) { j -> rotatedMatrix[i, j] }
    }
}

fun getOrientation(calibrationData: List<HashMap<String, String>>): Orientation {
    val accelColumns = getAccelColumns(calibrationData)
    return determineSmartphoneOrientation(accelColumns)
}

// Get the orientation, Fine Rotation Matrix and the Finely Transformed Calibration Data
@RequiresApi(Build.VERSION_CODES.O)
fun processCalibrationData(calibrationFilePath: String): Triple<Orientation, SimpleMatrix, List<HashMap<String, String>>> {
    var calibrationData: List<HashMap<String, String>> = ArrayList()
    if (calibrationFilePath.isNotEmpty()) {
        calibrationData = loadAccCsvData(calibrationFilePath)
    }
    return processCalibrationDataCore(calibrationData)
}

// Get the orientation, Fine Rotation Matrix and the Finely Transformed Calibration Data
@RequiresApi(Build.VERSION_CODES.O)
fun processCalibrationData(calibrationData: List<HashMap<String, String>>): Triple<Orientation, SimpleMatrix, List<HashMap<String, String>>> {
    return processCalibrationDataCore(calibrationData)
}

// Get the orientation, Fine Rotation Matrix and the Finely Transformed Calibration Data
@RequiresApi(Build.VERSION_CODES.O)
private fun processCalibrationDataCore(calibrationData: List<HashMap<String, String>>): Triple<Orientation, SimpleMatrix, List<HashMap<String, String>>> {
    // Get the orientation of the smartphone from the calibration data.
    val orientation: Orientation = getOrientation(calibrationData)

    var calibAccelColumns = getAccelColumns(calibrationData)
    var calibAngVelocityColumns = getAngVelocityColumns(calibrationData)

    // Rough-Coordinate Transformation of the acceleration and angular velocity data
    // of calibrationData and imuData.
    calibAccelColumns = roughTransformAccelData(calibAccelColumns, orientation)
    for (i in calibAccelColumns.indices) {
        calibrationData[i]["accel_x_mps2"] = calibAccelColumns[i][0].toString()
        calibrationData[i]["accel_y_mps2"] = calibAccelColumns[i][1].toString()
        calibrationData[i]["accel_z_mps2"] = calibAccelColumns[i][2].toString()
    }
    calibAngVelocityColumns = roughTransformAngVelocityData(calibAngVelocityColumns, orientation)
    for (i in calibAngVelocityColumns.indices) {
        calibrationData[i]["angvelocity_x_radps"] = calibAngVelocityColumns[i][0].toString()
        calibrationData[i]["angvelocity_y_radps"] = calibAngVelocityColumns[i][1].toString()
        calibrationData[i]["angvelocity_z_radps"] = calibAngVelocityColumns[i][2].toString()
    }

    // Use calibrationData to calculate the local z-axis vectors
    val zVectFromCalib: Array<SimpleMatrix> = getZVectFromCalib(getAccelColumns(calibrationData))
    val localZ: SimpleMatrix = zVectFromCalib[0]
    val rotationMatrix: SimpleMatrix = getZTransformationMatrix(localZ)

    val transformedAccelData = applyRotationSensorData(rotationMatrix, calibAccelColumns)
    for (i in transformedAccelData.indices) {
        calibrationData[i]["accel_x_mps2"] = transformedAccelData[i][0].toString()
        calibrationData[i]["accel_y_mps2"] = transformedAccelData[i][1].toString()
        calibrationData[i]["accel_z_mps2"] = transformedAccelData[i][2].toString()
    }
    val transformedAngVelocityData = applyRotationSensorData(rotationMatrix, calibAngVelocityColumns)
    for (i in transformedAngVelocityData.indices) {
        calibrationData[i]["angvelocity_x_radps"] = transformedAngVelocityData[i][0].toString()
        calibrationData[i]["angvelocity_y_radps"] = transformedAngVelocityData[i][1].toString()
        calibrationData[i]["angvelocity_z_radps"] = transformedAngVelocityData[i][2].toString()
    }

    // Return the orientation, rotationMatrix and the transformed calibrationData
    return Triple(orientation, rotationMatrix, calibrationData)
}

fun transformIMUData(
    accelData: Array<DoubleArray>,
    angVelocityData: Array<DoubleArray>,
    orientation: Orientation,
    rotationMatrix: SimpleMatrix,
): Pair<Array<DoubleArray>, Array<DoubleArray>> {
    var transformedAccelData = roughTransformAccelData(accelData, orientation)
    var transformedAngVelocityData = roughTransformAngVelocityData(angVelocityData, orientation)

    transformedAccelData = applyRotationSensorData(rotationMatrix, transformedAccelData)
    transformedAngVelocityData = applyRotationSensorData(rotationMatrix, transformedAngVelocityData)

    return transformedAccelData to transformedAngVelocityData
}

fun applyTransformations(
    rawData: List<HashMap<String, String>>,
    orientation: Orientation,
    rotationMatrix: SimpleMatrix,
): List<HashMap<String, String>> {
    val accelColumns = getAccelColumns(rawData)
    val angVelocityColumns = getAngVelocityColumns(rawData)

    var transformedAccelData = roughTransformAccelData(accelColumns, orientation)
    var transformedAngVelocityData = roughTransformAngVelocityData(angVelocityColumns, orientation)

    transformedAccelData = applyRotationSensorData(rotationMatrix, transformedAccelData)
    transformedAngVelocityData = applyRotationSensorData(rotationMatrix, transformedAngVelocityData)

    for (i in transformedAccelData.indices) {
        rawData[i]["accel_x_mps2"] = transformedAccelData[i][0].toString()
        rawData[i]["accel_y_mps2"] = transformedAccelData[i][1].toString()
        rawData[i]["accel_z_mps2"] = transformedAccelData[i][2].toString()
    }
    for (i in transformedAngVelocityData.indices) {
        rawData[i]["angvelocity_x_radps"] = transformedAngVelocityData[i][0].toString()
        rawData[i]["angvelocity_y_radps"] = transformedAngVelocityData[i][1].toString()
        rawData[i]["angvelocity_z_radps"] = transformedAngVelocityData[i][2].toString()
    }

    return rawData
}
