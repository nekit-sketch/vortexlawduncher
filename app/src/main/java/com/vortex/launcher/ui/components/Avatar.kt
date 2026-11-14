package com.vortex.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.vortex.launcher.ui.VIcons

val ProfileColors = listOf(
    Color(0xFF4DA3FF), Color(0xFFFF6B6B), Color(0xFF34C759),
    Color(0xFFFFB84D), Color(0xFFB36BFF), Color(0xFF2EC4B6)
)

@Composable
fun ProfileAvatar(colorIndex: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(ProfileColors[colorIndex.mod(ProfileColors.size)], CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(VIcons.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}
