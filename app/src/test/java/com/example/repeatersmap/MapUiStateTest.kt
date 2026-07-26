package com.example.repeatersmap

import com.example.repeatersmap.data.model.RepeaterItem
import com.example.repeatersmap.ui.map.MapUiState
import com.example.repeatersmap.ui.map.RepeaterFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUiStateTest {

    @Test
    fun `test default MapUiState has loading true and all filters enabled`() {
        val state = MapUiState()
        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.allRepeaters.isEmpty())
        assertTrue(state.filters.working)
        assertTrue(state.filters.cm23)
    }

    @Test
    fun `test toggling filter in RepeaterFilters updates state correctly`() {
        var filters = RepeaterFilters()
        assertTrue(filters.working)
        
        filters = filters.copy(working = !filters.working)
        assertFalse(filters.working)
    }

    @Test
    fun `test selected repeater in state updates properly`() {
        val repeater = RepeaterItem(
            callsign = "SR1A",
            status = "working",
            coordinates = listOf(52.0, 19.0)
        )
        val state = MapUiState(allRepeaters = listOf(repeater), selectedRepeater = repeater)
        assertEquals("SR1A", state.selectedRepeater?.callsign)
    }

    @Test
    fun `test default repeatersGeoJson is a valid empty FeatureCollection and not empty string`() {
        val state = MapUiState()
        // avoid maplibre opengl crash on empty string
        assertTrue(state.repeatersGeoJson.isNotEmpty())
        assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", state.repeatersGeoJson)
    }
}
