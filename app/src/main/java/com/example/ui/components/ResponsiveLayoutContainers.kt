package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive Split Layout that renders side-by-side columns on wide screens (desktop/tablet landscape)
 * and vertically stacked layouts on compact screens (mobile).
 */
@Composable
fun ResponsiveTwoPaneLayout(
    isWideScreen: Boolean,
    primaryPane: @Composable () -> Unit,
    secondaryPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    primaryWeight: Float = 0.45f,
    secondaryWeight: Float = 0.55f,
    spacing: Dp = 16.dp
) {
    if (isWideScreen) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .weight(primaryWeight)
                    .fillMaxWidth()
            ) {
                primaryPane()
            }
            Box(
                modifier = Modifier
                    .weight(secondaryWeight)
                    .fillMaxWidth()
            ) {
                secondaryPane()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            primaryPane()
            secondaryPane()
        }
    }
}

/**
 * Centered responsive wrapper to maintain natural max-widths on ultra-wide desktop monitors
 * without stretching buttons, forms, and cards unnaturally.
 */
@Composable
fun ResponsiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1400.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
