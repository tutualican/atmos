package com.alican.atmos.ui.scene

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.alican.atmos.core.AnimationQuality
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.atmosphereFor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Weather is rendered, never decorated: every particle, cloud layer and flash is driven by the
 * current conditions passed in. Lightning can only exist when the WMO code says thunderstorm,
 * and the storm flash never claims a strike location or timing.
 */
@Composable
fun WeatherScene(
    kind: WeatherKind,
    isDay: Boolean,
    hourOfDay: Int,
    precipitationMm: Double,
    precipitationProbability: Int?,
    windMs: Double,
    quality: AnimationQuality,
    reduceMotion: Boolean,
    onLightningFlash: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val engine = remember { SceneEngine() }
    var frame by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(kind, isDay, hourOfDay, quality, reduceMotion, precipitationMm) {
        engine.configure(kind, isDay, hourOfDay, precipitationMm, precipitationProbability, windMs, quality, reduceMotion)
    }

    LaunchedEffect(kind, isDay, reduceMotion) {
        var last = 0L
        var emaFrameMs = 16.0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
                    val ms = (now - last) / 1_000_000.0
                    emaFrameMs = emaFrameMs * 0.9 + ms * 0.1
                    val adaptive = if (quality == AnimationQuality.AUTO) {
                        when {
                            emaFrameMs > 24 -> 0.55f
                            emaFrameMs > 19 -> 0.78f
                            emaFrameMs < 14 -> 1f
                            else -> 0.92f
                        }
                    } else 1f
                    val flash = engine.update(dt, adaptive)
                    if (flash) onLightningFlash?.invoke()
                    frame++
                } else {
                    engine.prime(System.nanoTime())
                }
                last = now
            }
            if (reduceMotion) break
        }
    }

    // Frame counter is read inside the draw phase only, so recomposition stays out of the hot path.
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        frame
        engine.width = size.width
        engine.height = size.height
        engine.density = density.density
        engine.draw(this)
    }
}

private class Drop(
    var x: Float,
    var y: Float,
    var speed: Float,
    var length: Float,
    var depth: Float,
    var drift: Float
)

private class Flake(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var phase: Float,
    var depth: Float
)

private class Mote(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var phase: Float,
    var warm: Boolean
)

private class SceneEngine {
    var width = 0f
    var height = 0f
    var density = 2f

    private var kind = WeatherKind.OVERCAST
    private var isDay = true
    private var hourOfDay = 12
    private var intensity = 0.4f
    private var wind = 0f
    private var motionScale = 1f
    private var particleCap = 1f
    private var precipitationProbability = 0
    private var frozen = false

    private var time = 0f
    private val drops = ArrayList<Drop>(320)
    private val flakes = ArrayList<Flake>(220)
    private val motes = ArrayList<Mote>(40)
    private val rnd = Random(7)

    private var flash = 0f
    private var flashPhase = 0f
    private var nextFlashIn = 2.2f
    private var flashCount = 0

    private val cloudPuffs = ArrayList<CloudPuff>(24)
    private var cloudShift = 0f

    private class CloudPuff(val rx: Float, val ry: Float, val rw: Float, val rh: Float, val alpha: Float, val lit: Float)

    fun prime(nanos: Long) {
        time = (nanos % 1_000_000_000L) / 1_000_000_000f
    }

    fun configure(
        kind: WeatherKind,
        isDay: Boolean,
        hourOfDay: Int,
        precipMm: Double,
        precipProbability: Int?,
        windMs: Double,
        quality: AnimationQuality,
        reduceMotion: Boolean
    ) {
        val changed = this.kind != kind || this.isDay != isDay
        this.kind = kind
        this.isDay = isDay
        this.hourOfDay = hourOfDay
        this.wind = (windMs / 12.0).toFloat().coerceIn(0f, 2.2f)
        this.precipitationProbability = precipProbability ?: 0
        this.frozen = reduceMotion
        this.motionScale = when (quality) {
            AnimationQuality.LOW -> 0.45f
            AnimationQuality.BALANCED -> 0.75f
            AnimationQuality.HIGH -> 1f
            AnimationQuality.AUTO -> 1f
        }
        this.particleCap = when (quality) {
            AnimationQuality.LOW -> 0.32f
            AnimationQuality.BALANCED -> 0.68f
            AnimationQuality.HIGH -> 1f
            AnimationQuality.AUTO -> 0.9f
        }
        val wetness = when (kind) {
            WeatherKind.THUNDER, WeatherKind.HAIL -> 1f
            WeatherKind.HEAVY_RAIN -> 0.95f
            WeatherKind.RAIN -> 0.62f
            WeatherKind.DRIZZLE, WeatherKind.FREEZING_RAIN, WeatherKind.SLEET -> 0.3f
            else -> 0.12f
        }
        val observed = (precipMm / 6.0).toFloat().coerceIn(0f, 1f)
        intensity = max(wetness * 0.55f, observed)
        if (!kind.isWet) intensity = 0f

        if (changed || cloudPuffs.isEmpty()) {
            buildClouds()
            buildParticles()
        } else if (cloudPuffs.isEmpty()) {
            buildClouds()
        }
        if (drops.isEmpty() && flakes.isEmpty()) buildParticles()
    }

