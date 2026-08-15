package systems.madi.repeatersmap

import android.location.LocationManager
import systems.madi.repeatersmap.util.LocationTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationProviderTest {

    @Test
    fun `test fine permission with GPS sensor enabled selects GPS`() {
        val provider = LocationTracker.selectProvider(
            hasFine = true,
            hasCoarse = true,
            gpsEnabled = true,
            fusedEnabled = true,
            networkEnabled = true
        )
        assertEquals(LocationManager.GPS_PROVIDER, provider)
    }

    @Test
    fun `test coarse permission only selects Fused when available without using GPS`() {
        val provider = LocationTracker.selectProvider(
            hasFine = false,
            hasCoarse = true,
            gpsEnabled = true,
            fusedEnabled = true,
            networkEnabled = true
        )
        assertEquals("fused", provider)
    }

    @Test
    fun `test coarse permission falls back to Network when Fused is disabled`() {
        val provider = LocationTracker.selectProvider(
            hasFine = false,
            hasCoarse = true,
            gpsEnabled = true,
            fusedEnabled = false,
            networkEnabled = true
        )
        assertEquals(LocationManager.NETWORK_PROVIDER, provider)
    }

    @Test
    fun `test fine permission with GPS sensor disabled falls back to Fused`() {
        val provider = LocationTracker.selectProvider(
            hasFine = true,
            hasCoarse = true,
            gpsEnabled = false,
            fusedEnabled = true,
            networkEnabled = true
        )
        assertEquals("fused", provider)
    }

    @Test
    fun `test no location permissions granted returns null provider`() {
        val provider = LocationTracker.selectProvider(
            hasFine = false,
            hasCoarse = false,
            gpsEnabled = true,
            fusedEnabled = true,
            networkEnabled = true
        )
        assertNull(provider)
    }

    @Test
    fun `test all providers disabled in system settings returns null provider`() {
        val provider = LocationTracker.selectProvider(
            hasFine = true,
            hasCoarse = true,
            gpsEnabled = false,
            fusedEnabled = false,
            networkEnabled = false
        )
        assertNull(provider)
    }
}
