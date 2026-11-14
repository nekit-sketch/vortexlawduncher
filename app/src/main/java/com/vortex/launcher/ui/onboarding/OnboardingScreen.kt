package com.vortex.launcher.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vortex.launcher.data.settings.SkinType
import com.vortex.launcher.ui.MainViewModel
import com.vortex.launcher.ui.VIcons
import com.vortex.launcher.ui.components.*
import kotlinx.coroutines.delay
import kotlin.math.hypot

/** Онбординг: заставка -> скин -> Wi-Fi -> обои -> по умолчанию -> профиль. Переходы: slide + fade. */
@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val active by vm.activeProfile.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(step > 0) { step-- }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF162B5C), Color(0xFF070B18), Color.Black)))
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val fwd = targetState > initialState
                val spec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessLow)
                (slideInHorizontally(spec) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(400))) togetherWith
                    (slideOutHorizontally(spec) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(250)))
            },
            label = "onboarding"
        ) { s ->
            when (s) {
                0 -> Welcome { step = 1 }
                1 -> StepScaffold("Выберите стиль", "Его можно изменить позже в настройках", onNext = { step = 2 }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SkinCard("Классический", "Обои, часы и док-панель", VIcons.Home, settings.skinType == SkinType.CLASSIC) { vm.setSkin(SkinType.CLASSIC) }
                        SkinCard("Гейминг", "Карусель, баннеры, достижения", VIcons.Games, settings.skinType == SkinType.GAMING) { vm.setSkin(SkinType.GAMING) }
                    }
                }
                2 -> {
                    val ctx = LocalContext.current
                    val net by rememberNetState()
                    StepScaffold("Подключение к сети", "Нужно для погоды", onNext = { step = 3 }) {
                        Icon(if (net == NetState.NONE) VIcons.WifiOff else VIcons.Wifi, null, tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(if (net == NetState.NONE) "Нет подключения" else "Подключено", fontSize = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        PillButton("Открыть настройки Wi-Fi") { openSystem(ctx, Settings.ACTION_WIFI_SETTINGS) }
                    }
                }
                3 -> StepScaffold("Обои", "Выберите фон главного экрана", onNext = { step = 4 }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Wallpapers.names.forEachIndexed { i, name ->
                            GlassButton(
                                onClick = { vm.setWallpaper(i) },
                                modifier = Modifier.size(110.dp, 76.dp),
                                shape = RoundedCornerShape(14.dp),
                                selected = settings.wallpaperIndex == i
                            ) {
                                Box(
                                    Modifier.fillMaxSize().padding(4.dp)
                                        .background(Brush.verticalGradient(Wallpapers.at(i)), RoundedCornerShape(10.dp))
                                )
                                Text(name, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                                if (settings.wallpaperIndex == i) {
                                    Icon(VIcons.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp))
                                }
                            }
                        }
                    }
                }
                4 -> {
                    val requestDefault = rememberDefaultLauncherRequester()
                    StepScaffold("Основной лаунчер", "Кнопка Home будет открывать Vortex", onNext = { step = 5 }) {
                        Icon(VIcons.Home, null, tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(18.dp))
                        PillButton("Поставить по умолчанию") { requestDefault() }
                    }
                }
                else -> StepScaffold("Кто смотрит?", "Выберите профиль или создайте новый", nextLabel = "Готово", onNext = { vm.finishOnboarding() }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        profiles.forEach { p ->
                            GlassButton(
                                onClick = { vm.selectProfile(p.id) },
                                modifier = Modifier.size(100.dp, 112.dp),
                                shape = RoundedCornerShape(20.dp),
                                selected = p.id == active?.id
                            ) {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    ProfileAvatar(p.colorIndex, 52.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(p.name, fontSize = 13.sp)
                                }
                            }
                        }
                        GlassButton(
                            onClick = { vm.addProfile() },
                            modifier = Modifier.size(100.dp, 112.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(VIcons.Add, null, tint = Color.White, modifier = Modifier.size(34.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Новый", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onStart: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var showTitle by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1700, easing = FastOutSlowInEasing)) }
    LaunchedEffect(Unit) {
        delay(800); showTitle = true
        delay(900); showButton = true
        delay(250); runCatching { focus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                showTitle,
                enter = fadeIn(tween(700)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f))
            ) {
                Text("Vortex Launcher", fontSize = 52.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(30.dp))
            AnimatedVisibility(showButton, enter = fadeIn(tween(500)) + slideInVertically(spring(stiffness = Spring.StiffnessLow)) { it / 2 }) {
                PillButton("Начнём", focus, onStart)
            }
        }
        // чёрный слой с расширяющейся круглой «дырой»
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawBehind {
                    drawRect(Color.Black)
                    drawCircle(
                        Color.Transparent,
                        radius = progress.value * hypot(size.width, size.height) / 2f * 1.05f,
                        blendMode = BlendMode.Clear
                    )
                }
        )
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    nextLabel: String = "Далее",
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val nextFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(400); runCatching { nextFocus.requestFocus() } }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 60.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f))
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
        PillButton(nextLabel, nextFocus, onNext)
    }
}

@Composable
private fun PillButton(text: String, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    GlassButton(onClick = onClick, shape = RoundedCornerShape(50), focusRequester = focusRequester) {
        Text(text, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 36.dp, vertical = 11.dp))
    }
}

@Composable
private fun SkinCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    GlassButton(
        onClick = onClick, modifier = Modifier.size(250.dp, 170.dp),
        shape = RoundedCornerShape(26.dp), selected = selected
    ) {
        Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
        if (selected) {
            Icon(VIcons.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(22.dp))
        }
    }
}
