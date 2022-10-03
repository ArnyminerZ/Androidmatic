package com.arnyminerz.androidmatic.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arnyminerz.androidmatic.utils.doAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Used for storing user's preferences.
 * @author Arnau Mora
 * @since 20220924
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "androidmatic")

/**
 * Holds the keys of all the preferences of the app.
 * @author Arnau Mora
 * @since 20220924
 */
object Keys {
    val ANALYTICS_COLLECTION = booleanPreferencesKey("analytics")
    val CRASH_COLLECTION = booleanPreferencesKey("crash")
    val LAST_WORKER_RUN = longPreferencesKey("last_worker_run")
}

/**
 * Gets the value of a preference as a [Flow].
 * @author Arnau mora
 * @since 20220924
 * @param key The key of the preference to get.
 * @param default The default value to give to the preference if none is set.
 * @return A [Flow] that gets new data whenever it's updated in the preference at [key].
 */
fun <P : Preferences, T> DataStore<P>.get(key: Preferences.Key<T>, default: T): Flow<T> =
    data.map { prefs -> prefs[key] ?: default }

/**
 * Gets the value of a preference as a State.
 * @author Arnau Mora
 * @since 20220924
 * @param key The key of the preference to get.
 * @param default The default value for the preference if it has not been set yet.
 * @param initial The initial value for the preference before it has been loaded.
 * @return An state that gets updated whenever the preference at [key] is updated.
 */
@Composable
fun <P : Preferences, T> DataStore<P>.getAsState(
    key: Preferences.Key<T>,
    default: T,
    initial: T = default,
) = get(key, default).collectAsState(initial)

/**
 * Updates the value of a preference.
 * @author Arnau Mora
 * @since 20220924
 * @param key The key of the preference to update.
 * @param value The value to set to the preference.
 * @return A job that observes the progress of the update.
 */
operator fun <T> DataStore<Preferences>.set(key: Preferences.Key<T>, value: T) =
    doAsync { edit { it[key] = value } }
