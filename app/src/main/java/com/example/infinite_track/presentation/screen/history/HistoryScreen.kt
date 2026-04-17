package com.example.infinite_track.presentation.screen.history

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.cards.AttendanceHistoryC
import com.example.infinite_track.presentation.components.cards.OverviewCardAttendance
import com.example.infinite_track.presentation.components.empty.EmptyListAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.tittle.Tittle
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_500

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    // Collect the single UI state from the ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State to control filter visibility
    var showFilters by remember { mutableStateOf(false) }

    // Create a LazyListState to track scrolling
    val lazyListState = rememberLazyListState()
    val scaffoldState = rememberBottomSheetScaffoldState()

    // Check if we should load more data (near end of the list)
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                    lastVisibleItem.index >= uiState.records.size - 5 &&
                    uiState.canLoadMore &&
                    !uiState.isLoadingMore
        }
    }

    // Trigger loadNextPage when we're near the end of the list
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 200.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = Color.White.copy(alpha = 0.5f),
        containerColor = Color.Transparent,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Attendance History",
                    style = headline3,
                )
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Title row with filter icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Attendance Overview title
                    Tittle(tittle = "Attendance Overview")

                    // Filter icon
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Purple_500
                        )
                    }
                }

                // Show filter chips only when showFilters is true
                if (showFilters) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Period filter chips
                    PeriodFilterChips(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { period ->
                            viewModel.onFilterChanged(period)
                            // Optionally hide filters after selection
                            // showFilters = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary card displayed as a LazyRow of overview cards
                uiState.summary?.let { summary ->
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OverviewCardAttendance(
                                title = "On Time",
                                count = summary.totalOntime,
                                unit = "times",
                                onClick = {}
                            )
                        }
                        item {
                            OverviewCardAttendance(
                                title = "Late",
                                count = summary.totalLate,
                                unit = "times",
                                onClick = {}
                            )
                        }
                        item {
                            OverviewCardAttendance(
                                title = "Absent",
                                count = summary.totalAlpha,
                                unit = "times",
                                onClick = {}
                            )
                        }
                        item {
                            OverviewCardAttendance(
                                title = "WFO",
                                count = summary.totalWfo,
                                unit = "times",
                                onClick = {}
                            )
                        }
                        item {
                            OverviewCardAttendance(
                                title = "WFA",
                                count = summary.totalWfa,
                                unit = "times",
                                onClick = {}
                            )
                        }
                    }
                }
            }
        },
        // KONTEN DI DALAM BOTTOM SHEET
        sheetContent = {
            // Konten LazyColumn untuk riwayat absensi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .defaultMinSize(minHeight = 200.dp)
            ) {
                // Initial loading state
                if (uiState.isLoading && uiState.records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingAnimation()
                    }
                }
                // Error state
                else if (uiState.error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyListAnimation(modifier = Modifier.size(150.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = headline4,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Content state
                else {
                    if (uiState.records.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                EmptyListAnimation(modifier = Modifier.size(150.dp))
                                Text(
                                    text = "No attendance records found",
                                    style = headline4
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                        ) {
                            item {
                                Tittle(tittle = "Attendance Summary")
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(uiState.records) { record ->
                                AttendanceHistoryC(record = record)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingAnimation(
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodFilterChips(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf(
        "daily" to "Daily",
        "weekly" to "Weekly",
        "monthly" to "Monthly",
        "all" to "All"
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (periodValue, periodLabel) ->
            FilterChip(
                selected = selectedPeriod == periodValue,
                onClick = { onPeriodSelected(periodValue) },
                label = { Text(periodLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Blue_500,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
