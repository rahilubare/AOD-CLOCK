package com.standby.clock

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

enum class DeviceKind { PHONE, WATCH, HEADPHONES, CASE, OTHER }

class DeviceCharge(
    val id: String,
    val name: String,
    val pct: Int,
    val charging: Boolean,
    val kind: DeviceKind,
)

private val RingGreen = Color(0xFF30D158)
private val RingTrack = Color(0xFF2C2C2E)

fun hasBluetoothPermission(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 31)
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    else true

@Composable
fun rememberDeviceCharges(): androidx.compose.runtime.State<List<DeviceCharge>> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(readDevices(ctx)) }
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state.value = readDevices(ctx) }
        }
        ContextCompat.registerReceiver(ctx, r, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(r) } catch (_: Exception) { } }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            state.value = readDevices(ctx)
        }
    }
    return state
}

@SuppressLint("MissingPermission")
private fun readDevices(ctx: Context): List<DeviceCharge> {
    val phone = readBattery(ctx)
    val out = mutableListOf(
        DeviceCharge("phone", "Phone", phone.pct, phone.charging, DeviceKind.PHONE),
    )
    if (!hasBluetoothPermission(ctx)) return out
    val mgr = ctx.getSystemService(BluetoothManager::class.java) ?: return out
    val adapter = mgr.adapter ?: return out
    val bonded = try { adapter.bondedDevices.orEmpty() } catch (_: SecurityException) { emptySet() }
    for (d in bonded) {
        val level = bluetoothBattery(d)
        if (level < 0) continue
        val name = try { d.name?.ifBlank { null } } catch (_: SecurityException) { null } ?: "Device"
        out.add(DeviceCharge(d.address, shortName(name), level, false, kindOf(d, name)))
    }
    return out.distinctBy { it.id }.take(4)
}

private fun shortName(name: String): String {
    val n = name.trim()
    return if (n.length <= 14) n else n.take(13) + "…"
}

@SuppressLint("MissingPermission")
private fun kindOf(device: BluetoothDevice, name: String): DeviceKind {
    val n = name.lowercase()
    if (n.contains("watch") || n.contains("band") || n.contains("galaxy watch") || n.contains("pixel watch"))
        return DeviceKind.WATCH
    if (n.contains("case") || n.contains("charging case")) return DeviceKind.CASE
    if (n.contains("bud") || n.contains("pod") || n.contains("headphone") || n.contains("headset") || n.contains("airpod") || n.contains("ear"))
        return DeviceKind.HEADPHONES
    val major = try { device.bluetoothClass?.majorDeviceClass } catch (_: Exception) { null }
    return when (major) {
        android.bluetooth.BluetoothClass.Device.Major.WEARABLE -> DeviceKind.WATCH
        android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceKind.HEADPHONES
        else -> DeviceKind.OTHER
    }
}

private fun bluetoothBattery(device: BluetoothDevice): Int {
    return try {
        val m = BluetoothDevice::class.java.getMethod("getBatteryLevel")
        (m.invoke(device) as? Int) ?: -1
    } catch (_: Exception) {
        -1
    }
}

