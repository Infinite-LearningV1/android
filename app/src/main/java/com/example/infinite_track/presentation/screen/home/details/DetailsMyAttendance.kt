package com.example.infinite_track.presentation.screen.home.details

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.button.InfiniteTracButtonBack
import com.example.infinite_track.presentation.components.calendar.TodayDateWithFilterHeader
import com.example.infinite_track.presentation.components.cards.AttendanceHistoryC
import com.example.infinite_track.presentation.components.empty.EmptyListAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.screen.history.HistoryViewModel
import com.example.infinite_track.presentation.screen.history.PeriodFilterChips
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsMyAttendance(
	viewModel: HistoryViewModel = hiltViewModel(), // Use the shared HistoryViewModel
	onBackClick: () -> Unit,
) {
	// Collect state from HistoryViewModel
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	var showFilters by remember { mutableStateOf(false) }

	// Create a LazyListState for the attendance list
	val lazyListState = rememberLazyListState()

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		containerColor = Color.Transparent,
		topBar = {
			InfiniteTracButtonBack(
				title = "My Attendance",
				navigationBack = onBackClick,
				modifier = Modifier.padding(top = 12.dp)
			)
		}
	) { innerPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp)
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding)
			) {
				// Header with period filter toggle
				TodayDateWithFilterHeader(
					displayDate = uiState.selectedPeriod.capitalize(),
					onFilterClick = { showFilters = !showFilters }
				)

				// Period filter chips
				if (showFilters) {
					Spacer(modifier = Modifier.height(8.dp))
					PeriodFilterChips(
						selectedPeriod = uiState.selectedPeriod,
						onPeriodSelected = { period ->
							viewModel.onFilterChanged(period)
							showFilters = false
						}
					)
				}

				Spacer(modifier = Modifier.height(16.dp))

				// Display loading, error, or content based on UI state
				when {
					uiState.isLoading && uiState.records.isEmpty() -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							LoadingAnimation()
						}
					}

					uiState.error != null -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
							) {
								EmptyListAnimation(modifier = Modifier.size(150.dp))
								Text(
									text = uiState.error ?: "Unknown error",
									style = headline4,
								)
							}
						}
					}

					uiState.records.isEmpty() -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
							) {
								EmptyListAnimation(modifier = Modifier.size(150.dp))
								Text(
									text = "No attendance data available yet.",
									style = headline4,
								)
							}
						}
					}

					else -> {
						// Replace AttendanceHistoryList with a LazyColumn using AttendanceHistoryC
						LazyColumn(
							state = lazyListState,
							modifier = Modifier
								.weight(1f)
								.fillMaxSize(),
							contentPadding = PaddingValues(bottom = 16.dp)
						) {
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
										LoadingAnimation()
									}
								}
							}
						}
					}
				}
			}
		}
	}
}

// Extension function to capitalize the first letter of a string
private fun String.capitalize(): String {
	return this.replaceFirstChar {
		if (it.isLowerCase()) it.titlecase(Locale.getDefault())
		else it.toString()
	}
}
