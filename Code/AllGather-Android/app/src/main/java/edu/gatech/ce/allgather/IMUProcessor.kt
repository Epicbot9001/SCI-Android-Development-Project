package edu.gatech.ce.allgather

import edu.gatech.ce.allgather.filters.SignalFilter
import edu.gatech.ce.allgather.utils.calculateBBI
import edu.gatech.ce.allgather.utils.getSuperelevation
import kotlin.math.atan
import kotlin.math.tan


fun processCrossSlope(speed: Double, accelerationData: DoubleArray, angVelocityData: DoubleArray, accelFilters: Array<SignalFilter>, angVelocityFilters: Array<SignalFilter>): Triple<Double, Double, Double> {
    val filteredAccelXMps2 = accelFilters[0].apply(accelerationData[0])
    val filteredAccelYMps2 = accelFilters[1].apply(accelerationData[1])
    val filteredAccelZMps2 = accelFilters[2].apply(accelerationData[2])
    val filteredAngVelocityXRadps = angVelocityFilters[0].apply(angVelocityData[0])
    val filteredAngVelocityYRadps = angVelocityFilters[1].apply(angVelocityData[1])
    val filteredAngVelocityZRadps = angVelocityFilters[2].apply(angVelocityData[2])

    // Compute BBI and superelevation with the new filtered values
    val bbi = calculateBBI(accelerationData[1], accelerationData[2])
    val bbiFiltered = calculateBBI(filteredAccelYMps2, filteredAccelZMps2)
    val superelevation = getSuperelevation(
        speed,
        filteredAngVelocityZRadps,
        bbiFiltered,
        0.0,
        "mps",
        "radps"
    )

    return Triple(bbi, bbiFiltered, superelevation)
}
