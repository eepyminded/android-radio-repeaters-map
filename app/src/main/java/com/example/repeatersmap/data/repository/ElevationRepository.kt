package com.example.repeatersmap.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.Position
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ElevationRepository {
    
    fun calculatePathPoints(start: Position, end: Position, maxPoints: Int = 100): List<Position> {
        val points = mutableListOf<Position>()
        points.add(start)
        
        val distance = calculateDistanceMeters(start, end)
        // 1 point every 200 meters, capped at maxPoints
        val numIntervals = (distance / 200.0).toInt().coerceIn(2, maxPoints - 1)

        // convert to radians for great circle calculation
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)
        
        // angular distance
        val d = distance / 6371000.0
        
        if (d > 0.0) {
            for (i in 1 until numIntervals) {
                val f = i.toDouble() / numIntervals
                
                val a = sin((1 - f) * d) / sin(d)
                val b = sin(f * d) / sin(d)
                
                val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
                val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
                val z = a * sin(lat1) + b * sin(lat2)
                
                val newLat = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
                val newLon = Math.toDegrees(atan2(y, x))
                
                points.add(Position(longitude = newLon, latitude = newLat))
            }
        }
        
        points.add(end)
        return points
    }

    fun calculateDistanceMeters(start: Position, end: Position): Double {
        val earthRadius = 6371000.0 // in meters
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    suspend fun getElevationProfile(points: List<Position>): List<Double> = withContext(Dispatchers.IO) {
        val lats = points.joinToString(",") { it.latitude.toString() }
        val lons = points.joinToString(",") { it.longitude.toString() }
        
        val urlString = "https://api.open-meteo.com/v1/elevation?latitude=$lats&longitude=$lons"
        Log.d("ELEVATION_API", "Request URL: $urlString")
        
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = InputStreamReader(connection.inputStream).readText()
            Log.d("ELEVATION_API", "Raw JSON Response: $response")
            
            val json = Json.parseToJsonElement(response) as JsonObject
            val elevationArray = json["elevation"]?.jsonArray
            val heights = elevationArray?.map { it.jsonPrimitive.content.toDoubleOrNull() ?: 0.0 } ?: emptyList()
            
            Log.d("ELEVATION_API", "Parsed Heights (${heights.size} points): $heights")
            return@withContext heights
        } else {
            throw Exception("Failed to fetch elevation: HTTP ${connection.responseCode}")
        }
    }
}
