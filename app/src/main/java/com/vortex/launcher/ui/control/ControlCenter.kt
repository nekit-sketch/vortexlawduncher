package com.vortex.launcher.ui.control

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.apps.AppInfo
import com.vortex.launcher.ui.VIcons
import com.vortex.launcher.ui.components.*
import kotlinx.coroutines.delay

private val Ink = Color(0xFF0B0F1A)

/** Центр управления (Фото 3): крупные стеклянные плитки + ряд круглых кнопок. */
@Composable
fun ControlCenter(
    net: NetState,
    sleepMinutes: Int,
    recent: List<AppInfo>,
    onPower: () -> Unit,
    onWifi: () -> Unit,
    onAudio: () -> Unit,
    onSleepTimer: () -> Unit,
    onLaunch: (String) -> Unit,
    onSearch: () -> Unit,
    onGames: () -> Unit,
    onWallpaper: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit
) {
    val now by rememberNow()
    var showRecent by remember { mutableStateOf(false) }
    var visitedRecent by remember { mutableStateOf(false) }
    val sleepFocus = remember { FocusRequester() }
    val recentTileFocus = remember { FocusRequester() }
    val firstRecentFocus = remember { FocusRequester() }

    BackHandler(showRecent) { showRecent = false }
    LaunchedEffect(showRecent) {
        delay(150)
        runCatching {
            when {
                showRecent -> firstRecentFocus.requestFocus()
                visitedRecent -> recentTileFocus.requestFocus()
                else -> sleepFocus.requestFocus()
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.68f)))
        )
    ) {
        Text(
            now.timeText(), fontSize = 28.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 24.dp)
        )
        Row(
            Modifier.align(Alignment.TopEnd).padding(end = 40.dp, top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundIcon(VIcons.Home, 42.dp, onHome)
            RoundIcon(VIcons.Settings, 42.dp, onSettings)
            RoundIcon(VIcons.Tune, 42.dp, {}, selected = true)
            RoundIcon(VIcons.Person, 42.dp, onProfile)
        }

        Column(
            Modifier.align(Alignment.Center).width(500.dp).padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showRecent) {
                RecentPanel(recent, firstRecentFocus, onLaunch)
            } else {
                Row(Modifier.fillMaxWidth().height(172.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlTile(
                        icon = VIcons.Power, title = "Power", subtitle = null,
                        modifier = Modifier.weight(0.9f).fillMaxHeight(),
                        shape = RoundedCornerShape(30.dp), vertical = true, onClick = onPower
                    )
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ControlTile(
                            icon = if (net == NetState.NONE) VIcons.WifiOff else VIcons.Wifi,
                            title = when (net) {
                                NetState.WIFI -> "Wi-Fi Connected"
                                NetState.OTHER -> "Network Connected"
                                NetState.NONE -> "Wi-Fi Off"
                            },
                            subtitle = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shape = RoundedCornerShape(34.dp), onClick = onWifi
                        )
                        ControlTile(
                            icon = VIcons.Layers, title = "Recent Apps", subtitle = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shape = RoundedCornerShape(34.dp),
                            focusRequester = recentTileFocus,
                            onClick = { visitedRecent = true; showRecent = true }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlTile(
                        icon = VIcons.Volume, title = "Audio HDMI", subtitle = null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp), onClick = onAudio
                    )
                    ControlTile(
                        icon = VIcons.Timer, title = "Sleep Timer",
                        subtitle = if (sleepMinutes == 0) "Off" else "$sleepMinutes min",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp),
                        focusRequester = sleepFocus, onClick = onSleepTimer
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundIcon(VIcons.Search, 62.dp, onSearch)
                RoundIcon(VIcons.Games, 62.dp, onGames)
                RoundIcon(VIcons.Image, 62.dp, onWallpaper)
                RoundIcon(VIcons.Home, 62.dp, onHome)
            }
        }
    }
}

@Composable
private fun ControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier,
    shape: Shape,
    vertical: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    GlassButton(
        onClick = onClick, modifier = modifier, shape = shape,
        focusRequester = focusRequester, fillOnFocus = true, focusScale = 1.04f
    ) { focused ->
        val c = if (focused) Ink else Color.White
        if (vertical) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = c, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(10.dp))
                Text(title, color = c, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Row(
                Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, null, tint = if (!focused && icon == VIcons.Wifi) Color(0xFF4DA3FF) else c, modifier = Modifier.size(28.dp))
                Column {
                    Text(title, color = c, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                    if (subtitle != null) Text(subtitle, color = c.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, size: Dp, onClick: () -> Unit, selected: Boolean = false) {
    GlassButton(
        onClick = onClick, shape = CircleShape, selected = selected,
        fillOnFocus = true, modifier = Modifier.size(size)
    ) { focused ->
        Icon(
            icon, null, tint = if (focused) Ink else Color.White,
            modifier = Modifier.align(Alignment.Center).size(size * 0.45f)
        )
    }
}

@Composable
private fun RecentPanel(recent: List<AppInfo>, firstFocus: FocusRequester, onLaunch: (String) -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().height(172.dp), RoundedCornerShape(30.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("Recent Apps", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            if (recent.isEmpty()) {
                Text("Пока пусто", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
                    itemsIndexed(recent, key = { _, a -> a.pkg }) { i, app ->
                        AppIconTile(
                            app = app, size = 72.dp, focusScale = 1.15f,
                            focusRequester = if (i == 0) firstFocus else null,
                            onClick = { onLaunch(app.pkg) }
                        )
                    }
                }
            }
        }
    }
}
