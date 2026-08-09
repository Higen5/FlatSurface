package com.ahmet.suterazisi

import kotlin.math.abs
import kotlin.math.hypot

/** İki açı arasındaki en kısa farkı -180..180 aralığında verir. */
fun angleDelta(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

/** Pusula açısı için alçak geçiren filtre. 360/0 sınırında geri sarmaz, sonuç 0..360. */
fun smoothAngle(prev: Float, next: Float, alpha: Float = 0.15f): Float {
    val r = prev + angleDelta(prev, next) * alpha
    return (r % 360f + 360f) % 360f
}

/** Eğim açıları için düz alçak geçiren filtre. */
fun smooth(prev: Float, next: Float, alpha: Float = 0.15f): Float = prev + (next - prev) * alpha

/**
 * Eğimi kubbe içindeki (x, y) piksel kaymasına çevirir.
 * y yukarı yönde pozitiftir; çizim tarafında ekran koordinatına çevrilir.
 * maxAngle: kabarcığın kubbe kenarına dayandığı eğim.
 */
fun bubbleOffset(
    pitchDeg: Float,
    rollDeg: Float,
    radius: Float,
    maxAngle: Float = 30f
): Pair<Float, Float> {
    var x = rollDeg / maxAngle * radius
    var y = pitchDeg / maxAngle * radius
    val d = hypot(x, y)
    if (d > radius) {
        x = x / d * radius
        y = y / d * radius
    }
    return x to y
}

fun isLevel(pitchDeg: Float, rollDeg: Float, tolerance: Float = 0.5f): Boolean =
    abs(pitchDeg) <= tolerance && abs(rollDeg) <= tolerance

/** 0..360 dereceyi sekiz yönlü Türkçe kısaltmaya çevirir. */
fun headingLabel(azimuthDeg: Float): String {
    val names = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
    val norm = (azimuthDeg % 360f + 360f) % 360f
    return names[((norm / 45f + 0.5f).toInt()) % 8]
}
