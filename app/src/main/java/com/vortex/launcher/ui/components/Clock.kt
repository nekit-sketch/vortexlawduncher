package com.vortex.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.weather.Weather
import com.vortex.launcher.ui.VIcons
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru"))

fun LocalDateTime.timeText(): String = format(timeFmt)
fun LocalDateTime.dateText(): String = format(dateFmt).replaceFirstChar { it.uppercase() }

@Composable
fun rememberNow(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - (System.currentTimeMillis() % 60_000L) + 20L)
    }
}

/** Огромные полупрозрачные часы (24ч) + строка погоды с векторными иконками. */
@Composable
fun BigClock(
    scale: Float,
    weather: Weather?,
    showWeather: Boolean,
    modifier: Modifier = Modifier
) {
    val now by rememberNow()
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(now.dateText(), fontSize = (16 * scale).sp, color = Color.White.copy(alpha = 0.85f))
            if (showWeather && weather != null) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(4.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
                Spacer(Modifier.width(10.dp))
                Icon(weather.icon(), null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size((20 * scale).dp))
                Spacer(Modifier.width(6.dp))
                Text("${weather.temp}° ${weather.label()}", fontSize = (16 * scale).sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Text(
            now.timeText(),
            fontSize = (120 * scale).sp,
            lineHeight = (120 * scale).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-3).sp,
            color = Color.White.copy(alpha = 0.55f)
        )
    }
}
