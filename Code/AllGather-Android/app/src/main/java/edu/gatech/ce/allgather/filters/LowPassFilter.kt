package edu.gatech.ce.allgather.filters

import java.util.ArrayDeque
import java.util.Deque

/**
 * Created by bsomu3 on 2/12/2024.
 */
class LowPassFilter(private val alpha: Double) : SignalFilter {

    private var NUM_HISTORY_DATA = 3
    override var dataHistory: Deque<Double> = ArrayDeque(NUM_HISTORY_DATA)
    override var filteredDataHistory: Deque<Double> = ArrayDeque(NUM_HISTORY_DATA)

    override fun apply(data: Double) : Double {
        if (dataHistory.isEmpty()) {
            dataHistory.add(data)
            filteredDataHistory.add(data)
            return data
        }

        val previousData: Double = dataHistory.peekLast()!!
        val previousFilteredData: Double = filteredDataHistory.peekLast()!!
        val filteredData = (alpha * data) + (alpha * previousData) + ((1 - 2 * alpha) * previousFilteredData)

        if (dataHistory.size == NUM_HISTORY_DATA) {
            dataHistory.pollFirst()
            filteredDataHistory.pollFirst()
        }
        dataHistory.add(data)
        filteredDataHistory.add(filteredData)

        return filteredDataHistory.peekLast()!!
    }

    override fun reset() {
        dataHistory.clear()
        filteredDataHistory.clear()
    }
}
