package com.standby.clock

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

/** Full-screen StandBy: landscape, screen always on, system bars hidden. */
class StandbyActivity : ComponentActivity() {
    private val picker = registerPhotoPicker()
    internal var autoStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoStarted = intent.getBooleanExtra("auto", false)
        ref = WeakReference(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)                    // show over the lock screen
        if (autoStarted) setTurnScreenOn(true)     // wake the screen when started automatically

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            StandbyScreen(
                onBrightness = { b -> window.attributes = window.attributes.apply { screenBrightness = b } },
                onPickPhotos = { picker.launch(arrayOf("image/*")) },
            )
        }
    }

    override fun onDestroy() {
        if (ref?.get() === this) ref = null
        super.onDestroy()
    }

    companion object {
        private var ref: WeakReference<StandbyActivity>? = null
        val isOpen: Boolean get() = ref?.get() != null
        /** Called by the orientation service when the phone leaves landscape. */
        fun finishIfAuto() { ref?.get()?.takeIf { it.autoStarted }?.finish() }
    }
}
