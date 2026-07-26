package com.example.repeatersmap.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.repeatersmap.data.model.RepeaterItem
import com.example.repeatersmap.data.repository.RepeaterRepository
import com.example.repeatersmap.util.LocationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson

class MapViewModel(
    application: Application,
    private val repository: RepeaterRepository,
    private val locationTracker: LocationTracker
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = RepeaterRepository,
        locationTracker = LocationTracker(application)
    )

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<MapUiEvent>()
    val uiEvents: SharedFlow<MapUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadRepeaters()
    }

    private fun loadRepeaters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val repeaters = withContext(Dispatchers.IO) {
                    repository.loadRepeatersFromAssets(getApplication())
                }.filter { it.isValid }

                val geoJson = withContext(Dispatchers.Default) {
                    computeGeoJson(repeaters)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allRepeaters = repeaters,
                        repeatersGeoJson = geoJson,
                        errorMessage = if (repeaters.isEmpty()) "No repeater data found in asset dataset." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to parse repeater dataset."
                    )
                }
            }
        }
    }

    private fun computeGeoJson(repeaters: List<RepeaterItem>): String {
        val features = repeaters.mapNotNull { item ->
            if (item.hasValidCoordinates) {
                Feature(
                    geometry = Point(Position(longitude = item.coordinates[1], latitude = item.coordinates[0])),
                    properties = buildJsonObject {
                        put("name", item.callsign)
                        put("tx_freq", item.tx_frequency)
                        put("rx_freq", item.rx_frequency)
                        put("status", item.status)
                    }
                )
            } else null
        }
        return FeatureCollection(features).toJson()
    }

    fun selectRepeater(callsign: String) {
        val repeater = _uiState.value.allRepeaters.find { it.callsign == callsign }
        _uiState.update { it.copy(selectedRepeater = repeater) }
    }

    fun clearSelectedRepeater() {
        _uiState.update { it.copy(selectedRepeater = null) }
    }

    fun onMyLocationClicked(hasPermission: Boolean) {
        if (hasPermission) {
            fetchUserLocation()
        } else {
            viewModelScope.launch {
                _uiEvents.emit(MapUiEvent.RequestLocationPermission)
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            fetchUserLocation()
        } else {
            viewModelScope.launch {
                _uiEvents.emit(MapUiEvent.ShowToast("Location permission denied. Please enable location in Android Settings."))
            }
        }
    }

    fun fetchUserLocation() {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                _uiState.update { it.copy(userPosition = location) }
                _uiEvents.emit(MapUiEvent.AnimateCamera(position = location, zoom = 12.0))
            } else {
                _uiEvents.emit(MapUiEvent.ShowToast("Location unavailable. Please check if GPS/location services are enabled."))
            }
        }
    }

    fun updateFilters(transform: (RepeaterFilters) -> RepeaterFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
    }
}
