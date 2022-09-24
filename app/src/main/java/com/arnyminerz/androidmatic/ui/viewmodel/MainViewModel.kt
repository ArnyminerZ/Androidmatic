package com.arnyminerz.androidmatic.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.singleton.DatabaseSingleton
import com.arnyminerz.androidmatic.utils.launch
import com.arnyminerz.androidmatic.utils.ui
import kotlinx.coroutines.flow.map
import timber.log.Timber

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val databaseSingleton = DatabaseSingleton.getInstance(app)

    /**
     * Stores a flow with all the ids of the enabled stations.
     * @author Arnau Mora
     * @since 20220923
     */
    val stations = databaseSingleton
        .stationsDao()
        .getAllFlow()
        .map { list ->
            list.map { it.toStation() }
        }

    /**
     * Stores a flow with all the ids of the enabled stations.
     * @author Arnau Mora
     * @since 20220923
     */
    val enabledStations = databaseSingleton
        .stationsDao()
        .getEnabledStations()
        .map { list ->
            list.map { it.stationUid }
        }

    val weather = mutableStateMapOf<String, WeatherState>()

    /**
     * Holds a list of all the stations that are being loaded. If empty, no background tasks are
     * running.
     * @author Arnau Mora
     * @since 20220924
     */
    val loadingTasks = mutableStateListOf<String>()

    /**
     * Loads the current weather for the given station into [weather]. The task progress can be
     * supervised with [loadingTasks].
     * @author Arnau Mora
     * @since 20220924
     * @param station The [Station] to load.
     * @param force If true, the weather will be loaded without taking into account if it has
     * already been loaded.
     * @see weather
     * @see loadingTasks
     * @throws VolleyError When there's an exception while loading the data from the server.
     * @throws ArrayIndexOutOfBoundsException When any station could be found on the server's response.
     * @throws IllegalStateException When the loaded station doesn't have any description data.
     * @throws IllegalArgumentException When the station's description doesn't have the weather
     * information in it.
     * @throws NullPointerException When the received station data doesn't contain a timestamp.
     */
    @Throws(
        VolleyError::class,
        ArrayIndexOutOfBoundsException::class,
        IllegalStateException::class,
        IllegalArgumentException::class,
        NullPointerException::class,
    )
    fun loadWeather(station: Station, force: Boolean = false) =
        if (force || (!weather.contains(station.uid) && !loadingTasks.contains(station.uid)))
            launch {
                ui { loadingTasks.add(station.uid) }
                Timber.d("Fetching weather data for $station...")
                val stationWeather = station.fetchWeather(getApplication())
                Timber.d("Weather data for $station is ready. Showing in UI...")
                ui {
                    weather[station.uid] = stationWeather
                    loadingTasks.remove(station.uid)
                }
            }
        else null
}
