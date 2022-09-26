package com.arnyminerz.androidmatic.data.providers

import android.content.Context
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.data.numeric.MaxValue
import com.arnyminerz.androidmatic.data.numeric.MinMaxValue
import com.arnyminerz.androidmatic.singleton.VolleySingleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.reflect.KClass

/**
 * The formatter used for the timestamp parameter in meteoclimatic templates.
 * @author Arnau Mora
 * @since 20220925
 */
private val DateFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)

class WeewxTemplateProvider :
    WeatherProvider<WeewxTemplateProvider.FetchParams, WeewxTemplateProvider>() {
    override val providerName: String = "weewx"

    class FetchParams(
        val url: String,
        val name: String,
    ) : FetchParameters {
        override val parameters: Map<String, KClass<*>>
            get() = TODO("Not yet implemented")
    }

    override suspend fun list(context: Context): List<Station<FetchParams, WeewxTemplateProvider>> {
        throw UnsupportedOperationException("WeewxTemplateProvider doesn't support listing stations.")
    }

    override suspend fun fetch(
        context: Context,
        params: FetchParams
    ): StationFeedResult<FetchParams, WeewxTemplateProvider> {
        val template = VolleySingleton.getInstance(context)
            .getString(params.url)
        val (station, weather) = parse(template, params)
        return StationFeedResult(weather.timestamp, listOf(station))
    }

    override suspend fun fetchWeather(context: Context, params: FetchParams): WeatherState {
        val template = VolleySingleton.getInstance(context)
            .getString(params.url)
        val (_, weather) = parse(template, params)
        return weather
    }

    private fun parse(
        template: String,
        params: FetchParams,
    ): Pair<Station<FetchParams, WeewxTemplateProvider>, WeatherState> {
        val lines = template
            // Remove all asterisks
            .replace("*", "")
            // Break on each line
            .split('\n')
            // Remove empty lines
            .filter { it.isNotBlank() }
            // Map to create pairs of values
            .mapNotNull { l -> l.split('=').takeIf { it.size >= 2 }?.let { it[0] to it[1] } }
        var uid: String? = null
        var guid: String? = null
        var timestamp: Date? = null
        var temperature: Double? = null
        var windSpeed: Double? = null
        var windDirection: Int? = null
        var barometer: Double? = null
        var humidity: Double? = null
        var temperatureDayMax: Double? = null
        var temperatureDayMin: Double? = null
        var windSpeedDayMax: Double? = null
        var barometerDayMax: Double? = null
        var barometerDayMin: Double? = null
        var humidityDayMax: Double? = null
        var humidityDayMin: Double? = null
        var rain: Double? = null
        lines.forEach { (key, value) ->
            when (key) {
                "VER" -> if (value != "DATA2")
                    throw UnsupportedOperationException("Only version DATA2 is supported.")
                "COD" -> uid = value
                "SIG" -> guid = value
                "UPD" -> timestamp = DateFormatter.parse(value)
                "TMP" -> temperature = value.toDoubleOrNull()
                "WND" -> windSpeed = value.toDoubleOrNull()
                "AZI" -> windDirection = value.toIntOrNull()
                "BAR" -> barometer = value.toDoubleOrNull()
                "HUM" -> humidity = value.toDoubleOrNull()
                "DHTM" -> temperatureDayMax = value.toDoubleOrNull()
                "DLTM" -> temperatureDayMin = value.toDoubleOrNull()
                "DHHM" -> humidityDayMax = value.toDoubleOrNull()
                "DLHM" -> humidityDayMin = value.toDoubleOrNull()
                "DHBR" -> barometerDayMax = value.toDoubleOrNull()
                "DLBR" -> barometerDayMin = value.toDoubleOrNull()
                "DGST" -> windSpeedDayMax = value.toDoubleOrNull()
                "DPCP" -> rain = value.toDoubleOrNull()
            }
        }
        val station = Station<FetchParams, WeewxTemplateProvider>(
            "${params.name} ()",
            uid!!,
            guid!!,
            null,
            null,
            FetchParams(params.url, params.name),
        )
        val weather = WeatherState(
            timestamp!!,
            MinMaxValue(temperature!!, temperatureDayMin!!, temperatureDayMax!!),
            MinMaxValue(humidity!!, humidityDayMin!!, humidityDayMax!!),
            MinMaxValue(barometer!!, barometerDayMin!!, barometerDayMax!!),
            MaxValue(windSpeed!!, windSpeedDayMax!!),
            windDirection!!,
            rain!!,
            "unknown",
        )
        return station to weather
    }
}