package com.vortex.launcher.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.apps.AppInfo
import com.vortex.launcher.data.db.AppStat
import com.vortex.launcher.ui.VIcons
import com.vortex.launcher.ui.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF05070F)

/** Гейминг-режим (Фото 2, PS5): вкладки, карусель, баннер, «Играть», достижения. */
@Composable
fun Ps5HomeScreen(
    apps: List<AppInfo>,
    stats: Map<String, AppStat>,
    profileColor: Int,
    onLaunch: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onAllApps: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val list = remember(apps, tab) {
        (if (tab == 0) apps.filter { it.isGame } else apps.filter { !it.isGame }).ifEmpty { apps }
    }
    var focused by remember { mutableStateOf<AppInfo?>(null) }
    val shown = focused?.takeIf { it in list } ?: list.firstOrNull()
    val firstItem = remember { FocusRequester() }
    val now by rememberNow()

    LaunchedEffect(Unit) {
        delay(450)
        runCatching { firstItem.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        // --- баннер + название + достижения (кроссфейд) ---
        AnimatedContent(
            targetState = shown,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
            label = "banner"
        ) { app ->
            if (app != null) GameBanner(app, stats[app.pkg])
        }

        // --- кнопки «Играть» / «...» ---
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WhiteButton(
                text = if (shown?.isGame == true) "Играть" else "Открыть",
                icon = VIcons.Play,
                modifier = Modifier.width(190.dp),
                onClick = { shown?.let { onLaunch(it.pkg) } }
            )
            GlassButton(
                onClick = onAllApps,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(VIcons.More, "Ещё", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(22.dp))
            }
        }

        // --- верхняя панель ---
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabLabel("Игры", tab == 0, onFocus = { tab = 0 })
            Spacer(Modifier.width(26.dp))
            TabLabel("Мультимедиа", tab == 1, onFocus = { tab = 1 })
            Spacer(Modifier.weight(1f))
            FlatIconButton(onSearch) { Icon(VIcons.Search, "Поиск", tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp))
            FlatIconButton(onSettings) { Icon(VIcons.Settings, "Настройки", tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp))
            FlatIconButton(onProfile) {
                ProfileAvatar(profileColor, 30.dp)
                Box(
                    Modifier.size(9.dp).align(Alignment.BottomEnd)
                        .background(Color(0xFF34C759), CircleShape)
                )
            }
            Spacer(Modifier.width(18.dp))
            Text(now.timeText(), fontSize = 20.sp, color = Color.White)
        }

        // --- карусель ---
        Column(Modifier.padding(top = 74.dp)) {
            LazyRow(
                modifier = Modifier.height(124.dp),
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                itemsIndexed(list, key = { _, a -> a.pkg }) { i, app ->
                    CarouselTile(
                        app = app,
                        requester = if (i == 0) firstItem else null,
                        onFocus = { f -> if (f) focused = app },
                        onClick = { onLaunch(app.pkg) }
                    )
                }
                item {
                    GlassButton(
                        onClick = onAllApps,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(78.dp)
                    ) {
                        Icon(VIcons.Grid, "Все приложения", tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                }
            }
            Text(
                shown?.label.orEmpty(),
                modifier = Modifier.padding(start = 44.dp, top = 2.dp),
                fontSize = 16.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GameBanner(app: AppInfo, stat: AppStat?) {
    Box(Modifier.fillMaxSize()) {
        val art = app.banner ?: app.icon
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.72f)
                .then(if (app.banner == null) Modifier.blur(36.dp).alpha(0.55f) else Modifier.blur(2.dp))
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Bg, 0.38f to Bg.copy(alpha = 0.85f), 0.85f to Color.Transparent
                )
            )
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Bg)))

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 112.dp).widthIn(max = 420.dp)
        ) {
            Text(app.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(describe(app, stat), fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
        }

        // --- достижения (трофей + проценты), считаются по запускам ---
        val launches = (stat?.launches ?: 0).coerceAtMost(50)
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 52.dp)
                .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(6.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            Icon(VIcons.Trophy, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(34.dp))
            Column {
                Text("Выполнено", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text("${launches * 100 / 50}%", fontSize = 22.sp, color = Color.White)
            }
            Column {
                Text("Получено", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text("$launches/50", fontSize = 22.sp, color = Color.White)
            }
        }
    }
}

private fun describe(app: AppInfo, stat: AppStat?): String {
    val kind = if (app.isGame) "Игра" else "Приложение"
    if (stat == null || stat.launches == 0) return "$kind · ещё не запускалось"
    val last = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("ru")).format(Date(stat.lastLaunch))
    return "$kind · запусков: ${stat.launches} · последний: $last"
}

@Composable
private fun CarouselTile(app: AppInfo, requester: FocusRequester?, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocus(focused) }
    val size by animateDpAsState(
        if (focused) 118.dp else 78.dp,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "tileSize"
    )
    val shape = RoundedCornerShape(if (focused) 18.dp else 14.dp)
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(if (focused) Modifier.border(2.5.dp, Color.White, shape) else Modifier)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    )
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onFocus: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val underline by animateDpAsState(if (focused) 26.dp else 0.dp, spring(stiffness = Spring.StiffnessMedium), label = "tabLine")
    Column(
        Modifier.clickable(interactionSource = source, indication = null, onClick = onFocus),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text, fontSize = 22.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f)
        )
        Box(Modifier.padding(top = 4.dp).height(3.dp).width(underline).background(Color.White, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun FlatIconButton(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, spring(dampingRatio = 0.6f), label = "flat")
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (focused) Color.White.copy(alpha = 0.22f) else Color.Transparent, CircleShape)
            .then(if (focused) Modifier.border(1.5.dp, Color.White, CircleShape) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
