package com.standby.clock

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class WeatherData(val temp: Int, val code: Int, val hi: Int, val lo: Int, val unit: String)

/**
 * Weather from Open-Meteo (free, no key). You type a city once; it is looked up by name,
 * so no location permission is needed. Only the city name and the coordinates go online.
 */
object WeatherStore {
    var city by mutableStateOf(""); private set
    var data by mutableStateOf<WeatherData?>(null); private set
    var status by mutableStateOf(""); private set
    private var lat = 0.0
    private var lon = 0.0
    private var lastFetch = 0L
    private var loaded = false

    fun init(c: Context) {
        if (loaded) return
        loaded = true
        val sp = Prefs.sp(c)
        city = sp.getString("wCity", "") ?: ""
        lat = sp.getString("wLat", null)?.toDoubleOrNull() ?: 0.0
        lon = sp.getString("wLon", null)?.toDoubleOrNull() ?: 0.0

        val fahrenheit = Locale.getDefault().country == "US"
        if (city.isEmpty()) {
            val tzCity = try {
                java.time.ZoneId.systemDefault().id.substringAfterLast('/').replace('_', ' ')
            } catch (e: Exception) { "Cupertino" }
            city = if (tzCity.isNotBlank()) tzCity else "Local"
            data = if (fahrenheit) WeatherData(72, 1, 78, 64, "°F") else WeatherData(26, 1, 30, 22, "°C")
            setCity(c, city)
        } else if (data == null) {
            data = if (fahrenheit) WeatherData(72, 1, 78, 64, "°F") else WeatherData(26, 1, 30, 22, "°C")
            refresh()
        }
    }

    fun setCity(c: Context, name: String) {
        val q = name.trim()
        if (q.isEmpty()) return
        status = "Searching"
        val app = c.applicationContext
        thread {
            try {
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(q, "UTF-8") + "&count=1"
                val results = JSONObject(get(url)).optJSONArray("results")
                if (results == null || results.length() == 0) { status = "City not found"; return@thread }
                val o = results.getJSONObject(0)
                lat = o.getDouble("latitude"); lon = o.getDouble("longitude"); city = o.getString("name")
                Prefs.sp(app).edit().putString("wCity", city).putString("wLat", lat.toString()).putString("wLon", lon.toString()).apply()
                fetch()
            } catch (e: Exception) { status = "Offline" }
        }
    }

    fun refresh() {
        if (city.isEmpty() || System.currentTimeMillis() - lastFetch < 15 * 60_000L) return
        lastFetch = System.currentTimeMillis()
        thread { try { fetch() } catch (e: Exception) { status = "Offline" } }
    }

    private fun fetch() {
        val fahrenheit = Locale.getDefault().country == "US"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1" +
            if (fahrenheit) "&temperature_unit=fahrenheit" else ""
        val j = JSONObject(get(url))
        val cur = j.getJSONObject("current")
        val daily = j.getJSONObject("daily")
        data = WeatherData(
            cur.getDouble("temperature_2m").roundToInt(), cur.getInt("weather_code"),
            daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            if (fahrenheit) "°F" else "°C",
        )
        status = ""
        lastFetch = System.currentTimeMillis()
    }

    private fun get(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        try { return c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
}

/** WMO weather code -> (symbol, label) */
private fun describe(code: Int): Pair<String, String> = when (code) {
    0 -> "\u2600\uFE0F" to "Sunny"
    1 -> "\uD83C\uDF24\uFE0F" to "Mostly Clear"
    2 -> "\u26C5" to "Partly Cloudy"
    3 -> "\u2601\uFE0F" to "Overcast"
    45, 48 -> "\uD83C\uDF2B\uFE0F" to "Foggy"
    in 51..57 -> "\uD83C\uDF26\uFE0F" to "Drizzle"
    in 61..67, in 80..82 -> "\uD83C\uDF27\uFE0F" to "Rain"
    in 71..77, 85, 86 -> "\u2744\uFE0F" to "Snow"
    in 95..99 -> "\u26C8\uFE0F" to "Thunderstorm"
    else -> "\u2601\uFE0F" to "Cloudy"
}

@Composable
fun WeatherWidget() {
    val ctx = LocalContext.current
    remember { WeatherStore.init(ctx); true }
    LaunchedEffect(WeatherStore.city) {
        while (true) {
            WeatherStore.refresh()
            delay(5 * 60_000L)
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val d = LocalDensity.current
        val totalH = constraints.maxHeight.toFloat()
        val tempSize = with(d) { (totalH * 0.32f).toSp() }
        val titleSize = with(d) { (totalH * 0.11f).toSp() }
        val subSize = with(d) { (totalH * 0.10f).toSp() }
        val infoSize = with(d) { (totalH * 0.09f).toSp() }

        val w = WeatherStore.data ?: WeatherData(26, 1, 30, 22, "°C")
        val (symbol, label) = describe(w.code)
        val cityName = if (WeatherStore.city.isNotBlank()) WeatherStore.city else "Local"

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            // City Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = cityName.uppercase(),
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.em,
                )
            }

            // Big Temperature
            Text(
                text = "${w.temp}°",
                color = Color.White,
                fontSize = tempSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.03).em,
            )

            // Condition & High/Low
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$symbol $label",
                    color = Color.White,
                    fontSize = subSize,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "H:${w.hi}°  L:${w.lo}°",
                    color = Color(0xFF8E8E93),
                    fontSize = infoSize,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}
