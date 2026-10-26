package com.standby.clock

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Card = Color(0xFF1C1C1E)
private val Hairline = Color(0xFF2C2C2E)
private val Muted = Color(0xFF8E8E93)

class MainActivity : ComponentActivity() {
    private val picker = registerPhotoPicker()
    private var tick by mutableIntStateOf(0)    // bumped on resume so status rows refresh
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiState.init(this)
        if (Prefs.autoEnabled(this)) startWatcher()
        setContent {
            val palette = Palettes[UiState.palette.coerceIn(0, Palettes.lastIndex)]
            val accent = palette.light
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black, primary = accent)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    val ctx = this@MainActivity
                    val t = tick
                    val canOverlay = remember(t) { Settings.canDrawOverlays(ctx) }
                    val notifOk = remember(t) {
                        Build.VERSION.SDK_INT < 33 ||
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    }
                    val batteryOk = remember(t) {
                        (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
                    }
                    var auto by remember { mutableStateOf(Prefs.autoEnabled(ctx)) }
                    var needCharging by remember { mutableStateOf(Prefs.needCharging(ctx)) }
                    val switchColors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.Black)

                    Column(
                        Modifier.safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text("StandBy Clock", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)

                        // live preview: tap to open the real thing
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(2.1f).clip(RoundedCornerShape(28.dp))
                                .background(Color.Black).clickable { startActivity(Intent(ctx, StandbyActivity::class.java)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            BigOverlapClock(palette.dark, palette.light, Modifier.fillMaxSize().padding(14.dp))
                            Text("Tap to open", color = Muted, fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp))
                        }

                        Group {
                            SettingRow(
                                "Start when placed on its side",
                                "Stand the phone in landscape and keep it still. The clock opens, even on the lock screen.",
                            ) {
                                Switch(checked = auto, colors = switchColors, onCheckedChange = { on ->
                                    if (on && !canOverlay) {
                                        openOverlaySettings()
                                    } else {
                                        auto = on
                                        Prefs.setAuto(ctx, on)
                                        if (on) {
                                            askNotificationPermission()
                                            startWatcher()
                                        } else stopService(Intent(ctx, OrientationService::class.java))
                                    }
                                })
                            }
                            Divider2()
                            SettingRow(
                                "Only while charging",
                                "Turn off to work on battery too. Watching with the screen locked then uses more battery.",
                            ) {
                                Switch(checked = needCharging, colors = switchColors, onCheckedChange = {
                                    needCharging = it
                                    Prefs.setNeedCharging(ctx, it)
                                    if (auto) startWatcher()
                                })
                            }
                        }

                        Heading("Allow these once")
                        Group {
                            PermissionRow("Display over other apps", "Lets the clock open by itself", canOverlay, accent) { openOverlaySettings() }
                            Divider2()
                            PermissionRow("Notifications", "Keeps the background watcher visible", notifOk, accent) { askNotificationPermission() }
                            Divider2()
                            PermissionRow("Unrestricted battery", "Stops the system from stopping the watcher", batteryOk, accent) { openBatterySettings() }
                        }

                        Heading("More")
                        Group {
                            SettingRow("Choose photos", "For the photo frame view", onClick = { picker.launch(arrayOf("image/*")) })
                            Divider2()
                            SettingRow(
                                "Show notifications on the clock", "Opens Android's notification access settings",
                                onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            )
                        }

                        Text(
                            "On Xiaomi and HyperOS, also allow pop-up windows in the background, Show on lock screen and Autostart in this app's permissions.",
                            color = Muted, fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }

    private fun startWatcher() =
        ContextCompat.startForegroundService(this, Intent(this, OrientationService::class.java))

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openOverlaySettings() =
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun openBatterySettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
}

@Composable
private fun Heading(text: String) =
    Text(text, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card)) { content() }
}

@Composable
private fun Divider2() = HorizontalDivider(color = Hairline, modifier = Modifier.padding(start = 18.dp))

@Composable
private fun SettingRow(
    title: String, subtitle: String? = null, onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val base = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Row(base.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        trailing()
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, ok: Boolean, accent: Color, onFix: () -> Unit) {
    SettingRow(title, subtitle) {
        if (ok) Text("Allowed", color = accent, fontSize = 15.sp)
        else TextButton(onClick = onFix) { Text("Allow", color = accent, fontSize = 15.sp) }
    }
}
