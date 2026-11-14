package com.vortex.launcher.ui.settings

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vortex.launcher.data.settings.ClockPosition
import com.vortex.launcher.data.settings.SkinType
import com.vortex.launcher.data.weather.Cities
import com.vortex.launcher.ui.MainViewModel
import com.vortex.launcher.ui.VIcons
import com.vortex.launcher.ui.components.*

private val Ink = Color(0xFF0B0F1A)

private enum class Category(val title: String, val icon: ImageVector) {
    APPEARANCE("Внешний вид", VIcons.Palette),
    CLOCK("Часы", VIcons.Schedule),
    DOCK("Док", VIcons.Apps),
    PROFILES("Профили", VIcons.People),
    WIDGETS("Виджеты", VIcons.Widgets),
    WEATHER("Погода", VIcons.Cloud),
    STARTUP("Запуск", VIcons.Power),
    SYSTEM("Система", VIcons.Tv)
}

private fun <T> List<T>.next(cur: T): T = this[(indexOf(cur) + 1) % size]

/** Настройки: слева категории, справа контент (2 колонки). */
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val active by vm.activeProfile.collectAsStateWithLifecycle()
    val requestDefault = rememberDefaultLauncherRequester()
    var current by rememberSaveable { mutableStateOf(Category.APPEARANCE) }

    Row(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF05070F))))
    ) {
        // ---- левая колонка ----
        Column(
            Modifier.width(250.dp).fillMaxHeight().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Category.values().forEach { cat ->
                GlassButton(
                    onClick = { current = cat },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    selected = cat == current,
                    fillOnFocus = true,
                    focusScale = 1.03f,
                    onFocusChange = { f -> if (f) current = cat }
                ) { f ->
                    val c = if (f) Ink else Color.White
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(cat.icon, null, tint = c, modifier = Modifier.size(20.dp))
                        Text(cat.title, color = c, fontSize = 15.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            GlassButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), fillOnFocus = true) { f ->
                val c = if (f) Ink else Color.White
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(VIcons.Back, null, tint = c, modifier = Modifier.size(20.dp))
                    Text("Назад", color = c, fontSize = 15.sp)
                }
            }
        }

        // ---- правая колонка ----
        Box(Modifier.weight(1f).fillMaxHeight().padding(top = 28.dp, end = 32.dp, bottom = 28.dp)) {
            GlassSurface(Modifier.fillMaxSize(), RoundedCornerShape(26.dp)) {
                Crossfade(current, animationSpec = tween(200), label = "category") { cat ->
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text(cat.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
                        when (cat) {
                            Category.APPEARANCE -> {
                                item {
                                    SettingRow("Скин", if (s.skinType == SkinType.CLASSIC) "Классический" else "Гейминг", VIcons.Palette) {
                                        vm.setSkin(if (s.skinType == SkinType.CLASSIC) SkinType.GAMING else SkinType.CLASSIC)
                                    }
                                }
                                item {
                                    SettingRow("Обои", Wallpapers.name(s.wallpaperIndex), VIcons.Image) { vm.nextWallpaper() }
                                }
                                item {
                                    Text(
                                        "Выбранный скин запоминается и восстанавливается после выключения ТВ.",
                                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Category.CLOCK -> {
                                item {
                                    SettingRow("Положение часов", when (s.clockPosition) {
                                        ClockPosition.TOP_LEFT -> "Слева сверху"
                                        ClockPosition.TOP_RIGHT -> "Справа сверху"
                                        ClockPosition.CENTER -> "По центру"
                                    }) { vm.edit { copy(clockPosition = ClockPosition.values().toList().next(clockPosition)) } }
                                }
                                item {
                                    SettingRow("Размер часов", "${(s.clockScale * 100).toInt()}%") {
                                        vm.edit { copy(clockScale = listOf(0.6f, 0.8f, 1f, 1.2f, 1.4f).next(clockScale)) }
                                    }
                                }
                            }
                            Category.DOCK -> {
                                item {
                                    SettingRow("Размер дока", "${(s.dockScale * 100).toInt()}%") {
                                        vm.edit { copy(dockScale = listOf(0.8f, 1f, 1.2f).next(dockScale)) }
                                    }
                                }
                                item {
                                    Text("Закреплено: ${pinned.size}/5 (нажмите на приложение)", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                                items(apps, key = { it.pkg }) { app ->
                                    val isPinned = pinned.any { it.pkg == app.pkg }
                                    SettingRow(
                                        title = app.label, value = null,
                                        leading = { Image(app.icon, null, Modifier.size(28.dp)) },
                                        trailing = { if (isPinned) Icon(VIcons.Check, null, tint = Color(0xFF34C759), modifier = Modifier.size(20.dp)) }
                                    ) { vm.togglePin(app.pkg) }
                                }
                            }
                            Category.PROFILES -> {
                                items(profiles, key = { it.id }) { p ->
                                    SettingRow(
                                        title = p.name, value = null,
                                        leading = { ProfileAvatar(p.colorIndex, 30.dp) },
                                        trailing = { if (p.id == active?.id) Icon(VIcons.Check, null, tint = Color(0xFF34C759), modifier = Modifier.size(20.dp)) }
                                    ) { vm.selectProfile(p.id) }
                                }
                                item { SettingRow("Добавить профиль", null, VIcons.Add) { vm.addProfile() } }
                                if (profiles.size > 1) {
                                    item { SettingRow("Удалить активный профиль", null, VIcons.Delete) { vm.deleteActiveProfile() } }
                                }
                            }
                            Category.WIDGETS -> {
                                item { SwitchRow("Часы и дата на главном экране", s.showWidgets) { vm.edit { copy(showWidgets = !showWidgets) } } }
                            }
                            Category.WEATHER -> {
                                item { SwitchRow("Показывать погоду", s.showWeather) { vm.edit { copy(showWeather = !showWeather) } } }
                                item {
                                    SettingRow("Город", Cities.list[s.cityIndex.mod(Cities.list.size)].name, VIcons.Cloud) {
                                        vm.edit { copy(cityIndex = (cityIndex + 1) % Cities.list.size) }
                                    }
                                }
                            }
                            Category.STARTUP -> {
                                item { SwitchRow("Спрашивать профиль при включении", s.askProfileOnStart) { vm.edit { copy(askProfileOnStart = !askProfileOnStart) } } }
                                item { SettingRow("Поставить лаунчер по умолчанию", null, VIcons.Home) { requestDefault() } }
                            }
                            Category.SYSTEM -> {
                                item { SettingRow("Настройки Wi-Fi", null, VIcons.Wifi) { openSystem(ctx, Settings.ACTION_WIFI_SETTINGS) } }
                                item { SettingRow("Звук", null, VIcons.Volume) { openSystem(ctx, Settings.ACTION_SOUND_SETTINGS) } }
                                item { SettingRow("Системные настройки Android", null, VIcons.Settings) { openSystem(ctx, Settings.ACTION_SETTINGS) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String?,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassButton(onClick, Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), fillOnFocus = true, focusScale = 1.02f) { f ->
        val c = if (f) Ink else Color.White
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
            else if (icon != null) { Icon(icon, null, tint = c, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)) }
            Text(title, color = c, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (value != null) Text(value, color = c.copy(alpha = 0.7f), fontSize = 14.sp)
            if (trailing != null) trailing()
        }
    }
}

@Composable
private fun SwitchRow(title: String, on: Boolean, onToggle: () -> Unit) {
    SettingRow(
        title = title, value = null,
        trailing = {
            Box(
                Modifier.size(38.dp, 22.dp)
                    .background(if (on) Color(0xFF34C759) else Color.White.copy(alpha = 0.3f), CircleShape)
                    .padding(3.dp)
            ) {
                Box(
                    Modifier.size(16.dp).align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                        .background(Color.White, CircleShape)
                )
            }
        },
        onClick = onToggle
    )
}
