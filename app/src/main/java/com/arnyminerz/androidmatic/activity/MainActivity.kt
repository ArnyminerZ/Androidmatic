package com.arnyminerz.androidmatic.activity

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.arnyminerz.androidmatic.BuildConfig
import com.arnyminerz.androidmatic.R
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
import com.arnyminerz.androidmatic.utils.areGooglePlayServicesAvailable
import com.arnyminerz.androidmatic.utils.capitalized
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
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONException
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

    private var playServicesAvailable: Boolean = false

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

        playServicesAvailable = areGooglePlayServicesAvailable()

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
                        pagerState.currentPage,
                        listOf(
                            NavigationBarItem(Icons.Rounded.Home, R.string.menu_item_home),
                            NavigationBarItem(Icons.Rounded.Settings, R.string.menu_item_settings),
                        ),
                        onChange = { page ->
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
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
            .addOnSuccessListener(this) { link ->
                try {
                    val selectedStationEntity = SelectedStationEntity.fromDynamicLink(link)

                    Timber.i("Adding station ${selectedStationEntity.stationUid}...")
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
                            WeatherCard(
                                it,
                                selectedStation.customDescriptor,
                                playServicesAvailable
                            ) {
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
        var showingLanguageSelectionDialog by remember { mutableStateOf(false) }

        if (showingLanguageSelectionDialog)
            AlertDialog(
                onDismissRequest = { showingLanguageSelectionDialog = false },
                confirmButton = {
                    Button(onClick = { showingLanguageSelectionDialog = false }) {
                        Text(stringResource(R.string.dialog_action_close))
                    }
                },
                title = { Text(stringResource(R.string.dialog_language_title)) },
                text = {
                    val languages = BuildConfig.TRANSLATION_ARRAY
                        .map { Locale.forLanguageTag(it) }
                    val selectedLocale = AppCompatDelegate.getApplicationLocales()
                        .also { Timber.i("Application locales: $it") }
                        .getFirstMatch(BuildConfig.TRANSLATION_ARRAY)

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(languages) { language ->
                            ListItem(
                                headlineText = { Text(language.displayName.capitalized) },
                                leadingContent = {
                                    Icon(
                                        imageVector = when (selectedLocale?.language) {
                                            language.language -> Icons.Rounded.RadioButtonChecked
                                            else -> Icons.Rounded.RadioButtonUnchecked
                                        },
                                        contentDescription = when (selectedLocale?.language) {
                                            language.language -> stringResource(R.string.image_desc_language_selected)
                                            else -> stringResource(R.string.image_desc_language_not_selected)
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .clickable {
                                        val appLocale: LocaleListCompat =
                                            LocaleListCompat.create(language)
                                        AppCompatDelegate.setApplicationLocales(appLocale)
                                        showingLanguageSelectionDialog = false
                                    },
                            )
                        }
                    }
                },
            )

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

                SettingsItem(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_summary),
                    onClick = { showingLanguageSelectionDialog = true },
                )

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
