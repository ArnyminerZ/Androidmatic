package com.arnyminerz.androidmatic.data.providers.model

import androidx.compose.runtime.Composable
import com.arnyminerz.androidmatic.data.Station

abstract class WeatherManualProvider : WeatherProvider() {
    /**
     * Should provide a form for configuring the provider.
     * @author Arnau Mora
     * @since 20221003
     */
    @Composable
    abstract fun Contents(onSubmit: (station: Station) -> Unit)
}
