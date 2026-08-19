package systems.madi.repeatersmap.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QthLocatorConverterTest {

    @Test
    fun `test valid 4-character locator JO92`() {
        val result = QthLocatorConverter.convertToCoordinates("JO92")
        
        // JO92 center should be Lat 52.5, Lon 19.0
        // [Latitude, Longitude]
        assertEquals(2, result?.size)
        assertEquals(52.5, result!![0], 0.0001)
        assertEquals(19.0, result[1], 0.0001)
    }

    @Test
    fun `test valid 4-character locator KO02`() {
        val result = QthLocatorConverter.convertToCoordinates("KO02")
        
        // KO02 center should be Lat 52.5, Lon 21.0
        assertEquals(2, result?.size)
        assertEquals(52.5, result!![0], 0.0001)
        assertEquals(21.0, result[1], 0.0001)
    }

    @Test
    fun `test valid 6-character locator KO02MM`() {
        val result = QthLocatorConverter.convertToCoordinates("KO02MM")
        
        // KO02MM center
        // Lat: 52 (square) + 12 * (2.5/60) (subsquare) + (1.25/60) (center) = 52.520833
        // Lon: 20 (field) + 0 (square) + 12 * (5/60) (subsquare) + (2.5/60) (center) = 21.041666
        assertEquals(2, result?.size)
        assertEquals(52.520833, result!![0], 0.0001)
        assertEquals(21.041666, result[1], 0.0001)
    }

    @Test
    fun `test case insensitivity and whitespace`() {
        val result = QthLocatorConverter.convertToCoordinates(" ko02mm ")
        assertEquals(52.520833, result!![0], 0.0001)
        assertEquals(21.041666, result[1], 0.0001)
    }

    @Test
    fun `test invalid odd length string`() {
        val result = QthLocatorConverter.convertToCoordinates("KO0")
        assertNull(result)
    }

    @Test
    fun `test invalid characters`() {
        // Z is out of bounds for field
        assertNull(QthLocatorConverter.convertToCoordinates("ZO02"))
        // Letter where number should be
        assertNull(QthLocatorConverter.convertToCoordinates("KOMA"))
    }

    @Test
    fun `test null or empty`() {
        assertNull(QthLocatorConverter.convertToCoordinates(null))
        assertNull(QthLocatorConverter.convertToCoordinates(""))
        assertNull(QthLocatorConverter.convertToCoordinates("   "))
    }
}
