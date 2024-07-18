package edu.gatech.ce.allgather.utils

import org.apache.commons.math3.geometry.euclidean.threed.Rotation
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.ejml.simple.SimpleMatrix
import kotlin.math.acos
import kotlin.math.min

/**
 * Get local z-axis vectors from calibration data.
 *
 * @param accelData A 2D array containing the calibration data with only the columns as accel_x_mps2, accel_y_mps2, accel_z_mps2.
 * @return A tuple (represented as an array of SimpleMatrix objects) containing the local z-axis vector and the unit gravity measurements.
 */
fun getZVectFromCalib(accelData: Array<DoubleArray>): Array<SimpleMatrix> {
    // Create a copy of calibrationData as gMeasMatrix
    val numRows = accelData.size
    val numCols = accelData[0].size
    val gMeas = Array(numRows) {DoubleArray(numCols)}
    for (i in 0 until numRows) {
        gMeas[i] = accelData[i].clone()
    }

    val gMeasMatrix = SimpleMatrix(gMeas)
    val unitGMeasMatrix = SimpleMatrix(numRows, numCols)

    for (row in 0 until numRows) {
        val norm: Double = gMeasMatrix.extractVector(true, row).normF()
        for (col in 0 until numCols) {
            unitGMeasMatrix.set(row, col, gMeasMatrix.get(row, col) / norm)
        }
    }

    val localZSum = SimpleMatrix(1, numCols)
    for (col in 0 until numCols) {
        localZSum.set(col, unitGMeasMatrix.extractVector(false, col).elementSum())
    }

    val normLocalZSum: Double = localZSum.normF()
    val localZ: SimpleMatrix = localZSum.divide(normLocalZSum)

    return arrayOf(localZ, unitGMeasMatrix)
}

/**
 * Get local z-axis vector from IMU data based on acceleration measurements.
 *
 * @param accelData A 2D array containing the calibration data with only the columns as accel_x_mps2, accel_y_mps2, accel_z_mps2.
 * @param windowSize The number of rows to consider from the start of the imuData for averaging. Defaults to 40 if not specified.
 * @return A SimpleMatrix representing the local z-axis vector.
 */
fun getZVectFromGrav(
    accelData: Array<DoubleArray>,
    windowSize: Int = 40
): SimpleMatrix {
    var windowSize = min(windowSize, accelData.size)

    // Convert the relevant slices of imuData into a SimpleMatrix
    var accelMatrix = SimpleMatrix(accelData)
    val numRows: Int = accelMatrix.numRows()
    val numCols: Int = accelMatrix.numCols()
    accelMatrix = accelMatrix.rows(0, windowSize)

    val averages = SimpleMatrix(1, numCols)
    for (col in 0 until numCols) {
        averages.set(col, accelMatrix.extractVector(false, col).elementSum() / windowSize)
    }

    // Normalize the average vector to get the local z-axis
    val norm: Double = averages.normF()
    return averages.divide(norm)
}

/**
 * Calculates the rotation matrix needed to align the local z-axis with the global z-axis.
 *
 * @param localZ A SimpleMatrix representing the local z-axis (from the smartphone).
 * @return A SimpleMatrix representing the rotation matrix to align the smartphone's z-axis with the vehicle's z-axis.
 */
fun getZTransformationMatrix(localZ: SimpleMatrix): SimpleMatrix {
    // Normalize the local z-axis vector
    val normLocalZ: Double = localZ.normF()
    val unitLocalZ: SimpleMatrix = localZ.divide(normLocalZ)

    // Define the target global z-axis vector (0, 0, 1)
    val unitNewZ = SimpleMatrix(
        arrayOf<DoubleArray>(
            doubleArrayOf(0.0),
            doubleArrayOf(0.0),
            doubleArrayOf(1.0)
        )
    )

    // Calculate the cross product and angle for the rotation
    val dotProduct: Double = unitLocalZ.dot(unitNewZ)
    val angle = acos(dotProduct)
    var axis: Vector3D = Vector3D.crossProduct(
        Vector3D(unitLocalZ.get(0), unitLocalZ.get(1), unitLocalZ.get(2)),
        Vector3D(unitNewZ.get(0), unitNewZ.get(1), unitNewZ.get(2))
    )
    if (axis.getNorm() == 0.0) {
        axis = Vector3D(1.0, 0.0, 0.0)
    }
    axis = axis.normalize()

    // Create a rotation using the axis and angle.
    val rotation = Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR)

    return SimpleMatrix(rotation.getMatrix())
}
