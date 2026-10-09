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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val picker = registerPhotoPicker()
    private var tick by mutableIntStateOf(0)    // bumped on resume so status rows refresh
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs.autoEnabled(this)) startWatcher()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
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

                    Column(
                        Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("StandBy Clock", style = MaterialTheme.typography.headlineMedium)

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-start when placed in landscape", style = MaterialTheme.typography.titleMedium)
                                Text("Opens StandBy by itself when the phone is stood up sideways and stays still.")
                            }
                            Switch(checked = auto, onCheckedChange = { on ->
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Only while charging")
                                Text("Turn off to also work on battery. Watching with the screen locked then uses extra battery.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = needCharging, onCheckedChange = {
                                needCharging = it
                                Prefs.setNeedCharging(ctx, it)
                                if (auto) startWatcher()
                            })
                        }

                        Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
                        Status("Display over other apps (required)", canOverlay) { openOverlaySettings() }
                        Status("Notifications (keeps the watcher visible)", notifOk) { askNotificationPermission() }
                        Status("Battery: unrestricted (recommended)", batteryOk) { openBatterySettings() }

                        HorizontalDivider()
                        Button(
                            onClick = { startActivity(Intent(ctx, StandbyActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Start StandBy now") }
                        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Choose photos for the Photos view")
                        }
                        OutlinedButton(
                            onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Enable notification access (glanceable notifications)") }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }

    @Composable
    private fun Status(label: String, ok: Boolean, onFix: () -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (ok) "\u2713  $label" else "\u2717  $label", Modifier.weight(1f))
            if (!ok) TextButton(onClick = onFix) { Text("Fix") }
        }
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
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

}
