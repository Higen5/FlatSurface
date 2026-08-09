package com.ahmet.suterazisi

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFD9B863)
private val North = Color(0xFFFF6B6B)
private val Glass = Color(0xFF11151C)
private val LevelGreen = Color(0xFF3ED87F)
private val BubbleBlue = Color(0xFF7BC0FF)

@Composable
fun Dial(azimuth: Float, pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = minOf(size.width, size.height) / 2f - 6.dp.toPx()
        val domeR = outer * 0.60f
        val bubbleR = 18.dp.toPx()
        val tint = if (isLevel(pitch, roll)) LevelGreen else BubbleBlue

        drawRing(c, outer, azimuth, measurer)
        drawDome(c, domeR)
        drawTarget(c, bubbleR + 5.dp.toPx(), tint)

        val (bx, by) = bubbleOffset(pitch, roll, domeR - bubbleR)
        // y yukari pozitif; ekran koordinatinda asagi pozitif oldugu icin isaret cevriliyor
        drawBubble(Offset(c.x + bx, c.y - by), bubbleR, tint)
    }
}

private fun DrawScope.drawRing(c: Offset, r: Float, azimuth: Float, m: TextMeasurer) {
    drawCircle(Color(0xFF0C0F14), r, c)
    drawCircle(Gold.copy(alpha = 0.30f), r, c, style = Stroke(1.5.dp.toPx()))

    rotate(-azimuth, c) {
        for (deg in 0 until 360 step 15) {
            val major = deg % 45 == 0
            val a = Math.toRadians(deg.toDouble() - 90.0)
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            val len = if (major) 13.dp.toPx() else 6.dp.toPx()
            drawLine(
                color = if (major) Gold else Gold.copy(alpha = 0.35f),
                start = Offset(c.x + ca * r, c.y + sa * r),
                end = Offset(c.x + ca * (r - len), c.y + sa * (r - len)),
                strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx()
            )
        }

        val labelR = r - 32.dp.toPx()
        listOf(0 to "K", 90 to "D", 180 to "G", 270 to "B").forEach { (deg, txt) ->
            val a = Math.toRadians(deg.toDouble() - 90.0)
            val layout = m.measure(
                txt,
                TextStyle(
                    color = if (deg == 0) North else Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                layout,
                topLeft = Offset(
                    c.x + cos(a).toFloat() * labelR - layout.size.width / 2f,
                    c.y + sin(a).toFloat() * labelR - layout.size.height / 2f
                )
            )
        }
    }
}

private fun DrawScope.drawDome(c: Offset, r: Float) {
    drawCircle(Glass, r, c)
    // sol ustten gelen isik: camin hacim hissi
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(c.x - r * 0.35f, c.y - r * 0.35f),
            radius = r * 1.1f
        ),
        radius = r,
        center = c
    )
    // ic golge halkasi: kubbe kenarinin derinligi
    drawCircle(Color.Black.copy(alpha = 0.55f), r, c, style = Stroke(6.dp.toPx()))
    drawCircle(Gold.copy(alpha = 0.25f), r, c, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawTarget(c: Offset, r: Float, tint: Color) {
    drawCircle(tint.copy(alpha = 0.55f), r, c, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawBubble(p: Offset, r: Float, tint: Color) {
    drawCircle(
        Color.Black.copy(alpha = 0.45f),
        r,
        Offset(p.x + 2.dp.toPx(), p.y + 3.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.90f), tint, tint.copy(alpha = 0.65f)),
            center = Offset(p.x - r * 0.35f, p.y - r * 0.35f),
            radius = r * 1.6f
        ),
        radius = r,
        center = p
    )
    // spekuler nokta: cam kabarcik hissinin tamamlayicisi
    drawCircle(
        Color.White.copy(alpha = 0.85f),
        r * 0.18f,
        Offset(p.x - r * 0.38f, p.y - r * 0.40f)
    )
}
