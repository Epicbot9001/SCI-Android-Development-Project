package edu.gatech.ce.allgather;

import static org.junit.Assert.assertEquals;

import static edu.gatech.ce.allgather.utils.IMUUtilsKt.calculateBBI;
import static edu.gatech.ce.allgather.utils.IMUUtilsKt.getSuperelevation;

import org.junit.Test;

public class BBIUtilsTest {
    @Test
    public void testCalculateBBI() {
        // Test positive BBI
        assertEquals(45, calculateBBI(1, 1), 0.001);
        assertEquals(30, calculateBBI(1, Math.sqrt(3)), 0.001);

        // Test negative BBI
        assertEquals(-45, calculateBBI(-1, 1), 0.001);
        assertEquals(-30, calculateBBI(-1, Math.sqrt(3)), 0.001);

        // Test zero BBI
        assertEquals(0, calculateBBI(0, 1), 0.001);
    }

    @Test
    public void testGetSuperelevation() {
        // Test left turns (metric units)
        assertEquals(0, getSuperelevation(9.80665, 1, 45, 0, "mps", "radps"), 0.001);
        assertEquals(100, getSuperelevation(9.80665, 1, 0, 0, "mps", "radps"), 0.001);

        // Test left turns (imperial units)
        assertEquals(0, getSuperelevation(9.80665 / 0.44704, 1, 45, 0, "mph", "radps"), 0.001);
        assertEquals(100, getSuperelevation(9.80665 / 0.44704, 1, 0, 0, "mph", "radps"), 0.001);

        // Test right turns (metric units)
        assertEquals(0, getSuperelevation(9.80665, -1, -45, 0, "mps", "radps"), 0.001);
        assertEquals(-100, getSuperelevation(9.80665, -1, 0, 0, "mps", "radps"), 0.001);

        // Test right turns (imperial units)
        assertEquals(0, getSuperelevation(9.80665 / 0.44704, -1, -45, 0, "mph", "radps"), 0.001);
        assertEquals(-100, getSuperelevation(9.80665 / 0.44704, -1, 0, 0, "mph", "radps"), 0.001);
    }
}
