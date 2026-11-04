package com.ayko.trackitt.ui.component

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True when the window is too short for layouts designed around a tall phone screen, such as a
 * phone in landscape. Screens then lay out side by side or scroll instead of stacking.
 */
@Composable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < 480

/**
 * Bottom sheets span the full width in landscape instead of a narrow centred column. Pair with
 * `skipPartiallyExpanded = isCompactHeight()` so they open fully on the short screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun sheetMaxWidth(): Dp = if (isCompactHeight()) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth

/** Category grids fit four of these across a portrait phone, and more in landscape. */
val CATEGORY_CELL_MIN_WIDTH = 84.dp
