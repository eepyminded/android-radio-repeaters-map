package systems.madi.repeatersmap.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import systems.madi.repeatersmap.data.model.RepeaterItem
import systems.madi.repeatersmap.data.repository.RepeaterRepository
import systems.madi.repeatersmap.util.LocationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
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
import systems.madi.repeatersmap.data.repository.ElevationRepository

class MapViewModel(
    application: Application,
    private val repository: RepeaterRepository,
    private val locationTracker: LocationTracker,
    private val elevationRepository: ElevationRepository = ElevationRepository()
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = RepeaterRepository,
        locationTracker = LocationTracker(application),
        elevationRepository = ElevationRepository()
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
                        if (item.country_code != null) {
                            put("country_code", item.country_code)
                        }
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

    private var locationUpdatesJob: kotlinx.coroutines.Job? = null

    fun fetchUserLocation() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            var isFirstUpdate = true
            var receivedUpdate = false
            
            // first attempt an immediate fetch so the UI doesnt hang waiting for the first GPS fix
            val quickFix = locationTracker.getCurrentLocation()
            if (quickFix != null) {
                receivedUpdate = true
                _uiState.update { it.copy(userPosition = quickFix) }
                _uiEvents.emit(MapUiEvent.AnimateCamera(position = quickFix, zoom = 12.0))
                isFirstUpdate = false
            }

            locationTracker.getLocationFlow()
                .onCompletion {
                    if (!receivedUpdate) {
                        _uiEvents.emit(MapUiEvent.ShowToast("Location unavailable. Please check if GPS/location services are enabled."))
                    }
                }
                .collect { location ->
                    receivedUpdate = true
                    _uiState.update { it.copy(userPosition = location) }
                    if (isFirstUpdate) {
                        _uiEvents.emit(MapUiEvent.AnimateCamera(position = location, zoom = 12.0))
                        isFirstUpdate = false
                    }
                }
        }
    }

    fun updateFilters(transform: (RepeaterFilters) -> RepeaterFilters) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
    }

    fun calculateElevationProfile(repeater: RepeaterItem) {
        val userPos = _uiState.value.userPosition
        if (userPos == null) {
            viewModelScope.launch {
                _uiEvents.emit(MapUiEvent.ShowToast("Please enable GPS first."))
            }
            return
        }

        if (!repeater.hasValidCoordinates) {
            viewModelScope.launch {
                _uiEvents.emit(MapUiEvent.ShowToast("Repeater has no valid coordinates."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isElevationLoading = true, elevationProfile = null) }
            try {
                val repeaterPos = Position(longitude = repeater.coordinates[1], latitude = repeater.coordinates[0])
                val points = elevationRepository.calculatePathPoints(start = userPos, end = repeaterPos, maxPoints = 100)
                val heights = elevationRepository.getElevationProfile(points)
                val totalDistance = elevationRepository.calculateDistanceMeters(userPos, repeaterPos)
                
                _uiState.update { it.copy(
                    isElevationLoading = false,
                    elevationProfile = ElevationProfile(
                        points = heights,
                        repeater = repeater,
                        totalDistanceMeters = totalDistance
                    )
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isElevationLoading = false) }
                _uiEvents.emit(MapUiEvent.ShowToast("Failed to fetch elevation: ${e.message}"))
            }
        }
    }

    fun clearElevationProfile() {
        _uiState.update { it.copy(elevationProfile = null, isElevationLoading = false) }
    }

    fun updateUserAntennaHeight(height: Float) {
        _uiState.update { state ->
            val profile = state.elevationProfile
            if (profile != null) {
                state.copy(elevationProfile = profile.copy(userAntennaHeight = height))
            } else state
        }
    }

    fun updateRepeaterAntennaHeight(height: Float) {
        _uiState.update { state ->
            val profile = state.elevationProfile
            if (profile != null) {
                state.copy(elevationProfile = profile.copy(repeaterAntennaHeight = height))
            } else state
        }
    }
}
