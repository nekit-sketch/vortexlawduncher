package com.vortex.launcher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vortex.launcher.data.apps.AppInfo

/** Иконка приложения: при фокусе увеличивается (spring) + белая обводка. */
@Composable
fun AppIconTile(
    app: AppInfo,
    size: Dp,
    modifier: Modifier = Modifier,
    focusScale: Float = 1.2f,
    liftFromBottom: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocus(focused) }
    val scale by animateFloatAsState(
        if (focused) focusScale else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )
    val shape = RoundedCornerShape(size * 0.23f)
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = modifier
            .size(size)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = if (liftFromBottom) TransformOrigin(0.5f, 1f) else TransformOrigin.Center
            }
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, Color.White, shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    )
}
