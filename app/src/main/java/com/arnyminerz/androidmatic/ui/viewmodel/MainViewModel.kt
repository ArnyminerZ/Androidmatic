package com.arnyminerz.androidmatic.ui.viewmodel

import android.app.Application
import androidx.annotation.IntDef
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.android.volley.NoConnectionError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.data.WeatherStateSample
import com.arnyminerz.androidmatic.data.providers.model.WeatherProvider
import com.arnyminerz.androidmatic.singleton.DatabaseSingleton
import com.arnyminerz.androidmatic.utils.launch
import com.arnyminerz.androidmatic.utils.ui
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * There has not been any error.
 * @author Arnau Mora
 * @since 2020925
 */
const val ERROR_NONE = 0

/**
 * The server could not be reached, or the response is invalid.
 * @author Arnau Mora
 * @since 20220925
 * @see NoConnectionError
 */
const val ERROR_SERVER_SEARCH = 1

/**
 * Usually the device doesn't have an active internet connection.
 * @author Arnau Mora
 * @since 20220925
 * @see TimeoutError
 */
const val ERROR_TIMEOUT = 2

/**
 * The given data is not valid.
 * @author Arnau Mora
 * @since 20220926
 */
const val ERROR_DATA_FORMAT = 3


@IntDef(ERROR_NONE, ERROR_SERVER_SEARCH, ERROR_TIMEOUT, ERROR_DATA_FORMAT)
annotation class ErrorType

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
        .map { list -> list.map { it.toStation() } }

    /**
     * Stores a flow with all the ids of the enabled stations.
     * @author Arnau Mora
     * @since 20220923
     */
    val enabledStations = databaseSingleton
        .stationsDao()
        .getEnabledStations()
        .map { list -> list.map { it.stationUid } }

    val weather = mutableStateMapOf<String, WeatherState>()

    /**
     * Holds a list of all the stations that are being loaded. If empty, no background tasks are
     * running.
     * @author Arnau Mora
     * @since 20220924
     */
    val loadingTasks = mutableStateListOf<String>()

    /**
     * Holds the error code for whenever there's an error while loading the data.
     * @author Arnau Mora
     * @since 20220925
     * @see ERROR_NONE
     * @see ERROR_SERVER_SEARCH
     * @see ERROR_TIMEOUT
     * @see ERROR_DATA_FORMAT
     */
    @ErrorType
    var error: Int by mutableStateOf(0)
        private set

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
    fun loadWeather(station: Station, force: Boolean = false, first: Boolean = true): Job? =
        if (force || (!weather.contains(station.uid) && !loadingTasks.contains(station.uid)))
            launch {
                try {
                    ui {
                        error = ERROR_NONE
                        loadingTasks.add(station.uid)
                    }
                    Timber.d("Fetching weather data for $station...")
                    val stationWeather = station.fetchWeather(getApplication())
                    Timber.d("Weather data for $station is ready. Showing in UI...")
                    ui { weather[station.uid] = stationWeather }
                } catch (e: NoConnectionError) {
                    if (e.networkResponse?.statusCode == 304)
                        Timber.e("Could not load data since it has not changed.")
                    else {
                        ui { error = ERROR_SERVER_SEARCH }
                        if (first)
                            loadWeather(station, force, false)
                    }
                } catch (e: TimeoutError) {
                    ui { error = ERROR_TIMEOUT }
                } catch (e: IllegalStateException) {
                    Timber.w("Station descriptor: ${station.descriptor::class.java}")
                    Timber.w("Weather providers: ${WeatherProvider.providers.map { it::class }}")
                    Timber.e(
                        e,
                        "Could not find a provider for the station. Provider: ${station.provider}. Descriptor class: ${station.descriptor::class}"
                    )
                    ui { weather[station.uid] = WeatherStateSample }
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Could not build the weather data for station $station.")
                    ui { error = ERROR_DATA_FORMAT }
                } finally {
                    ui { loadingTasks.remove(station.uid) }
                }
            }
        else null
}
