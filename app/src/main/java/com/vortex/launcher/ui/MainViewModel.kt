package com.vortex.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortex.launcher.data.apps.AppInfo
import com.vortex.launcher.data.apps.AppRepository
import com.vortex.launcher.data.db.AppStat
import com.vortex.launcher.data.db.ProfileEntity
import com.vortex.launcher.data.db.VortexDatabase
import com.vortex.launcher.data.settings.SettingsRepository
import com.vortex.launcher.data.settings.SkinType
import com.vortex.launcher.data.settings.VortexSettings
import com.vortex.launcher.data.weather.Cities
import com.vortex.launcher.data.weather.Weather
import com.vortex.launcher.data.weather.WeatherRepository
import com.vortex.launcher.ui.components.Wallpapers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Screen { ONBOARDING, HOME, ALL_APPS, SETTINGS }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val appRepo: AppRepository,
    private val weatherRepo: WeatherRepository,
    private val db: VortexDatabase
) : ViewModel() {

    private val _settings = MutableStateFlow(VortexSettings())
    val settings: StateFlow<VortexSettings> = _settings.asStateFlow()
    private var restored = false

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _cc = MutableStateFlow(false)
    val controlCenterOpen: StateFlow<Boolean> = _cc.asStateFlow()

    private val _pick = MutableStateFlow(false)
    val pickProfile: StateFlow<Boolean> = _pick.asStateFlow()

    private val _standby = MutableStateFlow(false)
    val standby: StateFlow<Boolean> = _standby.asStateFlow()

    private val _sleep = MutableStateFlow(0)
    val sleepMinutes: StateFlow<Int> = _sleep.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = appRepo.apps

    val profiles: StateFlow<List<ProfileEntity>> =
        db.profileDao().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<List<AppStat>> =
        db.statDao().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeProfile: StateFlow<ProfileEntity?> =
        combine(profiles, settings) { p, s -> p.firstOrNull { it.id == s.activeProfileId } ?: p.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pinnedApps: StateFlow<List<AppInfo>> =
        combine(apps, activeProfile) { all, prof ->
            val saved = prof?.pinned
            if (saved == null) all.take(5)
            else {
                val byPkg = all.associateBy { it.pkg }
                saved.split(",").filter { it.isNotBlank() }.mapNotNull { byPkg[it] }.take(5)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentApps: StateFlow<List<AppInfo>> =
        combine(apps, stats) { all, st ->
            val byPkg = all.associateBy { it.pkg }
            st.filter { it.lastLaunch > 0 }.sortedByDescending { it.lastLaunch }
                .mapNotNull { byPkg[it.pkg] }.take(10)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var sleepJob: Job? = null

    init {
        viewModelScope.launch { settingsRepo.settings.collect { _settings.value = it } }
        viewModelScope.launch { appRepo.refresh(); ensureProfile() }
        viewModelScope.launch {
            settings.map { it.cityIndex }.distinctUntilChanged().collectLatest { idx ->
                while (true) {
                    val city = Cities.list[idx.mod(Cities.list.size)]
                    _weather.value = weatherRepo.fetch(city) ?: _weather.value
                    delay(30 * 60_000L)
                }
            }
        }
    }

    /**
     * Вызывается из MainActivity.onCreate ДО setContent.
     * skinType (Классика/Гейминг) читается из DataStore и восстанавливается как есть — без сброса.
     */
    fun restore(saved: VortexSettings) {
        if (restored) return
        restored = true
        _settings.value = saved
        _screen.value = if (saved.onboardingDone) Screen.HOME else Screen.ONBOARDING
        _pick.value = saved.onboardingDone && saved.askProfileOnStart
    }

    private suspend fun ensureProfile() {
        val dao = db.profileDao()
        if (dao.count() == 0) dao.insert(ProfileEntity(name = "Профиль 1"))
        val list = dao.observeAll().first()
        val current = settingsRepo.settings.first().activeProfileId
        if (list.none { it.id == current }) edit { copy(activeProfileId = list.first().id) }
    }

    fun edit(block: VortexSettings.() -> VortexSettings) {
        val next = _settings.value.block()
        _settings.value = next
        viewModelScope.launch { settingsRepo.save(next) }
    }

    // --- навигация ---
    fun goHome() { _cc.value = false; _screen.value = Screen.HOME }
    fun openSettings() { _screen.value = Screen.SETTINGS }
    fun openAllApps() { _screen.value = Screen.ALL_APPS }
    fun toggleControlCenter() { _cc.value = !_cc.value }
    fun openControlCenter() { _cc.value = true }
    fun closeControlCenter() { _cc.value = false }
    fun openPicker() { _pick.value = true }
    fun dismissPicker() { _pick.value = false }
    fun refreshApps() { viewModelScope.launch { appRepo.refresh() } }

    fun finishOnboarding() {
        edit { copy(onboardingDone = true) }
        _screen.value = Screen.HOME
    }

    // --- скин и обои ---
    fun setSkin(skin: SkinType) = edit { copy(skinType = skin) }
    fun setWallpaper(i: Int) = edit { copy(wallpaperIndex = i) }
    fun nextWallpaper() = edit { copy(wallpaperIndex = (wallpaperIndex + 1) % Wallpapers.names.size) }

    // --- приложения ---
    fun launchApp(pkg: String) {
        _cc.value = false
        viewModelScope.launch {
            val old = db.statDao().get(pkg)
            db.statDao().upsert(AppStat(pkg, (old?.launches ?: 0) + 1, System.currentTimeMillis()))
        }
        appRepo.launch(pkg)
    }

    fun togglePin(pkg: String) {
        val prof = activeProfile.value ?: return
        viewModelScope.launch {
            val current = pinnedApps.value.map { it.pkg }
            val next = when {
                pkg in current -> current - pkg
                current.size < 5 -> current + pkg
                else -> current
            }
            db.profileDao().update(prof.copy(pinned = next.joinToString(",")))
        }
    }

    // --- профили ---
    fun addProfile() {
        viewModelScope.launch {
            val n = profiles.value.size + 1
            val id = db.profileDao().insert(ProfileEntity(name = "Профиль $n", colorIndex = n % 6))
            edit { copy(activeProfileId = id) }
        }
    }

    fun selectProfile(id: Long) = edit { copy(activeProfileId = id) }

    fun deleteActiveProfile() {
        val list = profiles.value
        val cur = activeProfile.value ?: return
        if (list.size <= 1) return
        viewModelScope.launch {
            db.profileDao().delete(cur)
            edit { copy(activeProfileId = list.first { it.id != cur.id }.id) }
        }
    }

    // --- питание / таймер сна ---
    fun enterStandby() { _cc.value = false; _standby.value = true }
    fun wake() { _standby.value = false }

    fun cycleSleepTimer() {
        val options = listOf(0, 15, 30, 60, 90)
        val next = options[(options.indexOf(_sleep.value) + 1) % options.size]
        _sleep.value = next
        sleepJob?.cancel()
        if (next > 0) {
            sleepJob = viewModelScope.launch {
                delay(next * 60_000L)
                _standby.value = true
                _sleep.value = 0
            }
        }
    }
}