    private fun buildClouds() {
        cloudPuffs.clear()
        val r = Random(1337 + kind.ordinal)
        // A believable cumulus/stratocumulus cluster: tall centre, softer shoulders.
        val count = when (kind) {
            WeatherKind.CLEAR -> 0
            WeatherKind.MOSTLY_CLEAR -> 13
            else -> 16
        }
        repeat(count) { i ->
            val t = i / (count - 1f)
            val spread = (t - 0.5f) * 2f
            val rx = spread * 0.42f + r.nextFloat() * 0.07f - 0.035f
            val baseY = -abs(spread) * 0.1f
            val ry = baseY + (r.nextFloat() - 0.35f) * 0.09f
            val central = 1f - abs(spread)
            val rw = (0.18f + central * 0.22f + r.nextFloat() * 0.07f)
            val rh = (0.10f + central * 0.20f + r.nextFloat() * 0.05f)
            val alpha = 0.5f + central * 0.35f
            val lit = if (isDay) 0.7f else 0.28f
            cloudPuffs += CloudPuff(rx, ry, rw, rh, alpha, lit)
        }
        // Lower shelf that catches rim light.
        repeat(5) { i ->
            val rx = (i - 2) * 0.17f + r.nextFloat() * 0.04f
            cloudPuffs += CloudPuff(
                rx = rx,
                ry = 0.18f + r.nextFloat() * 0.05f,
                rw = 0.2f + r.nextFloat() * 0.12f,
                rh = 0.06f + r.nextFloat() * 0.03f,
                alpha = 0.55f,
                lit = 0.35f
            )
        }
    }

    private fun buildParticles() {
        drops.clear()
        flakes.clear()
        motes.clear()
        val base = when (kind) {
            WeatherKind.THUNDER, WeatherKind.HAIL -> 230
            WeatherKind.HEAVY_RAIN -> 200
            WeatherKind.RAIN -> 130
            WeatherKind.DRIZZLE, WeatherKind.FREEZING_RAIN, WeatherKind.SLEET -> 55
            else -> 0
        }
        val snowBase = when (kind) {
            WeatherKind.SNOW -> 150
            WeatherKind.SLEET -> 40
            else -> 0
        }
        val dropsTarget = (base * particleCap * (0.45f + intensity * 0.8f)).toInt()
        repeat(dropsTarget) {
            drops += Drop(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                speed = 0.55f + rnd.nextFloat() * 0.95f,
                length = 0.012f + rnd.nextFloat() * 0.045f,
                depth = 0.25f + rnd.nextFloat() * 0.75f,
                drift = (rnd.nextFloat() - 0.5f) * 0.06f
            )
        }
        repeat((snowBase * particleCap).toInt()) {
            flakes += Flake(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 0.0016f + rnd.nextFloat() * 0.0042f,
                speed = 0.018f + rnd.nextFloat() * 0.05f,
                phase = rnd.nextFloat() * 6.28f,
                depth = 0.3f + rnd.nextFloat() * 0.7f
            )
        }
        val moteCount = if (kind == WeatherKind.CLEAR && isDay) 26 else 14
        repeat(moteCount) {
            motes += Mote(
                x = rnd.nextFloat(),
                y = 0.08f + rnd.nextFloat() * 0.45f,
                radius = 0.5f + rnd.nextFloat() * 1.6f,
                speed = 0.005f + rnd.nextFloat() * 0.02f,
                phase = rnd.nextFloat() * 6.28f,
                warm = true
            )
        }
    }

