package com.example.repeatersmap.ui.map

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.repeatersmap.R
import com.example.repeatersmap.data.model.RepeaterItem
import com.example.repeatersmap.data.repository.RepeaterRepository
import com.example.repeatersmap.ui.components.CustomInfoWindow
import com.example.repeatersmap.ui.components.FilteringButton
import com.example.repeatersmap.ui.components.fadingEdge
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
import org.maplibre.compose.expressions.dsl.lt
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val antennaIcon = painterResource(R.drawable.antenna_icon)

    var userPosition by remember { mutableStateOf<Position?>(null) }

    var allRepeaters by remember { mutableStateOf<List<RepeaterItem>>(emptyList()) }
    var selectedRepeater by remember { mutableStateOf<RepeaterItem?>(null) }

    var checkedWorking by remember { mutableStateOf(true) }
    var checkedStopped by remember { mutableStateOf(true) }
    var checkedPlanned by remember { mutableStateOf(true) }
    var checkedTesting by remember { mutableStateOf(true) }
    var checkedBuilding by remember { mutableStateOf(true) }
    var checkedUnverified by remember { mutableStateOf(true) }

    var checked23cm by remember { mutableStateOf(true) }
    var checked70cm by remember { mutableStateOf(true) }
    var checked2m by remember { mutableStateOf(true) }
    var checked4m by remember { mutableStateOf(true) }
    var checked6m by remember { mutableStateOf(true) }
    var checked10m by remember { mutableStateOf(true) }

    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 52.0, longitude = 19.0),
            zoom = 6.0
        )
    )

    fun fetchUserLocation() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider == null) {
                    Toast.makeText(context, "Location services (GPS) are disabled.", Toast.LENGTH_SHORT).show()
                    return
                }

                val lastLocation = try {
                    val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    when {
                        gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                        gpsLoc != null -> gpsLoc
                        else -> netLoc
                    }
                } catch (e: SecurityException) {
                    null
                }

                if (lastLocation != null) {
                    val pos = Position(longitude = lastLocation.longitude, latitude = lastLocation.latitude)
                    userPosition = pos
                    coroutineScope.launch {
                        camera.animateTo(CameraPosition(target = pos, zoom = 12.0))
                    }
                } else {
                    LocationManagerCompat.getCurrentLocation(
                        locationManager,
                        provider,
                        CancellationSignal(),
                        ContextCompat.getMainExecutor(context)
                    ) { freshLocation ->
                        if (freshLocation != null) {
                            val pos = Position(longitude = freshLocation.longitude, latitude = freshLocation.latitude)
                            userPosition = pos
                            coroutineScope.launch {
                                camera.animateTo(CameraPosition(target = pos, zoom = 12.0))
                            }
                        } else {
                            Toast.makeText(context, "Location unavailable. Please check if GPS is enabled.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Location permission not granted.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MAP_LOCATION", "Error acquiring user location", e)
            Toast.makeText(context, "Unable to access location services.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            fetchUserLocation()
        }
    }

    var isLoadingData by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val offlineManager = rememberOfflineManager()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val repeaters = RepeaterRepository.loadRepeatersFromAssets(context)
                withContext(Dispatchers.Main) {
                    allRepeaters = repeaters
                    isLoadingData = false
                    if (repeaters.isEmpty()) {
                        loadError = "No repeater data found in asset dataset."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingData = false
                    loadError = e.message ?: "Failed to parse repeater dataset."
                }
            }
        }
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

    val repeatersGeoJsonData = remember(allRepeaters) {
        val features = allRepeaters.mapNotNull { item ->
            if (item.coordinates.size >= 2) {
                Feature(
                    geometry = Point(
                        Position(
                            longitude = item.coordinates[1],
                            latitude = item.coordinates[0]
                        )
                    ),
                    properties = buildJsonObject {
                        put("name", item.callsign)
                        put("tx_freq", item.tx_frequency)
                        put("rx_freq", item.rx_frequency)
                        put("status", item.status)
                    }
                )
            } else null
        }
        FeatureCollection(features).toJson()
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            fetchUserLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
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
                        item { FilteringButton(checkedWorking, "working", { checkedWorking = !checkedWorking }) }
                        item { FilteringButton(checkedStopped, "turned off", { checkedStopped = !checkedStopped }) }
                        item { FilteringButton(checkedPlanned, "planned", { checkedPlanned = !checkedPlanned }) }
                        item { FilteringButton(checkedTesting, "testing", { checkedTesting = !checkedTesting }) }
                        item { FilteringButton(checkedBuilding, "being built", { checkedBuilding = !checkedBuilding }) }
                        item { FilteringButton(checkedUnverified, "unverified", { checkedUnverified = !checkedUnverified }) }
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
                        item { FilteringButton(checked23cm, "23cm", { checked23cm = !checked23cm }) }
                        item { FilteringButton(checked70cm, "70cm", { checked70cm = !checked70cm }) }
                        item { FilteringButton(checked2m, "2m", { checked2m = !checked2m }) }
                        item { FilteringButton(checked4m, "4m", { checked4m = !checked4m }) }
                        item { FilteringButton(checked6m, "6m", { checked6m = !checked6m }) }
                        item { FilteringButton(checked10m, "10m", { checked10m = !checked10m }) }
                    }
                }
            }
        }
        Box(modifier = modifier) {
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
                val repeaterSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(repeatersGeoJsonData)
                )

                val statusFilters = mutableListOf<Expression<BooleanValue>>().apply {
                    if (checkedWorking) add(feature["status"].asString().eq(const("working")))
                    if (checkedStopped) add(feature["status"].asString().eq(const("off")))
                    if (checkedPlanned) add(feature["status"].asString().eq(const("planned")))
                    if (checkedTesting) add(feature["status"].asString().eq(const("testing")))
                    if (checkedBuilding) add(feature["status"].asString().eq(const("inprogress")))
                    if (checkedUnverified) add(feature["status"].asString().eq(const("unverified")))
                }

                val frequencyFilters = mutableListOf<Expression<BooleanValue>>().apply {
                    if (checked23cm) {
                        add(feature["tx_freq"].asNumber().gt(const(1240.0f)).and(feature["tx_freq"].asNumber().lt(const(1300.0f))).and(feature["rx_freq"].asNumber().gt(const(1240.0f)).and(feature["rx_freq"].asNumber().lt(const(1300.0f)))))
                    }
                    if (checked70cm) {
                        add(feature["tx_freq"].asNumber().gt(const(420.0f)).and(feature["tx_freq"].asNumber().lt(const(450.0f))).and(feature["rx_freq"].asNumber().gt(const(420.0f)).and(feature["rx_freq"].asNumber().lt(const(450.0f)))))
                    }
                    if (checked2m) {
                        add(feature["tx_freq"].asNumber().gt(const(144.0f)).and(feature["tx_freq"].asNumber().lt(const(146.0f))).and(feature["rx_freq"].asNumber().gt(const(144.0f)).and(feature["rx_freq"].asNumber().lt(const(146.0f)))))
                    }
                    if (checked4m) {
                        add(feature["tx_freq"].asNumber().gt(const(70.0f)).and(feature["tx_freq"].asNumber().lt(const(70.5f))).and(feature["rx_freq"].asNumber().gt(const(70.0f)).and(feature["rx_freq"].asNumber().lt(const(70.5f)))))
                    }
                    if (checked6m) {
                        add(feature["tx_freq"].asNumber().gt(const(50.0f)).and(feature["tx_freq"].asNumber().lt(const(52.0f))).and(feature["rx_freq"].asNumber().gt(const(50.0f)).and(feature["rx_freq"].asNumber().lt(const(52.0f)))))
                    }
                    if (checked10m) {
                        add(feature["tx_freq"].asNumber().gt(const(28.0f)).and(feature["tx_freq"].asNumber().lt(const(29.7f))).and(feature["rx_freq"].asNumber().gt(const(28.0f)).and(feature["rx_freq"].asNumber().lt(const(29.7f)))))
                    }
                }
                val finalFilter = if (statusFilters.isEmpty() || frequencyFilters.isEmpty()) {
                    const(false)
                } else {
                    all(
                        any(*statusFilters.toTypedArray()),
                        any(*frequencyFilters.toTypedArray())
                    )
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
                            selectedRepeater = allRepeaters.find { it.callsign == name }
                        }
                        ClickResult.Consume
                    }
                )

                userPosition?.let { pos ->
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
                        radius = const(18.dp)
                    )

                    CircleLayer(
                        id = "user-location-dot",
                        source = userSource,
                        color = const(Color(0xFF2196F3)),
                        radius = const(8.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )
                }
            }

            if (isLoadingData) {
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
            } else if (loadError != null || allRepeaters.isEmpty()) {
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
                            text = loadError ?: "No repeaters found in dataset.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            selectedRepeater?.let { item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedRepeater = null }
                ) {
                    CustomInfoWindow(
                        repeater = item,
                        onClose = { selectedRepeater = null },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
