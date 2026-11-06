package com.alican.atmos.ui

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.alican.atmos.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.i18n.Loc
import com.alican.atmos.location.LocationHelper
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.screens.DetailSheet
import com.alican.atmos.ui.screens.ForecastScreen
import com.alican.atmos.ui.screens.HomeScreen
import com.alican.atmos.ui.screens.LocationSheet
import com.alican.atmos.ui.screens.MapScreen
import com.alican.atmos.ui.screens.MoreScreen
import com.alican.atmos.ui.screens.OfficialAlertSheet
import com.alican.atmos.ui.theme.Atmos
import kotlinx.coroutines.launch

@Composable
fun AtmosApp(vm: AtmosViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        vm.onLocationPermissionResult(granted)
    }

    fun requestLocation() {
        if (LocationHelper.hasPermission(context)) {
            vm.locate()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(Loc.tr(it))
            vm.toast(null)
        }
    }

    Box(Modifier.fillMaxSize().background(Atmos.Midnight)) {
        Crossfade(targetState = state.tab, animationSpec = tween(220), label = "tab") { tab ->
            stateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        state = state,
                        onOpenLocation = vm::openLocationPicker,
                        onRefresh = { vm.refresh(force = true) },
                        onOpenDetail = vm::openDetail,
                        onOpenSettings = { vm.setTab(AppTab.MORE) },
                        onLightning = { performLightningHaptic(context, state.settings) },
                        onOpenForecast = { vm.setTab(AppTab.FORECAST) },
                        onOpenMap = { vm.setTab(AppTab.MAP) },
                        onOpenAlert = vm::openAlert
                    )
                    AppTab.MAP -> MapScreen(
                        state = state,
                        onOpenLocation = vm::openLocationPicker,
                        onOpenForecast = { vm.setTab(AppTab.FORECAST) }
                    )
                    AppTab.FORECAST -> ForecastScreen(
                        state = state,
                        onOpenLocation = vm::openLocationPicker,
                        onOpenDetail = vm::openDetail
                    )
                    AppTab.MORE -> MoreScreen(
                        state = state,
                        onUpdateSettings = vm::updateSettings,
                        onOpenLocation = vm::openLocationPicker,
                        onRefresh = { vm.refresh(force = true) },
                        onRequestLocation = { requestLocation() },
                        onRemovePlace = vm::removeSavedPlace
                    )
                }
            }
        }

        BottomBar(
            tab = state.tab,
            onTab = vm::setTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp, start = 16.dp, end = 16.dp)) {
            SnackbarHost(snackbar)
        }
    }

    if (state.locationPickerOpen) {
        LocationSheet(
            state = state,
            onDismiss = vm::closeLocationPicker,
            onSearch = vm::onSearchQuery,
            onSelect = { place ->
                vm.selectPlace(place)
                scope.launch { snackbar.showSnackbar(Loc.t("Showing {}", place.name)) }
            },
            onUseLocation = { requestLocation() }
        )
    }

    state.detail?.let { card ->
        DetailSheet(card = card, state = state, onDismiss = vm::closeDetail)
    }

    state.detailAlert?.let { alert ->
        OfficialAlertSheet(alert = alert, onDismiss = vm::closeAlert)
    }
}

@Composable
private fun BottomBar(
    tab: AppTab,
    onTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Atmos.Surface.copy(alpha = 0.97f), Atmos.Charcoal.copy(alpha = 0.99f))
                    )
                )
                .border(0.7.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(AppTab.HOME, "Home", AtmosIcons.Home, tab, Modifier.weight(1f), onTab)
            NavItem(AppTab.MAP, "Map", AtmosIcons.Map, tab, Modifier.weight(1f), onTab)
            NavItem(AppTab.FORECAST, "Forecast", AtmosIcons.Calendar, tab, Modifier.weight(1f), onTab)
            NavItem(AppTab.MORE, "More", AtmosIcons.More, tab, Modifier.weight(1f), onTab)
        }
    }
}

@Composable
private fun NavItem(
    item: AppTab,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: AppTab,
    modifier: Modifier = Modifier,
    onTab: (AppTab) -> Unit
) {
    val active = item == selected
    val alpha by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "navAlpha")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Atmos.Ice.copy(alpha = 0.10f * alpha))
            .clickable(interactionSource = interaction, indication = null) { onTab(item) }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Atmos.Ice else Atmos.Muted,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = if (active) Atmos.White else Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Atmos.Ice.copy(alpha = 0.85f))
            )
        }
    }
}

private fun performLightningHaptic(context: Context, settings: AtmosSettings) {
    if (!settings.lightningHaptics) return
    runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(26, 90))
        }
    }
}
