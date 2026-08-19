package systems.madi.repeatersmap.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import systems.madi.repeatersmap.data.model.RepeaterItem

class RepeaterRepositoryTest {

    @Test
    fun `test fallback maidenhead coordinates injected successfully`() {
        val mockJson = """
            [
                {
                    "callsign": "SR1VALID",
                    "coordinates": [52.0, 21.0],
                    "locator": "KO02"
                },
                {
                    "callsign": "SR2MISSING",
                    "coordinates": [],
                    "locator": "KO02"
                },
                {
                    "callsign": "SR3ZERO",
                    "coordinates": [0.0, 0.0],
                    "locator": "JO92"
                }
            ]
        """.trimIndent()

        val parsedList = RepeaterRepository.parseRepeatersJson(mockJson)

        assertEquals(3, parsedList.size)

        // 1. SR1VALID shouldn't be modified because it has valid coordinates
        val validItem = parsedList.find { it.callsign == "SR1VALID" }!!
        assertTrue(validItem.hasValidCoordinates)
        assertEquals(52.0, validItem.coordinates[0], 0.0001)

        // 2. SR2MISSING should have coordinates injected via KO02 (Lat 52.5, Lon 21.0)
        val missingItem = parsedList.find { it.callsign == "SR2MISSING" }!!
        assertTrue(missingItem.hasValidCoordinates)
        assertEquals(52.5, missingItem.coordinates[0], 0.0001)
        assertEquals(21.0, missingItem.coordinates[1], 0.0001)

        // 3. SR3ZERO should have coordinates injected via JO92 (Lat 52.5, Lon 19.0)
        val zeroItem = parsedList.find { it.callsign == "SR3ZERO" }!!
        assertTrue(zeroItem.hasValidCoordinates)
        assertEquals(52.5, zeroItem.coordinates[0], 0.0001)
        assertEquals(19.0, zeroItem.coordinates[1], 0.0001)
    }

    @Test
    fun `test invalid maidenhead falls back gracefully`() {
        val mockJson = """
            [
                {
                    "callsign": "SR4BAD",
                    "coordinates": [],
                    "locator": "INVALID"
                }
            ]
        """.trimIndent()

        val parsedList = RepeaterRepository.parseRepeatersJson(mockJson)
        val badItem = parsedList[0]

        // fallback should fail and leave it empty, but it's still flagged as having no valid coordinates
        assertFalse(badItem.hasValidCoordinates)
        assertTrue(badItem.coordinates.isEmpty())
    }
}
