package com.vortex.launcher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class SkinType { CLASSIC, GAMING }
enum class ClockPosition { TOP_LEFT, TOP_RIGHT, CENTER }

data class VortexSettings(
    val onboardingDone: Boolean = false,
    val skinType: SkinType = SkinType.CLASSIC,
    val clockPosition: ClockPosition = ClockPosition.TOP_LEFT,
    val clockScale: Float = 1f,
    val dockScale: Float = 1f,
    val wallpaperIndex: Int = 0,
    val showWeather: Boolean = true,
    val cityIndex: Int = 0,
    val showWidgets: Boolean = true,
    val askProfileOnStart: Boolean = false,
    val activeProfileId: Long = -1L
)

private val Context.dataStore by preferencesDataStore(name = "vortex_settings")

private object Keys {
    val ONBOARDING = booleanPreferencesKey("onboarding_done")
    val SKIN = stringPreferencesKey("skin_type")
    val CLOCK_POS = stringPreferencesKey("clock_position")
    val CLOCK_SCALE = floatPreferencesKey("clock_scale")
    val DOCK_SCALE = floatPreferencesKey("dock_scale")
    val WALLPAPER = intPreferencesKey("wallpaper_index")
    val WEATHER = booleanPreferencesKey("show_weather")
    val CITY = intPreferencesKey("city_index")
    val WIDGETS = booleanPreferencesKey("show_widgets")
    val ASK_PROFILE = booleanPreferencesKey("ask_profile")
    val ACTIVE_PROFILE = longPreferencesKey("active_profile")
}

private fun Preferences.toSettings() = VortexSettings(
    onboardingDone = this[Keys.ONBOARDING] ?: false,
    skinType = SkinType.values().firstOrNull { it.name == this[Keys.SKIN] } ?: SkinType.CLASSIC,
    clockPosition = ClockPosition.values().firstOrNull { it.name == this[Keys.CLOCK_POS] } ?: ClockPosition.TOP_LEFT,
    clockScale = this[Keys.CLOCK_SCALE] ?: 1f,
    dockScale = this[Keys.DOCK_SCALE] ?: 1f,
    wallpaperIndex = this[Keys.WALLPAPER] ?: 0,
    showWeather = this[Keys.WEATHER] ?: true,
    cityIndex = this[Keys.CITY] ?: 0,
    showWidgets = this[Keys.WIDGETS] ?: true,
    askProfileOnStart = this[Keys.ASK_PROFILE] ?: false,
    activeProfileId = this[Keys.ACTIVE_PROFILE] ?: -1L
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<VortexSettings> = context.dataStore.data.map { it.toSettings() }

    /** Синхронное чтение при старте MainActivity: skinType восстанавливается до первого кадра. */
    fun readBlocking(): VortexSettings = runBlocking { settings.first() }

    suspend fun save(s: VortexSettings) {
        context.dataStore.edit { p ->
            p[Keys.ONBOARDING] = s.onboardingDone
            p[Keys.SKIN] = s.skinType.name
            p[Keys.CLOCK_POS] = s.clockPosition.name
            p[Keys.CLOCK_SCALE] = s.clockScale
            p[Keys.DOCK_SCALE] = s.dockScale
            p[Keys.WALLPAPER] = s.wallpaperIndex
            p[Keys.WEATHER] = s.showWeather
            p[Keys.CITY] = s.cityIndex
            p[Keys.WIDGETS] = s.showWidgets
            p[Keys.ASK_PROFILE] = s.askProfileOnStart
            p[Keys.ACTIVE_PROFILE] = s.activeProfileId
        }
    }
}
