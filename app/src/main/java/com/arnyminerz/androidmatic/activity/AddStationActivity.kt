package com.arnyminerz.androidmatic.activity

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.TimeoutError
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.storage.database.entity.SelectedStationEntity
import com.arnyminerz.androidmatic.ui.components.SortingChip
import com.arnyminerz.androidmatic.ui.components.StationCard
import com.arnyminerz.androidmatic.ui.components.ToggleableChip
import com.arnyminerz.androidmatic.ui.theme.setThemedContent
import com.arnyminerz.androidmatic.ui.viewmodel.StationManagerViewModel
import com.arnyminerz.androidmatic.utils.filterSearch
import com.arnyminerz.androidmatic.utils.toggle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
open class AddStationActivity(
    private val single: Boolean = false,
    private val onClosed: (AddStationActivity.() -> Unit)? = null,
) : AppCompatActivity() {
    companion object {
        /**
         * Returned when the user haven't added nor removed any stations.
         * @author Arnau Mora
         * @since 20220923
         */
        const val RESULT_IDLE = 0

        /**
         * Returned when the user have added or removed one or more stations.
         * @author Arnau Mora
         * @since 20220923
         */
        const val RESULT_UPDATED = 1
    }

    /**
     * The view model used to load data in the main activity.
     * @author Arnau Mora
     * @since 20220923
     */
    private val viewModel: StationManagerViewModel by viewModels()

    private var updatedStations = false

    /**
     * Called whenever the user presses the back button, or the close button is tapped.
     * @author Arnau Mora
     * @since 20220924
     * @see onClosed
     */
    private fun onBack() {
        onClosed?.invoke(this) ?: run {
            setResult(if (updatedStations) RESULT_UPDATED else RESULT_IDLE)
            finish()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("Loading stations...")
        viewModel.loadStations()

        setThemedContent {
            BackHandler(onBack = ::onBack)

            // TODO: Add loading indicator page

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(if (single) R.string.title_choose_station else R.string.title_add_station)) },
                        navigationIcon = {
                            IconButton(
                                onClick = ::onBack,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    stringResource(R.string.image_desc_close),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.loadStations(true) },
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    stringResource(R.string.image_desc_refresh),
                                )
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    Pager()

                    val error = viewModel.error

                    AnimatedVisibility(
                        visible = error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.error_load_stations),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (error is TimeoutError)
                                    Text(
                                        text = stringResource(R.string.error_server_reach),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                TextButton(onClick = { viewModel.loadStations(true) }) {
                                    Text(stringResource(R.string.action_try_again))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    @ExperimentalPagerApi
    private fun Pager() {
        val scope = rememberCoroutineScope()
        val state = rememberPagerState(0)
        HorizontalPager(
            count = 2,
            state = state,
            userScrollEnabled = false,
        ) {
            when (it) {
                0 -> Contents { scope.launch { state.animateScrollToPage(1) } }
                1 -> LoadingPage()
            }
        }
    }

    @Composable
    private fun Contents(onStationTapped: (station: Station) -> Unit) {
        val loading = viewModel.loading
        val stations by viewModel.stationsFlow.collectAsState(initial = emptyList())
        val enabledStations by viewModel.enabledStationsFlow.collectAsState(initial = emptyList())
        val filterLocations = remember { mutableStateListOf<String>() }
        var filterSearch by remember { mutableStateOf("") }
        var filterEnabled by remember { mutableStateOf(false) }

        /**
         * Stores the kind of sorting chosen by the user. Values:
         * - `0`: Sorting by name
         * - `1`: Sorting by location
         * - `2`: Sorting by distance // TODO
         * @author Arnau Mora
         * @since 20220923
         */
        var sortingBy by remember { mutableStateOf(0) }

        if (loading)
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        else Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = filterSearch,
                onValueChange = { filterSearch = it },
                label = { Text(stringResource(R.string.title_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, stringResource(R.string.title_search)) },
                trailingIcon = {
                    AnimatedVisibility(visible = filterSearch.isNotBlank()) {
                        IconButton(onClick = { filterSearch = "" }) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.action_clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.FilterList,
                    stringResource(R.string.title_filter_province),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = stringResource(R.string.title_filter_province),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.filter_toggle_all_off),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .clickable {
                            // Add all the locations that have already not been added
                            filterLocations.clear()
                            filterLocations.addAll(
                                stations
                                    .map { it.location }
                                    // Makes sure there are no duplicates
                                    .toSet()
                            )
                        },
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    stations
                        .map { it.location }
                        // Set removes duplicates
                        .toSet()
                        // Sort alphabetically
                        .sorted()
                        // Convert back to list for displaying
                        .toList(),
                ) { location ->
                    val selected = !filterLocations.contains(location)
                    FilterChip(
                        selected = selected,
                        modifier = Modifier
                            .padding(horizontal = 2.dp),
                        onClick = {
                            filterLocations.toggle(location)
                            Timber.i("Locations: ${filterLocations.joinToString(", ")}")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Check
                                    .takeIf { selected }
                                    ?: Icons.Rounded.Close,
                                stringResource(R.string.image_desc_filter_enabled)
                                    .takeIf { selected }
                                    ?: stringResource(R.string.image_desc_filter_disabled),
                            )
                        },
                        label = { Text(location) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.FilterList,
                    stringResource(R.string.title_filter),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = stringResource(R.string.title_filter),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                ToggleableChip(
                    filterEnabled,
                    stringResource(R.string.filter_enabled),
                ) { filterEnabled = !filterEnabled }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Sort,
                    stringResource(R.string.title_sort_by),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = stringResource(R.string.title_sort_by),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                SortingChip(
                    sortingBy == 0,
                    R.string.sort_by_name,
                ) { sortingBy = 0 }
                SortingChip(
                    sortingBy == 1,
                    R.string.sort_by_location,
                ) { sortingBy = 1 }
                SortingChip(
                    sortingBy == 2,
                    R.string.sort_by_distance,
                    enabled = false,
                ) { sortingBy = 2 }
            }
            Divider()

            AnimatedVisibility(
                visible = filterStations(
                    stations,
                    enabledStations,
                    filterSearch,
                    sortingBy,
                    filterEnabled
                )
                    .isEmpty(),
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    stringResource(R.string.no_stations_found),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            LazyColumn {
                items(
                    filterStations(
                        stations,
                        enabledStations,
                        filterSearch,
                        sortingBy,
                        filterEnabled
                    ),
                ) { station ->
                    var checkboxLoading by remember { mutableStateOf(false) }
                    if (!filterLocations.contains(station.location))
                        StationCard(
                            station,
                            modifier = Modifier.clickable(enabled = single) {
                                onStationTapped(station)
                                onSelectStation(station)
                            },
                            checkboxEnabled = !checkboxLoading,
                            checked = enabledStations.find { it.stationUid == station.uid } != null,
                            showCheckbox = !single,
                        ) { enabled ->
                            updatedStations = true
                            checkboxLoading = true
                            val switchJob = if (enabled)
                                viewModel.enableStation(station)
                            else
                                viewModel.disableStation(station)
                            switchJob.invokeOnCompletion {
                                checkboxLoading = false
                            }
                        }
                }
            }
        }
    }

    @Composable
    private fun LoadingPage() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.state_adding),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 24.sp,
                )
            }
        }
    }

    private fun filterStations(
        stations: List<Station>,
        enabledStations: List<SelectedStationEntity>,
        filterSearch: String,
        sortingBy: Int,
        filterEnabled: Boolean,
    ) = stations
        .filterSearch(filterSearch) { it.title }
        .filter { s -> !filterEnabled || enabledStations.find { it.stationUid == s.uid } != null }
        .sortedBy {
            when (sortingBy) {
                0 -> it.name
                1 -> it.location
                else -> it.title
            }
        }

    // Only used by extension activities
    protected open fun onSelectStation(station: Station) {}
}