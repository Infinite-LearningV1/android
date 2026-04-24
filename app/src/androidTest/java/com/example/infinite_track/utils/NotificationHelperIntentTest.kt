package com.example.infinite_track.utils

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.main.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperIntentTest {

    @Test
    fun buildLaunchAppIntent_opensMainActivity_withExpectedFlags_withoutAttendanceExtra() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val intent = NotificationHelper.buildLaunchAppIntent(context)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
            intent.flags
        )
        assertFalse(intent.hasExtra("navigate_to_attendance"))
    }
}
