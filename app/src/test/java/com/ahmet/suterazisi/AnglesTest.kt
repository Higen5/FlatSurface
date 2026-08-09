package com.ahmet.suterazisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnglesTest {

    @Test
    fun `angleDelta kisa yoldan gider`() {
        assertEquals(2f, angleDelta(359f, 1f), 0.001f)
        assertEquals(-2f, angleDelta(1f, 359f), 0.001f)
        assertEquals(90f, angleDelta(0f, 90f), 0.001f)
    }

    @Test
    fun `smoothAngle 360 sinirinda geri sarmaz`() {
        // 359 -> 1 arasi ileri 2 derece; alpha 0.5 ile tam ortasi = 0 derece
        assertEquals(0f, smoothAngle(359f, 1f, 0.5f), 0.001f)
        // sonuc her zaman 0..360 araliginda
        val r = smoothAngle(1f, 359f, 0.5f)
        assertTrue("0..360 disinda: $r", r in 0f..360f)
        assertEquals(0f, r, 0.001f)
    }

    @Test
    fun `isLevel tolerans sinirinda`() {
        assertTrue(isLevel(0.4f, -0.3f))
        assertFalse(isLevel(0.6f, 0f))
        assertFalse(isLevel(0f, -0.6f))
    }

    @Test
    fun `headingLabel sekiz yon`() {
        assertEquals("K", headingLabel(0f))
        assertEquals("K", headingLabel(350f))
        assertEquals("KD", headingLabel(45f))
        assertEquals("D", headingLabel(90f))
        assertEquals("G", headingLabel(180f))
        assertEquals("B", headingLabel(270f))
    }
}
