package systems.madi.repeatersmap.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Context
import android.os.Build
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import systems.madi.repeatersmap.R
import systems.madi.repeatersmap.data.model.RepeaterItem
import systems.madi.repeatersmap.data.repository.RepeaterRepository
import systems.madi.repeatersmap.ui.components.CustomInfoWindow
import systems.madi.repeatersmap.ui.components.ElevationProfileGraph
import systems.madi.repeatersmap.ui.components.FilteringButton
import systems.madi.repeatersmap.ui.components.fadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.and
import org.maplibre.compose.expressions.dsl.any
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.gt
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.interpolate
import org.maplibre.compose.expressions.dsl.linear
import org.maplibre.compose.expressions.dsl.lt
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.expressions.value.BooleanValue
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.rememberOfflineManager
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson

private var activeToast: Toast? = null

private fun showSingleToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    activeToast?.cancel()
    activeToast = Toast.makeText(context.applicationContext, message, duration)
    activeToast?.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val antennaIcon = painterResource(R.drawable.antenna_icon)

    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 52.0, longitude = 19.0),
            zoom = 6.0
        )
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is MapUiEvent.ShowToast -> showSingleToast(context, event.message, Toast.LENGTH_LONG)
                is MapUiEvent.AnimateCamera -> camera.animateTo(CameraPosition(target = event.position, zoom = event.zoom))
                is MapUiEvent.RequestLocationPermission -> {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    val offlineManager = rememberOfflineManager()

    LaunchedEffect(Unit) {
        try {
            if (offlineManager.packs.isEmpty()) {
                val polandBounds = BoundingBox(
                    west = 14.1,
                    south = 49.0,
                    east = 24.1,
                    north = 54.9
                )
                val polandPack = offlineManager.create(
                    definition = OfflinePackDefinition.TilePyramid(
                        styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                        bounds = polandBounds,
                        minZoom = 5,
                        maxZoom = 12
                    )
                )
                offlineManager.resume(polandPack)
            }
        } catch (e: Exception) {
            Log.e("MAP_OFFLINE", "Error initializing offline map pack", e)
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        viewModel.onMyLocationClicked(hasFine || hasCoarse)
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }

                ExtendedFloatingActionButton(
                    text = { Text("Filters") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = { showBottomSheet = true }
                )
            }
        }
    ) { innerPadding ->
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Repeaters Status",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadingEdge(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { FilteringButton(uiState.filters.working, "working") { viewModel.updateFilters { it.copy(working = !it.working) } } }
                        item { FilteringButton(uiState.filters.stopped, "turned off") { viewModel.updateFilters { it.copy(stopped = !it.stopped) } } }
                        item { FilteringButton(uiState.filters.planned, "planned") { viewModel.updateFilters { it.copy(planned = !it.planned) } } }
                        item { FilteringButton(uiState.filters.testing, "testing") { viewModel.updateFilters { it.copy(testing = !it.testing) } } }
                        item { FilteringButton(uiState.filters.building, "being built") { viewModel.updateFilters { it.copy(building = !it.building) } } }
                        item { FilteringButton(uiState.filters.unverified, "unverified") { viewModel.updateFilters { it.copy(unverified = !it.unverified) } } }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadingEdge(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { FilteringButton(uiState.filters.cm23, "23cm") { viewModel.updateFilters { it.copy(cm23 = !it.cm23) } } }
                        item { FilteringButton(uiState.filters.cm70, "70cm") { viewModel.updateFilters { it.copy(cm70 = !it.cm70) } } }
                        item { FilteringButton(uiState.filters.m2, "2m") { viewModel.updateFilters { it.copy(m2 = !it.m2) } } }
                        item { FilteringButton(uiState.filters.m4, "4m") { viewModel.updateFilters { it.copy(m4 = !it.m4) } } }
                        item { FilteringButton(uiState.filters.m6, "6m") { viewModel.updateFilters { it.copy(m6 = !it.m6) } } }
                        item { FilteringButton(uiState.filters.m10, "10m") { viewModel.updateFilters { it.copy(m10 = !it.m10) } } }
                    }
                }
            }
        }
        Box(modifier = modifier.padding(innerPadding)) {
            MaplibreMap(
                cameraState = camera,
                options = MapOptions(
                    gestureOptions = GestureOptions(
                        isRotateEnabled = false,
                        isTiltEnabled = false
                    )
                ),
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty")
            ) {
                // cache geojson parsing to prevent map flickering
                val geoJsonData = remember(uiState.repeatersGeoJson) {
                    GeoJsonData.JsonString(uiState.repeatersGeoJson)
                }
                val repeaterSource = rememberGeoJsonSource(
                    data = geoJsonData
                )

                // cache filters to prevent continuous re-evaluation
                val finalFilter = remember(uiState.filters) {
                    val statusFilters = mutableListOf<Expression<BooleanValue>>().apply {
                        if (uiState.filters.working) add(feature["status"].asString().eq(const("working")))
                        if (uiState.filters.stopped) add(feature["status"].asString().eq(const("off")))
                        if (uiState.filters.planned) add(feature["status"].asString().eq(const("planned")))
                        if (uiState.filters.testing) add(feature["status"].asString().eq(const("testing")))
                        if (uiState.filters.building) add(feature["status"].asString().eq(const("inprogress")))
                        if (uiState.filters.unverified) add(feature["status"].asString().eq(const("unverified")))
                    }

                    val frequencyFilters = mutableListOf<Expression<BooleanValue>>().apply {
                        if (uiState.filters.cm23) {
                            add(feature["tx_freq"].asNumber().gt(const(1240.0f)).and(feature["tx_freq"].asNumber().lt(const(1300.0f))).and(feature["rx_freq"].asNumber().gt(const(1240.0f)).and(feature["rx_freq"].asNumber().lt(const(1300.0f)))))
                        }
                        if (uiState.filters.cm70) {
                            add(feature["tx_freq"].asNumber().gt(const(420.0f)).and(feature["tx_freq"].asNumber().lt(const(450.0f))).and(feature["rx_freq"].asNumber().gt(const(420.0f)).and(feature["rx_freq"].asNumber().lt(const(450.0f)))))
                        }
                        if (uiState.filters.m2) {
                            add(feature["tx_freq"].asNumber().gt(const(144.0f)).and(feature["tx_freq"].asNumber().lt(const(146.0f))).and(feature["rx_freq"].asNumber().gt(const(144.0f)).and(feature["rx_freq"].asNumber().lt(const(146.0f)))))
                        }
                        if (uiState.filters.m4) {
                            add(feature["tx_freq"].asNumber().gt(const(70.0f)).and(feature["tx_freq"].asNumber().lt(const(70.5f))).and(feature["rx_freq"].asNumber().gt(const(70.0f)).and(feature["rx_freq"].asNumber().lt(const(70.5f)))))
                        }
                        if (uiState.filters.m6) {
                            add(feature["tx_freq"].asNumber().gt(const(50.0f)).and(feature["tx_freq"].asNumber().lt(const(52.0f))).and(feature["rx_freq"].asNumber().gt(const(50.0f)).and(feature["rx_freq"].asNumber().lt(const(52.0f)))))
                        }
                        if (uiState.filters.m10) {
                            add(feature["tx_freq"].asNumber().gt(const(28.0f)).and(feature["tx_freq"].asNumber().lt(const(29.7f))).and(feature["rx_freq"].asNumber().gt(const(28.0f)).and(feature["rx_freq"].asNumber().lt(const(29.7f)))))
                        }
                    }
                    
                    if (statusFilters.isEmpty() || frequencyFilters.isEmpty()) {
                        const(false)
                    } else {
                        all(
                            any(*statusFilters.toTypedArray()),
                            any(*frequencyFilters.toTypedArray())
                        )
                    }
                }

                CircleLayer(
                    id = "repeaters-circle-fallback",
                    source = repeaterSource,
                    color = const(Color(0xFF1976D2)),
                    radius = const(5.dp),
                    filter = finalFilter
                )

                SymbolLayer(
                    id = "working-repeaters",
                    source = repeaterSource,
                    iconImage = image(antennaIcon),
                    iconSize = const(2.0f),
                    filter = finalFilter,
                    iconAllowOverlap = const(true),
                    iconIgnorePlacement = const(true),
                    onClick = { features ->
                        val name = features.firstOrNull()?.properties?.get("name")?.jsonPrimitive?.content
                        if (name != null) {
                            viewModel.selectRepeater(name)
                        }
                        ClickResult.Consume
                    },
                    onLongClick = { features ->
                        val name = features.firstOrNull()?.properties?.get("name")?.jsonPrimitive?.content
                        if (name != null) {
                            val repeater = uiState.allRepeaters.find { it.callsign == name }
                            if (repeater != null) {
                                viewModel.calculateElevationProfile(repeater)
                            }
                        }
                        ClickResult.Consume
                    }
                )

                uiState.userPosition?.let { pos ->
                    val userGeoJson = remember(pos) {
                        FeatureCollection(
                            listOf(
                                Feature(
                                    geometry = Point(pos),
                                    properties = buildJsonObject {}
                                )
                            )
                        ).toJson()
                    }
                    val userSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(userGeoJson)
                    )

                    CircleLayer(
                        id = "user-location-halo",
                        source = userSource,
                        color = const(Color(0x332196F3)),
                        radius = interpolate(
                            linear(),
                            zoom(),
                            5 to const(10.dp),
                            12 to const(18.dp)
                        )
                    )

                    CircleLayer(
                        id = "user-location-dot",
                        source = userSource,
                        color = const(Color(0xFF2196F3)),
                        radius = interpolate(
                            linear(),
                            zoom(),
                            5 to const(5.dp),
                            12 to const(8.dp)
                        ),
                        strokeColor = const(Color.White),
                        strokeWidth = interpolate(
                            linear(),
                            zoom(),
                            5 to const(2.dp),
                            12 to const(3.dp)
                        )
                    )
                }
            }

            if (uiState.isLoading) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Loading Repeaters Map",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Parsing offline dataset...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.errorMessage != null || uiState.allRepeaters.isEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Dataset Load Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = uiState.errorMessage ?: "No repeaters found in dataset.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            uiState.selectedRepeater?.let { item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.clearSelectedRepeater() }
                ) {
                    CustomInfoWindow(
                        repeater = item,
                        onClose = { viewModel.clearSelectedRepeater() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (uiState.isElevationLoading) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Calculating Line of Sight.......", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            uiState.elevationProfile?.let { profile ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.clearElevationProfile() }
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 160.dp)
                            .fillMaxWidth()
                            .height(280.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Text(
                                    text = "Line of Sight: ${profile.repeater.callsign}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                
                                ElevationProfileGraph(
                                    points = profile.points,
                                    totalDistanceMeters = profile.totalDistanceMeters,
                                    frequencyMHz = profile.repeater.tx_frequency,
                                    userAntennaHeight = profile.userAntennaHeight,
                                    repeaterAntennaHeight = profile.repeaterAntennaHeight,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                                
                                // antenna height sliders
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(text = "User Ant: ${profile.userAntennaHeight.toInt()}m", fontSize = 10.sp)
                                        androidx.compose.material3.Slider(
                                            value = profile.userAntennaHeight,
                                            onValueChange = { viewModel.updateUserAntennaHeight(it) },
                                            valueRange = 0f..50f
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        Text(text = "Rep Ant: ${profile.repeaterAntennaHeight.toInt()}m", fontSize = 10.sp)
                                        androidx.compose.material3.Slider(
                                            value = profile.repeaterAntennaHeight,
                                            onValueChange = { viewModel.updateRepeaterAntennaHeight(it) },
                                            valueRange = 0f..100f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
