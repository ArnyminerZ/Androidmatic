package com.arnyminerz.androidmatic.data.providers.model

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.androidmatic.data.Station

abstract class WeatherListProvider : WeatherProvider() {
    @WorkerThread
    abstract suspend fun list(context: Context): List<Station>
}
