package com.kazewell.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KW {
    val Bg = Color(0xFF08100F)
    val Surface = Color(0xFF0F1B19)
    val Surface2 = Color(0xFF162624)
    val Surface3 = Color(0xFF1F3733)
    val Outline = Color(0xFF244540)

    val Teal = Color(0xFF2DD4BF)
    val Green = Color(0xFF34D399)
    val Red = Color(0xFFF87171)
    val Amber = Color(0xFFFBBF24)

    val Text = Color(0xFFEAF7F4)
    val TextDim = Color(0xFF8BA8A2)
    val TextFaint = Color(0xFF5B7873)
}

private val Scheme = darkColorScheme(
    primary = KW.Teal,
    secondary = KW.Green,
    background = KW.Bg,
    surface = KW.Surface,
    onPrimary = Color(0xFF04201C),
    onBackground = KW.Text,
    onSurface = KW.Text,
    outline = KW.Outline,
    error = KW.Red
)

@Composable
fun KazeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    color: Color = KW.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, KW.Outline.copy(alpha = 0.7f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) KW.Teal.copy(alpha = 0.2f) else KW.Surface2)
            .border(1.dp, if (selected) KW.Teal else KW.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) KW.Text else KW.TextDim,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}
