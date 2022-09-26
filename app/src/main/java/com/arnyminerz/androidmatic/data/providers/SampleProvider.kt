package com.arnyminerz.androidmatic.data.providers

import android.content.Context
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import kotlin.reflect.KClass

class SampleProvider : WeatherProvider<SampleProvider.Params>() {
    class Params : FetchParameters {
        override val parameters: Map<String, KClass<*>> = emptyMap()
    }

    override val providerName: String = "sample"

    override suspend fun fetchWeather(context: Context, params: Params): WeatherState {
        throw UnsupportedOperationException("SampleProvider doesn't support fetching weather.")
    }

    override suspend fun fetch(context: Context, params: Params): StationFeedResult {
        throw UnsupportedOperationException("SampleProvider doesn't support fetching data.")
    }

    override suspend fun list(context: Context): List<Station<Params, SampleProvider>> {
        throw UnsupportedOperationException("SampleProvider doesn't support listing stations.")
    }
}