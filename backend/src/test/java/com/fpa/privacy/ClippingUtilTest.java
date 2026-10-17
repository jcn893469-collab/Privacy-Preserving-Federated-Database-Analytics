package com.fpa.privacy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClippingUtilTest {

    @Test
    void testClipValueWithinBounds() {
        assertEquals(50.0, ClippingUtil.clip(50.0, 10.0, 100.0), 1e-6);
    }

    @Test
    void testClipValueBelowLower() {
        assertEquals(10.0, ClippingUtil.clip(5.0, 10.0, 100.0), 1e-6);
    }

    @Test
    void testClipValueAboveUpper() {
        assertEquals(100.0, ClippingUtil.clip(150.0, 10.0, 100.0), 1e-6);
    }

    @Test
    void testClipAllList() {
        List<Double> raw = List.of(-5.0, 20.0, 50.0, 120.0);
        double[] clipped = ClippingUtil.clipAll(raw, 0.0, 100.0);

        assertEquals(4, clipped.length);
        assertEquals(0.0, clipped[0], 1e-6);
        assertEquals(20.0, clipped[1], 1e-6);
        assertEquals(50.0, clipped[2], 1e-6);
        assertEquals(100.0, clipped[3], 1e-6);
    }

    @Test
    void testSumSensitivity() {
        assertEquals(90.0, ClippingUtil.sumSensitivity(10.0, 100.0), 1e-6);
        assertEquals(50.0, ClippingUtil.sumSensitivity(-25.0, 25.0), 1e-6);
    }

    @Test
    void testInvalidBoundsThrow() {
        assertThrows(IllegalArgumentException.class, () -> ClippingUtil.clip(50.0, 100.0, 10.0));
    }
}
