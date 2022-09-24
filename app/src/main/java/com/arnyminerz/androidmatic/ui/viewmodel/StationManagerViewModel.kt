package com.arnyminerz.androidmatic.ui.viewmodel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.parseFeed
import com.arnyminerz.androidmatic.storage.database.dao.enableStation
import com.arnyminerz.androidmatic.storage.database.entity.toEntity
import com.arnyminerz.androidmatic.singleton.DatabaseSingleton
import com.arnyminerz.androidmatic.singleton.VolleySingleton
import com.arnyminerz.androidmatic.utils.launch
import com.arnyminerz.androidmatic.utils.ui
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Used for loading data for the main Activity asynchronously. Holds UI states.
 * @author Arnau Mora
 * @since 20220923
 */
class StationManagerViewModel(app: Application) : AndroidViewModel(app) {
    private val databaseSingleton = DatabaseSingleton.getInstance(getApplication())

    /**
     * Flags whether or not there's some process ongoing.
     * @author Arnau Mora
     * @since 20220923
     */
    var loading: Boolean by mutableStateOf(false)
        private set

    /**
     * Stores an error when it occurs.
     * @author Arnau Mora
     * @since 20220923
     */
    var error: Exception? by mutableStateOf(null)
        private set

    val stationsFlow: Flow<List<Station>> = databaseSingleton
        .stationsDao()
        .getAllFlow()
        .map { list -> list.map { it.toStation() } }

    val enabledStationsFlow = databaseSingleton
        .stationsDao()
        .getEnabledStations()
        /*.map { stations ->
            stations.mapNotNull { enabledStation ->
                stationsFlow.last().find { it.uid == enabledStation.stationUid }
            }
        }*/

    /**
     * Loads all the stations available from Meteoclimatic's RSS feed. Skips if there are already
     * stations stored, unless [force] is true.
     * @author Arnau Mora
     * @since 20220923
     * @param force If true, the already stored contents will be overridden.
     */
    @Throws(VolleyError::class)
    fun loadStations(force: Boolean = false) {
        Timber.d("Loading stations asynchronously...")
        error = null

        launch {
            Timber.d("Checking if should load stations...")
            val stationsCount = databaseSingleton
                .stationsDao()
                .getAllFlow()
                .firstOrNull()
                ?.size ?: 0
            if (!force && stationsCount > 0) {
                Timber.d("Not forcing, and stations count is not 0. (stationsCount=$stationsCount)")
                return@launch
            }

            loading = true

            if (stationsCount > 0) {
                Timber.d("Deleting all stations...")
                databaseSingleton.stationsDao().deleteAll()
            }

            Timber.d("Getting data from Meteoclimatic...")
            val feed: String? = try {
                suspendCoroutine { cont ->
                    VolleySingleton
                        .getInstance(getApplication())
                        .addToRequestQueue(
                            StringRequest(
                                Request.Method.GET,
                                "https://www.meteoclimatic.net/feed/rss/ES",
                                { cont.resume(it) },
                                { cont.resumeWithException(it) },
                            )
                        )
                }
            } catch (e: VolleyError) {
                ui { error = e }
                Timber.e(e, "Could not fetch data from Meteoclimatic.")
                null
            }
            feed?.let { processStations(it) }

            loading = false
        }
    }

    /**
     * Converts the [feed] into a list of stations, and stores them into the [DatabaseSingleton].
     * @author Arnau Mora
     * @since 20220923
     * @param feed The result obtained from the Meteoclimatic's RSS feed.
     */
    @WorkerThread
    private suspend fun processStations(feed: String) {
        parseFeed(feed)
            .stations
            .map { it.toEntity() }
            .let { databaseSingleton.stationsDao().insertAll(*it.toTypedArray()) }
    }

    fun enableStation(station: Station) =
        launch {
            Timber.i("Enabling station $station...")
            databaseSingleton.stationsDao().enableStation(station.uid)
        }

    fun disableStation(station: Station) =
        launch {
            Timber.i("Disabling station $station...")
            databaseSingleton.stationsDao().disableStation(station.uid)
        }
}