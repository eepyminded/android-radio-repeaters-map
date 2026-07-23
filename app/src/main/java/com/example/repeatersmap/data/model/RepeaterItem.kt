package com.example.repeatersmap.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RepeaterItem(
    val callsign: String = "",
    val status: String = "",
    val tx_frequency: Double = 0.0,
    val rx_frequency: Double = 0.0,
    val coordinates: List<Double> = emptyList(),
    val tx_ctcss: JsonElement? = null,
    val rx_ctcss: JsonElement? = null,
    val qth: String? = null,
    val locator: String? = null
)
