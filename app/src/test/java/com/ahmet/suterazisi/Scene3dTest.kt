package com.ahmet.suterazisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class Scene3dTest {

    private val e = Vec3(1f, 0f, 0f)
    private val n = Vec3(0f, 1f, 0f)
    private val u = Vec3(0f, 0f, 1f)

    @Test
    fun `worldToDevice birim tabanda ayni koordinati verir`() {
        val p = worldToDevice(e, n, u, 2f, -3f, 0.5f)
        assertEquals(2f, p.x, 0.001f)
        assertEquals(-3f, p.y, 0.001f)
        assertEquals(0.5f, p.z, 0.001f)
    }

    @Test
    fun `project merkezi merkeze tasir ve olcekler`() {
        val (cx, cy) = project(Vec3(0f, 0f, 0f), 5f, 5f)
        assertEquals(0f, cx, 0.001f)
        assertEquals(0f, cy, 0.001f)

        // z=0 duzleminde: focal * x / eyeDist = 5 * 1 / 5 = 1
        val (x, y) = project(Vec3(1f, 0f, 0f), 5f, 5f)
        assertEquals(1f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test
    fun `project yakin nokta buyuk gorunur`() {
        val (uzak, _) = project(Vec3(1f, 0f, -1f), 5f, 5f)
        val (yakin, _) = project(Vec3(1f, 0f, 1f), 5f, 5f)
        assertTrue("yakin nokta daha buyuk olmali: $yakin vs $uzak", yakin > uzak)
    }

    @Test
    fun `bubbleDir duz tutulunca merkezde durur`() {
        val d = bubbleDir(Vec3(0f, 0f, 1f))
        assertEquals(0f, d.x, 0.001f)
        assertEquals(0f, d.y, 0.001f)
        assertEquals(1f, d.z, 0.001f)
    }

    @Test
    fun `bubbleDir yukselen tarafa gider`() {
        // dunya yukarisi cihazin +x tarafina yatmis: sag kenar kalkmis demektir
        val d = bubbleDir(Vec3(0.2f, 0f, 1f))
        assertTrue("kabarcik +x tarafina gitmeli, x=${d.x}", d.x > 0f)
        assertTrue("kabarcik tepe noktasindan ayrilmali, z=${d.z}", d.z < 1f)
        assertEquals("birim vektor olmali", 1f, d.length(), 0.001f)
    }

    @Test
    fun `bubbleDir maxTilt sinirini asmaz`() {
        val d = bubbleDir(Vec3(5f, 0f, 0.1f), gain = 3f, maxTilt = 1.0f)
        // en fazla 1.0 radyan yatabilir, yani z >= cos(1.0)
        assertTrue("z=${d.z} cos(1.0)=0.5403 altina inmemeli", d.z >= 0.5403f - 0.001f)
    }

    @Test
    fun `surfaceCircle kure kesitini dogru hesaplar`() {
        val (center, r) = surfaceCircle(Vec3(0f, 0f, 1f), sphereR = 1f, fill = 0.6f)
        assertEquals(0.6f, center.z, 0.001f)
        assertEquals(sqrt(1f - 0.36f), r, 0.001f)
    }

    @Test
    fun `basisFor dik vektorler uretir`() {
        val nrm = Vec3(0.3f, -0.5f, 0.8f).normalized()
        val (a, b) = basisFor(nrm)
        assertEquals("a birim olmali", 1f, a.length(), 0.001f)
        assertEquals("b birim olmali", 1f, b.length(), 0.001f)
        assertEquals("a . n = 0", 0f, a.x * nrm.x + a.y * nrm.y + a.z * nrm.z, 0.001f)
        assertEquals("b . n = 0", 0f, b.x * nrm.x + b.y * nrm.y + b.z * nrm.z, 0.001f)
        assertEquals("a . b = 0", 0f, a.x * b.x + a.y * b.y + a.z * b.z, 0.001f)
    }
}
