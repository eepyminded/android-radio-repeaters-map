package systems.madi.repeatersmap

import systems.madi.repeatersmap.data.model.RepeaterItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RepeaterItemSerializationTest {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `test parsing valid JSON string into RepeaterItem`() {
        val sampleJson = """
            {
                "callsign": "SR1C",
                "status": "working",
                "tx_frequency": 145.675,
                "rx_frequency": 145.075,
                "coordinates": [15.5833, 54.1667],
                "qth": "Kołobrzeg",
                "locator": "JO74od"
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(sampleJson)

        assertEquals("SR1C", item.callsign)
        assertEquals("working", item.status)
        assertEquals(145.675, item.tx_frequency, 0.0001)
        assertEquals(145.075, item.rx_frequency, 0.0001)
        assertEquals(listOf(15.5833, 54.1667), item.coordinates)
        assertEquals("Kołobrzeg", item.qth)
        assertEquals("JO74od", item.locator)
    }

    @Test
    fun `test parsing JSON with missing optional fields uses default values`() {
        val minimalJson = """
            {
                "callsign": "SR2Z",
                "tx_frequency": 439.200,
                "rx_frequency": 431.600
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(minimalJson)

        assertEquals("SR2Z", item.callsign)
        assertEquals(439.200, item.tx_frequency, 0.0001)
        assertEquals("", item.status)
        assertEquals(emptyList<Double>(), item.coordinates)
        assertNull(item.qth)
        assertNull(item.locator)
    }

    @Test
    fun `test parser ignores unknown JSON keys without crashing`() {
        val jsonWithExtraKeys = """
            {
                "callsign": "SR5W",
                "status": "off",
                "tx_frequency": 145.700,
                "rx_frequency": 145.100,
                "unknown_future_field": "some_value",
                "another_new_tag": 12345
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(jsonWithExtraKeys)
        assertNotNull(item)
        assertEquals("SR5W", item.callsign)
    }

    @Test
    fun `test json with more than two coordinates parses without error`() {
        // checks parsing when coordinates array has more than two numbers
        val jsonWith3DCoords = """
            {
                "callsign": "SR1C",
                "coordinates": [54.1667, 15.5833, 120.5, 0.0]
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(jsonWith3DCoords)

        assertEquals("SR1C", item.callsign)
        assertEquals(4, item.coordinates.size)
        assertEquals(54.1667, item.coordinates[0], 0.0001)
        assertEquals(15.5833, item.coordinates[1], 0.0001)
        assertEquals(120.5, item.coordinates[2], 0.0001)
    }

    @Test
    fun `test json with only names and missing coordinates defaults cleanly`() {
        // checks parsing when json only has name fields without coordinates
        val jsonOnlyNames = """
            {
                "callsign": "SR5W",
                "qth": "Warsaw Center"
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(jsonOnlyNames)

        assertEquals("SR5W", item.callsign)
        assertEquals("Warsaw Center", item.qth)
        assertEquals(emptyList<Double>(), item.coordinates)
        assertEquals(0, item.coordinates.size)
        assertEquals(0.0, item.tx_frequency, 0.0001)
        assertEquals(0.0, item.rx_frequency, 0.0001)
    }

    @Test
    fun `test json with out of bounds coordinates is flagged as invalid`() {
        // checks wacky out of bounds coordinates like latitude 500 or longitude -999
        val jsonWackyCoords = """
            {
                "callsign": "SR9X",
                "coordinates": [500.0, -999.0]
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(jsonWackyCoords)

        assertEquals("SR9X", item.callsign)
        assertEquals(2, item.coordinates.size)
        assertEquals(false, item.hasValidCoordinates)
    }

    @Test
    fun `test json with only one coordinate is flagged as invalid`() {
        // checks wacky single coordinate array missing longitude
        val jsonSingleCoord = """
            {
                "callsign": "SR3Y",
                "coordinates": [52.0]
            }
        """.trimIndent()

        val item: RepeaterItem = jsonParser.decodeFromString(jsonSingleCoord)

        assertEquals("SR3Y", item.callsign)
        assertEquals(1, item.coordinates.size)
        assertEquals(false, item.hasValidCoordinates)
    }

    @Test
    fun `test repeater item with nan or infinite coordinates is flagged as invalid`() {
        // checks validation when programmatically passed nan or infinity values
        val itemNan = RepeaterItem(callsign = "SR0NAN", coordinates = listOf(Double.NaN, 19.0))
        val itemInf = RepeaterItem(callsign = "SR0INF", coordinates = listOf(52.0, Double.POSITIVE_INFINITY))

        assertEquals(false, itemNan.hasValidCoordinates)
        assertEquals(false, itemInf.hasValidCoordinates)
    }

    @Test
    fun `test repeater with invalid coordinates but valid maidenhead locator is valid`() {
        // checks fallback validity when coordinates are missing but maidenhead exists
        val item = RepeaterItem(callsign = "SR1MAIDEN", coordinates = emptyList(), locator = "JO74od")
        assertEquals(false, item.hasValidCoordinates)
        assertEquals(true, item.hasValidLocator)
        assertEquals(true, item.isValid)
    }

    @Test
    fun `test repeater with invalid coordinates and missing maidenhead locator is invalid`() {
        // checks removal condition when neither coordinates nor maidenhead exist
        val item = RepeaterItem(callsign = "SR1INVALID", coordinates = listOf(500.0, -999.0), locator = null)
        assertEquals(false, item.hasValidCoordinates)
        assertEquals(false, item.hasValidLocator)
        assertEquals(false, item.isValid)
    }

    @Test
    fun `test repeater with zero zero placeholder coordinates is flagged as invalid coordinates`() {
        // checks that 0,0 placeholder coordinates are ignored unless maidenhead exists
        val itemZero = RepeaterItem(callsign = "SR0ZERO", coordinates = listOf(0.0, 0.0), locator = null)
        val itemZeroMaiden = RepeaterItem(callsign = "SR0MAIDEN", coordinates = listOf(0.0, 0.0), locator = "JO74od")

        assertEquals(false, itemZero.hasValidCoordinates)
        assertEquals(false, itemZero.isValid)

        assertEquals(false, itemZeroMaiden.hasValidCoordinates)
        assertEquals(true, itemZeroMaiden.isValid)
    }

    @Test
    fun `test actual assets json file parses successfully`() {
        val file = java.io.File("src/main/assets/przemienniki.eu.json")
        
        val jsonString = file.readText()

        val items: List<RepeaterItem> = jsonParser.decodeFromString(jsonString)

        // regression, if 500 or more arevalid its fine to pass
        org.junit.Assert.assertTrue(
            "JSON file should contain at least 500 repeaters (found ${items.size})", 
            items.size > 500
        )
        
        // SR6ACZ needs to persist no matter what,
        val hasSRACZ = items.any { it.callsign == "SR6ACZ" }
        org.junit.Assert.assertTrue("SR6ACZ repeater is there", hasSRACZ)
        
        // if 90% of coords are valid let it pass
        val validCount = items.count { it.isValid }
        val validPercentage = validCount.toDouble() / items.size

        println("total items valid: ${items.size}, percentage valid: ${String.format("%.2f", validPercentage * 100)}%")

        org.junit.Assert.assertTrue(
            "At least 90% of repeaters should be valid (was ${String.format("%.2f", validPercentage * 100)}%)", 
            validPercentage > 0.90
        )
    }
}
