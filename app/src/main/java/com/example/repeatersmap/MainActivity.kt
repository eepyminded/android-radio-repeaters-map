package com.example.repeatersmap

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
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
import org.maplibre.compose.expressions.dsl.any
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
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
                MyApp(modifier = Modifier.fillMaxSize())
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

data class repeaterFilters(
    val showWorking: Boolean = true,
    val showTesting: Boolean = true,
    val showOff: Boolean = true,
    val seventyCm: Boolean = true,
    val twoMeters: Boolean = true,
    val fourMeters: Boolean = true,
    val sixMeters: Boolean = true,
    val tenMeters: Boolean = true,
    val crossband: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(modifier: Modifier) {
    val context = LocalContext.current
    val antennaIcon = painterResource(R.drawable.antenna_icon)

    var allRepeaters by remember { mutableStateOf<List<RepeaterItem>>(emptyList()) }
    var selectedRepeater by remember { mutableStateOf<RepeaterItem?>(null) }

    var filters by remember {mutableStateOf(repeaterFilters())}

    LaunchedEffect(Unit) {
        allRepeaters = loadRepeatersFromAssets(context)
    }

    // putting the data into the weird geojson format
    val repeatersGeoJsonData = remember(allRepeaters) {
        val features = allRepeaters.mapNotNull { item ->
            if (item.coordinates.size >= 2) {
                Feature(
                    geometry = Point(
                        Position(longitude = item.coordinates[1], latitude = item.coordinates[0])
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
                // filter button to be added
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

                        // it couldn't find the correct SymbolLayer function so it must be this ugly now..
                        // showing map here
                        org.maplibre.compose.layers.SymbolLayer(
                            id = "working-repeaters",
                            source = repeaterSource,
                            iconImage = image(antennaIcon),
                            iconSize = const(2.0f),
                            filter = feature["status"].asString().eq(const("working")),
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