    fun update(dt: Float, adaptive: Float): Boolean {
        if (frozen) return false
        time += dt
        cloudShift = sin(time * 0.16f) * 0.045f + wind * 0.014f * sin(time * 0.24f)
        val scaled = dt * motionScale * adaptive

        // Rain: falls across the whole viewport, originating under the cloud body.
        val gust = wind * 0.18f
        for (i in drops.indices) {
            val d = drops[i]
            d.y += d.speed * scaled * (0.55f + d.depth * 0.9f)
            d.x += (d.drift + gust * (0.3f + d.depth)) * scaled * 2.2f
            if (d.y > 1.02f) {
                d.y = -0.02f - rnd.nextFloat() * 0.25f
                d.x = 0.16f + rnd.nextFloat() * 0.68f
            }
            if (d.x > 1.05f) d.x -= 1.1f
            if (d.x < -0.05f) d.x += 1.1f
        }
        for (i in flakes.indices) {
            val f = flakes[i]
            f.y += f.speed * scaled * 4.2f * f.depth
            f.phase += scaled * (0.6f + f.depth)
            f.x += (sin(f.phase) * 0.0016f + gust * 0.02f) * scaled * 6f
            if (f.y > 1.03f) {
                f.y = -0.02f
                f.x = rnd.nextFloat()
            }
            if (f.x > 1.03f) f.x = -0.02f
            if (f.x < -0.03f) f.x = 1.02f
        }
        for (i in motes.indices) {
            val m = motes[i]
            m.phase += scaled * 0.5f
            m.y -= m.speed * scaled * 2.2f
            m.x += sin(m.phase) * 0.0008f
            if (m.y < 0.04f) {
                m.y = 0.5f
                m.x = rnd.nextFloat()
            }
        }

        // Lightning only exists for thunderstorm codes. It is an atmospheric cue, not a strike report.
        var triggered = false
        if (kind.isStorm) {
            nextFlashIn -= dt
            if (nextFlashIn <= 0f) {
                flashPhase = 0.9f
                flash = 1f
                nextFlashIn = 3.5f + rnd.nextFloat() * 6.5f
                flashCount++
                triggered = true
            }
        }
        if (flash > 0f) {
            flashPhase += dt
            flash = when {
                flashPhase < 0.06f -> 1f
                flashPhase < 0.12f -> 0.25f
                flashPhase < 0.18f -> 0.8f
                else -> (flash - dt * 2.6f).coerceAtLeast(0f)
            }
        }
        return triggered
    }

