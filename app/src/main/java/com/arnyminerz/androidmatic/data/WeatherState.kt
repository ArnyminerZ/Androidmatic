package com.arnyminerz.androidmatic.data

import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.annotation.WeatherLiteral
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import com.arnyminerz.androidmatic.data.numeric.MaxValue
import com.arnyminerz.androidmatic.data.numeric.MinMaxValue
import org.jetbrains.annotations.Range
import org.json.JSONObject
import java.util.Date

data class WeatherState(
    val timestamp: Date,
    val temperatureValues: MinMaxValue,
    val humidityValues: MinMaxValue,
    val barometerValues: MinMaxValue,
    val windSpeedValues: MaxValue,
    val windDirection: Int,
    val rain: Double,
    @WeatherLiteral
    val literalState: String,
) : JsonSerializable {
    companion object : JsonSerializer<WeatherState> {
        override fun fromJson(json: JSONObject): WeatherState = WeatherState(
            Date(json.getLong("timestamp")),
            MinMaxValue.fromJson(json.getJSONObject("temperature")),
            MinMaxValue.fromJson(json.getJSONObject("humidity")),
            MinMaxValue.fromJson(json.getJSONObject("barometer")),
            MaxValue.fromJson(json.getJSONObject("wind_speed")),
            json.getInt("wind_direction"),
            json.getDouble("rain"),
            json.getString("literal_state")
        )
    }

    override fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp.time)
        put("temperature", temperatureValues.toJson())
        put("humidity", humidityValues.toJson())
        put("barometer", barometerValues.toJson())
        put("wind_speed", windSpeedValues.toJson())
        put("wind_direction", windDirection)
        put("rain", rain)
        put("literal_state", literalState)
    }

    /**
     * Returns the index which represents the current wind direction in the directions string array.
     * @author Arnau Mora
     * @since 20220925
     * @see R.array.wind_directions
     */
    fun windDirectionLocalizedIndex(): @Range(from = 0L, to = 8L) Int =
        if (windDirection > 337.5 || windDirection <= 22.5)
            0
        else if (windDirection > 22.5 || windDirection <= 45 + 22.5)
            1
        else if (windDirection > 45 + 22.5 || windDirection <= 90 + 22.5)
            2
        else if (windDirection > 90 + 22.5 || windDirection <= 135 + 22.5)
            3
        else if (windDirection > 135 + 22.5 || windDirection <= 180 + 22.5)
            4
        else if (windDirection > 180 + 22.5 || windDirection <= 225 + 22.5)
            5
        else if (windDirection > 225 + 22.5 || windDirection <= 270 + 22.5)
            6
        else if (windDirection > 270 + 22.5 || windDirection <= 315 + 22.5)
            7
        else
            8
}

val WeatherStateSample
    get() = WeatherState(
        Date(),
        MinMaxValue(
            24.1,
            30.0,
            20.0,
        ),
        MinMaxValue(
            60.0,
            58.9,
            75.0,
        ),
        MinMaxValue(
            1017.6,
            1010.5,
            1020.0,
        ),
        MaxValue(17.4, 50.0),
        30,
        0.0,
        "sun",
    )
