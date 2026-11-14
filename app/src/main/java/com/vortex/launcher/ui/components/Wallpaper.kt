package com.vortex.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

object Wallpapers {
    val names = listOf("Океан", "Лес", "Закат", "Туман", "Ночь")
    val gradients: List<List<Color>> = listOf(
        listOf(Color(0xFF0B3C5D), Color(0xFF2A7FB8), Color(0xFF10222E)),
        listOf(Color(0xFF123226), Color(0xFF3C7F63), Color(0xFF0A1A14)),
        listOf(Color(0xFF2B1055), Color(0xFFC2527A), Color(0xFF3A1C2E)),
        listOf(Color(0xFFCFE0EE), Color(0xFF8FAEC2), Color(0xFF3F5E5A)),
        listOf(Color(0xFF05070D), Color(0xFF111C3D), Color(0xFF1C2E63))
    )
    fun at(i: Int): List<Color> = gradients[i.mod(gradients.size)]
    fun name(i: Int): String = names[i.mod(names.size)]
}

@Composable
fun WallpaperBackground(index: Int, gaming: Boolean) {
    val g by animateFloatAsState(if (gaming) 1f else 0f, tween(700), label = "bgMorph")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(Wallpapers.at(index))))
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = g }
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1330), Color(0xFF05070F), Color.Black)))
        )
    }
}
