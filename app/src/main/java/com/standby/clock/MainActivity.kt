package com.standby.clock

import android.Manifest
import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private val BgColor = Color(0xFF000000)
private val CardBg = Color(0xFF1C1C1E)
private val Hairline = Color(0xFF2C2C2E)
private val Muted = Color(0xFF8E8E93)
private val GreenActive = Color(0xFF34C759)

class MainActivity : ComponentActivity() {
    private val picker = registerPhotoPicker()
    private var tick by mutableIntStateOf(0)
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    private val calendarPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiState.init(this)
        if (Prefs.autoEnabled(this)) startWatcher()

        setContent {
            val paletteIndex = UiState.palette.coerceIn(0, Palettes.lastIndex)
            val palette = Palettes[paletteIndex]
            val accent = palette.light

            MaterialTheme(colorScheme = darkColorScheme(background = BgColor, surface = BgColor, primary = accent)) {
                Surface(Modifier.fillMaxSize(), color = BgColor) {
                    val ctx = this@MainActivity
                    val t = tick

                    // Permission checks
                    val canOverlay = remember(t) { Settings.canDrawOverlays(ctx) }
                    val notifOk = remember(t) {
                        Build.VERSION.SDK_INT < 33 ||
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    }
                    val batteryOk = remember(t) {
                        (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
                    }
                    val listenerOk = remember(t) { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(packageName) }
                    val calendarOk = remember(t) {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    }
                    val fullScreenOk = remember(t) {
                        Build.VERSION.SDK_INT < 34 || getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
                    }

                    val running = OrientationService.running

                    var auto by remember { mutableStateOf(Prefs.autoEnabled(ctx)) }
                    var needCharging by remember { mutableStateOf(Prefs.needCharging(ctx)) }
                    var unlockedOk by remember { mutableStateOf(Prefs.unlockedOk(ctx)) }
                    var sens by remember { mutableStateOf(Prefs.sensitivity(ctx)) }
                    val calibrated = remember(t) { Prefs.calibration(ctx) != null }
                    val switchColors = SwitchDefaults.colors(
                        checkedTrackColor = accent,
                        checkedThumbColor = Color.Black,
                        uncheckedTrackColor = Hairline,
                    )

                    val setAuto: (Boolean) -> Unit = { on ->
                        if (on && !canOverlay) {
                            openOverlaySettings()
                        } else {
                            auto = on
                            Prefs.setAuto(ctx, on)
                            if (on) {
                                askNotificationPermission()
                                startWatcher()
                            } else {
                                stopService(Intent(ctx, OrientationService::class.java))
                            }
                        }
                    }
                    val refreshWatcher = { if (auto) startWatcher() }

                    val oem = remember { Oem.detect() }
                    var oemDone by remember { mutableStateOf(Prefs.oemDone(ctx)) }

                    // Setup progress
                    val setupItems = remember(canOverlay, notifOk, batteryOk, oemDone) {
                        listOf(
                            SetupItem("Display over apps", "Allows opening the bedside clock automatically", canOverlay) { openOverlaySettings() },
                            SetupItem("Notifications", "Keeps the landscape sensor service active", notifOk) { askNotificationPermission() },
                            SetupItem("Unrestricted battery", "Prevents Android from putting sensor to sleep", batteryOk) { openBatterySettings() },
                        )
                    }
                    val completedCount = setupItems.count { it.done }
                    val allSetupDone = completedCount == setupItems.size

                    Column(
                        Modifier
                            .safeDrawingPadding()
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Header with Live Status
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "StandBy",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.02).em,
                                )
                                Text(
                                    "Bedside Always-On Display",
                                    color = Muted,
                                    fontSize = 13.sp,
                                )
                            }

                            // Live Watcher Status Pill
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(CardBg)
                                    .border(1.dp, Hairline, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .background(if (running) GreenActive else Muted, CircleShape)
                                    )
                                    Text(
                                        if (running) "Watching" else "Idle",
                                        color = if (running) Color.White else Muted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        // Live Clock Preview Card
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2.1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black)
                                .border(1.dp, Hairline, RoundedCornerShape(24.dp))
                                .clickable { startActivity(Intent(ctx, StandbyActivity::class.java)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            MinimalAnalog(Modifier.fillMaxSize().padding(8.dp), accent = accent)

                            // Launch pill button
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xCC1C1C1E))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Preview Fullscreen ↗",
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // Setup Assistant Card (if anything needs permission)
                        if (!allSetupDone) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CardBg)
                                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Required Setup",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "$completedCount of ${setupItems.size} complete",
                                        color = accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { completedCount.toFloat() / setupItems.size },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = accent,
                                    trackColor = Hairline,
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    setupItems.forEach { item ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                                                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                Text(item.desc, color = Muted, fontSize = 12.sp)
                                            }
                                            if (item.done) {
                                                Text("✓ Allowed", color = GreenActive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            } else {
                                                Button(
                                                    onClick = item.action,
                                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier.height(34.dp),
                                                ) {
                                                    Text("Grant", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Ready Banner
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF142918))
                                    .border(1.dp, GreenActive.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("✓", color = GreenActive, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Ready to Stand. Place your phone on its side to open StandBy.",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        // Section 1: AUTOMATION & DETECTION
                        SectionHeader("DETECTION & AUTOMATION")
                        Group {
                            SettingSwitchRow(
                                title = "Auto-start in landscape",
                                subtitle = "Opens StandBy automatically when phone is rested in a stand",
                                checked = auto,
                                switchColors = switchColors,
                                onCheckedChange = setAuto,
                            )
                            Divider2()
                            SettingSwitchRow(
                                title = "Only while charging",
                                subtitle = "If off, StandBy can also open on battery when placed in stand",
                                checked = needCharging,
                                switchColors = switchColors,
                                onCheckedChange = {
                                    needCharging = it
                                    Prefs.setNeedCharging(ctx, it)
                                    refreshWatcher()
                                },
                            )
                            Divider2()
                            SettingSwitchRow(
                                title = "Launch when unlocked",
                                subtitle = "Opens even if the phone screen is unlocked",
                                checked = unlockedOk,
                                switchColors = switchColors,
                                onCheckedChange = {
                                    unlockedOk = it
                                    Prefs.setUnlockedOk(ctx, it)
                                },
                            )
                            Divider2()
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
                                    Text("Stand Sensitivity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text("How still and steady the phone must rest before activating", color = Muted, fontSize = 13.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Relaxed", "Balanced", "Strict").forEachIndexed { i, label ->
                                        ChoicePill(label, sens == i, accent) {
                                            sens = i
                                            Prefs.setSensitivity(ctx, i)
                                            refreshWatcher()
                                        }
                                    }
                                }
                            }
                            Divider2()
                            SettingNavRow(
                                title = "Calibrate Stand Angle",
                                subtitle = if (calibrated) "Calibrated for your specific stand" else "Teach the app your dock or stand angle",
                                status = if (calibrated) "Calibrated" else null,
                                accent = accent,
                                onClick = { startActivity(Intent(ctx, CalibrateActivity::class.java)) },
                            )
                        }

                        // Section 2: THEME & COLOR PALETTES
                        SectionHeader("COLOR THEME")
                        Group {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Accent Palette", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(palette.name, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Palettes.forEachIndexed { i, p ->
                                        val selected = i == paletteIndex
                                        Box(
                                            Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(p.light)
                                                .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape)
                                                .clickable { UiState.setPalette(ctx, i) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selected) {
                                                Box(Modifier.size(8.dp).background(Color.Black, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: DEVICE INTEGRATIONS (OFFLINE / BUILT-IN)
                        SectionHeader("FEATURES & INTEGRATIONS")
                        Group {
                            SettingActionRow(
                                title = "Calendar Events",
                                subtitle = "Shows your upcoming agenda directly on the clock",
                                status = if (calendarOk) "Allowed" else "Allow",
                                isAllowed = calendarOk,
                                accent = accent,
                                onClick = { calendarPerm.launch(Manifest.permission.READ_CALENDAR) },
                            )
                            Divider2()
                            SettingActionRow(
                                title = "Now Playing & Media",
                                subtitle = "Shows music album art and playback controls",
                                status = if (listenerOk) "Allowed" else "Allow",
                                isAllowed = listenerOk,
                                accent = accent,
                                onClick = { safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            )
                            if (Build.VERSION.SDK_INT >= 34) {
                                Divider2()
                                SettingActionRow(
                                    title = "Full-Screen Alerts",
                                    subtitle = "Backup waking method on Android 14+",
                                    status = if (fullScreenOk) "Allowed" else "Configure",
                                    isAllowed = fullScreenOk,
                                    accent = accent,
                                    onClick = { safeStart(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))) },
                                )
                            }
                            Divider2()
                            SettingNavRow(
                                title = "Quick Settings Tile",
                                subtitle = "Toggle StandBy watcher from the quick settings panel",
                                accent = accent,
                                onClick = { addTile() },
                            )
                            Divider2()
                            SettingNavRow(
                                title = "Photo Frame Album",
                                subtitle = "Choose custom photos for the photo frame view",
                                accent = accent,
                                onClick = { picker.launch(arrayOf("image/*")) },
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }

    private fun startWatcher() {
        try {
            ContextCompat.startForegroundService(this, Intent(this, OrientationService::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start sensor watcher. Check permissions.", Toast.LENGTH_LONG).show()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun safeStart(i: Intent) {
        try { startActivity(i) } catch (e: Exception) { openAppInfo() }
    }

    private fun openAppInfo() =
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun openOverlaySettings() =
        safeStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun openBatterySettings() =
        safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    private fun addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                getSystemService(StatusBarManager::class.java).requestAddTileService(
                    ComponentName(this, AutoStartTile::class.java),
                    getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile),
                    mainExecutor,
                ) { }
                return
            } catch (e: Exception) { }
        }
        Toast.makeText(this, "Swipe down twice, tap the pencil, and drag StandBy to your tiles.", Toast.LENGTH_LONG).show()
    }
}

private class SetupItem(val title: String, val desc: String, val done: Boolean, val action: () -> Unit)

@Composable
private fun SectionHeader(text: String) =
    Text(
        text,
        color = Muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.05.em,
        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
    )

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun Divider2() = HorizontalDivider(color = Hairline, modifier = Modifier.padding(start = 18.dp))

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    switchColors: SwitchColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Switch(checked = checked, colors = switchColors, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    subtitle: String,
    status: String? = null,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (status != null) {
                Text(status, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text("›", color = Muted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    status: String,
    isAllowed: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        if (isAllowed) {
            Text("Allowed", color = GreenActive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(32.dp),
            ) {
                Text(status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Hairline)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
