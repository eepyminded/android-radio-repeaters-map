package systems.madi.repeatersmap.ui.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import systems.madi.repeatersmap.R

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "A few words",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Interactive map of Polish ham radio repeaters, with dynamic filters",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.elegant_creature_at_puter),
                contentDescription = "Luna at computer",
                modifier = Modifier.size(180.dp)
            )

            Text(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            "https://madi.systems/",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("My website")
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append("Elevation data provided by ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://open-meteo.com/",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("Open-Meteo.com")
                        }
                    )
                    append(" under CC-BY 4.0.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = buildAnnotatedString {
                    append("Map tile rendering provided by ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://openfreemap.org/",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("OpenFreeMap")
                        }
                    )
                    append(" and map data by ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://www.openstreetmap.org/copyright",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("OpenStreetMap")
                        }
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = buildAnnotatedString {
                    append("Ham radio repeaters data provided by Wojtek Jakieła SQ8W from ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://przemienniki.eu/",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("przemienniki.eu")
                        }
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = buildAnnotatedString {
                    append("Map powered by ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://github.com/maplibre/maplibre-compose",
                            TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                        ),
                        {
                            append("maplibre compose")
                        }
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
