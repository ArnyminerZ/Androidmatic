package com.arnyminerz.androidmatic.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.data.NavigationBarItem
import com.arnyminerz.androidmatic.storage.Keys
import com.arnyminerz.androidmatic.storage.dataStore
import com.arnyminerz.androidmatic.storage.getAsState
import com.arnyminerz.androidmatic.storage.set
import com.arnyminerz.androidmatic.ui.components.NavigationBar
import com.arnyminerz.androidmatic.ui.components.SettingsCategory
import com.arnyminerz.androidmatic.ui.components.SettingsItem
import com.arnyminerz.androidmatic.ui.components.WeatherCard
import com.arnyminerz.androidmatic.ui.theme.setThemedContent
import com.arnyminerz.androidmatic.ui.viewmodel.MainViewModel
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.utils.launch
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setThemedContent {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState()

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.title_main)) },
                        actions = {
                            IconButton(
                                onClick = { launch(AddStationActivity::class) },
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    stringResource(R.string.image_desc_close),
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

    @Composable
    private fun HomeLayout() {
        val tasks = viewModel.loadingTasks
        val stations by viewModel.stations.collectAsState(initial = emptyList())
        val enabledStations by viewModel.enabledStations.collectAsState(initial = null)

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = tasks.isNotEmpty()),
            onRefresh = {
                enabledStations
                    ?.mapNotNull { uid -> stations.find { it.uid == uid } }
                    ?.forEach { viewModel.loadWeather(it, true) }
            },
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (enabledStations?.isEmpty() == true)
                launch(AddStationActivity::class)
            else {
                val weatherMap = viewModel.weather

                LazyColumn {
                    enabledStations?.let { enStations ->
                        items(enStations) { stationId ->
                            val station = stations.find { it.uid == stationId }
                            if (station != null) {
                                try {
                                    viewModel.loadWeather(station)
                                } catch (e: Exception) {
                                    // TODO: Show some message in the UI
                                }

                                WeatherCard(station, weatherMap[stationId])
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val analyticsCollection by dataStore.getAsState(Keys.ANALYTICS_COLLECTION, true)
            val crashCollection by dataStore.getAsState(Keys.CRASH_COLLECTION, true)

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
        }
    }
}
