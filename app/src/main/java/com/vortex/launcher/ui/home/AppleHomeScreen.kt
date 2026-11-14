package com.vortex.launcher.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.apps.AppInfo
import com.vortex.launcher.data.settings.ClockPosition
import com.vortex.launcher.data.settings.VortexSettings
import com.vortex.launcher.data.weather.Weather
import com.vortex.launcher.ui.VIcons
import com.vortex.launcher.ui.components.*
import kotlinx.coroutines.delay

/** Классический режим (Фото 1): обои, огромные часы, док-капсула снизу, мини-островок справа сверху. */
@Composable
fun AppleHomeScreen(
    settings: VortexSettings,
    pinned: List<AppInfo>,
    weather: Weather?,
    onLaunch: (String) -> Unit,
    onAllApps: () -> Unit,
    onControlCenter: () -> Unit
) {
    val firstItem = remember { FocusRequester() }
    var focusedLabel by remember { mutableStateOf<String?>(null) }
    val net by rememberNetState()
    val now by rememberNow()

    LaunchedEffect(pinned.isNotEmpty()) {
        delay(350)
        runCatching { firstItem.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        // --- часы + погода ---
        if (settings.showWidgets) {
            val (alignment, topPad) = when (settings.clockPosition) {
                ClockPosition.TOP_LEFT -> Alignment.TopStart to 28.dp
                ClockPosition.TOP_RIGHT -> Alignment.TopEnd to 84.dp
                ClockPosition.CENTER -> Alignment.Center to 0.dp
            }
            BigClock(
                scale = settings.clockScale,
                weather = weather,
                showWeather = settings.showWeather,
                modifier = Modifier.align(alignment).padding(start = 40.dp, end = 40.dp, top = topPad)
            )
        }

        // --- мини-островок: Wi-Fi, время, Центр управления ---
        GlassButton(
            onClick = onControlCenter,
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 32.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (net == NetState.NONE) VIcons.WifiOff else VIcons.Wifi,
                    null, tint = Color.White, modifier = Modifier.size(18.dp)
                )
                Text(now.timeText(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Box(Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.4f)))
                Icon(VIcons.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // --- док ---
        val iconSize = 52 * settings.dockScale
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(34.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = focusedLabel,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "dockLabel"
                ) { label ->
                    if (label != null) {
                        Text(
                            label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 8f)
                            )
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassSurface(shape = RoundedCornerShape(34.dp)) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pinned.forEachIndexed { i, app ->
                            AppIconTile(
                                app = app,
                                size = iconSize.dp,
                                focusScale = 1.35f,
                                liftFromBottom = true,
                                focusRequester = if (i == 0) firstItem else null,
                                onFocus = { f ->
                                    if (f) focusedLabel = app.label
                                    else if (focusedLabel == app.label) focusedLabel = null
                                },
                                onClick = { onLaunch(app.pkg) }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                GlassButton(
                    onClick = onAllApps,
                    shape = CircleShape,
                    modifier = Modifier.size((iconSize + 28).dp),
                    onFocusChange = { f ->
                        if (f) focusedLabel = "Все приложения"
                        else if (focusedLabel == "Все приложения") focusedLabel = null
                    }
                ) {
                    Icon(VIcons.Apps, "Все приложения", tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(28.dp))
                }
            }
        }
    }
}
