package com.vortex.launcher.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vortex.launcher.data.settings.SkinType
import com.vortex.launcher.ui.apps.AllAppsScreen
import com.vortex.launcher.ui.components.*
import com.vortex.launcher.ui.control.ControlCenter
import com.vortex.launcher.ui.home.HomeHost
import com.vortex.launcher.ui.onboarding.OnboardingScreen
import com.vortex.launcher.ui.settings.SettingsScreen
import com.vortex.launcher.ui.theme.VortexTheme

@Composable
fun VortexRoot(vm: MainViewModel) {
    val ctx = LocalContext.current
    val screen by vm.screen.collectAsStateWithLifecycle()
    val ccOpen by vm.controlCenterOpen.collectAsStateWithLifecycle()
    val pick by vm.pickProfile.collectAsStateWithLifecycle()
    val standby by vm.standby.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val active by vm.activeProfile.collectAsStateWithLifecycle()
    val sleep by vm.sleepMinutes.collectAsStateWithLifecycle()
    val recent by vm.recentApps.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val net by rememberNetState()

    val showPicker = pick && profiles.size > 1
    LaunchedEffect(pick, profiles.size) {
        if (pick && profiles.isNotEmpty() && profiles.size <= 1) vm.dismissPicker()
    }

    // Единая «дизайн-сетка» 960x540dp на любом ТВ (1080p/4K/mdpi/tvdpi)
    val metrics = ctx.resources.displayMetrics
    val designDensity = remember(metrics.widthPixels, metrics.heightPixels) {
        Density(maxOf(metrics.widthPixels, metrics.heightPixels) / 960f, 1f)
    }

    BackHandler(enabled = ccOpen || showPicker || screen == Screen.ALL_APPS || screen == Screen.SETTINGS) {
        when {
            ccOpen -> vm.closeControlCenter()
            showPicker -> vm.dismissPicker()
            else -> vm.goHome()
        }
    }

    CompositionLocalProvider(LocalDensity provides designDensity) {
        VortexTheme {
            Surface(Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
                Box(Modifier.fillMaxSize()) {
                    // RenderEffect blur всего контента под Центром управления
                    val blurRadius by animateDpAsState(if (ccOpen) 24.dp else 0.dp, tween(400), label = "ccBlur")
                    Box(
                        Modifier.fillMaxSize()
                            .then(if (blurRadius > 0.5.dp) Modifier.blur(blurRadius) else Modifier)
                            .focusProperties { canFocus = !(ccOpen || showPicker) }
                    ) {
                        AnimatedContent(
                            targetState = screen,
                            transitionSpec = {
                                if (targetState == Screen.HOME) {
                                    (fadeIn(tween(350)) + scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessLow))) togetherWith
                                        (fadeOut(tween(250)) + scaleOut(targetScale = 1.04f))
                                } else {
                                    val spec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessMediumLow)
                                    (slideInHorizontally(spec) { it / 5 } + fadeIn(tween(350))) togetherWith
                                        (fadeOut(tween(200)) + scaleOut(targetScale = 0.96f))
                                }
                            },
                            label = "screens"
                        ) { s ->
                            when (s) {
                                Screen.ONBOARDING -> OnboardingScreen(vm)
                                Screen.HOME -> HomeHost(vm)
                                Screen.ALL_APPS -> AllAppsScreen(apps, vm::launchApp)
                                Screen.SETTINGS -> SettingsScreen(vm, onBack = vm::goHome)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = ccOpen && screen != Screen.ONBOARDING,
                        enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeIn(tween(300)),
                        exit = slideOutVertically(tween(250)) { -it / 3 } + fadeOut(tween(220))
                    ) {
                        ControlCenter(
                            net = net,
                            sleepMinutes = sleep,
                            recent = recent,
                            onPower = vm::enterStandby,
                            onWifi = { openSystem(ctx, Settings.ACTION_WIFI_SETTINGS) },
                            onAudio = { openSystem(ctx, Settings.ACTION_SOUND_SETTINGS) },
                            onSleepTimer = vm::cycleSleepTimer,
                            onLaunch = vm::launchApp,
                            onSearch = { vm.closeControlCenter(); openSearch(ctx) },
                            onGames = { vm.setSkin(SkinType.GAMING); vm.goHome() },
                            onWallpaper = vm::nextWallpaper,
                            onHome = vm::goHome,
                            onSettings = { vm.closeControlCenter(); vm.openSettings() },
                            onProfile = { vm.closeControlCenter(); vm.openPicker() }
                        )
                    }

                    AnimatedVisibility(
                        visible = showPicker,
                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.96f),
                        exit = fadeOut(tween(200))
                    ) {
                        ProfilePicker(
                            profiles = profiles,
                            activeId = active?.id ?: -1L,
                            onSelect = { vm.selectProfile(it); vm.dismissPicker() }
                        )
                    }

                    if (standby) Box(Modifier.fillMaxSize().background(Color.Black))
                }
            }
        }
    }
}

@Composable
private fun ProfilePicker(
    profiles: List<com.vortex.launcher.data.db.ProfileEntity>,
    activeId: Long,
    onSelect: (Long) -> Unit
) {
    val first = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(profiles.size) {
        kotlinx.coroutines.delay(250)
        runCatching { first.requestFocus() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Кто смотрит?", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                profiles.forEachIndexed { i, p ->
                    GlassButton(
                        onClick = { onSelect(p.id) },
                        modifier = Modifier.size(110.dp, 128.dp),
                        shape = RoundedCornerShape(22.dp),
                        selected = p.id == activeId,
                        focusRequester = if (i == 0) first else null
                    ) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            ProfileAvatar(p.colorIndex, 60.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(p.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
