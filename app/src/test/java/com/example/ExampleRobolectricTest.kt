package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActivityType
import com.example.data.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies PulseTrack branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseTrack", appName)
    }

    @Test
    fun `activity types have valid MET values and titles`() {
        assertEquals(9.8, ActivityType.RUNNING.metValue, 0.01)
        assertEquals(3.8, ActivityType.WALKING.metValue, 0.01)
        assertEquals(7.5, ActivityType.CYCLING.metValue, 0.01)
        assertEquals(6.0, ActivityType.HIKING.metValue, 0.01)

        val parsed = ActivityType.fromString("CYCLING")
        assertEquals(ActivityType.CYCLING, parsed)
    }

    @Test
    fun `unit conversions operate correctly`() {
        val metric = UnitSystem.METRIC
        val imperial = UnitSystem.IMPERIAL

        assertEquals("km", metric.distanceUnit)
        assertEquals("mi", imperial.distanceUnit)
        assertEquals("min/km", metric.paceUnit)
        assertEquals("min/mi", imperial.paceUnit)
    }
}
