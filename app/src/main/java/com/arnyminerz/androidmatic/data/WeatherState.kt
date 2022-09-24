package com.arnyminerz.androidmatic.data

import com.arnyminerz.androidmatic.annotation.WeatherLiteral
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
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
