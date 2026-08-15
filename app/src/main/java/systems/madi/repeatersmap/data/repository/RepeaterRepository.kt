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

            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            jsonParser.decodeFromString<List<RepeaterItem>>(jsonString)
        } catch (e: Exception) {
            Log.e("MAP_ERROR", "Error loading JSON dataset", e)
            emptyList()
        }
    }
}
