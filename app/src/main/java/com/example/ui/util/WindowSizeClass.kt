package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive Window Size Classes and Screen Size categorization for RVH Marketing Studio.
 * Supports Compact (Phone), Medium (Foldable / Tablet portrait), and Expanded (Tablet landscape / Desktop).
 */
enum class WindowSizeCategory {
    COMPACT, // < 600dp (Mobile)
    MEDIUM,  // 600dp .. 839dp (Tablet portrait / Large foldables)
    EXPANDED // >= 840dp (Desktop / Tablet landscape / Browser preview)
}

data class ScreenLayoutInfo(
    val category: WindowSizeCategory,
    val screenWidthDp: Dp,
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val isTabletOrDesktop: Boolean
)

@Composable
fun rememberScreenLayoutInfo(simulatedMode: String = "AUTO"): ScreenLayoutInfo {
    val configuration = LocalConfiguration.current
    val physicalWidth = configuration.screenWidthDp.dp

    val effectiveCategory = when (simulatedMode) {
        "MOBILE" -> WindowSizeCategory.COMPACT
        "TABLET" -> WindowSizeCategory.MEDIUM
        "DESKTOP" -> WindowSizeCategory.EXPANDED
        else -> when {
            physicalWidth < 600.dp -> WindowSizeCategory.COMPACT
            physicalWidth < 840.dp -> WindowSizeCategory.MEDIUM
            else -> WindowSizeCategory.EXPANDED
        }
    }

    val effectiveWidth = when (simulatedMode) {
        "MOBILE" -> 390.dp
        "TABLET" -> 720.dp
        "DESKTOP" -> 1100.dp
        else -> physicalWidth
    }

    return ScreenLayoutInfo(
        category = effectiveCategory,
        screenWidthDp = effectiveWidth,
        isCompact = effectiveCategory == WindowSizeCategory.COMPACT,
        isMedium = effectiveCategory == WindowSizeCategory.MEDIUM,
        isExpanded = effectiveCategory == WindowSizeCategory.EXPANDED,
        isTabletOrDesktop = effectiveCategory != WindowSizeCategory.COMPACT
    )
}
