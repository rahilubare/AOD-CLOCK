package com.standby.clock

import android.content.Context

/**
 * Land dots from Natural Earth 110m coastlines (public domain), pre-rasterised to res/raw/land_mask.txt:
 * 57 rows x 144 columns, one character per 2.5 degree cell, '1' = land.
 */
object WorldMap {
    const val STEP = 2.5f
    private const val LON0 = -178.75f
    private const val LAT0 = 81.25f

    @Volatile private var cache: FloatArray? = null

    /** Flat list of land dots: lon0, lat0, lon1, lat1, ... */
    fun points(ctx: Context): FloatArray = cache ?: load(ctx).also { cache = it }

    private fun load(ctx: Context): FloatArray {
        val out = ArrayList<Float>()
        ctx.resources.openRawResource(R.raw.land_mask).bufferedReader().useLines { lines ->
            lines.forEachIndexed { j, line ->
                line.forEachIndexed { i, ch ->
                    if (ch == '1') { out.add(LON0 + i * STEP); out.add(LAT0 - j * STEP) }
                }
            }
        }
        return out.toFloatArray()
    }
}
