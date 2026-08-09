package com.ahmet.suterazisi

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Üç bileşenli vektör.
 *
 * Cihaz koordinatları: x ekranın sağı, y ekranın üstü, z ekrandan dışarı (kullanıcıya doğru).
 * Dünya koordinatları: x doğu, y kuzey, z yukarı.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-6f) Vec3(0f, 0f, 1f) else this * (1f / l)
    }
}

fun cross(a: Vec3, b: Vec3) = Vec3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x
)

/**
 * Dünya koordinatlarındaki bir noktayı cihaz koordinatlarına çevirir.
 *
 * east/north/up, Android rotasyon matrisinin üç satırıdır: her biri bir dünya ekseninin
 * cihaz uzayındaki karşılığını verir. Bu yüzden dönüşüm basit bir doğrusal birleşimdir.
 */
fun worldToDevice(east: Vec3, north: Vec3, up: Vec3, e: Float, n: Float, u: Float): Vec3 =
    east * e + north * n + up * u

/**
 * Perspektif izdüşümü. Kamera (0, 0, eyeDist) noktasında durur ve -z yönüne bakar.
 * Dönüş değeri ekran merkezine göre (sağa, yukarı) kaymadır.
 */
fun project(p: Vec3, eyeDist: Float, focal: Float): Pair<Float, Float> {
    val denom = (eyeDist - p.z).coerceAtLeast(0.05f)
    return (focal * p.x / denom) to (focal * p.y / denom)
}

/**
 * Kabarcığın kubbe iç yüzeyindeki birim yönü.
 *
 * Kabarcık daima yükselen tarafa, yani dünya yukarısına gider; bu yön cihaz uzayında
 * doğrudan [up] vektörüdür, dolayısıyla işaret tahmini gerekmez.
 *
 * gain gerçek eğimi görsel olarak büyütür (gerçek vial'ın büyük eğrilik yarıçapının
 * karşılığı), maxTilt ise kabarcığın kubbe kenarına dayandığı sınırdır (radyan).
 */
fun bubbleDir(up: Vec3, gain: Float = 3f, maxTilt: Float = 1.15f): Vec3 {
    val u = up.normalized()
    val horiz = hypot(u.x, u.y)
    if (horiz < 1e-5f) return Vec3(0f, 0f, 1f)
    val t = min(atan2(horiz, u.z) * gain, maxTilt)
    return Vec3(u.x / horiz * sin(t), u.y / horiz * sin(t), cos(t))
}

/**
 * Su yüzeyi: [normal] yönüne dik düzlemin, merkezi başlangıçta olan [sphereR] yarıçaplı
 * küreyi kestiği çember. Dönüş, çemberin merkezi ve yarıçapıdır.
 *
 * fill, düzlemin küre merkezinden normal yönünde ne kadar yukarıda durduğunun
 * yarıçapa oranıdır; büyüdükçe kabarcık boşluğu küçülür.
 */
fun surfaceCircle(normal: Vec3, sphereR: Float, fill: Float): Pair<Vec3, Float> {
    val h = sphereR * fill
    val r = sqrt((sphereR * sphereR - h * h).coerceAtLeast(0f))
    return (normal.normalized() * h) to r
}

/** [n] vektörüne dik, birbirine de dik iki birim vektör üretir. */
fun basisFor(n: Vec3): Pair<Vec3, Vec3> {
    val nn = n.normalized()
    val seed = if (abs(nn.z) < 0.9f) Vec3(0f, 0f, 1f) else Vec3(1f, 0f, 0f)
    val u = cross(seed, nn).normalized()
    return u to cross(nn, u).normalized()
}

/**
 * Vektör için alçak geçiren filtre.
 *
 * ponytail: üç taban vektörü ayrı ayrı yumuşatılıp normalize ediliyor, aralarındaki
 * diklik hızlı dönüşlerde bir miktar bozulabilir. Görünür bir bozulma olursa
 * kuaterniyon slerp'e geçilir.
 */
fun lerpVec(prev: Vec3, next: Vec3, alpha: Float = 0.15f): Vec3 = Vec3(
    smooth(prev.x, next.x, alpha),
    smooth(prev.y, next.y, alpha),
    smooth(prev.z, next.z, alpha)
).normalized()
