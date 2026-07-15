package com.j.m3play.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j.m3play.viewmodels.MetrolistViewModel

@Composable
fun MetrolistLyricsView(
    viewModel: MetrolistViewModel,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines by viewModel.lyricsLines
    val position = viewModel.currentPosition.longValue
    val listState = rememberLazyListState()
    
    val activeIndex by remember(position, lines) {
        derivedStateOf {
            lines.indexOfLast { it.startTime <= position + 200L }.coerceAtLeast(0)
        }
    }

    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty() && activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -450)
        }
    }

    if (lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No synced lyrics available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 350.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val isPassed = index < activeIndex
            
            val progress = if (isActive) {
                ((position - line.startTime).toFloat() / (line.endTime - line.startTime).toFloat()).coerceIn(0f, 1f)
            } else if (isPassed) 1f else 0f
            
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f, 
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "scale"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPassed) 0.3f else 0.45f,
                animationSpec = tween(400),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSeek(line.startTime) },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = line.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 44.sp,
                    color = Color.White.copy(alpha = if (isActive) 0.25f else 0.8f),
                    textAlign = TextAlign.Start,
                    style = TextStyle(
                        shadow = if (isActive) Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            offset = Offset.Zero,
                            blurRadius = 30f 
                        ) else null
                    ),
                    modifier = if (isActive) {
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val width = size.width * progress
                                val edgeWidth = 36.dp.toPx()
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color.Transparent),
                                        startX = width - edgeWidth,
                                        endX = width + edgeWidth
                                    ),
                                    blendMode = BlendMode.SrcIn
                                )
                            }
                    } else Modifier
                )
            }
        }
    }
}
