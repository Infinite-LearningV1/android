package com.example.infinite_track.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.GetBookingHistoryUseCase
import com.example.infinite_track.domain.use_case.history.GetAttendanceHistoryUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeTodayStatusUiState(
	val status: UiState<TodayStatus> = UiState.Loading,
	val warningMessage: String? = null,
	val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
	private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
	private val getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase,
	private val getCurrentAddressUseCase: GetCurrentAddressUseCase,
	private val getBookingHistoryUseCase: GetBookingHistoryUseCase,
	private val getTodayStatusUseCase: GetTodayStatusUseCase
) : ViewModel() {

	// User profile state
	private val _userProfileState = MutableStateFlow<UserModel?>(null)
	val userProfileState: StateFlow<UserModel?> = _userProfileState

	// Top attendance history state for Employee/Manager
	private val _topAttendanceHistoryState =
		MutableStateFlow<UiState<List<AttendanceRecord>>>(UiState.Loading)
	val topAttendanceHistoryState: StateFlow<UiState<List<AttendanceRecord>>> =
		_topAttendanceHistoryState

	private val _topAttendanceSummaryState =
		MutableStateFlow<UiState<AttendanceSummaryInfo>>(UiState.Loading)
	val topAttendanceSummaryState: StateFlow<UiState<AttendanceSummaryInfo>> =
		_topAttendanceSummaryState

	private val _topAttendancePeriodInfo = MutableStateFlow<AttendancePeriodInfo?>(null)
	val topAttendancePeriodInfo: StateFlow<AttendancePeriodInfo?> = _topAttendancePeriodInfo

	// Location state
	private val _currentAddressState = MutableStateFlow("Loading...")
	val currentAddressState: StateFlow<String> = _currentAddressState

	private val _todayStatusState = MutableStateFlow(HomeTodayStatusUiState())
	val todayStatusState: StateFlow<HomeTodayStatusUiState> = _todayStatusState

	// Detailed booking history state for legacy DetailsMyBooking route; WFA tab owns the primary booking history UI.
	// Detailed booking history state for DetailsMyBooking screen
	data class BookingHistoryDetailsState(
		val isLoading: Boolean = true,
		val bookings: List<BookingHistoryItem> = emptyList(),
		val error: String? = null,
		val selectedStatus: String? = null,
		val currentPage: Int = 1,
		val canLoadMore: Boolean = true,
		val sortBy: String = "created_at",
		val sortOrder: String = "DESC"
	)

	private val _bookingHistoryDetailsState = MutableStateFlow(BookingHistoryDetailsState())
	val bookingHistoryDetailsState: StateFlow<BookingHistoryDetailsState> =
		_bookingHistoryDetailsState

	init {
		fetchUserProfile()
		fetchTopAttendanceHistory()
		fetchCurrentAddress()
		fetchTodayStatus(forceRefresh = false)
	}

	private fun fetchUserProfile() {
		viewModelScope.launch {
			getLoggedInUserUseCase().collect { user ->
				_userProfileState.value = user
			}
		}
	}

	private fun fetchTopAttendanceHistory() {
		viewModelScope.launch {
			_topAttendanceHistoryState.value = UiState.Loading
			_topAttendanceSummaryState.value = UiState.Loading

			getAttendanceHistoryUseCase(
				period = AttendancePeriod.MONTHLY,
				page = 1,
				limit = 3
			).onSuccess { historyPage ->
				_topAttendanceHistoryState.value = UiState.Success(historyPage.records.take(3))
				_topAttendanceSummaryState.value = UiState.Success(historyPage.summary)
				_topAttendancePeriodInfo.value = historyPage.period
			}.onFailure { error ->
				val message = error.message ?: "Unknown error occurred"
				_topAttendanceHistoryState.value = UiState.Error(message)
				_topAttendanceSummaryState.value = UiState.Error(message)
			}
		}
	}

	private fun fetchCurrentAddress() {
		viewModelScope.launch {
			when (val result = getCurrentAddressUseCase()) {
				is AddressResolutionResult.Resolved ->
					_currentAddressState.value = result.address.formattedAddress
				is AddressResolutionResult.CoordinateOnly ->
					_currentAddressState.value = result.coordinate.toDisplayText()
				is AddressResolutionResult.Failed ->
					_currentAddressState.value = "Unable to fetch location"
			}
		}
	}

	private fun com.example.infinite_track.domain.model.location.GeoCoordinate.toDisplayText(): String =
		"Lat: %.6f, Lng: %.6f".format(java.util.Locale.US, latitude, longitude)

	fun fetchTodayStatus(forceRefresh: Boolean = false) {
		viewModelScope.launch {
			val currentState = _todayStatusState.value
			val hasExistingStatus = currentState.status is UiState.Success

			_todayStatusState.value = when {
				forceRefresh && hasExistingStatus -> currentState.copy(
					warningMessage = null,
					isRefreshing = true
				)

				else -> HomeTodayStatusUiState(status = UiState.Loading)
			}

			getTodayStatusUseCase(forceRefresh = forceRefresh)
				.onSuccess { todayStatus ->
					_todayStatusState.value = HomeTodayStatusUiState(
						status = UiState.Success(todayStatus)
					)
				}
				.onFailure { error ->
					val message = error.message ?: "Unable to refresh today's attendance status"
					_todayStatusState.value = if (hasExistingStatus) {
						currentState.copy(
							warningMessage = message,
							isRefreshing = false
						)
					} else {
						HomeTodayStatusUiState(status = UiState.Error(message))
					}
				}
		}
	}

	fun refreshDashboard(forceRefresh: Boolean = true) {
		fetchTodayStatus(forceRefresh = forceRefresh)
		fetchTopAttendanceHistory()
		fetchCurrentAddress()
	}

	/**
	 * Function to refresh the attendance history data
	 * Can be called when user performs a pull-to-refresh
	 */
	fun refreshAttendanceHistory() {
		fetchTopAttendanceHistory()
	}

	// ===== Detailed Booking History Functions =====

	/**
	 * Function to handle status filter changes in DetailsMyBooking screen
	 */
	fun onBookingStatusFilterChanged(newStatus: String) {
		viewModelScope.launch {
			val statusParam = if (newStatus == "all") null else newStatus

			// Reset state with new filter
			_bookingHistoryDetailsState.value = _bookingHistoryDetailsState.value.copy(
				selectedStatus = statusParam,
				currentPage = 1,
				bookings = emptyList(),
				isLoading = true,
				error = null,
				canLoadMore = true
			)

			// Load first page with new filter
			loadBookingHistory(status = statusParam, page = 1)
		}
	}

	/**
	 * Function to handle sorting changes
	 */
	fun onBookingSortingChanged(sortBy: String, sortOrder: String) {
		viewModelScope.launch {
			val currentState = _bookingHistoryDetailsState.value

			// Reset state with new sorting
			_bookingHistoryDetailsState.value = currentState.copy(
				sortBy = sortBy,
				sortOrder = sortOrder,
				currentPage = 1,
				bookings = emptyList(),
				isLoading = true,
				error = null,
				canLoadMore = true
			)

			// Load first page with new sorting
			loadBookingHistory(
				status = currentState.selectedStatus,
				page = 1,
				sortBy = sortBy,
				sortOrder = sortOrder
			)
		}
	}

	/**
	 * Function to load more bookings for infinite scroll
	 */
	fun loadMoreBookings() {
		val currentState = _bookingHistoryDetailsState.value
		if (!currentState.isLoading && currentState.canLoadMore) {
			val nextPage = currentState.currentPage + 1
			loadBookingHistory(
				status = currentState.selectedStatus,
				page = nextPage,
				sortBy = currentState.sortBy,
				sortOrder = currentState.sortOrder,
				appendToExisting = true
			)
		}
	}

	fun retryBookingHistoryLoad() {
		val currentState = _bookingHistoryDetailsState.value
		if (currentState.isLoading) return

		val shouldRetryNextPage = currentState.bookings.isNotEmpty() && currentState.error != null
		val retryPage = if (shouldRetryNextPage) currentState.currentPage + 1 else 1

		loadBookingHistory(
			status = currentState.selectedStatus,
			page = retryPage,
			sortBy = currentState.sortBy,
			sortOrder = currentState.sortOrder,
			appendToExisting = shouldRetryNextPage
		)
	}

	/**
	 * Function to initialize detailed booking history (call when entering DetailsMyBooking screen)
	 */
	fun initializeDetailedBookingHistory() {
		val currentState = _bookingHistoryDetailsState.value
		if (currentState.bookings.isEmpty() && !currentState.isLoading) {
			loadBookingHistory(
				status = currentState.selectedStatus,
				page = 1,
				sortBy = currentState.sortBy,
				sortOrder = currentState.sortOrder
			)
		}
	}

	/**
	 * Private helper function to load booking history with filtering and pagination
	 */
	private fun loadBookingHistory(
		status: String? = null,
		page: Int = 1,
		sortBy: String = "created_at",
		sortOrder: String = "DESC",
		appendToExisting: Boolean = false
	) {
		viewModelScope.launch {
			val currentState = _bookingHistoryDetailsState.value

			// Set loading state
			_bookingHistoryDetailsState.value = currentState.copy(
				isLoading = true,
				error = null
			)

			try {
				val result = getBookingHistoryUseCase(
					status = status,
					page = page,
					limit = 10,
					sortBy = sortBy,
					sortOrder = sortOrder
				)

				result.onSuccess { pageData ->
					val newBookings = pageData.bookings
					val updatedBookings = if (appendToExisting) {
						currentState.bookings + newBookings
					} else {
						newBookings
					}

					_bookingHistoryDetailsState.value = currentState.copy(
						isLoading = false,
						bookings = updatedBookings,
						error = null,
						currentPage = page,
						canLoadMore = pageData.hasNextPage
					)
				}.onFailure { exception ->
					_bookingHistoryDetailsState.value = currentState.copy(
						isLoading = false,
						error = exception.message ?: "Unknown error occurred"
					)
				}
			} catch (e: Exception) {
				_bookingHistoryDetailsState.value = currentState.copy(
					isLoading = false,
					error = e.message ?: "Unknown error occurred"
				)
			}
		}
	}
}
