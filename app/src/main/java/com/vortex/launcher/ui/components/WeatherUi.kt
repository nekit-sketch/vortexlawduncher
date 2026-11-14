package com.vortex.launcher.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.vortex.launcher.data.weather.Weather
import com.vortex.launcher.ui.VIcons

fun Weather.icon(): ImageVector = when (code) {
    0 -> VIcons.Sun
    in 51..67, in 80..82 -> VIcons.Rain
    in 71..77, 85, 86 -> VIcons.Snow
    in 95..99 -> VIcons.Storm
    else -> VIcons.Cloud
}

fun Weather.label(): String = when (code) {
    0 -> "Ясно"
    1, 2 -> "Малооблачно"
    3 -> "Облачно"
    45, 48 -> "Туман"
    in 51..67 -> "Дождь"
    in 71..77, 85, 86 -> "Снег"
    in 80..82 -> "Ливень"
    in 95..99 -> "Гроза"
    else -> "Облачно"
}
