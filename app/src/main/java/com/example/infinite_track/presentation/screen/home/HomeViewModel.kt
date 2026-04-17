package com.example.infinite_track.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.dashboard.InternshipSummary
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.GetBookingHistoryUseCase
import com.example.infinite_track.domain.use_case.dashboard.GetInternshipDashboardDataUseCase
import com.example.infinite_track.domain.use_case.history.GetAttendanceHistoryUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
	private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
	private val getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase,
	private val getCurrentAddressUseCase: GetCurrentAddressUseCase,
	private val getInternshipDashboardDataUseCase: GetInternshipDashboardDataUseCase,
	private val getBookingHistoryUseCase: GetBookingHistoryUseCase
) : ViewModel() {

	// User profile state
	private val _userProfileState = MutableStateFlow<UserModel?>(null)
	val userProfileState: StateFlow<UserModel?> = _userProfileState

	// Top attendance history state for Employee/Manager
	private val _topAttendanceHistoryState =
		MutableStateFlow<UiState<List<AttendanceRecord>>>(UiState.Loading)
	val topAttendanceHistoryState: StateFlow<UiState<List<AttendanceRecord>>> =
		_topAttendanceHistoryState

	// Location state
	private val _currentAddressState = MutableStateFlow("Loading...")
	val currentAddressState: StateFlow<String> = _currentAddressState

	// Internship Dashboard Summary state - simplified to just hold the data or null
	private val _internshipSummaryState = MutableStateFlow<InternshipSummary?>(null)
	val internshipSummaryState: StateFlow<InternshipSummary?> = _internshipSummaryState

	// Dummy leave data for Employee/Manager
	private val _annualBalance = MutableStateFlow(12)
	val annualBalance: StateFlow<Int> = _annualBalance

	private val _annualUsed = MutableStateFlow(5)
	val annualUsed: StateFlow<Int> = _annualUsed

	// Booking history state for WFA
	private val _bookingHistoryState =
		MutableStateFlow<UiState<List<BookingHistoryItem>>>(UiState.Loading)
	val bookingHistoryState: StateFlow<UiState<List<BookingHistoryItem>>> = _bookingHistoryState

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
		loadDummyLeaveData()
		fetchInternshipDashboardData()
		fetchTopBookingHistory()
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

			getAttendanceHistoryUseCase(
				period = "monthly",
				page = 1,
				limit = 5
			).onSuccess { historyPage ->
				_topAttendanceHistoryState.value = UiState.Success(historyPage.records)
			}.onFailure { error ->
				_topAttendanceHistoryState.value =
					UiState.Error(error.message ?: "Unknown error occurred")
			}
		}
	}

	private fun fetchCurrentAddress() {
		viewModelScope.launch {
			getCurrentAddressUseCase().onSuccess { address ->
				_currentAddressState.value = address
			}.onFailure {
				_currentAddressState.value = "Unable to fetch location"
			}
		}
	}

	private fun loadDummyLeaveData() {
		// These values are already set in the StateFlow initialization
		// This function is included for clarity and future expansion
		_annualBalance.value = 12
		_annualUsed.value = 5
	}

	private fun fetchInternshipDashboardData() {
		viewModelScope.launch {
			getInternshipDashboardDataUseCase().onSuccess { summary ->
				_internshipSummaryState.value = summary
			}.onFailure {
				// On failure, just leave the state as null
				_internshipSummaryState.value = null
			}
		}
	}

	private fun fetchTopBookingHistory() {
		viewModelScope.launch {
			_bookingHistoryState.value = UiState.Loading
			try {
				val result = getBookingHistoryUseCase(
					status = null, // null means all
					page = 1,
					limit = 3
				)
				result.onSuccess { bookingPage ->
					_bookingHistoryState.value = UiState.Success(bookingPage.bookings)
				}.onFailure { exception ->
					_bookingHistoryState.value =
						UiState.Error(exception.message ?: "Unknown error occurred")
				}
			} catch (e: Exception) {
				_bookingHistoryState.value = UiState.Error(e.message ?: "Unknown error occurred")
			}
		}
	}

	/**
	 * Function to refresh the internship dashboard data
	 * Can be called when user performs a pull-to-refresh or after check-in/check-out
	 */
	fun refreshInternshipDashboard() {
		fetchInternshipDashboardData()
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
