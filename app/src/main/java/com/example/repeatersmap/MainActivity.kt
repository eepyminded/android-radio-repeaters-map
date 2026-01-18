package com.example.repeatersmap

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.android.style.expressions.Expression.literal
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
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.gt
import org.maplibre.compose.expressions.dsl.lt
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.BooleanValue
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson
import org.maplibre.compose.util.ClickResult

@Serializable
data class RepeaterItem(
    val callsign: String = "",
    val status: String = "",
    val tx_frequency: Double = 0.0,
    val rx_frequency: Double = 0.0,
    val coordinates: List<Double> = emptyList(),
    val tx_ctcss: JsonElement? = null,
    val rx_ctcss: JsonElement? = null,
    val qth: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.White)) {
                MainStructure(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

suspend fun loadRepeatersFromAssets(context: Context): List<RepeaterItem> {
    return withContext(Dispatchers.IO) {
        try {
            val files = context.assets.list("")
            if (files?.contains("przemienniki.eu.json") != true) {
                return@withContext emptyList()
            }

            val jsonString = context.assets.open("przemienniki.eu.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            jsonParser.decodeFromString<List<RepeaterItem>>(jsonString)
        } catch (e: Exception) {
            Log.e("MAP_ERROR", "Error loading JSON, there's some weird issue", e)
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStructure(modifier: Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentStateID = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Navigation Menu", modifier = Modifier.padding(16.dp))
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("View repeaters") },
                    selected = false,
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map with repeaters")},
                    onClick = {
                        if (currentStateID != "repeaters_screen")
                        {
                            navController.navigate("repeaters_screen")
                            drawerScope.launch { drawerState.close() }
                        }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Info screen") },
                    selected = false,
                    icon = { Icon(Icons.Default.Info, contentDescription = "Informations")},
                    onClick = {
                        if (currentStateID != "info_screen")
                        {
                            navController.navigate("info_screen")
                            drawerScope.launch { drawerState.close() }
                        }
                    }
                )
            }
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = { Text("Radio Repeaters Map") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                drawerScope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "info_screen",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("repeaters_screen") {
                    MapScreen(modifier = Modifier)
                }
                composable("info_screen") {
                    InfoScreen(modifier = Modifier)
                }
            }

        }
    }
    }

    @Composable
    fun InfoScreen(modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Projekt końcowy",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Android - Laboratorium",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Interaktywna mapa, która pozwala na znalezienie przemienników radiowych w swojej okolicy.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo_pwr_kolor_pion_ang__bez_tla),
                    contentDescription = "Technological University of Wrocław logo",
                    modifier = Modifier.size(180.dp)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MapScreen(modifier: Modifier) {
        val context = LocalContext.current
        val antennaIcon = painterResource(R.drawable.antenna_icon)

        var allRepeaters by remember { mutableStateOf<List<RepeaterItem>>(emptyList()) }
        var selectedRepeater by remember { mutableStateOf<RepeaterItem?>(null) }

        var checkedWorking by remember { mutableStateOf(true) }
        var checkedStopped by remember { mutableStateOf(true) }
        var checkedPlanned by remember { mutableStateOf(true) }

        var checked70cm by remember { mutableStateOf(true) }
        var checked2m by remember { mutableStateOf(true) }
        var checked6m by remember { mutableStateOf(true) }
        var checked10m by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            allRepeaters = loadRepeatersFromAssets(context)
        }

        // putting the data into the weird geojson format
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
                    // will add handling of qth codes here to coords in the future so they dont appear in the middle of nowhere
                } else null
            }
            FeatureCollection(features).toJson()
        }

        val camera = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(latitude = 52.0, longitude = 19.0),
                zoom = 6.0
            )
        )

        val sheetState = rememberModalBottomSheetState()
        var showBottomSheet by remember { mutableStateOf(false) }

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Filters") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = { showBottomSheet = true }
                )
            }
        ) { innerPadding ->
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    // filtering UI

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        Text("Repeaters Status")
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Working")
                            Checkbox(
                                checked = checkedWorking,
                                onCheckedChange = { checkedWorking = it}
                            )
                            Text("Off")
                            Checkbox(
                                checked = checkedStopped,
                                onCheckedChange = { checkedStopped = it}
                            )
                            Text("Planned")
                            Checkbox(
                                checked = checkedPlanned,
                                onCheckedChange = { checkedPlanned = it}
                            )
                        }
                        Text("Frequency")
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("70CM")
                            Checkbox(
                                checked = checked70cm,
                                onCheckedChange = { checked70cm = it}
                            )

                            Text("2M")
                            Checkbox(
                                checked = checked2m,
                                onCheckedChange = { checked2m = it}
                            )

                            Text("6M")
                            Checkbox(
                                checked = checked6m,
                                onCheckedChange = { checked6m = it}
                            )

                            Text("10M")
                            Checkbox(
                                checked = checked10m,
                                onCheckedChange = { checked10m = it}
                            )
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

                    // filtering logic
                    val statusFilters = mutableListOf<Expression<BooleanValue>>().apply {
                        if (checkedWorking) add(feature["status"].asString().eq(const("working")))
                        if (checkedStopped) add(feature["status"].asString().eq(const("off")))
                        if (checkedPlanned) add(feature["status"].asString().eq(const("planned")))
                    }

                    val frequencyFilters = mutableListOf<Expression<BooleanValue>>().apply {
                        if (checked70cm) {
                            add(feature["tx_freq"].asNumber().gt(const(420.0f)).and(feature["tx_freq"].asNumber().lt(const(450.0f))))
                        }
                        if (checked2m) {
                            add(feature["tx_freq"].asNumber().gt(const(144.0f)).and(feature["tx_freq"].asNumber().lt(const(146.0f))))
                        }
                        if (checked6m) {
                            add(feature["tx_freq"].asNumber().gt(const(50.0f)).and(feature["tx_freq"].asNumber().lt(const(54.0f))))
                        }
                        if (checked10m) {
                            add(feature["tx_freq"].asNumber().gt(const(28.0f)).and(feature["tx_freq"].asNumber().lt(const(29.7f))))
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

                    // showing repeaters here
                    SymbolLayer(
                        id = "working-repeaters",
                        source = repeaterSource,
                        iconImage = image(antennaIcon),
                        iconSize = const(2.0f),
                        filter = finalFilter,
                        iconAllowOverlap = const(true),
                        iconIgnorePlacement = const(true),
                        onClick = { features ->
                            val name =
                                features.firstOrNull()?.properties?.get("name")?.jsonPrimitive?.content
                            if (name != null) {
                                selectedRepeater = allRepeaters.find { it.callsign == name }
                            }
                            ClickResult.Consume
                        }
                    )
                }

                if (allRepeaters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text("Loading data..", color = Color.Black)
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

        @Composable
        fun CustomInfoWindow(
            repeater: RepeaterItem,
            onClose: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            Box(
                modifier = modifier
                    .padding(32.dp)
                    .background(Color.White)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = repeater.callsign,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = repeater.qth ?: "Unknown Location",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onClose() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabelValue("TX Freq", "${repeater.tx_frequency} MHz")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoLabelValue("TX CTCSS", formatCtcss(repeater.tx_ctcss))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabelValue("RX Freq", "${repeater.rx_frequency} MHz")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoLabelValue("RX CTCSS", formatCtcss(repeater.rx_ctcss))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val statusColor = if (repeater.status.equals("working", ignoreCase = true))
                        Color(0xFF2E7D32) else Color.Red

                    Text(
                        text = "STATUS: ${repeater.status.uppercase()}",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        @Composable
        fun InfoLabelValue(label: String, value: String) {
            Column {
                Text(text = label, fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }

        fun formatCtcss(element: JsonElement?): String {
            val raw = element.toString().replace("\"", "")
            return if (raw == "false" || raw == "null") {
                "None"
            } else
                raw
        }