    fun draw(scope: DrawScope) {
        val w = width
        val h = height
        if (w <= 0f || h <= 0f) return

        drawSky(scope, w, h)

        if (kind == WeatherKind.CLEAR || kind == WeatherKind.MOSTLY_CLEAR || kind == WeatherKind.PARTLY_CLOUDY) {
            if (isDay) drawSun(scope, w, h) else drawNight(scope, w, h)
        } else {
            if (isDay) drawDiffusedLight(scope, w, h)
            drawStarsIfNight(scope, w, h)
        }

        drawFarClouds(scope, w, h)
        drawMainCloud(scope, w, h)
        drawFog(scope, w, h)
        drawMotes(scope, w, h)
        drawPrecipitation(scope, w, h)

        if (flash > 0.01f) {
            val a = flash * 0.16f
            scope.drawRect(Color(0xFFBFD4FF).copy(alpha = a))
            // Cloud body lights from within on a flash.
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE8EFFF).copy(alpha = flash * 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.24f),
                    radius = w * 0.5f
                ),
                topLeft = Offset(w * 0.08f, h * 0.02f),
                size = Size(w * 0.84f, h * 0.48f)
            )
        }
    }

    private fun drawSky(scope: DrawScope, w: Float, h: Float) {
        val palette = atmosphereFor(kind, isDay, hourOfDay)
        scope.drawRect(
            Brush.verticalGradient(
                0f to palette.skyTop,
                0.46f to palette.skyMid,
                1f to palette.skyBottom
            )
        )
        // Soft horizon glow, coloured by the same accent as the interface.
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.warm.copy(alpha = if (isDay) 0.13f else 0.05f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.62f),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.5f, h * 0.62f)
        )
    }

    private fun drawDiffusedLight(scope: DrawScope, w: Float, h: Float) {
        val center = Offset(w * 0.68f, h * 0.14f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE9D3A8).copy(alpha = if (isDay) 0.20f else 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = center
        )
    }

    private fun drawSun(scope: DrawScope, w: Float, h: Float) {
        val cx = w * 0.66f
        val cy = h * 0.20f
        val r = w * 0.058f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF4DDB3).copy(alpha = 0.42f),
                    Color(0xFFD9B981).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(cx, cy)
        )
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF6E6).copy(alpha = 0.96f), Color(0xFFEFD7A9).copy(alpha = 0.9f)),
                center = Offset(cx - r * 0.2f, cy - r * 0.2f),
                radius = r * 1.3f
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        // Slow corona ring.
        scope.drawCircle(
            color = Color(0xFFF0D8AC).copy(alpha = 0.06f + 0.02f * sin(time * 0.4f)),
            radius = r * 2.6f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f * density)
        )
    }

    private fun drawNight(scope: DrawScope, w: Float, h: Float) {
        drawStarsIfNight(scope, w, h)
        val cx = w * 0.7f
        val cy = h * 0.18f
        val r = w * 0.045f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDCE6F7).copy(alpha = 0.20f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = Offset(cx, cy)
        )
        scope.drawCircle(Color(0xFFE7EDF8).copy(alpha = 0.92f), radius = r, center = Offset(cx, cy))
        scope.drawCircle(Color(0xFFB9C6DC).copy(alpha = 0.35f), radius = r * 0.26f, center = Offset(cx - r * 0.32f, cy - r * 0.18f))
        scope.drawCircle(Color(0xFFB9C6DC).copy(alpha = 0.22f), radius = r * 0.16f, center = Offset(cx + r * 0.3f, cy + r * 0.26f))
    }

    private fun drawStarsIfNight(scope: DrawScope, w: Float, h: Float) {
        if (isDay) return
        if (kind == WeatherKind.OVERCAST || kind == WeatherKind.FOG ||
            kind == WeatherKind.RAIN || kind == WeatherKind.HEAVY_RAIN ||
            kind == WeatherKind.SNOW || kind == WeatherKind.THUNDER
        ) return
        val r = Random(42)
        repeat(46) {
            val x = r.nextFloat() * w
            val y = r.nextFloat() * h * 0.42f
            val alpha = 0.08f + r.nextFloat() * 0.22f
            scope.drawCircle(Color.White.copy(alpha = alpha), radius = 0.6f + r.nextFloat() * 1.1f, center = Offset(x, y))
        }
    }

    private fun drawFarClouds(scope: DrawScope, w: Float, h: Float) {
        if (kind == WeatherKind.CLEAR && isDay) return
        val drift = (sin(time * 0.05f) * 0.03f + time * 0.0018f) % 1f
        val baseAlpha = when {
            kind == WeatherKind.CLEAR -> 0.0f
            kind == WeatherKind.MOSTLY_CLEAR -> 0.34f
            kind == WeatherKind.PARTLY_CLOUDY -> 0.46f
            else -> 0.54f
        }
        if (baseAlpha == 0f) return
        val cy = h * 0.19f
        repeat(3) { i ->
            val cx = ((i * 0.42f + drift * 0.6f + 0.05f) % 1.25f - 0.12f) * w
            val cw = w * (0.34f + i * 0.06f)
            val ch = h * (0.06f + i * 0.012f)
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB9C7DB).copy(alpha = baseAlpha * 0.55f),
                        Color(0xFF5E6E86).copy(alpha = baseAlpha * 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy + i * h * 0.03f),
                    radius = cw * 0.6f
                ),
                topLeft = Offset(cx - cw / 2, cy - ch / 2 + i * h * 0.03f),
                size = Size(cw, ch * 2.1f)
            )
        }
    }

    private fun drawMainCloud(scope: DrawScope, w: Float, h: Float) {
        val showCloud = kind != WeatherKind.CLEAR
        if (!showCloud) return
        // Keep the sun visible for "mostly clear": nudge the cloud cluster left and lower.
        val clusterCenterX = w * if (kind == WeatherKind.MOSTLY_CLEAR) 0.42f else 0.5f
        val clusterCenterY = h * if (kind == WeatherKind.MOSTLY_CLEAR) 0.27f else 0.235f
        val scale = w * 0.9f

        val strength = when (kind) {
            WeatherKind.MOSTLY_CLEAR -> 0.68f
            WeatherKind.PARTLY_CLOUDY -> 0.86f
            WeatherKind.OVERCAST, WeatherKind.FOG -> 0.94f
            else -> 1f
        }
        val shadowTint = if (kind.isStorm) Color(0xFF141A26) else Color(0xFF161E2B)
        val midTint = if (kind.isStorm) Color(0xFF2D3648) else Color(0xFF394355)
        val palette = atmosphereFor(kind, isDay, hourOfDay)
        val rimTint = when {
            kind.isStorm -> palette.accent
            !isDay -> palette.accent
            else -> Color(0xFFC6D2E2)
        }

        // Floating dust / depth behind the object.
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9FB3CE).copy(alpha = 0.10f * strength), Color.Transparent),
                center = Offset(clusterCenterX, clusterCenterY + h * 0.06f),
                radius = w * 0.55f
            ),
            topLeft = Offset(clusterCenterX - w * 0.55f, clusterCenterY - h * 0.16f),
            size = Size(w * 1.1f, h * 0.46f)
        )

        for (p in cloudPuffs) {
            val bob = sin(time * 0.55f + p.rx * 5.2f) * h * 0.004f
            val px = clusterCenterX + (p.rx + cloudShift) * scale
            val py = clusterCenterY + p.ry * h * 0.9f + bob
            val rw = p.rw * scale
            val rh = p.rh * h * 2.4f
            val lit = p.lit * (if (isDay) 1f else 0.55f)
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rimTint.copy(alpha = 0.42f * p.alpha * strength * lit),
                        midTint.copy(alpha = 0.72f * p.alpha * strength),
                        shadowTint.copy(alpha = 0.82f * p.alpha * strength),
                        Color.Transparent
                    ),
                    center = Offset(px - rw * 0.12f, py - rh * 0.28f),
                    radius = rw * 0.62f
                ),
                topLeft = Offset(px - rw / 2, py - rh / 2),
                size = Size(rw, rh)
            )
        }

        // Dark, weighted base — the source of the rain.
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF101620).copy(alpha = 0.85f * strength),
                    Color(0xFF0B1119).copy(alpha = 0.5f * strength),
                    Color.Transparent
                ),
                center = Offset(clusterCenterX, clusterCenterY + h * 0.085f),
                radius = w * 0.42f
            ),
            topLeft = Offset(clusterCenterX - w * 0.44f, clusterCenterY + h * 0.01f),
            size = Size(w * 0.88f, h * 0.16f)
        )

        // Gold rim light: restrained, only a hint of warmth at the cloud's edge.
        if (!kind.isStorm) {
            val rimAlpha = if (isDay) 0.20f else 0.10f
            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, palette.warm.copy(alpha = rimAlpha), Color.Transparent),
                    center = Offset(clusterCenterX + w * 0.16f, clusterCenterY - h * 0.06f),
                    radius = w * 0.22f
                ),
                topLeft = Offset(clusterCenterX - w * 0.1f, clusterCenterY - h * 0.18f),
                size = Size(w * 0.62f, h * 0.3f)
            )
        }
    }

    private fun drawFog(scope: DrawScope, w: Float, h: Float) {
        if (kind != WeatherKind.FOG && kind != WeatherKind.OVERCAST) return
        val bands = if (kind == WeatherKind.FOG) 5 else 2
        val maxAlpha = if (kind == WeatherKind.FOG) 0.16f else 0.06f
        repeat(bands) { i ->
            val y = h * (0.30f + i * 0.12f)
            val phase = time * (0.05f + i * 0.013f) + i
            val dx = sin(phase) * w * 0.05f
            scope.drawOval(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.25f to Color(0xFFB9C6D8).copy(alpha = maxAlpha),
                    0.6f to Color(0xFF8FA1B8).copy(alpha = maxAlpha * 0.8f),
                    1f to Color.Transparent
                ),
                topLeft = Offset(-w * 0.2f + dx, y),
                size = Size(w * 1.4f, h * (0.07f + i * 0.014f))
            )
        }
    }

    private fun drawMotes(scope: DrawScope, w: Float, h: Float) {
        if (motes.isEmpty()) return
        for (m in motes) {
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * 0.9f + m.phase))
            scope.drawCircle(
                color = Atmos.Gold.copy(alpha = (if (isDay) 0.22f else 0.12f) * twinkle),
                radius = m.radius * density,
                center = Offset(m.x * w, m.y * h)
            )
        }
    }

    private fun drawPrecipitation(scope: DrawScope, w: Float, h: Float) {
        // Rain is drawn in depth layers: far drops are short and dim, near drops long and bright.
        for (d in drops) {
            val depth = d.depth
            val x = d.x * w
            val y = d.y * h
            val len = d.length * h * (0.55f + depth)
            val alpha = (0.06f + 0.30f * depth) * (0.55f + intensity * 0.6f) *
                (0.35f + precipitationProbability / 160f)
            val drift = (d.drift + wind * 0.12f) * h
            scope.drawLine(
                color = Atmos.Ice.copy(alpha = alpha.coerceIn(0f, 0.42f)),
                start = Offset(x, y),
                end = Offset(x + drift, y + len),
                strokeWidth = (0.5f + depth * 0.9f) * density
            )
        }
        for (f in flakes) {
            val x = f.x * w
            val y = f.y * h
            val alpha = (0.10f + 0.45f * f.depth)
            scope.drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = f.radius * min(w, h) * (0.6f + f.depth),
                center = Offset(x, y)
            )
        }
    }
}
