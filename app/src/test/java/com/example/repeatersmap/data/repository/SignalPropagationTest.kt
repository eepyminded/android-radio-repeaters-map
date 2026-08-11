package com.example.repeatersmap.data.repository

import org.junit.Test
import org.junit.Assert.assertEquals
import org.maplibre.spatialk.geojson.Position
import kotlin.math.log10

class SignalPropagationTest {

    private val repository = ElevationRepository()

    @Test
    fun testSignalPropagationAndDistance() {
        // example user in central Warsaw
        val userPos = Position(longitude = 21.0122, latitude = 52.2297)

        // Warsaw center repeater
        // coordinates from przemienniki.eu
        val sr5wa = Position(longitude = 21.0067, latitude = 52.2319)
        val sr5waFreq = 438.800 // 70cm band

        // Outside of warsaw
        val sr5w = Position(longitude = 20.9328, latitude = 52.2683)
        val sr5wFreq = 145.625 // 2m band

        // Gdansk
        val sr2c = Position(longitude = 18.6466, latitude = 54.3520)
        val sr2cFreq = 145.725 // 2m band

        println("=== SIGNAL PROPAGATION TEST ===")
        
        // Test SR5WA
        val dist1 = repository.calculateDistanceMeters(userPos, sr5wa)
        val fspl1 = calculateFSPL(dist1, sr5waFreq)
        println("SR5WA (Warsaw Center): Distance = ${dist1.toInt()} m, Frequency = $sr5waFreq MHz -> FSPL = ${fspl1.toInt()} dB")
        
        // Test SR5W
        val dist2 = repository.calculateDistanceMeters(userPos, sr5w)
        val fspl2 = calculateFSPL(dist2, sr5wFreq)
        println("SR5W (Warsaw Outer): Distance = ${dist2.toInt()} m, Frequency = $sr5wFreq MHz -> FSPL = ${fspl2.toInt()} dB")

        // Test SR2C
        val dist3 = repository.calculateDistanceMeters(userPos, sr2c)
        val fspl3 = calculateFSPL(dist3, sr2cFreq)
        println("SR2C (Gdańsk): Distance = ${dist3.toInt()} m (approx ${dist3.toInt()/1000} km), Frequency = $sr2cFreq MHz -> FSPL = ${fspl3.toInt()} dB")

        println("===============================")
        
        // assertions just to make sure it doesn't crash and numbers are positive
        assert(dist1 > 0)
        assert(fspl1 > 0)
    }

    private fun calculateFSPL(distanceMeters: Double, frequencyMHz: Double): Double {
        val distanceKm = distanceMeters / 1000.0
        return if (distanceKm > 0 && frequencyMHz > 0) {
            20 * log10(distanceKm) + 20 * log10(frequencyMHz) + 32.44
        } else 0.0
    }
}