@Composable
fun DevicesBatteryWidget(palette: Palette, modifier: Modifier = Modifier.fillMaxSize()) {
    val devices by rememberDeviceCharges()
    val ctx = LocalContext.current
    val btOk = hasBluetoothPermission(ctx)
    BoxWithConstraints(modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val maxH = constraints.maxHeight
        if (devices.size == 1) {
            DeviceRing(devices[0], palette, Modifier.fillMaxSize(0.92f), bigPercent = true)
        } else {
            val rows = devices.chunked(2)
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                rows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { dev ->
                            DeviceRing(dev, palette, Modifier.weight(1f).padding(6.dp), bigPercent = false)
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
                if (!btOk) {
                    Text(
                        "Allow Bluetooth to show earbuds and watches",
                        color = Color(0xFF8E8E93),
                        fontSize = with(LocalDensity.current) { (maxH * 0.045f).toSp() },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRing(device: DeviceCharge, palette: Palette, modifier: Modifier, bigPercent: Boolean) {
    val ring = if (device.pct <= 20) Color(0xFFFF453A) else RingGreen
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(constraints.maxWidth, constraints.maxHeight)
            val d = LocalDensity.current
            Box(Modifier.size(with(d) { side.toDp() }), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val s = size.minDimension
                    val stroke = s * 0.09f
                    val inset = stroke / 2f + s * 0.03f
                    val tl = Offset(inset, inset)
                    val sz = Size(s - inset * 2, s - inset * 2)
                    val style = Stroke(width = stroke, cap = StrokeCap.Round)
                    drawArc(RingTrack, -90f, 360f, false, tl, sz, style = style)
                    val sweep = 360f * (device.pct.coerceIn(0, 100) / 100f)
                    if (sweep > 0.5f) drawArc(ring, -90f, sweep, false, tl, sz, style = style)
                    if (device.charging) drawLightning(center, s * 0.07f, Color.White, Offset(0f, -s * 0.40f))
                    drawDeviceIcon(device.kind, Offset(center.x, center.y - s * 0.12f), s * 0.13f, Color.White)
                }
                Text(
                    "${device.pct}%",
                    color = Color.White,
                    fontSize = with(d) { (side * if (bigPercent) 0.20f else 0.16f).toSp() },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).padding(top = with(d) { (side * 0.22f).toDp() }),
                )
            }
        }
        Text(
            device.name,
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

private fun DrawScope.drawLightning(origin: Offset, r: Float, color: Color, extra: Offset) {
    val c = origin + extra
    val path = Path().apply {
        moveTo(c.x + r * 0.15f, c.y - r)
        lineTo(c.x - r * 0.4f, c.y + r * 0.05f)
        lineTo(c.x + r * 0.08f, c.y + r * 0.05f)
        lineTo(c.x - r * 0.15f, c.y + r)
        lineTo(c.x + r * 0.4f, c.y - r * 0.05f)
        lineTo(c.x - r * 0.08f, c.y - r * 0.05f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawDeviceIcon(kind: DeviceKind, c: Offset, s: Float, color: Color) {
    val stroke = Stroke(width = s * 0.14f, cap = StrokeCap.Round)
    when (kind) {
        DeviceKind.PHONE -> {
            drawRoundRect(
                color,
                Offset(c.x - s * 0.38f, c.y - s * 0.72f),
                Size(s * 0.76f, s * 1.44f),
                CornerRadius(s * 0.16f, s * 0.16f),
                style = stroke,
            )
        }
        DeviceKind.WATCH -> {
            drawRoundRect(
                color,
                Offset(c.x - s * 0.42f, c.y - s * 0.50f),
                Size(s * 0.84f, s * 1.00f),
                CornerRadius(s * 0.22f, s * 0.22f),
                style = stroke,
            )
            drawLine(color, Offset(c.x - s * 0.18f, c.y - s * 0.50f), Offset(c.x - s * 0.18f, c.y - s * 0.78f), s * 0.14f, StrokeCap.Round)
            drawLine(color, Offset(c.x + s * 0.18f, c.y - s * 0.50f), Offset(c.x + s * 0.18f, c.y - s * 0.78f), s * 0.14f, StrokeCap.Round)
            drawLine(color, Offset(c.x - s * 0.18f, c.y + s * 0.50f), Offset(c.x - s * 0.18f, c.y + s * 0.78f), s * 0.14f, StrokeCap.Round)
            drawLine(color, Offset(c.x + s * 0.18f, c.y + s * 0.50f), Offset(c.x + s * 0.18f, c.y + s * 0.78f), s * 0.14f, StrokeCap.Round)
        }
        DeviceKind.HEADPHONES -> {
            drawArc(color, 200f, 140f, false, Offset(c.x - s * 0.7f, c.y - s * 0.7f), Size(s * 1.4f, s * 1.4f), style = stroke)
            drawCircle(color, s * 0.22f, Offset(c.x - s * 0.62f, c.y + s * 0.15f))
            drawCircle(color, s * 0.22f, Offset(c.x + s * 0.62f, c.y + s * 0.15f))
        }
        DeviceKind.CASE -> {
            drawRoundRect(
                color,
                Offset(c.x - s * 0.70f, c.y - s * 0.42f),
                Size(s * 1.40f, s * 0.84f),
                CornerRadius(s * 0.28f, s * 0.28f),
                style = stroke,
            )
            drawLine(color, Offset(c.x - s * 0.20f, c.y), Offset(c.x + s * 0.20f, c.y), s * 0.12f, StrokeCap.Round)
        }
        DeviceKind.OTHER -> {
            drawCircle(color, s * 0.55f, c, style = stroke)
        }
    }
}
