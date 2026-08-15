package systems.madi.repeatersmap.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.sqrt
import kotlin.math.log10

@Composable
fun ElevationProfileGraph(
    points: List<Double>,
    totalDistanceMeters: Double,
    frequencyMHz: Double,
    userAntennaHeight: Float,
    repeaterAntennaHeight: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
) {
    if (points.isEmpty()) return

    // account for antenna heights in the max elevation to scale the graph properly
    val distanceKm = totalDistanceMeters / 1000.0
    val maxTerrain = points.maxOrNull() ?: 100.0
    val minTerrain = points.minOrNull() ?: 0.0
    val userAbsoluteHeight = points.first() + userAntennaHeight
    val repeaterAbsoluteHeight = points.last() + repeaterAntennaHeight
    val maxElevation = maxOf(maxTerrain, userAbsoluteHeight.toDouble(), repeaterAbsoluteHeight.toDouble())
    val minElevation = minTerrain
    val range = (maxElevation - minElevation).coerceAtLeast(10.0)

    var isLosBlocked = false
    var isFresnelBlocked = false
    val freqGHz = frequencyMHz / 1000.0

    if (freqGHz > 0) {
        for (i in points.indices) {
            val terrainHeight = points[i]
            val d1 = (i.toDouble() / (points.size - 1)) * distanceKm
            val d2 = distanceKm - d1
            val r = if (d1 > 0 && d2 > 0) 17.32 * sqrt((d1 * d2) / (distanceKm * freqGHz)) else 0.0
            
            val losHeightAtPoint = userAbsoluteHeight + (repeaterAbsoluteHeight - userAbsoluteHeight) * (i.toDouble() / (points.size - 1).coerceAtLeast(1))
            val fresnelBottomHeight = losHeightAtPoint - (r * 0.6)
            
            if (terrainHeight > losHeightAtPoint) isLosBlocked = true
            if (terrainHeight > fresnelBottomHeight) isFresnelBlocked = true
        }
    }

    val statusColor = when {
        isLosBlocked -> Color(0xFFD32F2F) // red
        isFresnelBlocked -> Color(0xFFFBC02D) // yellow
        else -> Color(0xFF388E3C) // green
    }
    val statusText = when {
        isLosBlocked -> "Non line of Sight"
        isFresnelBlocked -> "Fresnel Impaired"
        else -> "Clear"
    }

    // free space path loss (fspl) in db
    val fspl = if (distanceKm > 0 && frequencyMHz > 0) {
        20 * log10(distanceKm) + 20 * log10(frequencyMHz) + 32.44
    } else 0.0

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (points.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()
            
            fillPath.moveTo(0f, height)
            
            points.forEachIndexed { index, elevation ->
                val x = index * stepX
                val normalizedY = ((elevation - minElevation) / range).toFloat()
                val y = height - (normalizedY * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            
            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // draw line of sight (los) and fresnel zone
            val userY = height - (((userAbsoluteHeight - minElevation) / range).toFloat() * height)
            val repeaterY = height - (((repeaterAbsoluteHeight - minElevation) / range).toFloat() * height)
            
            // direct los line
            drawLine(
                color = statusColor.copy(alpha = 0.8f),
                start = androidx.compose.ui.geometry.Offset(0f, userY),
                end = androidx.compose.ui.geometry.Offset(width, repeaterY),
                strokeWidth = 2.dp.toPx()
            )

            // draw fresnel zone
            val fresnelPath = Path()
            
            if (freqGHz > 0) {
                for (i in points.indices) {
                    val x = i * stepX
                    val d1 = (i.toDouble() / (points.size - 1)) * distanceKm
                    val d2 = distanceKm - d1
                    
                    // fresnel radius in meters
                    val r = if (d1 > 0 && d2 > 0) 17.32 * sqrt((d1 * d2) / (distanceKm * freqGHz)) else 0.0
                    
                    // convert radius to y pixels
                    val rPixels = (r / range).toFloat() * height
                    
                    // center of the beam at this x
                    val beamCenterY = userY + (repeaterY - userY) * (i.toFloat() / (points.size - 1).coerceAtLeast(1))
                    
                    // only 60% of fresnel zone requires clearance
                    val bottomFresnelY = beamCenterY + (rPixels * 0.6f)
                    
                    if (i == 0) fresnelPath.moveTo(x, userY)
                    else fresnelPath.lineTo(x, bottomFresnelY)
                }
                
                drawPath(
                    path = fresnelPath,
                    color = Color.Yellow.copy(alpha = 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // draw dots
            drawCircle(color = Color(0xFF2196F3), radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(0f, userY))
            drawCircle(color = Color(0xFFE53935), radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width, repeaterY))
            
            // draw mast lines (from dot to terrain)
            val userTerrainY = height - (((points.first() - minElevation) / range).toFloat() * height)
            val repeaterTerrainY = height - (((points.last() - minElevation) / range).toFloat() * height)
            
            drawLine(color = Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, userY), end = androidx.compose.ui.geometry.Offset(0f, userTerrainY), strokeWidth = 2.dp.toPx())
            drawLine(color = Color.Gray, start = androidx.compose.ui.geometry.Offset(width, repeaterY), end = androidx.compose.ui.geometry.Offset(width, repeaterTerrainY), strokeWidth = 2.dp.toPx())
        }
        
        // labels
        Text(text = "User", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 4.dp))
        Text(text = "Repeater", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 4.dp))
        
        // y-axis min/max elevation labels
        val maxTerrain = points.maxOrNull() ?: 0.0
        val minTerrain = points.minOrNull() ?: 0.0
        Text(text = "${maxTerrain.toInt()}m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp))
        Text(text = "${minTerrain.toInt()}m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 24.dp))
        
        // dynamic status badge
        Text(
            text = "FSPL: ${fspl.toInt()} dB • Signal: $statusText", 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold, 
            color = if (isFresnelBlocked && !isLosBlocked) Color.Black else Color.White, 
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .background(color = statusColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
