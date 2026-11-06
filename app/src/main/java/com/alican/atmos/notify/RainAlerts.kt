package com.alican.atmos.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.alican.atmos.MainActivity
import com.alican.atmos.R
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.MinutePoint
import com.alican.atmos.core.WeatherData
import com.alican.atmos.data.AtmosStore
import com.alican.atmos.data.OpenMeteo
import com.alican.atmos.i18n.Loc
import com.alican.atmos.ui.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Short-range rain outlook derived only from the 15-minute nowcast series the API returned.
 * Nothing is extrapolated beyond the published window and no onset time is invented:
 * when the series is missing or ambiguous, the outlook is null.
 */
data class RainOutlook(
    val rainingNow: Boolean,
    val startsInMinutes: Int?,
    val endsInMinutes: Int?,
    val amountNext60mm: Double,
    val peakProbability: Int,
    val horizonMinutes: Int
) {
    val actionWorthy: Boolean
        get() = (startsInMinutes != null && startsInMinutes <= 45 && (amountNext60mm >= 0.15 || peakProbability >= 50))
}

object Nowcast {

    fun outlook(data: WeatherData): RainOutlook? {
        val points = data.minutely
        if (points.size < 3) return null
        val stepMinutes = stepMinutes(points)
        val first = points.first()
        val rainingNow = first.precipitation > 0.02
        val wetThreshold = 0.02

        val next60 = points.takeWhile { minutesBetween(first.time, it.time) < 60 }
        val amount = next60.sumOf { it.precipitation }
        val peakProb = next60.mapNotNull { it.probability }.maxOrNull() ?: 0

        if (rainingNow) {
            val dryIndex = points.indexOfFirst { it.precipitation <= wetThreshold && minutesBetween(first.time, it.time) > 0 }
            val endsIn = if (dryIndex > 0) minutesBetween(first.time, points[dryIndex].time) else null
            return RainOutlook(true, null, endsIn, amount, peakProb, points.size * stepMinutes)
        }

        val wetIndex = points.indexOfFirst { it.precipitation > wetThreshold }
        if (wetIndex <= 0) {
            return RainOutlook(false, null, null, amount, peakProb, points.size * stepMinutes)
        }
        val startsIn = minutesBetween(first.time, points[wetIndex].time)
        val endsIn = points.drop(wetIndex).indexOfFirst { it.precipitation <= wetThreshold }
            .takeIf { it > 0 }
            ?.let { wetIndex + it }
            ?.let { minutesBetween(first.time, points[it].time) }
        return RainOutlook(false, startsIn, endsIn, amount, peakProb, points.size * stepMinutes)
    }

    private fun stepMinutes(points: List<MinutePoint>): Int {
        val deltas = points.zipWithNext { a, b -> minutesBetween(a.time, b.time) }.filter { it > 0 }
        return deltas.sorted().getOrNull(deltas.size / 2) ?: 15
    }

    private fun minutesBetween(from: java.time.LocalDateTime, to: java.time.LocalDateTime): Int =
        java.time.Duration.between(from, to).toMinutes().toInt()
}

/**
 * Local, opt-in rain notifications. The app never pushes marketing messages and never
 * claims an official warning; the notification text always names the source as a nowcast.
 */
object RainAlerts {
    private const val CHANNEL = "rain_nowcast"
    private const val NOTIFICATION_ID = 41
    private const val ACTION = "com.alican.atmos.RAIN_CHECK"
    private const val KEY_LAST_SIG = "rain_alert_signature"
    private const val KEY_LAST_AT = "rain_alert_at"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun schedule(context: Context, settings: AtmosSettings) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pending(context)
        if (!settings.rainAlerts) {
            manager.cancel(pending)
            cancelNotification(context)
            return
        }
        manager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + 10 * 60_000L,
            15 * 60_000L,
            pending
        )
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, RainAlertReceiver::class.java).apply { action = ACTION }
        return PendingIntent.getBroadcast(
            context, 9, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Evaluate cached data and notify only on a fresh, actionable change of state. */
    fun evaluate(context: Context, data: WeatherData, settings: AtmosSettings, force: Boolean = false) {
        if (!settings.rainAlerts || !canPost(context)) return
        val outlook = Nowcast.outlook(data) ?: return
        if (!outlook.actionWorthy) {
            // Clear a stale notification once the rain has started or the window passed.
            if (outlook.rainingNow) cancelNotification(context)
            return
        }
        val prefs = context.getSharedPreferences("atmos_alerts", Context.MODE_PRIVATE)
        val lastAt = prefs.getLong(KEY_LAST_AT, 0L)
        val now = System.currentTimeMillis()
        // One gentle nudge per 90 minutes at most; refreshing the app must not spam.
        if (!force && lastAt > 0 && now - lastAt < 90 * 60_000L) return
        val signature = "${data.place.id}|${outlook.startsInMinutes}"

        val minutes = outlook.startsInMinutes ?: return
        val whenText = if (minutes <= 10) Loc.tr("any minute now") else Loc.t("in about {} min", minutes)
        val amount = "${Fmt.precip(outlook.amountNext60mm, settings.precipitationUnit)} ${settings.precipitationUnit.symbol}"
        val title = Loc.t("Rain likely {}", whenText)
        val body = Loc.t(
            "Nowcast for {}: up to {} in the next hour, {}% peak chance. Not an official warning.",
            data.place.name, amount, outlook.peakProbability
        )
        post(context, title, body)
        prefs.edit().putString(KEY_LAST_SIG, signature).putLong(KEY_LAST_AT, System.currentTimeMillis()).apply()
    }

    private fun post(context: Context, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL,
                Loc.tr("Rain nowcast"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = Loc.tr("Short-range rain onset from the 15-minute nowcast")
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 11, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_w_rain)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }

    fun refreshAndEvaluate(context: Context) {
        scope.launch { runCheck(context.applicationContext) }
    }

    /** Receiver entry point: owns the async work and finishes the broadcast when done. */
    fun launchCheck(context: Context, onDone: () -> Unit = {}) {
        scope.launch {
            try {
                runCheck(context.applicationContext)
            } finally {
                onDone()
            }
        }
    }

    suspend fun runCheck(context: Context) {
        val store = AtmosStore(context)
        val cached = store.lastBundle ?: return
        val age = System.currentTimeMillis() - cached.fetchedAt
        val bundle = if (age < 20 * 60_000L) cached else runCatching {
            OpenMeteo.fetchBundle(cached.place, store.settings.model).also { store.lastBundle = it }
        }.getOrNull() ?: cached
        val data = runCatching { OpenMeteo.parse(bundle) }.getOrNull() ?: return
        evaluate(context, data, store.settings)
    }
}

class RainAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        RainAlerts.launchCheck(context) { runCatching { pending.finish() } }
    }
}

/** Re-arms the opt-in 15-minute rain check after a reboot (alarms do not survive it). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching {
            val settings = AtmosStore(context).settings
            Loc.apply(settings.language)
            RainAlerts.schedule(context, settings)
        }
    }
}
