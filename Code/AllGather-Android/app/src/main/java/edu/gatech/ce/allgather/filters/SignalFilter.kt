package edu.gatech.ce.allgather.filters

import java.util.ArrayDeque
import java.util.Deque

/**
 * Created by bsomu3 on 2/12/2024.
 */
interface SignalFilter {
    var dataHistory: Deque<Double>
    var filteredDataHistory: Deque<Double>
    fun apply(data: Double): Double
    fun reset()
}