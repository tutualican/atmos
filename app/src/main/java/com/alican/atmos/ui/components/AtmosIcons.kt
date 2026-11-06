package com.alican.atmos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Minimal Lucide-style line icon set, hand-built so ATMOS ships no oversized icon pack.
 * 24x24 viewport, 1.6dp stroke, round caps — the calm geometric language of the reference.
 */
object AtmosIcons {
    private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(build).build()

    private fun ImageVector.Builder.stroke(vararg pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) { pathData.forEach { it() } }
    }

    val Location = icon("location") {
        stroke({
            moveTo(20f, 10f)
            curveTo(20f, 16f, 12f, 22f, 12f, 22f)
            curveTo(12f, 22f, 4f, 16f, 4f, 10f)
            curveTo(4f, 5.58f, 7.58f, 2f, 12f, 2f)
            curveTo(16.42f, 2f, 20f, 5.58f, 20f, 10f)
            close()
        }, {
            moveTo(12f, 13f)
            curveTo(13.66f, 13f, 15f, 11.66f, 15f, 10f)
            curveTo(15f, 8.34f, 13.66f, 7f, 12f, 7f)
            curveTo(10.34f, 7f, 9f, 8.34f, 9f, 10f)
            curveTo(9f, 11.66f, 10.34f, 13f, 12f, 13f)
            close()
        })
    }

    val Crosshair = icon("crosshair") {
        stroke({
            moveTo(12f, 2f); lineTo(12f, 6f); moveTo(12f, 18f); lineTo(12f, 22f)
            moveTo(2f, 12f); lineTo(6f, 12f); moveTo(18f, 12f); lineTo(22f, 12f)
            moveTo(12f, 17f); curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
            curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f); curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
            curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f); close()
        })
    }

    val ChevronDown = icon("chevronDown") {
        stroke({ moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) })
    }

    val ChevronRight = icon("chevronRight") {
        stroke({ moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f) })
    }

    val Settings = icon("settings") {
        stroke({
            moveTo(12f, 15f); curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f); curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f); close()
        }, {
            moveTo(19.4f, 15f)
            curveTo(19.67f, 14.37f, 19.86f, 13.7f, 19.94f, 13f)
            lineTo(21.54f, 13f); lineTo(21.54f, 11f); lineTo(19.94f, 11f)
            curveTo(19.86f, 10.3f, 19.67f, 9.63f, 19.4f, 9f)
            lineTo(20.79f, 8.2f); lineTo(19.79f, 6.47f); lineTo(18.4f, 7.27f)
            curveTo(17.9f, 6.78f, 17.32f, 6.37f, 16.68f, 6.06f)
            lineTo(17.07f, 4.5f); lineTo(15.34f, 3.5f); lineTo(14.95f, 5.06f)
            curveTo(14.64f, 5f, 14.32f, 4.97f, 14f, 4.97f)
            curveTo(13.68f, 4.97f, 13.36f, 5f, 13.05f, 5.06f)
            lineTo(12.66f, 3.5f); lineTo(10.93f, 4.5f); lineTo(11.32f, 6.06f)
            curveTo(10.68f, 6.37f, 10.1f, 6.78f, 9.6f, 7.27f)
            lineTo(8.21f, 6.47f); lineTo(7.21f, 8.2f); lineTo(8.6f, 9f)
            curveTo(8.33f, 9.63f, 8.14f, 10.3f, 8.06f, 11f)
            lineTo(6.46f, 11f); lineTo(6.46f, 13f); lineTo(8.06f, 13f)
            curveTo(8.14f, 13.7f, 8.33f, 14.37f, 8.6f, 15f)
            lineTo(7.21f, 15.8f); lineTo(8.21f, 17.53f); lineTo(9.6f, 16.73f)
            curveTo(10.1f, 17.22f, 10.68f, 17.63f, 11.32f, 17.94f)
            lineTo(10.93f, 19.5f); lineTo(12.66f, 20.5f); lineTo(13.05f, 18.94f)
            curveTo(13.36f, 19f, 13.68f, 19.03f, 14f, 19.03f)
            curveTo(14.32f, 19.03f, 14.64f, 19f, 14.95f, 18.94f)
            lineTo(15.34f, 20.5f); lineTo(17.07f, 19.5f); lineTo(16.68f, 17.94f)
            curveTo(17.32f, 17.63f, 17.9f, 17.22f, 18.4f, 16.73f)
            lineTo(19.79f, 17.53f); lineTo(20.79f, 15.8f); close()
        })
    }

    val Refresh = icon("refresh") {
        stroke({
            moveTo(21f, 12f)
            curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
            curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
            curveTo(14.76f, 3f, 17.26f, 4.24f, 18.9f, 6.1f)
            lineTo(21f, 8f)
        }, { moveTo(21f, 3f); lineTo(21f, 8f); lineTo(16f, 8f) })
    }

    val Search = icon("search") {
        stroke({
            moveTo(11f, 19f); curveTo(15.42f, 19f, 19f, 15.42f, 19f, 11f)
            curveTo(19f, 6.58f, 15.42f, 3f, 11f, 3f); curveTo(6.58f, 3f, 3f, 6.58f, 3f, 11f)
            curveTo(3f, 15.42f, 6.58f, 19f, 11f, 19f); close()
            moveTo(21f, 21f); lineTo(16.65f, 16.65f)
        })
    }

    val Close = icon("close") {
        stroke({ moveTo(18f, 6f); lineTo(6f, 18f); moveTo(6f, 6f); lineTo(18f, 18f) })
    }

    val Sun = icon("sun") {
        stroke({
            moveTo(12f, 16f); curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f)
            curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f); curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
            curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f); close()
            moveTo(12f, 1.5f); lineTo(12f, 3.5f); moveTo(12f, 20.5f); lineTo(12f, 22.5f)
            moveTo(4.22f, 4.22f); lineTo(5.64f, 5.64f); moveTo(18.36f, 18.36f); lineTo(19.78f, 19.78f)
            moveTo(1.5f, 12f); lineTo(3.5f, 12f); moveTo(20.5f, 12f); lineTo(22.5f, 12f)
            moveTo(4.22f, 19.78f); lineTo(5.64f, 18.36f); moveTo(18.36f, 5.64f); lineTo(19.78f, 4.22f)
        })
    }

    val Moon = icon("moon") {
        stroke({
            moveTo(21f, 12.8f)
            curveTo(20.2f, 16.9f, 16.6f, 20f, 12.2f, 20f)
            curveTo(7.3f, 20f, 3.3f, 16f, 3.3f, 11.1f)
            curveTo(3.3f, 6.8f, 6.4f, 3.2f, 10.5f, 2.4f)
            curveTo(9.3f, 4f, 8.7f, 5.9f, 8.7f, 7.9f)
            curveTo(8.7f, 12.8f, 12.7f, 16.8f, 17.6f, 16.8f)
            curveTo(18.8f, 16.8f, 19.9f, 16.5f, 21f, 15.9f)
            close()
        })
    }

    val Cloud = icon("cloud") {
        stroke({
            moveTo(17.5f, 19f)
            curveTo(19.71f, 19f, 21.5f, 17.21f, 21.5f, 15f)
            curveTo(21.5f, 12.9f, 19.9f, 11.2f, 17.8f, 11f)
            curveTo(17.4f, 7.2f, 14.2f, 4.3f, 10.3f, 4.3f)
            curveTo(6.4f, 4.3f, 3.2f, 7.5f, 3.2f, 11.4f)
            curveTo(3.2f, 11.8f, 3.2f, 12.2f, 3.3f, 12.6f)
            curveTo(1.9f, 13.2f, 1f, 14.5f, 1f, 16f)
            curveTo(1f, 17.7f, 2.3f, 19f, 4f, 19f)
            close()
        })
    }

    val CloudRain = icon("cloudRain") {
        stroke({
            moveTo(17.5f, 15f)
            curveTo(19.71f, 15f, 21.5f, 13.21f, 21.5f, 11f)
            curveTo(21.5f, 8.9f, 19.9f, 7.2f, 17.8f, 7f)
            curveTo(17.4f, 3.2f, 14.2f, 0.3f, 10.3f, 0.3f)
            curveTo(6.4f, 0.3f, 3.2f, 3.5f, 3.2f, 7.4f)
            curveTo(3.2f, 7.8f, 3.2f, 8.2f, 3.3f, 8.6f)
            curveTo(1.9f, 9.2f, 1f, 10.5f, 1f, 12f)
            curveTo(1f, 13.7f, 2.3f, 15f, 4f, 15f)
            close()
            moveTo(8f, 19f); lineTo(7f, 22f); moveTo(12f, 19f); lineTo(11f, 22f); moveTo(16f, 19f); lineTo(15f, 22f)
        })
    }

    val CloudSnow = icon("cloudSnow") {
        stroke({
            moveTo(17.5f, 14f)
            curveTo(19.71f, 14f, 21.5f, 12.21f, 21.5f, 10f)
            curveTo(21.5f, 7.9f, 19.9f, 6.2f, 17.8f, 6f)
            curveTo(17.4f, 2.2f, 14.2f, -0.7f, 10.3f, -0.7f)
            curveTo(6.4f, -0.7f, 3.2f, 2.5f, 3.2f, 6.4f)
            curveTo(3.2f, 6.8f, 3.2f, 7.2f, 3.3f, 7.6f)
            curveTo(1.9f, 8.2f, 1f, 9.5f, 1f, 11f)
            curveTo(1f, 12.7f, 2.3f, 14f, 4f, 14f)
            close()
            moveTo(8f, 18f); lineTo(8f, 18.1f); moveTo(12f, 21f); lineTo(12f, 21.1f); moveTo(16f, 18f); lineTo(16f, 18.1f)
            moveTo(10f, 20.5f); lineTo(10f, 20.6f); moveTo(14f, 20.5f); lineTo(14f, 20.6f)
        })
    }

    val CloudFog = icon("cloudFog") {
        stroke({
            moveTo(16f, 13f)
            curveTo(17.66f, 13f, 19f, 11.66f, 19f, 10f)
            curveTo(19f, 8.34f, 17.66f, 7f, 16f, 7f)
            curveTo(15.8f, 7f, 15.6f, 7.05f, 15.4f, 7.1f)
            curveTo(14.8f, 4.5f, 12.4f, 2.5f, 9.5f, 2.5f)
            curveTo(6.2f, 2.5f, 3.5f, 5.2f, 3.5f, 8.5f)
            curveTo(1.9f, 9f, 0.7f, 10.5f, 0.7f, 12.2f)
            moveTo(2f, 17f); lineTo(16f, 17f); moveTo(4f, 21f); lineTo(20f, 21f)
        })
    }

    val CloudLightning = icon("cloudLightning") {
        stroke({
            moveTo(17.5f, 14f)
            curveTo(19.71f, 14f, 21.5f, 12.21f, 21.5f, 10f)
            curveTo(21.5f, 7.9f, 19.9f, 6.2f, 17.8f, 6f)
            curveTo(17.4f, 2.2f, 14.2f, -0.7f, 10.3f, -0.7f)
            curveTo(6.4f, -0.7f, 3.2f, 2.5f, 3.2f, 6.4f)
            curveTo(3.2f, 6.8f, 3.2f, 7.2f, 3.3f, 7.6f)
            curveTo(1.9f, 8.2f, 1f, 9.5f, 1f, 11f)
            curveTo(1f, 12.7f, 2.3f, 14f, 4f, 14f)
            close()
            moveTo(13f, 16f); lineTo(9f, 22f); lineTo(13f, 22f); lineTo(11f, 26f)
        })
    }

    val Droplet = icon("droplet") {
        stroke({
            moveTo(12f, 2.7f)
            curveTo(12f, 2.7f, 5f, 10.3f, 5f, 14.7f)
            curveTo(5f, 18.6f, 8.1f, 21.7f, 12f, 21.7f)
            curveTo(15.9f, 21.7f, 19f, 18.6f, 19f, 14.7f)
            curveTo(19f, 10.3f, 12f, 2.7f, 12f, 2.7f)
            close()
        })
    }

    val Wind = icon("wind") {
        stroke({
            moveTo(3f, 8f); lineTo(13f, 8f)
            curveTo(15f, 8f, 15f, 4.5f, 12.7f, 4.5f)
            curveTo(11.6f, 4.5f, 11.2f, 5.3f, 11f, 6f)
            moveTo(3f, 12f); lineTo(18f, 12f)
            curveTo(20.2f, 12f, 20.2f, 16f, 17.7f, 16f)
            curveTo(16.6f, 16f, 16.2f, 15.2f, 16f, 14.5f)
            moveTo(3f, 16f); lineTo(9f, 16f)
        })
    }

    val Gauge = icon("gauge") {
        stroke({
            moveTo(12f, 14f); lineTo(16.5f, 9.5f)
            moveTo(21f, 12f)
            curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
            curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
            curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f); close()
        })
    }

    val Eye = icon("eye") {
        stroke({
            moveTo(2f, 12f)
            curveTo(4.5f, 7f, 8f, 4.8f, 12f, 4.8f)
            curveTo(16f, 4.8f, 19.5f, 7f, 22f, 12f)
            curveTo(19.5f, 17f, 16f, 19.2f, 12f, 19.2f)
            curveTo(8f, 19.2f, 4.5f, 17f, 2f, 12f); close()
            moveTo(12f, 15f); curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f); close()
        })
    }

    val Leaf = icon("leaf") {
        stroke({
            moveTo(11f, 20f)
            curveTo(6f, 20f, 3f, 16.5f, 3f, 11.5f)
            curveTo(3f, 6.5f, 7f, 3f, 12.5f, 3f)
            curveTo(17.5f, 3f, 21f, 6f, 21f, 11f)
            curveTo(21f, 16f, 17f, 20f, 11f, 20f); close()
            moveTo(3f, 21f); lineTo(14f, 10f)
        })
    }

    val Thermometer = icon("thermometer") {
        stroke({
            moveTo(14f, 14.8f); lineTo(14f, 5f)
            curveTo(14f, 3.9f, 13.1f, 3f, 12f, 3f)
            curveTo(10.9f, 3f, 10f, 3.9f, 10f, 5f)
            lineTo(10f, 14.8f)
            curveTo(9.4f, 15.3f, 9f, 16.1f, 9f, 17f)
            curveTo(9f, 18.66f, 10.34f, 20f, 12f, 20f)
            curveTo(13.66f, 20f, 15f, 18.66f, 15f, 17f)
            curveTo(15f, 16.1f, 14.6f, 15.3f, 14f, 14.8f); close()
        })
    }

    val Compass = icon("compass") {
        stroke({
            moveTo(12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f); close()
            moveTo(16.2f, 7.8f); lineTo(14f, 14f); lineTo(7.8f, 16.2f); lineTo(10f, 10f); close()
        })
    }

    val Clock = icon("clock") {
        stroke({
            moveTo(12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f); close()
            moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f)
        })
    }

    val Map = icon("map") {        stroke({
            moveTo(9f, 4f); lineTo(3f, 6.5f); lineTo(3f, 20f); lineTo(9f, 17.5f)
            lineTo(15f, 20.5f); lineTo(21f, 18f); lineTo(21f, 4.5f); lineTo(15f, 7f); close()
            moveTo(9f, 4f); lineTo(15f, 7f); lineTo(15f, 20.5f)
            moveTo(9f, 4f); lineTo(9f, 17.5f)
        })
    }

    val Calendar = icon("calendar") {
        stroke({
            moveTo(5f, 5.5f); lineTo(19f, 5.5f)
            curveTo(20.1f, 5.5f, 21f, 6.4f, 21f, 7.5f); lineTo(21f, 19f)
            curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f); lineTo(5f, 21f)
            curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f); lineTo(3f, 7.5f)
            curveTo(3f, 6.4f, 3.9f, 5.5f, 5f, 5.5f); close()
            moveTo(8f, 3f); lineTo(8f, 7f); moveTo(16f, 3f); lineTo(16f, 7f); moveTo(3f, 10.5f); lineTo(21f, 10.5f)
        })
    }

    val More = icon("more") {
        stroke({
            moveTo(5f, 12f); lineTo(5.01f, 12f); moveTo(12f, 12f); lineTo(12.01f, 12f); moveTo(19f, 12f); lineTo(19.01f, 12f)
        })
    }

    val Menu = icon("menu") {
        stroke({ moveTo(3f, 6f); lineTo(21f, 6f); moveTo(3f, 12f); lineTo(21f, 12f); moveTo(3f, 18f); lineTo(15f, 18f) })
    }

    val Warning = icon("warning") {
        stroke({
            moveTo(10.3f, 3.9f); lineTo(2.2f, 18f)
            curveTo(1.6f, 19f, 2.3f, 20.3f, 3.6f, 20.3f); lineTo(20.4f, 20.3f)
            curveTo(21.7f, 20.3f, 22.4f, 19f, 21.8f, 18f); lineTo(13.7f, 3.9f)
            curveTo(13f, 2.8f, 11f, 2.8f, 10.3f, 3.9f); close()
            moveTo(12f, 9f); lineTo(12f, 13f); moveTo(12f, 17f); lineTo(12.01f, 17f)
        })
    }

    val Info = icon("info") {
        stroke({
            moveTo(12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f); close()
            moveTo(12f, 16f); lineTo(12f, 11f); moveTo(12f, 8f); lineTo(12.01f, 8f)
        })
    }

    val Play = icon("play") {
        stroke({ moveTo(7f, 4.5f); lineTo(19f, 12f); lineTo(7f, 19.5f); close() })
    }

    val Pause = icon("pause") {
        stroke({ moveTo(9f, 4.5f); lineTo(9f, 19.5f); moveTo(15f, 4.5f); lineTo(15f, 19.5f) })
    }

    val ArrowUp = icon("arrowUp") {
        stroke({ moveTo(12f, 20f); lineTo(12f, 4f); moveTo(5f, 11f); lineTo(12f, 4f); lineTo(19f, 11f) })
    }

    val Navigation = icon("navigation") {
        stroke({ moveTo(3f, 11f); lineTo(21f, 3f); lineTo(13f, 21f); lineTo(11f, 13f); close() })
    }

    val Sunrise = icon("sunrise") {
        stroke({
            moveTo(12f, 3f); lineTo(12f, 9f); moveTo(9f, 6f); lineTo(12f, 3f); lineTo(15f, 6f)
            moveTo(4f, 17f); lineTo(2f, 17f); moveTo(22f, 17f); lineTo(20f, 17f)
            moveTo(6.3f, 11.3f); lineTo(4.9f, 9.9f); moveTo(17.7f, 11.3f); lineTo(19.1f, 9.9f)
            moveTo(3f, 21f); lineTo(21f, 21f)
            moveTo(7f, 17f); curveTo(7f, 14.2f, 9.2f, 12f, 12f, 12f)
            curveTo(14.8f, 12f, 17f, 14.2f, 17f, 17f)
        })
    }

    val Sunset = icon("sunset") {
        stroke({
            moveTo(12f, 9f); lineTo(12f, 3f); moveTo(9f, 6f); lineTo(12f, 9f); lineTo(15f, 6f)
            moveTo(4f, 17f); lineTo(2f, 17f); moveTo(22f, 17f); lineTo(20f, 17f)
            moveTo(6.3f, 11.3f); lineTo(4.9f, 9.9f); moveTo(17.7f, 11.3f); lineTo(19.1f, 9.9f)
            moveTo(3f, 21f); lineTo(21f, 21f)
            moveTo(7f, 17f); curveTo(7f, 14.2f, 9.2f, 12f, 12f, 12f)
            curveTo(14.8f, 12f, 17f, 14.2f, 17f, 17f)
        })
    }

    val Sparkle = icon("sparkle") {
        stroke({
            moveTo(12f, 3f); lineTo(13.8f, 9.2f); lineTo(20f, 11f); lineTo(13.8f, 12.8f)
            lineTo(12f, 19f); lineTo(10.2f, 12.8f); lineTo(4f, 11f); lineTo(10.2f, 9.2f); close()
        })
    }

    val Layers = icon("layers") {
        stroke({
            moveTo(12f, 2.5f); lineTo(21f, 7.2f); lineTo(12f, 11.9f); lineTo(3f, 7.2f); close()
            moveTo(3f, 12f); lineTo(12f, 16.7f); lineTo(21f, 12f)
            moveTo(3f, 16.8f); lineTo(12f, 21.5f); lineTo(21f, 16.8f)
        })
    }

    val External = icon("external") {
        stroke({
            moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f)
            moveTo(10f, 14f); lineTo(21f, 3f)
            moveTo(18f, 13f); lineTo(18f, 19f)
            curveTo(18f, 20.1f, 17.1f, 21f, 16f, 21f); lineTo(5f, 21f)
            curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f); lineTo(3f, 8f)
            curveTo(3f, 6.9f, 3.9f, 6f, 5f, 6f); lineTo(11f, 6f)
        })
    }

    val Snowflake = icon("snowflake") {
        stroke({
            moveTo(12f, 2f); lineTo(12f, 22f)
            moveTo(3.3f, 7f); lineTo(20.7f, 17f)
            moveTo(3.3f, 17f); lineTo(20.7f, 7f)
            moveTo(9f, 4f); lineTo(12f, 7f); lineTo(15f, 4f)
            moveTo(9f, 20f); lineTo(12f, 17f); lineTo(15f, 20f)
        })
    }

    val SunriseSmall = icon("sunriseSmall") {
        stroke({
            moveTo(12f, 4f); lineTo(12f, 12f); moveTo(8.5f, 8f); lineTo(12f, 11.5f); lineTo(15.5f, 8f)
            moveTo(3f, 18f); lineTo(21f, 18f)
        })
    }

    val Plus = icon("plus") {
        stroke({ moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) })
    }

    val Home = icon("home") {
        stroke({
            moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
            moveTo(5.5f, 9.5f); lineTo(5.5f, 20f)
            curveTo(5.5f, 20.6f, 6f, 21f, 6.5f, 21f)
            lineTo(17.5f, 21f)
            curveTo(18f, 21f, 18.5f, 20.6f, 18.5f, 20f)
            lineTo(18.5f, 9.5f)
            moveTo(10f, 21f); lineTo(10f, 15.5f)
            curveTo(10f, 14.7f, 10.7f, 14f, 11.5f, 14f)
            lineTo(12.5f, 14f)
            curveTo(13.3f, 14f, 14f, 14.7f, 14f, 15.5f)
            lineTo(14f, 21f)
        })
    }

    val Minus = icon("minus") {
        stroke({ moveTo(5f, 12f); lineTo(19f, 12f) })
    }

    val Widget = icon("widget") {
        stroke({
            moveTo(4f, 4f); lineTo(9f, 4f); lineTo(9f, 9f); lineTo(4f, 9f); close()
            moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f); lineTo(15f, 9f); close()
            moveTo(4f, 15f); lineTo(9f, 15f); lineTo(9f, 20f); lineTo(4f, 20f); close()
            moveTo(15f, 15f); lineTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f); close()
        })
    }

    val Bell = icon("bell") {
        stroke({
            moveTo(18f, 8f)
            curveTo(18f, 4.7f, 15.3f, 2f, 12f, 2f)
            curveTo(8.7f, 2f, 6f, 4.7f, 6f, 8f)
            curveTo(6f, 14f, 3f, 16f, 3f, 16f)
            lineTo(21f, 16f)
            curveTo(21f, 16f, 18f, 14f, 18f, 8f)
            close()
            moveTo(13.7f, 20f)
            curveTo(13.3f, 20.7f, 12.7f, 21.2f, 12f, 21.2f)
            curveTo(11.3f, 21.2f, 10.7f, 20.7f, 10.3f, 20f)
        })
    }
}
