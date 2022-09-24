package com.arnyminerz.androidmatic.widget.weather_widget

import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.WeatherState
import org.json.JSONObject

data class WeatherWidgetData(
    val station: Station,
    val weather: WeatherState,
) {
    companion object {
        fun fromJson(json: JSONObject) = WeatherWidgetData(
            Station.fromJson(json.getJSONObject("station")),
            WeatherState.fromJson(json.getJSONObject("weather"))
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("station", station.toJson())
        put("weather", weather.toJson())
    }

    override fun toString(): String = toJson().toString()
}
