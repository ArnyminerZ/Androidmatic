package com.arnyminerz.androidmatic.data

import com.arnyminerz.androidmatic.data.providers.FetchParameters
import com.arnyminerz.androidmatic.data.providers.WeatherProvider
import java.util.Date

data class StationFeedResult<T : FetchParameters, W : WeatherProvider<T, W>>(
    val timestamp: Date,
    val stations: List<Station<T, W>>,
)
