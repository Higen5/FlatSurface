package com.ahmet.suterazisi

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensors: SensorManager
    private var rotationSensor: Sensor? = null

    private var azimuth by mutableFloatStateOf(0f)
    private var pitch by mutableFloatStateOf(0f)
    private var roll by mutableFloatStateOf(0f)

    // Dünya eksenlerinin cihaz uzayındaki karşılığı: rotasyon matrisinin üç satırı.
    private var east by mutableStateOf(Vec3(1f, 0f, 0f))
    private var north by mutableStateOf(Vec3(0f, 1f, 0f))
    private var up by mutableStateOf(Vec3(0f, 0f, 1f))

    private val rm = FloatArray(9)
    private val orient = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sensors = getSystemService(SensorManager::class.java)
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06080B))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (rotationSensor == null) {
                    Text(
                        "Bu cihazda gerekli sensör yok",
                        color = Color(0xFFFF6B6B),
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        "${azimuth.roundToInt()}°  ${headingLabel(azimuth)}",
                        color = Color(0xFFD9B863),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Dial(
                        east = east,
                        north = north,
                        up = up,
                        level = isLevel(pitch, roll),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(vertical = 20.dp)
                    )
                    Text(
                        "X %.1f°   Y %.1f°".format(roll, pitch),
                        color = if (isLevel(pitch, roll)) Color(0xFF3ED87F) else Color(0xFF9AA6B2),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensors.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rm, event.values)
        east = lerpVec(east, Vec3(rm[0], rm[1], rm[2]))
        north = lerpVec(north, Vec3(rm[3], rm[4], rm[5]))
        up = lerpVec(up, Vec3(rm[6], rm[7], rm[8]))
        SensorManager.getOrientation(rm, orient)
        azimuth = smoothAngle(azimuth, Math.toDegrees(orient[0].toDouble()).toFloat())
        pitch = smooth(pitch, Math.toDegrees(orient[1].toDouble()).toFloat())
        roll = smooth(roll, Math.toDegrees(orient[2].toDouble()).toFloat())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
