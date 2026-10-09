package com.standby.clock

/** Coarse hand-made land outlines (lon, lat pairs) rasterised into a dot grid for the World clock. */
object WorldMap {
    const val STEP = 3f   // degrees between dots

    private val polygons: List<FloatArray> = listOf(
        floatArrayOf(-168f, 65f, -155f, 71f, -125f, 70f, -95f, 70f, -80f, 73f, -62f, 66f, -55f, 52f, -66f, 44f, -76f, 36f, -81f, 25f, -90f, 29f, -97f, 26f, -97f, 18f, -88f, 21f, -84f, 10f, -78f, 8f, -86f, 14f, -95f, 16f, -105f, 20f, -112f, 29f, -117f, 32f, -124f, 40f, -125f, 49f, -135f, 58f, -150f, 60f, -165f, 60f),  // northAmerica
        floatArrayOf(-72f, 78f, -58f, 82f, -30f, 83f, -20f, 76f, -22f, 70f, -40f, 65f, -44f, 60f, -52f, 63f, -55f, 68f, -68f, 70f),  // greenland
        floatArrayOf(-78f, 64f, -62f, 66f, -64f, 70f, -80f, 73f, -90f, 70f),  // baffin
        floatArrayOf(-118f, 70f, -102f, 69f, -102f, 73f, -118f, 73f),  // victoria
        floatArrayOf(-24f, 64f, -14f, 64f, -15f, 66f, -23f, 66f),  // iceland
        floatArrayOf(-78f, 9f, -72f, 12f, -62f, 10f, -51f, 4f, -35f, -6f, -39f, -15f, -48f, -26f, -57f, -35f, -65f, -41f, -68f, -53f, -74f, -50f, -73f, -40f, -71f, -28f, -70f, -18f, -76f, -13f, -81f, -5f, -80f, 2f),  // southAmerica
        floatArrayOf(-17f, 21f, -10f, 30f, -6f, 36f, 10f, 37f, 11f, 33f, 20f, 32f, 32f, 31f, 35f, 28f, 43f, 12f, 51f, 12f, 40f, -3f, 40f, -15f, 35f, -24f, 32f, -29f, 20f, -35f, 17f, -30f, 12f, -17f, 13f, -6f, 9f, 3f, 4f, 6f, -8f, 4f, -17f, 14f),  // africa
        floatArrayOf(44f, -24f, 47f, -25f, 50f, -16f, 49f, -12f, 44f, -16f),  // madagascar
        floatArrayOf(-9f, 37f, -9f, 43f, -2f, 44f, -5f, 48f, 2f, 51f, 5f, 53f, 8f, 54f, 8f, 58f, 5f, 59f, 6f, 62f, 12f, 66f, 16f, 69f, 25f, 71f, 33f, 70f, 42f, 67f, 44f, 68f, 60f, 69f, 70f, 73f, 80f, 73f, 105f, 77f, 113f, 74f, 130f, 71f, 150f, 71f, 170f, 70f, 180f, 68f, 180f, 64f, 170f, 60f, 162f, 58f, 156f, 51f, 143f, 52f, 140f, 48f, 135f, 43f, 130f, 42f, 129f, 35f, 126f, 38f, 125f, 40f, 121f, 40f, 122f, 37f, 120f, 32f, 122f, 30f, 117f, 23f, 110f, 21f, 108f, 16f, 106f, 10f, 103f, 10f, 100f, 13f, 100f, 8f, 104f, 1f, 100f, 4f, 98f, 8f, 98f, 16f, 94f, 17f, 92f, 22f, 88f, 22f, 86f, 20f, 80f, 15f, 80f, 10f, 77f, 8f, 73f, 16f, 72f, 21f, 68f, 23f, 66f, 25f, 57f, 25f, 56f, 27f, 50f, 30f, 48f, 29f, 50f, 25f, 56f, 26f, 58f, 23f, 52f, 16f, 43f, 13f, 39f, 22f, 35f, 28f, 34f, 31f, 36f, 36f, 30f, 36f, 27f, 37f, 26f, 40f, 23f, 40f, 23f, 36f, 20f, 40f, 19f, 42f, 14f, 45f, 13f, 45f, 16f, 41f, 18f, 40f, 16f, 38f, 12f, 42f, 8f, 44f, 3f, 43f, 0f, 39f, -2f, 37f, -5f, 36f),  // eurasia
        floatArrayOf(130f, 31f, 141f, 36f, 142f, 43f, 139f, 41f, 132f, 35f),  // japan
        floatArrayOf(-5f, 50f, 1f, 51f, 0f, 54f, -3f, 58f, -6f, 58f, -5f, 54f, -3f, 53f),  // uk
        floatArrayOf(-10f, 52f, -6f, 52f, -6f, 55f, -10f, 54f),  // ireland
        floatArrayOf(95f, 5f, 98f, 3f, 106f, -4f, 105f, -6f, 101f, -3f, 96f, 2f),  // sumatra
        floatArrayOf(109f, 1f, 117f, 7f, 119f, 1f, 116f, -4f, 110f, -3f),  // borneo
        floatArrayOf(105f, -6f, 114f, -7f, 114f, -9f, 106f, -8f),  // java
        floatArrayOf(131f, -1f, 141f, -3f, 150f, -10f, 141f, -9f, 134f, -4f),  // newGuinea
        floatArrayOf(120f, 18f, 122f, 18f, 126f, 8f, 122f, 7f, 120f, 14f),  // philippines
        floatArrayOf(114f, -22f, 122f, -18f, 130f, -12f, 137f, -12f, 142f, -11f, 146f, -19f, 153f, -26f, 151f, -34f, 147f, -39f, 141f, -38f, 135f, -34f, 130f, -31f, 124f, -33f, 115f, -34f, 113f, -26f),  // australia
        floatArrayOf(172f, -35f, 178f, -38f, 175f, -41f, 172f, -41f),  // nzNorth
        floatArrayOf(172f, -41f, 174f, -42f, 168f, -46f, 167f, -45f),  // nzSouth
    )

    /** Flat list of land dots: lon0, lat0, lon1, lat1, ... (computed once). */
    val points: FloatArray by lazy {
        val out = ArrayList<Float>()
        var lat = 80f
        while (lat >= -58f) {
            var lon = -178f
            while (lon <= 178f) {
                if (polygons.any { inside(it, lon, lat) }) { out.add(lon); out.add(lat) }
                lon += STEP
            }
            lat -= STEP
        }
        out.toFloatArray()
    }

    private fun inside(p: FloatArray, x: Float, y: Float): Boolean {
        var c = false
        var j = p.size - 2
        var i = 0
        while (i < p.size) {
            val xi = p[i]; val yi = p[i + 1]; val xj = p[j]; val yj = p[j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
            j = i
            i += 2
        }
        return c
    }
}
