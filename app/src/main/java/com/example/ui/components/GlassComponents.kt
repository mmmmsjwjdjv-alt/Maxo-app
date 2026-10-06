package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.random.Random

/**
 * Animated subtle ambient particles in monochrome for high-end aesthetic
 */
@Composable
fun SubtleMonochromeParticles(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animOffset"
    )

    // Generate static seed particles
    val particles = remember {
        List(25) {
            ParticleData(
                xNorm = Random.nextFloat(),
                yNorm = Random.nextFloat(),
                radius = Random.nextFloat() * 1.8f + 0.8f,
                speed = Random.nextFloat() * 0.4f + 0.1f,
                alpha = Random.nextFloat() * 0.25f + 0.05f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (p in particles) {
            val curY = ((p.yNorm * height - animOffset * p.speed) % height + height) % height
            val curX = p.xNorm * width
            drawCircle(
                color = Color.White.copy(alpha = p.alpha),
                radius = p.radius.dp.toPx(),
                center = Offset(curX, curY)
            )
        }
    }
}

private data class ParticleData(
    val xNorm: Float,
    val yNorm: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float
)

/**
 * High-end Glassmorphic Card container with thin border and subtle inner gradient
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.15f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha * 1.5f),
                        Color.White.copy(alpha = borderAlpha * 0.5f),
                        Color.White.copy(alpha = borderAlpha * 0.2f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaxoGlassCardBgDark
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            content = content
        )
    }
}
