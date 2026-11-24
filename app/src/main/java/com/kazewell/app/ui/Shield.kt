package com.kazewell.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The big status shield: glowing teal with a check when protected, red with a cross when off. */
@Composable
fun Shield(on: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glow")
    val glow by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val color by animateColorAsState(if (on) KW.Teal else KW.Red, tween(450), label = "shieldColor")

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        if (on) {
            drawCircle(color.copy(alpha = glow), radius = w * 0.58f, center = Offset(w / 2f, h / 2f))
        }

        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            lineTo(w * 0.88f, h * 0.2f)
            lineTo(w * 0.88f, h * 0.52f)
            cubicTo(w * 0.88f, h * 0.78f, w * 0.68f, h * 0.92f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.32f, h * 0.92f, w * 0.12f, h * 0.78f, w * 0.12f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.2f)
            close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f))))
        drawPath(shield, Color.White.copy(alpha = 0.22f), style = Stroke(width = 3.dp.toPx()))

        val glyph = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val ink = Color(0xFF04201C)
        if (on) {
            val check = Path().apply {
                moveTo(w * 0.33f, h * 0.5f)
                lineTo(w * 0.46f, h * 0.63f)
                lineTo(w * 0.68f, h * 0.37f)
            }
            drawPath(check, ink, style = glyph)
        } else {
            val a = Path().apply {
                moveTo(w * 0.36f, h * 0.36f)
                lineTo(w * 0.64f, h * 0.64f)
            }
            val b = Path().apply {
                moveTo(w * 0.64f, h * 0.36f)
                lineTo(w * 0.36f, h * 0.64f)
            }
            drawPath(a, Color(0xFF2A0A0A), style = glyph)
            drawPath(b, Color(0xFF2A0A0A), style = glyph)
        }
    }
}
