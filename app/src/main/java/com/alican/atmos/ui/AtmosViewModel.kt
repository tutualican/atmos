package com.alican.atmos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.Place
import com.alican.atmos.core.WeatherData
import com.alican.atmos.data.WeatherRepository
import com.alican.atmos.i18n.Loc
import com.alican.atmos.location.LocationHelper
import com.alican.atmos.widget.AtmosWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab { HOME, MAP, FORECAST, MORE }

enum class DetailCard { UV, WIND, HUMIDITY, PRESSURE, VISIBILITY, AIR, PRECIP, SUN }

data class AtmosUiState(
    val place: Place,
    val data: WeatherData? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val fromCache: Boolean = false,
    val settings: AtmosSettings = AtmosSettings(),
    val savedPlaces: List<Place> = emptyList(),
    val tab: AppTab = AppTab.HOME,
    val locationPickerOpen: Boolean = false,
    val detail: DetailCard? = null,
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    val locating: Boolean = false,
    val now: Long = System.currentTimeMillis(),
    val toast: String? = null
)

class AtmosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WeatherRepository(app)
    private val store = repo.store
    private val context = app.applicationContext

    private val defaultPlace = Place(
        id = "default-istanbul",
        name = "Istanbul",
        admin = "Istanbul",
        country = "Türkiye",
        lat = 41.0082,
        lon = 28.9784
    )

    private val _state = MutableStateFlow(
        AtmosUiState(
            place = store.selectedPlace ?: defaultPlace,
            settings = store.settings,
            savedPlaces = store.savedPlaces,
            tab = AppTab.HOME
        )
    )
    val state: StateFlow<AtmosUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var autoJob: Job? = null

    init {
        Loc.apply(store.settings.language)
        restoreCache()
        viewModelScope.launch {
            val granted = LocationHelper.hasPermission(context)
            if (granted && store.settings.useLocation) {
                locate(silent = true)
            } else {
                refresh(force = true)
            }
        }
        startAutoRefresh()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _state.update { it.copy(now = System.currentTimeMillis()) }
            }
        }
    }

    private fun restoreCache() {
        val cached = store.lastBundle ?: return
        runCatching {
            val data = com.alican.atmos.data.OpenMeteo.parse(cached)
            _state.update {
                it.copy(
                    place = cached.place,
                    data = data,
                    loading = false,
                    fromCache = true
                )
            }
        }
    }

    private fun startAutoRefresh() {
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (true) {
                val minutes = _state.value.settings.refreshMinutes.coerceIn(5, 120)
                delay(minutes * 60_000L)
                refresh(force = true, silent = true)
            }
        }
    }

    fun refresh(force: Boolean = true, silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        val current = _state.value
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = it.data == null,
                    refreshing = it.data != null && !silent,
                    error = null
                )
            }
            try {
                val result = withContext(Dispatchers.IO) {
                    repo.load(current.place, forceRefresh = force, settings = current.settings)
                }
                _state.update {
                    it.copy(
                        data = result.data,
                        place = result.data.place,
                        loading = false,
                        refreshing = false,
                        fromCache = result.fromCache,
                        error = result.errorMessage?.takeIf { result.fromCache }
                    )
                }
                withContext(Dispatchers.IO) { AtmosWidgets.refreshAll(context) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = t.message ?: "Unable to reach the weather service",
                        fromCache = it.data != null
                    )
                }
            }
        }
    }

    fun locate(silent: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val place = LocationHelper.currentPlace(context)
            if (place == null) {
                _state.update { it.copy(locating = false) }
                if (!silent) {
                    _state.update { it.copy(toast = "Location unavailable. Choose a city manually.") }
                }
                if (_state.value.data == null) refresh(force = true)
                return@launch
            }
            _state.update { it.copy(locating = false, locationPickerOpen = false) }
            selectPlace(place, save = false)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) locate() else {
            _state.update { it.copy(toast = "Location permission denied. Using saved city.") }
            if (_state.value.data == null) refresh(force = true)
        }
    }

    fun selectPlace(place: Place, save: Boolean = true) {
        val samePlace = _state.value.data?.place?.let { it.lat == place.lat && it.lon == place.lon } == true
        _state.update {
            it.copy(
                place = place,
                locationPickerOpen = false,
                searchQuery = "",
                searchResults = emptyList(),
                data = if (samePlace) it.data else null,
                loading = !samePlace,
                error = null
            )
        }
        store.selectedPlace = place
        if (save && !place.isCurrentLocation) {
            val existing = store.savedPlaces.filterNot { it.lat == place.lat && it.lon == place.lon }
            store.savedPlaces = listOf(place) + existing
            _state.update { it.copy(savedPlaces = store.savedPlaces) }
        }
        _state.update { it.copy(savedPlaces = store.savedPlaces) }
        refresh(force = true)
    }

    fun openLocationPicker() = _state.update { it.copy(locationPickerOpen = true) }
    fun closeLocationPicker() = _state.update {
        it.copy(locationPickerOpen = false, searchQuery = "", searchResults = emptyList(), searchError = null)
    }

    fun onSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(searchResults = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            delay(320)
            try {
                val lang = Loc.apiLanguage
                val results = withContext(Dispatchers.IO) { repo.search(query, lang) }
                _state.update { it.copy(searchResults = results, searching = false, searchError = null) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        searching = false,
                        searchResults = emptyList(),
                        searchError = "Search unavailable. Check your connection."
                    )
                }
            }
        }
    }

    fun removeSavedPlace(place: Place) {
        store.savedPlaces = store.savedPlaces.filterNot { it.id == place.id }
        _state.update { it.copy(savedPlaces = store.savedPlaces) }
    }

    fun updateSettings(transform: (AtmosSettings) -> AtmosSettings) {
        val before = _state.value.settings
        val next = transform(before)
        store.settings = next
        _state.update { it.copy(settings = next) }
        if (next.language != before.language) {
            Loc.apply(next.language)
            viewModelScope.launch(Dispatchers.IO) { AtmosWidgets.refreshAll(context) }
        }
        if (next.refreshMinutes != before.refreshMinutes) startAutoRefresh()
        if (next.model != before.model) refresh(force = true)
    }

    fun setTab(tab: AppTab) = _state.update { it.copy(tab = tab) }

    fun openDetail(card: DetailCard) = _state.update { it.copy(detail = card) }
    fun closeDetail() = _state.update { it.copy(detail = null) }

    fun toast(message: String?) = _state.update { it.copy(toast = message) }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        searchJob?.cancel()
        autoJob?.cancel()
    }
}
