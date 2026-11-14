package com.vortex.launcher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Liquid Glass: полупрозрачная подложка + белая обводка 1dp. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    filled: Boolean = false,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = if (filled)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.97f), Color.White.copy(alpha = 0.86f)))
    else
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.09f)))
    Box(
        modifier = modifier
            .background(brush, shape)
            .border(1.dp, Color.White.copy(alpha = if (filled || strong) 1f else 0.5f), shape),
        content = content
    )
}

/** Фокусируемая стеклянная кнопка: spring-масштаб + белая обводка при фокусе. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    focusRequester: FocusRequester? = null,
    fillOnFocus: Boolean = false,
    selected: Boolean = false,
    focusScale: Float = 1.05f,
    onFocusChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChange(focused) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "glassScale"
    )
    GlassSurface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        shape = shape,
        filled = focused && fillOnFocus,
        strong = focused || selected
    ) { content(focused) }
}

/** Большая белая кнопка («Играть», «Открыть»). */
@Composable
fun WhiteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "whiteBtn"
    )
    val shape = RoundedCornerShape(50)
    val ink = Color(0xFF0B0F1A)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Color.White.copy(alpha = if (focused) 1f else 0.82f), shape)
            .then(if (focused) Modifier.border(2.dp, Color(0xFF7FB2FF), shape) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
