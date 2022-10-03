package com.arnyminerz.androidmatic.activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.androidmatic.BuildConfig
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.data.providers.model.HoldingDescriptor
import com.arnyminerz.androidmatic.data.ui.NavigationBarItem
import com.arnyminerz.androidmatic.storage.Keys
import com.arnyminerz.androidmatic.storage.dataStore
import com.arnyminerz.androidmatic.storage.database.entity.SelectedStationEntity
import com.arnyminerz.androidmatic.storage.get
import com.arnyminerz.androidmatic.storage.getAsState
import com.arnyminerz.androidmatic.storage.set
import com.arnyminerz.androidmatic.ui.components.NavigationBar
import com.arnyminerz.androidmatic.ui.components.SettingsCategory
import com.arnyminerz.androidmatic.ui.components.SettingsItem
import com.arnyminerz.androidmatic.ui.components.WeatherCard
import com.arnyminerz.androidmatic.ui.theme.setThemedContent
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_NONE
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_SERVER_SEARCH
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_TIMEOUT
import com.arnyminerz.androidmatic.ui.viewmodel.MainViewModel
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.utils.launch
import com.arnyminerz.androidmatic.utils.toast
import com.arnyminerz.androidmatic.utils.ui
import com.arnyminerz.androidmatic.worker.UpdateDataOptions
import com.arnyminerz.androidmatic.worker.UpdateDataWorker
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    /**
     * Used for launching [AddStationActivity] for selecting new stations.
     * @author Arnau Mora
     * @since 20221003
     */
    private val addStationContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Timber.d("Add station result: ${result.resultCode}")

        // If no station has been selected
        if (result.resultCode == AddStationActivity.RESULT_IDLE) doAsync {
            // And there are no selected stations
            // Then close the app
            if (viewModel.enabledStations.firstOrNull()?.isEmpty() != false) {
                Timber.w("Closing app...")
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        doAsync {
            try {
                loadDynamicLinks()
            } catch (e: NullPointerException) {
                Timber.v("There are no dynamic links to load.")
            } catch (e: Exception) {
                Timber.e(e, "Could not load dynamic link.")
                ui { toast(R.string.toast_link_invalid) }
            }

            Timber.d("Checking for enabled stations...")
            val empty = viewModel.enabledStations.firstOrNull()?.isEmpty() ?: true
            Timber.d("Are enabled stations empty: $empty")
            if (empty) ui {
                addStationContract.launch(this@MainActivity, AddStationActivity::class)
            }
        }

        setThemedContent {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState()

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.title_main)) },
                        actions = {
                            IconButton(
                                onClick = {
                                    addStationContract.launch(
                                        this@MainActivity,
                                        AddStationActivity::class,
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    stringResource(R.string.image_desc_add_station),
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(
                        listOf(
                            NavigationBarItem(Icons.Rounded.Home, R.string.menu_item_home),
                            NavigationBarItem(Icons.Rounded.Settings, R.string.menu_item_settings),
                        ),
                        onChange = { page ->
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    )
                },
            ) { paddingValues ->
                HorizontalPager(
                    count = 2,
                    modifier = Modifier
                        .padding(paddingValues),
                    state = pagerState,
                ) { page ->
                    when (page) {
                        0 -> HomeLayout()
                        1 -> SettingsLayout()
                    }
                }
            }
        }
    }

    /**
     * Loads the Firebase's dynamic links.
     * @author Arnau Mora
     * @since 20221003
     * @return If doesn't throw any error, returns the job that is enabling the station given by the
     * dynamic link.
     * @throws NullPointerException When the dynamic link doesn't have any deep link.
     * @throws IllegalStateException When the deep link doesn't contain a descriptor query param.
     * @throws JSONException When the given descriptor JSON is not valid.
     */
    @WorkerThread
    @Throws(
        NullPointerException::class,
        IllegalStateException::class,
        JSONException::class,
    )
    private suspend fun loadDynamicLinks() = suspendCoroutine { cont ->
        Firebase.dynamicLinks.getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData: PendingDynamicLinkData? ->
                try {
                    // Get deep link from result (may be null if no link is found)
                    val deepLink: Uri = pendingDynamicLinkData?.link
                        ?: throw NullPointerException("The dynamic link doesn't have any deep link.")

                    Timber.i("Link: $deepLink. Extensions: ${pendingDynamicLinkData.extensions}")

                    Timber.i("Loading link...")
                    if (!deepLink.queryParameterNames.contains("descriptor"))
                        throw IllegalStateException("The deep link doesn't contain any descriptor: $deepLink")
                    val rawDescriptorJson: String = deepLink.getQueryParameter("descriptor")!!
                    val descriptor = try {
                        val jsonObject = JSONObject(rawDescriptorJson)
                        HoldingDescriptor.fromJson(jsonObject)
                    } catch (e: JSONException) {
                        Timber.e(e, "Could not parse descriptor JSON: $rawDescriptorJson")
                        throw e
                    }
                    val uid = descriptor.getValue<String>("uid")
                    val selectedStationEntity = SelectedStationEntity(0, uid, rawDescriptorJson)

                    Timber.i("Adding station $uid from ${descriptor.name}.")
                    cont.resume(viewModel.enableStation(selectedStationEntity))
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    @Composable
    private fun HomeLayout() {
        val tasks = viewModel.loadingTasks
        val enabledStations by viewModel.enabledStations.collectAsState(initial = null)

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = tasks.isNotEmpty()),
            onRefresh = {
                enabledStations?.forEach { viewModel.loadWeather(it, true) }
            },
            modifier = Modifier
                .fillMaxSize(),
        ) {
            val weatherMap = viewModel.weather
            val error = viewModel.error

            LazyColumn {
                item {
                    AnimatedVisibility(visible = error != ERROR_NONE) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.error_load_data),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            )
                            Text(
                                text = stringResource(
                                    when (error) {
                                        ERROR_SERVER_SEARCH -> R.string.error_server_reach
                                        ERROR_TIMEOUT -> R.string.error_server_timeout
                                        else -> R.string.error_unknown
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            )
                        }
                    }
                }

                enabledStations?.let { enStations ->
                    items(enStations) { selectedStation ->
                        viewModel.loadWeather(selectedStation)

                        weatherMap[selectedStation.id]?.let {
                            WeatherCard(it, selectedStation.customDescriptor) {
                                return@WeatherCard viewModel.disableStation(
                                    selectedStation
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsLayout() {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                val analyticsCollection by dataStore.getAsState(Keys.ANALYTICS_COLLECTION, true)
                val crashCollection by dataStore.getAsState(Keys.CRASH_COLLECTION, true)
                val lastWorkerRun by dataStore.get(Keys.LAST_WORKER_RUN, -1).collectAsState(-1)
                val serviceRunning by UpdateDataWorker.get(this@MainActivity)
                    .observeAsState(emptyList())

                SettingsCategory(text = stringResource(R.string.settings_category_advanced))
                SettingsItem(
                    title = stringResource(R.string.settings_analytics_enabled_title),
                    subtitle = stringResource(R.string.settings_analytics_enabled_summary),
                    switch = true,
                    stateBoolean = analyticsCollection,
                    setBoolean = { checked ->
                        dataStore[Keys.ANALYTICS_COLLECTION] = checked
                        Firebase.analytics.setAnalyticsCollectionEnabled(checked)
                    },
                )
                SettingsItem(
                    title = stringResource(R.string.settings_error_enabled_title),
                    subtitle = stringResource(R.string.settings_error_enabled_summary),
                    switch = true,
                    stateBoolean = crashCollection,
                    setBoolean = { checked ->
                        dataStore[Keys.CRASH_COLLECTION] = checked
                        Firebase.crashlytics.setCrashlyticsCollectionEnabled(checked)
                    },
                )
                @Suppress("UNCHECKED_CAST")
                SettingsItem(
                    title = stringResource(R.string.settings_service_title),
                    subtitle = if (serviceRunning.isEmpty())
                        stringResource(R.string.settings_service_schedule)
                    else lastWorkerRun
                        .takeIf { it >= 0 }
                        ?.let { Date(it) }
                        ?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) }
                        ?.let { lastRunTime ->
                            stringResource(
                                R.string.settings_service_running,
                                UpdateDataOptions.RepeatInterval,
                                lastRunTime
                            )
                        }
                        ?: stringResource(
                            R.string.settings_service_running_never,
                            UpdateDataOptions.RepeatInterval,
                        ),
                    onClick = {
                        doAsync { UpdateDataWorker.scheduleIfNotRunning(this@MainActivity) }
                    }.takeIf { serviceRunning.isEmpty() } as (() -> Unit)?,
                )
            }
            Text(
                stringResource(
                    R.string.settings_info,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
