package edu.gatech.ce.allgather

import edu.gatech.ce.allgather.filters.LowPassFilter
import edu.gatech.ce.allgather.filters.SignalFilter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStreamReader
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.ArrayList

@RunWith(Parameterized::class)
class LowPassFilterTest(private val fileName: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun fileNames(): Collection<Array<Any>> {
            return listOf(
                arrayOf("lowpassFilterTestData/test_lowpass_data1.csv"),
                arrayOf("lowpassFilterTestData/test_lowpass_data2.csv"),
                arrayOf("lowpassFilterTestData/test_lowpass_data3.csv"),
                arrayOf("lowpassFilterTestData/test_lowpass_data4.csv"),
                arrayOf("lowpassFilterTestData/test_lowpass_data5.csv"),
            )
        }
    }

    private fun roundToTenDecimalPlaces(value: Double): Double {
        return Math.round(value * 10000000000.0) / 10000000000.0
    }

    @Test
    @Throws(Exception::class)
    fun FilterAsPerExpectationsRandomInputsTest() {
        // Use getClassLoader() if you're calling from a static context, use getClass().getClassLoader() otherwise
        val inpStream = javaClass.getClassLoader()?.getResourceAsStream(fileName)
        assertNotNull("Asset file not found", inpStream)

        val filter: SignalFilter = LowPassFilter(0.1)
        val results: MutableList<Double> = ArrayList()

        // Lists to hold the columns
        val data: MutableList<Double> = ArrayList()
        val expectedResults: MutableList<Double> = ArrayList()
        InputStreamReader(inpStream).use { reader ->
            CSVParser(reader, CSVFormat.DEFAULT).use { csvParser ->
                var linesRead = 0
                for (record in csvParser) {
                    if (linesRead == 0) {
                        linesRead++
                        continue
                    }
                    data.add(record[0].toDouble())
                    expectedResults.add(roundToTenDecimalPlaces(record[1].toDouble()))
                    linesRead++
                }
            }
        }

        for (i in data.indices) {
            val result: Double = roundToTenDecimalPlaces(filter.apply(data[i]))
            results.add(result)
        }

        assertEquals("Not matching", results, expectedResults)
    }

    @Test
    @Throws(Exception::class)
    fun TestSingleDataPoint() {
        val filter: SignalFilter = LowPassFilter(0.1)
        val obtainedData: Double = filter.apply(1.23)
        println("obtainedData: $obtainedData")
        assertEquals("Not matching for single data point", 1.23, obtainedData, 0.0001)
    }
}