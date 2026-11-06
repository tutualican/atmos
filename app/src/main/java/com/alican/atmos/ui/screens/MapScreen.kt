package com.alican.atmos.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.alican.atmos.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.core.WeatherData
import com.alican.atmos.data.GeoMath
import com.alican.atmos.data.RadarApi
import com.alican.atmos.data.RadarMeta
import com.alican.atmos.data.Tiles
import com.alican.atmos.ui.AtmosUiState
import com.alican.atmos.ui.components.AtmosCard
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.components.IconButtonSoft
import com.alican.atmos.ui.components.Pill
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    state: AtmosUiState,
    onOpenLocation: () -> Unit,
    onOpenForecast: () -> Unit
) {
    val place = state.place
    val density = LocalDensity.current
    var centerLat by remember(place.id) { mutableFloatStateOf(place.lat.toFloat()) }
    var centerLon by remember(place.id) { mutableFloatStateOf(place.lon.toFloat()) }
    var zoom by remember(place.id) { mutableFloatStateOf(6.6f) }
    var radar by remember { mutableStateOf<RadarMeta?>(null) }
    var radarError by remember { mutableStateOf<String?>(null) }
    var frameIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var opacity by remember { mutableFloatStateOf(0.82f) }
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { RadarApi.load() }
            .onSuccess { meta ->
                radar = meta
                val pastCount = meta.frames.count { !it.isForecast }
                frameIndex = (pastCount - 1).coerceAtLeast(0)
            }
            .onFailure { radarError = it.message ?: "Radar unavailable" }
    }

    val frames = radar?.frames.orEmpty()
    LaunchedEffect(playing, frames.size) {
        if (!playing || frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(620)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Atmos.Midnight)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val tileZ = zoom.roundToInt().coerceIn(2, 13)
        val scale = Math.pow(2.0, (zoom - tileZ).toDouble()).toFloat()
        val tileSize = 256f * scale
        val centerWorldX = GeoMath.lonToWorldX(centerLon.toDouble(), tileZ.toDouble())
        val centerWorldY = GeoMath.latToWorldY(centerLat.toDouble(), tileZ.toDouble())
        val minTileX = floor((centerWorldX - widthPx / 2 / scale) / 256.0).toInt()
        val maxTileX = floor((centerWorldX + widthPx / 2 / scale) / 256.0).toInt()
        val minTileY = floor((centerWorldY - heightPx / 2 / scale) / 256.0).toInt()
        val maxTileY = floor((centerWorldY + heightPx / 2 / scale) / 256.0).toInt()
        val maxTileIndex = (1 shl tileZ) - 1
        val baseKeys = remember(tileZ, minTileX, maxTileX, minTileY, maxTileY) {
            buildList {
                for (x in minTileX..maxTileX) for (y in minTileY..maxTileY) {
                    if (y in 0..maxTileIndex) add("b|$tileZ|$x|$y")
                }
            }
        }
        val radarZ = tileZ.coerceAtMost(10)
        val radarScale = Math.pow(2.0, (zoom - radarZ).toDouble()).toFloat()
        val radarWorldX = GeoMath.lonToWorldX(centerLon.toDouble(), radarZ.toDouble())
        val radarWorldY = GeoMath.latToWorldY(centerLat.toDouble(), radarZ.toDouble())
        val radarTileSize = 256f * radarScale
        val radarMinX = floor((radarWorldX - widthPx / 2 / radarScale) / 256.0).toInt()
        val radarMaxX = floor((radarWorldX + widthPx / 2 / radarScale) / 256.0).toInt()
        val radarMinY = floor((radarWorldY - heightPx / 2 / radarScale) / 256.0).toInt()
        val radarMaxY = floor((radarWorldY + heightPx / 2 / radarScale) / 256.0).toInt()
        val radarMaxIndex = (1 shl radarZ) - 1
        val frame = frames.getOrNull(frameIndex)
        val radarKeys = remember(radarZ, radarMinX, radarMaxX, radarMinY, radarMaxY, frame?.path) {
            buildList {
                if (frame == null) return@buildList
                for (x in radarMinX..radarMaxX) for (y in radarMinY..radarMaxY) {
                    if (y in 0..radarMaxIndex) add("r|${frame.path}|$radarZ|$x|$y")
                }
            }
        }

        LaunchedEffect(baseKeys) {
            baseKeys.map { key ->
                async {
                    if (tiles[key] == null) {
                        val parts = key.split("|")
                        val bmp = Tiles.load(key, Tiles.baseMapUrl(parts[1].toInt(), parts[2].toInt(), parts[3].toInt()))
                        if (bmp != null) tiles[key] = bmp.asImageBitmap()
                    }
                }
            }.awaitAll()
        }
        LaunchedEffect(radarKeys, radar?.host) {
            val host = radar?.host ?: return@LaunchedEffect
            val path = frame?.path ?: return@LaunchedEffect
            radarKeys.map { key ->
                async {
                    if (tiles[key] == null) {
                        val parts = key.split("|")
                        val bmp = Tiles.load(key, Tiles.radarUrl(host, path, parts[2].toInt(), parts[3].toInt(), parts[4].toInt()))
                        if (bmp != null) tiles[key] = bmp.asImageBitmap()
                    }
                }
            }.awaitAll()
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val oldZoom = zoom
                        val worldOld = 256.0 * Math.pow(2.0, oldZoom.toDouble())
                        val focalWorldX = GeoMath.lonToWorldX(centerLon.toDouble(), oldZoom.toDouble()) + (centroid.x - widthPx / 2)
                        val focalWorldY = GeoMath.latToWorldY(centerLat.toDouble(), oldZoom.toDouble()) + (centroid.y - heightPx / 2)
                        val newZoom = (oldZoom + log2(gestureZoom.coerceAtLeast(0.2f))).coerceIn(3f, 13f)
                        val worldNew = 256.0 * Math.pow(2.0, newZoom.toDouble())
                        var nx = focalWorldX - (centroid.x - widthPx / 2) * (worldNew / worldOld) - pan.x * (worldNew / worldOld)
                        var ny = focalWorldY - (centroid.y - heightPx / 2) * (worldNew / worldOld) - pan.y * (worldNew / worldOld)
                        nx = nx.coerceIn(worldNew * 0.02, worldNew * 0.98)
                        ny = ny.coerceIn(worldNew * 0.02, worldNew * 0.98)
                        centerLon = GeoMath.worldXToLon(nx, newZoom.toDouble()).toFloat()
                        centerLat = GeoMath.worldYToLat(ny, newZoom.toDouble()).toFloat()
                        zoom = newZoom
                    }
                }
        ) {
            drawRect(Atmos.DeepNavy)
            // Base map.
            for (x in minTileX..maxTileX) for (y in minTileY..maxTileY) {
                if (y !in 0..maxTileIndex) continue
                val key = "b|$tileZ|$x|$y"
                val left = ((x * 256.0 - centerWorldX) * scale + widthPx / 2).toFloat()
                val top = ((y * 256.0 - centerWorldY) * scale + heightPx / 2).toFloat()
                val img = tiles[key]
                if (img != null) {
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(tileSize.roundToInt(), tileSize.roundToInt())
                    )
                } else {
                    drawRect(
                        color = Color(0xFF121A26),
                        topLeft = Offset(left, top),
                        size = Size(tileSize, tileSize)
                    )
                }
            }

            // Weather radar.
            if (frame != null) {
                for (x in radarMinX..radarMaxX) for (y in radarMinY..radarMaxY) {
                    if (y !in 0..radarMaxIndex) continue
                    val img = tiles["r|${frame.path}|$radarZ|$x|$y"] ?: continue
                    val left = ((x * 256.0 - radarWorldX) * radarScale + widthPx / 2).toFloat()
                    val top = ((y * 256.0 - radarWorldY) * radarScale + heightPx / 2).toFloat()
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(radarTileSize.roundToInt(), radarTileSize.roundToInt()),
                        alpha = opacity,
                        blendMode = BlendMode.Screen
                    )
                }
            }

            // Selected place marker.
            val mx = ((GeoMath.lonToWorldX(place.lon, tileZ.toDouble()) - centerWorldX) * scale + widthPx / 2).toFloat()
            val my = ((GeoMath.latToWorldY(place.lat, tileZ.toDouble()) - centerWorldY) * scale + heightPx / 2).toFloat()
            drawCircle(Atmos.Ice.copy(alpha = 0.14f), radius = 18.dp.toPx(), center = Offset(mx, my))
            drawCircle(Color.White, radius = 3.5.dp.toPx(), center = Offset(mx, my))
            drawCircle(Atmos.Ice, radius = 7.dp.toPx(), center = Offset(mx, my), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx()))
        }

        // Top bar.
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Atmos.Midnight.copy(alpha = 0.92f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weather map", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = Atmos.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AtmosIcons.Location, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${place.name} · ${place.subtitle}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Atmos.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButtonSoft(AtmosIcons.Crosshair, "Recenter") {
                    centerLat = place.lat.toFloat()
                    centerLon = place.lon.toFloat()
                    zoom = 6.6f
                }
            }
            Spacer(Modifier.weight(1f))

            // Zoom rail.
            Column(
                Modifier.align(Alignment.End).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButtonSoft(AtmosIcons.Plus, "Zoom in") {
                    zoom = (zoom + 0.8f).coerceAtMost(13f)
                }
                IconButtonSoft(AtmosIcons.Minus, "Zoom out") {
                    zoom = (zoom - 0.8f).coerceAtLeast(3f)
                }
            }

            // Timeline card.
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                AtmosCard(
                    Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButtonSoft(
                            if (playing) AtmosIcons.Pause else AtmosIcons.Play,
                            if (playing) "Pause radar" else "Play radar",
                            modifier = Modifier.size(36.dp)
                        ) { playing = !playing }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                frame?.let { formatRadarTime(it.epochSeconds) } ?: "Radar loading…",
                                fontFamily = GeistMono,
                                fontSize = 15.sp,
                                color = Atmos.White,
                                maxLines = 1
                            )
                            Text(
                                when {
                                    radar == null && radarError != null -> "Radar unavailable"
                                    frame == null -> "Fetching RainViewer frames"
                                    frame.isForecast -> "Nowcast · not an observation"
                                    else -> "Observed radar · past frame"
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = if (frame?.isForecast == true) Atmos.Gold else Atmos.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Pill(
                            if (frame?.isForecast == true) "NOWCAST" else "OBSERVED",
                            tint = if (frame?.isForecast == true) Atmos.Gold else Atmos.Good
                        )
                    }
                    if (frames.size > 1) {
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = frameIndex.toFloat(),
                            onValueChange = {
                                frameIndex = it.roundToInt().coerceIn(0, frames.lastIndex)
                                playing = false
                            },
                            valueRange = 0f..frames.lastIndex.toFloat(),
                            steps = (frames.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = Atmos.Ice,
                                activeTrackColor = Atmos.Ice.copy(alpha = 0.7f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.2f..1f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Atmos.Secondary,
                                activeTrackColor = Atmos.Secondary.copy(alpha = 0.5f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .height(5.dp)
                                .width(92.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF7BA7C9),
                                            Color(0xFF9BC4A8),
                                            Color(0xFFE3BE7C),
                                            Color(0xFFDD9F72),
                                            Color(0xFFD97A72)
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("light → heavy", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "© OpenStreetMap · © CARTO · RainViewer",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Atmos.Muted.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Radar shows observed precipitation. Frames marked NOWCAST are short-range extrapolation, not measurements.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(104.dp))
            }
        }
    }
}

private fun formatRadarTime(epochSeconds: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000))
