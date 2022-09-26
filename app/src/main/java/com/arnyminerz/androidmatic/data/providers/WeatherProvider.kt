package com.arnyminerz.androidmatic.data.providers

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import kotlin.reflect.KClass

abstract class WeatherProvider<P : FetchParameters, W : WeatherProvider<P, W>> {
    abstract val providerName: String

    @WorkerThread
    suspend fun check(context: Context, params: P): Boolean = try {
        fetch(context, params)
        true
    } catch (e: Exception) {
        false
    }

    @WorkerThread
    abstract suspend fun list(context: Context): List<Station<P, W>>

    @WorkerThread
    abstract suspend fun fetch(context: Context, params: P): StationFeedResult

    @WorkerThread
    abstract suspend fun fetchWeather(context: Context, params: P): WeatherState

    companion object {
        val providers: MutableCollection<WeatherProvider<*, *>> = arrayListOf()

        fun register(provider: WeatherProvider<*, *>) = providers.add(provider)

        fun find(providerName: String) = providers.find { it.providerName == providerName }

        fun <P : WeatherProvider<*, *>> findOfType(kClass: KClass<P>) =
            providers.find { it::class == kClass }
    }
}

interface FetchParameters : JsonSerializable {
    val parameters: Map<String, KClass<*>>
}
