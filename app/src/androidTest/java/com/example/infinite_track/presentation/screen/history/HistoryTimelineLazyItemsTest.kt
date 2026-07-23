package com.example.infinite_track.presentation.screen.history

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.data.mapper.attendance.toReportDateLabel
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryTimelineLazyItemsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recordsAreIndependentOuterLazyItemsAndLastRecordCanScrollIntoView() {
        val records = (1..20).map { index ->
            attendanceRecord(id = index, date = index.toString())
        }
        composeRule.setContent {
            Infinite_TrackTheme {
                LazyColumn(Modifier.testTag("history-list")) {
                    attendanceHistoryTimelineItems(
                        records = records,
                        focusByKey = emptyMap(),
                        timelineProgress = 0f,
                        motionEnabled = true
                    )
                }
            }
        }

        composeRule.onNodeWithText(records.last().toReportDateLabel())
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals("history-20", historyItemKey(records.last().id))
    }

    private fun attendanceRecord(id: Int, date: String) = AttendanceRecord(
        id = id,
        date = date,
        monthYear = "July 2026",
        timeIn = "08:00",
        timeOut = "17:00",
        workHour = "9h"
    )
}
