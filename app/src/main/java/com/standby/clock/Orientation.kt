package com.standby.clock

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Shared "is the phone on its stand?" logic, used by the watcher service and the calibration screen. */
object Orientation {
    // index = sensitivity preset: 0 relaxed, 1 balanced, 2 strict
    val HOLD_MS = longArrayOf(1000L, 1800L, 3000L)          // how long it must stay put
    val MAX_MOTION = floatArrayOf(0.9f, 0.45f, 0.3f)        // hand-shake tolerance (m/s^2)
    val TOLERANCE_DEG = doubleArrayOf(40.0, 30.0, 20.0)     // allowed angle from a calibrated pose

    fun angleDeg(a: FloatArray, b: FloatArray): Double {
        val na = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        val nb = sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2])
        if (na < 1e-3f || nb < 1e-3f) return 180.0
        val dot = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) / (na * nb)
        return Math.toDegrees(acos(dot.coerceIn(-1f, 1f).toDouble()))
    }

    /**
     * gravity: low-pass filtered accelerometer. ref: calibrated pose or null for the default
     * (short edge down, upright or leaned back up to ~50 degrees). A calibrated pose also
     * matches the same pose rotated 180 degrees in the screen plane.
     */
    fun isLandscapePose(gravity: FloatArray, ref: FloatArray?, sensitivity: Int): Boolean {
        if (ref != null) {
            val tol = TOLERANCE_DEG[sensitivity.coerceIn(0, 2)]
            if (angleDeg(gravity, ref) <= tol) return true
            return angleDeg(gravity, floatArrayOf(-ref[0], -ref[1], ref[2])) <= tol
        }
        val gx = abs(gravity[0])
        val gy = abs(gravity[1])
        val gz = abs(gravity[2])
        val minGx = when (sensitivity.coerceIn(0, 2)) {
            0 -> 2.6f
            1 -> 3.2f
            else -> 4.2f
        }
        val gyRatio = when (sensitivity.coerceIn(0, 2)) {
            0 -> 1.0f
            1 -> 1.2f
            else -> 1.4f
        }
        return (gx > gy * gyRatio + 1.0f) && (gx > minGx) && (gz < 9.4f)
    }
}
