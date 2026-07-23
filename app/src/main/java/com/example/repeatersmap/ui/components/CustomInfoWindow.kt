package com.example.repeatersmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repeatersmap.data.model.RepeaterItem
import kotlinx.serialization.json.JsonElement

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
