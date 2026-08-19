package systems.madi.repeatersmap.data.repository

import android.content.Context
import android.util.Log
import systems.madi.repeatersmap.data.model.RepeaterItem
import kotlinx.serialization.json.Json

object RepeaterRepository {
    fun loadRepeatersFromAssets(context: Context): List<RepeaterItem> {
        return try {
            val files = context.assets.list("")
            if (files?.contains("przemienniki.eu.json") != true) {
                return emptyList()
            }

            val jsonString = context.assets.open("przemienniki.eu.json")
                .bufferedReader()
                .use { it.readText() }

            parseRepeatersJson(jsonString)
        } catch (e: Exception) {
            Log.e("MAP_ERROR", "Error loading JSON dataset", e)
            emptyList()
        }
    }

    fun parseRepeatersJson(jsonString: String): List<RepeaterItem> {
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val repeaters = jsonParser.decodeFromString<List<RepeaterItem>>(jsonString)
        
        var convertedCount = 0
        val mappedRepeaters = repeaters.map { item ->
            if (!item.hasValidCoordinates && item.hasValidLocator) {
                val fallbackCoords = systems.madi.repeatersmap.util.QthLocatorConverter.convertToCoordinates(item.locator)
                if (fallbackCoords != null) {
                    convertedCount++
                    item.copy(coordinates = fallbackCoords)
                } else {
                    item
                }
            } else {
                item
            }
        }
        
        // Use println for unit tests if Log.d fails
        try {
            Log.d("MAP_DATASET", "Successfully loaded ${mappedRepeaters.size} repeaters. $convertedCount injected via Maidenhead fallback.")
        } catch (e: Exception) {
            println("Successfully loaded ${mappedRepeaters.size} repeaters. $convertedCount injected via Maidenhead fallback.")
        }
        
        return mappedRepeaters
    }
}
