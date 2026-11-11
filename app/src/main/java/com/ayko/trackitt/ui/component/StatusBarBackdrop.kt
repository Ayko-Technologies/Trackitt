package com.ayko.trackitt.ui.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colour behind the status bar. Every screen sits below the status bar on the app's surface
 * colour; a full-bleed screen (the year recap) sets this to its own background so the colour runs
 * to the top edge, and clears it on leaving.
 */
val LocalStatusBarBackdrop = staticCompositionLocalOf<MutableState<Color?>> { mutableStateOf(null) }
