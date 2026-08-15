package systems.madi.repeatersmap.data.model

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
) {
    // checks coordinates count, lat lon bounds, and ignores 0,0 placeholder
    val hasValidCoordinates: Boolean
        get() = coordinates.size >= 2 &&
                !coordinates[0].isNaN() && !coordinates[1].isNaN() &&
                !(coordinates[0] == 0.0 && coordinates[1] == 0.0) &&
                coordinates[0] in -90.0..90.0 && coordinates[1] in -180.0..180.0

    // checks if maidenhead locator is present
    val hasValidLocator: Boolean
        get() = !locator.isNullOrBlank() && locator.any { it.isLetterOrDigit() }

    // valid if it has coordinates or maidenhead locator fallback
    val isValid: Boolean
        get() = hasValidCoordinates || hasValidLocator
}
