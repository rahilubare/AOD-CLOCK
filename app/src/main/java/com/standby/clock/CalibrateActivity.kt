package com.standby.clock

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.abs

/** Teaches the app the exact pose of your stand. Works for any phone, tablet or angle. */
class CalibrateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val accent = Palettes[UiState.palette.coerceIn(0, Palettes.lastIndex)].light
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black, primary = accent)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) { CalibrateScreen(accent) { finish() } }
            }
        }
    }
}

@Composable
private fun CalibrateScreen(accent: Color, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val g = remember { FloatArray(3) }
    var tick by remember { mutableIntStateOf(0) }
    var motion by remember { mutableFloatStateOf(0f) }
    var saved by remember { mutableStateOf(Prefs.calibration(ctx)) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var first = true
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (first) { for (k in 0..2) g[k] = e.values[k]; first = false; return }
                var m = 0f
                for (k in 0..2) { g[k] = 0.8f * g[k] + 0.2f * e.values[k]; m += abs(e.values[k] - g[k]) }
                motion = motion * 0.8f + m * 0.2f
                tick++
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }

    val t = tick
    val sens = Prefs.sensitivity(ctx)
    val detected = Orientation.isLandscapePose(g, saved, sens)
    val angle = saved?.let { Orientation.angleDeg(g, it) }

    Column(
        Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Calibrate detection", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
        Text(
            "Put the phone on its stand exactly how you use it and keep it still. Then tap Save position. " +
                "The clock will open whenever the phone is held at that angle, in either landscape direction.",
            color = Color(0xFF8E8E93), fontSize = 16.sp,
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1C1C1E)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(if (detected) "In position" else "Not in position", color = if (detected) accent else Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (angle != null) "Angle from saved position: " + angle.toInt() + " degrees" else "Using the default detection (no saved position)",
                color = Color(0xFF8E8E93), fontSize = 15.sp,
            )
            Text(if (motion < 0.35f) "Steady" else "Moving, hold still to save", color = Color(0xFF8E8E93), fontSize = 15.sp)
        }
        Button(
            onClick = {
                if (motion < 0.35f) {
                    val v = g.copyOf()
                    Prefs.setCalibration(ctx, v)
                    saved = v
                    message = "Saved"
                    if (Prefs.autoEnabled(ctx)) ContextCompat.startForegroundService(ctx, Intent(ctx, OrientationService::class.java))
                } else message = "Hold the phone still first"
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save position") }
        if (message.isNotEmpty()) Text(message, color = accent, fontSize = 15.sp)
        OutlinedButton(
            onClick = {
                Prefs.setCalibration(ctx, null)
                saved = null
                message = "Back to default detection"
                if (Prefs.autoEnabled(ctx)) ContextCompat.startForegroundService(ctx, Intent(ctx, OrientationService::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Use default detection") }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
