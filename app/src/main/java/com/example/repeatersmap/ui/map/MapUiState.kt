package com.example.repeatersmap.ui.map

import com.example.repeatersmap.data.model.RepeaterItem
import org.maplibre.spatialk.geojson.Position

data class RepeaterFilters(
    val working: Boolean = true,
    val stopped: Boolean = true,
    val planned: Boolean = true,
    val testing: Boolean = true,
    val building: Boolean = true,
    val unverified: Boolean = true,
    val cm23: Boolean = true,
    val cm70: Boolean = true,
    val m2: Boolean = true,
    val m4: Boolean = true,
    val m6: Boolean = true,
    val m10: Boolean = true
)

data class MapUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allRepeaters: List<RepeaterItem> = emptyList(),
    val selectedRepeater: RepeaterItem? = null,
    val userPosition: Position? = null,
    val filters: RepeaterFilters = RepeaterFilters(),
    val repeatersGeoJson: String = "{\"type\":\"FeatureCollection\",\"features\":[]}"
)

sealed interface MapUiEvent {
    data class ShowToast(val message: String) : MapUiEvent
    data class AnimateCamera(val position: Position, val zoom: Double = 12.0) : MapUiEvent
    data object RequestLocationPermission : MapUiEvent
}
