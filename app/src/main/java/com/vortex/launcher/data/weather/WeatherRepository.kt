package com.vortex.launcher.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class City(val name: String, val lat: Double, val lon: Double)
data class Weather(val temp: Int, val code: Int)

object Cities {
    val list = listOf(
        City("Москва", 55.75, 37.62),
        City("Санкт-Петербург", 59.93, 30.34),
        City("Екатеринбург", 56.84, 60.61),
        City("Новосибирск", 55.03, 82.92),
        City("Казань", 55.79, 49.12),
        City("Минск", 53.90, 27.57),
        City("Алматы", 43.24, 76.89),
        City("Киев", 50.45, 30.52),
        City("Лондон", 51.51, -0.13),
        City("Нью-Йорк", 40.71, -74.01)
    )
}

/** Open-Meteo: бесплатный API без ключа. */
@Singleton
class WeatherRepository @Inject constructor() {
    suspend fun fetch(city: City): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
                    "&current=temperature_2m,weather_code"
            )
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val cur = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
            c.disconnect()
            Weather(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"))
        }.getOrNull()
    }
}
