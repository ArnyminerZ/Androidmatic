package com.arnyminerz.androidmatic.activity

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.arnyminerz.androidmatic.storage.get
import com.arnyminerz.androidmatic.ui.components.NavigationBar
import com.arnyminerz.androidmatic.ui.components.SettingsCategory
import com.arnyminerz.androidmatic.ui.components.SettingsItem
import com.arnyminerz.androidmatic.ui.components.WeatherCard
import com.arnyminerz.androidmatic.ui.theme.setThemedContent
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_NONE
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_SERVER_SEARCH
import com.arnyminerz.androidmatic.ui.viewmodel.ERROR_TIMEOUT
import com.arnyminerz.androidmatic.ui.viewmodel.MainViewModel
import com.arnyminerz.androidmatic.utils.capitalized
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.utils.launch
import com.arnyminerz.androidmatic.utils.ui
import com.arnyminerz.androidmatic.worker.UpdateDataOptions
import com.arnyminerz.androidmatic.worker.UpdateDataWorker
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
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

    @Composable
    private fun HomeLayout() {
        val tasks = viewModel.loadingTasks
        val enabledStations by viewModel.enabledStations.collectAsState(initial = null)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(
                    rememberPullRefreshState(
                        refreshing = tasks.isNotEmpty(),
                        onRefresh = { enabledStations?.forEach { viewModel.loadWeather(it, true) } },
                    )
                ),
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
                val lastWorkerRun by dataStore.get(Keys.LAST_WORKER_RUN, -1).collectAsState(-1)
                val serviceRunning by UpdateDataWorker.get(this@MainActivity)
                    .observeAsState(emptyList())

                SettingsItem(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_summary),
                    onClick = { showingLanguageSelectionDialog = true },
                )

                SettingsCategory(text = stringResource(R.string.settings_category_advanced))
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
