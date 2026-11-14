package com.vortex.launcher.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vortex.launcher.data.settings.SkinType
import com.vortex.launcher.ui.MainViewModel
import com.vortex.launcher.ui.components.WallpaperBackground
import com.vortex.launcher.ui.components.openSearch

/**
 * Morph-переход Классика <-> Гейминг:
 *  - док/часы уходят вверх со scale+fade (spring), карусель/верхняя панель въезжают снизу/сверху;
 *  - фон плавно перетекает из обоев в тёмно-синий градиент.
 */
@Composable
fun HomeHost(vm: MainViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val pinned by vm.pinnedApps.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val profile by vm.activeProfile.collectAsStateWithLifecycle()
    val statMap = remember(stats) { stats.associateBy { it.pkg } }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            // свайп вниз (пульт-мышь / тач) открывает Центр управления
            var sum = 0f
            detectVerticalDragGestures(
                onDragStart = { sum = 0f },
                onDragEnd = { if (sum > 120f) vm.openControlCenter() }
            ) { _, dy -> sum += dy }
        }
    ) {
        WallpaperBackground(settings.wallpaperIndex, settings.skinType == SkinType.GAMING)

        AnimatedContent(
            targetState = settings.skinType,
            transitionSpec = {
                val toGaming = targetState == SkinType.GAMING
                val spec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessLow)
                if (toGaming) {
                    (slideInVertically(spec) { it / 3 } + fadeIn(tween(500)) +
                        scaleIn(initialScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 0f))) togetherWith
                        (slideOutVertically(spec) { -it / 3 } + fadeOut(tween(350)) +
                            scaleOut(targetScale = 0.85f, transformOrigin = TransformOrigin(0.5f, 0f)))
                } else {
                    (slideInVertically(spec) { -it / 3 } + fadeIn(tween(500)) +
                        scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f))) togetherWith
                        (slideOutVertically(spec) { it / 3 } + fadeOut(tween(350)) +
                            scaleOut(targetScale = 1.05f, transformOrigin = TransformOrigin(0.5f, 1f)))
                }
            },
            label = "skinMorph"
        ) { skin ->
            when (skin) {
                SkinType.CLASSIC -> AppleHomeScreen(
                    settings = settings,
                    pinned = pinned,
                    weather = weather,
                    onLaunch = vm::launchApp,
                    onAllApps = vm::openAllApps,
                    onControlCenter = vm::openControlCenter
                )
                SkinType.GAMING -> Ps5HomeScreen(
                    apps = apps,
                    stats = statMap,
                    profileColor = profile?.colorIndex ?: 0,
                    onLaunch = vm::launchApp,
                    onSearch = { openSearch(ctx) },
                    onSettings = vm::openSettings,
                    onProfile = vm::openPicker,
                    onAllApps = vm::openAllApps
                )
            }
        }
    }
}
