package com.arnyminerz.androidmatic.data.providers

import android.content.Context
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.data.numeric.MaxValue
import com.arnyminerz.androidmatic.data.numeric.MinMaxValue
import com.arnyminerz.androidmatic.data.providers.model.Descriptor
import com.arnyminerz.androidmatic.data.providers.model.WeatherProvider
import com.arnyminerz.androidmatic.singleton.VolleySingleton
import java.text.ParseException
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

class WeewxTemplateProvider : WeatherProvider() {
    override val displayName: String = "Weewx"

    override val providerName: String = "weewx"

    override val descriptor: ProviderDescriptor = ProviderDescriptor

    object ProviderDescriptor : Descriptor() {
        override val name: String = "weewx_descriptor"

        override val parameters: Map<String, KClass<*>> = mapOf(
            "uid" to String::class,
            "name" to String::class,
            "url" to String::class,
            "guid" to String::class,
        )

        override val capabilities: List<Capability> = emptyList()

        fun provide(url: String, name: String, uid: String, guid: String) =
            provide("url" to url, "name" to name, "uid" to uid, "guid" to guid)
    }

    override suspend fun fetch(
        context: Context,
        vararg params: Pair<String, Any>
    ): StationFeedResult {
        val url = params.find { it.first == "url" }?.second as? String
            ?: throw IllegalArgumentException("params doesn't have any \"url\".")
        val template = VolleySingleton.getInstance(context)
            .getString(url)
        val (station, weather) = parse(template, *params)
        return StationFeedResult(weather.timestamp, listOf(station))
    }

    override suspend fun fetchWeather(
        context: Context,
        vararg params: Pair<String, Any>
    ): WeatherState {
        val url = params.find { it.first == "url" }?.second as? String
            ?: throw IllegalArgumentException("params doesn't have any \"url\".")
        val template = VolleySingleton.getInstance(context)
            .getString(url)
        val (_, weather) = parse(template, *params)
        return weather
    }

    /**
     * Tries to parse a meteoclimatic template into a [Station] and [WeatherState].
     * @author Arnau Mora
     * @since 20220926
     * @param template The template to parse.
     * @param params Parameters to use as extra values to the template. Must match
     * [ProviderDescriptor], this is containing `url` and `name`.
     * @throws ParseException If there's a parameter missing in the template.
     * [ParseException.getErrorOffset] gives which parameter is missing.
     */
    @Throws(ParseException::class)
    private fun parse(
        template: String,
        vararg params: Pair<String, Any>,
    ): Pair<Station, WeatherState> {
        val url = params.find { it.first == "url" }?.second as? String
            ?: throw IllegalArgumentException("params doesn't have any \"url\".")
        val name = params.find { it.first == "name" }?.second as? String
            ?: throw IllegalArgumentException("params doesn't have any \"name\".")

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
        val station = Station(
            descriptor.provide(url, name, uid!!, guid!!),
        )
        val weather = WeatherState(
            timestamp ?: throw ParseException("Could not find timestamp.", 0),
            name,
            MinMaxValue(
                temperature ?: throw ParseException("Could not find temperature.", 1),
                temperatureDayMin ?: throw ParseException("Could not find min temperature.", 2),
                temperatureDayMax ?: throw ParseException("Could not find max temperature.", 3),
            ),
            MinMaxValue(
                humidity ?: throw ParseException("Could not find humidity.", 4),
                humidityDayMin ?: throw ParseException("Could not find min humidity.", 5),
                humidityDayMax ?: throw ParseException("Could not find max humidity.", 6),
            ),
            MinMaxValue(
                barometer ?: throw ParseException("Could not find pressure.", 7),
                barometerDayMin ?: throw ParseException("Could not find min pressure.", 8),
                barometerDayMax ?: throw ParseException("Could not find max pressure.", 9),
            ),
            MaxValue(
                windSpeed ?: throw ParseException("Could not find wind speed.", 10),
                windSpeedDayMax ?: throw ParseException("Could not find max wind speed.", 11),
            ),
            windDirection ?: throw ParseException("Could not find wind direction.", 12),
            rain ?: throw ParseException("Could not find rain.", 13),
            "unknown", // Weewx doesn't support literal state
        )
        return station to weather
    }
}