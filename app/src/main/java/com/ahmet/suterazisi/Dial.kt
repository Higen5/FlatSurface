package com.ahmet.suterazisi

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFD9B863)
private val NorthRed = Color(0xFFFF6B6B)
private val WaterDeep = Color(0xFF0E3B57)
private val WaterTop = Color(0xFF2E86B8)
private val LevelGreen = Color(0xFF3ED87F)
private val BubblePale = Color(0xFFDCF2FF)

// Sahne birimleri: kubbe yaricapi 1.
private const val DOME_R = 1f
private const val RING_R = 1.78f
private const val EYE = 5.5f
private const val BUBBLE_R = 0.26f
// Su duzleminin kure merkezinden yuksekligi. Buyudukce kesit cemberi kuculur ve
// kubbenin cam govdesi yuzeyin cevresinde gorunur kalir; derinlik hissi bundan gelir.
private const val WATER_FILL = 0.72f

/**
 * 3B sahne: dunyaya sabit pusula kadrani, cihaza sabit cam kubbe, dunya yatayinda su yuzeyi.
 *
 * east/north/up, Android rotasyon matrisinin satirlaridir (dunya eksenlerinin cihaz
 * uzayindaki karsiligi). Kadran dunya duzleminde durdugu icin telefon egildikce
 * perspektifte elipse doner; kubbe cihaza bagli oldugu icin daire kalir.
 */
@Composable
fun Dial(east: Vec3, north: Vec3, up: Vec3, level: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = minOf(size.width, size.height) / 2f - 6.dp.toPx()
        val focal = outer * EYE / RING_R

        fun screen(p: Vec3): Offset {
            val (x, y) = project(p, EYE, focal)
            return Offset(c.x + x, c.y - y)
        }

        fun world(e: Float, n: Float): Offset =
            screen(worldToDevice(east, north, up, e, n, 0f))

        // --- Pusula kadrani: dunya yatay duzleminde, kuzeye hizali ---
        val ringPath = Path()
        for (i in 0..120) {
            val t = i / 120f * 2f * PI.toFloat()
            val o = world(RING_R * sin(t), RING_R * cos(t))
            if (i == 0) ringPath.moveTo(o.x, o.y) else ringPath.lineTo(o.x, o.y)
        }
        drawPath(ringPath, Color(0xFF0C0F14).copy(alpha = 0.85f))
        drawPath(ringPath, Gold.copy(alpha = 0.35f), style = Stroke(1.5.dp.toPx()))

        for (deg in 0 until 360 step 15) {
            val major = deg % 45 == 0
            val a = deg / 180f * PI.toFloat()
            val inner = RING_R - if (major) 0.16f else 0.075f
            drawLine(
                color = if (major) Gold else Gold.copy(alpha = 0.35f),
                start = world(RING_R * sin(a), RING_R * cos(a)),
                end = world(inner * sin(a), inner * cos(a)),
                strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // Harfler dik cizilir: 3B sahnede yatirilmis yazi okunmaz hale gelirdi.
        listOf(0 to "K", 90 to "D", 180 to "G", 270 to "B").forEach { (deg, txt) ->
            val a = deg / 180f * PI.toFloat()
            val pos = world(1.44f * sin(a), 1.44f * cos(a))
            val layout = measurer.measure(
                txt,
                TextStyle(
                    color = if (deg == 0) NorthRed else Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                layout,
                topLeft = Offset(
                    pos.x - layout.size.width / 2f,
                    pos.y - layout.size.height / 2f
                )
            )
        }

        // --- Cam kubbe: cihaza sabit, hep daire ---
        val domePx = focal * DOME_R / EYE
        val domeClip = Path().apply {
            addOval(Rect(c.x - domePx, c.y - domePx, c.x + domePx, c.y + domePx))
        }
        drawCircle(Color(0xFF0A0D12), domePx, c)

        // --- Su: dunya yatayinda duzlem, kureyi keser ---
        val eff = bubbleDir(up)
        val (surfC, surfR) = surfaceCircle(eff, DOME_R * 0.985f, WATER_FILL)
        val (bu, bv) = basisFor(eff)

        val surfCenter = screen(surfC)

        clipPath(domeClip) {
            // Icbukey govde: kenara dogru koyulasan gradyan derinlik hissini verir.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WaterDeep, Color(0xFF04080C)),
                    center = c,
                    radius = domePx
                ),
                radius = domePx,
                center = c
            )
            val surface = Path()
            for (i in 0..72) {
                val t = i / 72f * 2f * PI.toFloat()
                val p = surfC + bu * (cos(t) * surfR) + bv * (sin(t) * surfR)
                val o = screen(p)
                if (i == 0) surface.moveTo(o.x, o.y) else surface.lineTo(o.x, o.y)
            }
            surface.close()
            drawPath(
                surface,
                Brush.radialGradient(
                    colors = listOf(WaterTop.copy(alpha = 0.85f), WaterTop.copy(alpha = 0.4f)),
                    center = Offset(surfCenter.x - domePx * 0.2f, surfCenter.y - domePx * 0.2f),
                    radius = domePx
                )
            )
            drawPath(surface, WaterTop.copy(alpha = 0.95f), style = Stroke(1.5.dp.toPx()))
        }

        // --- Kabarcik: su yuzeyinin tepesinde, kubbe ic yuzeyine oturur ---
        // Yorunge yaricapi DOME_R - BUBBLE_R: kabarcik kurenin icine tam tegettir,
        // camin disina tasmaz. Perspektifte kalan tasma clipPath ile kirpilir.
        val orbit = DOME_R - BUBBLE_R
        val bubblePos = eff * orbit
        val bp = screen(bubblePos)
        val br = focal * BUBBLE_R / (EYE - bubblePos.z)
        val tint = if (level) LevelGreen else BubblePale

        val topPos = Vec3(0f, 0f, orbit)
        val tp = screen(topPos)
        val tr = focal * BUBBLE_R / (EYE - topPos.z) + 4.dp.toPx()

        clipPath(domeClip) {
            // Hedef halkasi kubbenin tepesinde durur, kabarcik buna oturunca duzdur.
            drawCircle(tint.copy(alpha = 0.5f), tr, tp, style = Stroke(1.5.dp.toPx()))

            drawCircle(
                Color.Black.copy(alpha = 0.35f),
                br,
                Offset(bp.x + 2.dp.toPx(), bp.y + 3.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.95f), tint, tint.copy(alpha = 0.5f)),
                    center = Offset(bp.x - br * 0.35f, bp.y - br * 0.35f),
                    radius = br * 1.7f
                ),
                radius = br,
                center = bp
            )
            drawCircle(
                Color.White.copy(alpha = 0.9f),
                br * 0.16f,
                Offset(bp.x - br * 0.4f, bp.y - br * 0.42f)
            )
        }

        // --- Cam ustu: yansima ve kenar derinligi, en sona cizilir ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(c.x - domePx * 0.4f, c.y - domePx * 0.4f),
                radius = domePx * 1.05f
            ),
            radius = domePx,
            center = c
        )
        drawCircle(Color.Black.copy(alpha = 0.5f), domePx, c, style = Stroke(7.dp.toPx()))
        drawCircle(Gold.copy(alpha = 0.3f), domePx, c, style = Stroke(1.dp.toPx()))
    }
}
