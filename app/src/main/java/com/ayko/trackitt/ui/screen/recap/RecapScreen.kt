package com.ayko.trackitt.ui.screen.recap

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.SideEffect
import com.ayko.trackitt.ui.component.LocalStatusBarBackdrop
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayko.trackitt.service.recap.YearRecap
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import kotlinx.coroutines.flow.collectLatest

/** True when the user turned animations off, or a screen reader is reading: show things as they are. */
internal val LocalReduceMotion = staticCompositionLocalOf { false }

private const val SLIDE_MILLIS = 7000

/**
 * A year of the user's money as a story they tap through, like Spotify Wrapped. Each slide makes one
 * point; tapping the right of the screen moves on, the left goes back, and holding pauses.
 */
@Composable
fun RecapScreen(year: Int, onClose: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: RecapViewModel = viewModel(
        key = "recap-$year",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RecapViewModel(application, year) as T
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val reduceMotion = remember {
        val animationsOff = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        val screenReader = context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
        animationsOff || screenReader
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        when (val current = state) {
            RecapState.Loading -> Box(Modifier.fillMaxSize().background(RecapPalette.Ink), contentAlignment = Alignment.Center) {
                StatusBar(RecapPalette.Ink, light = false)
                CircularProgressIndicator(color = RecapPalette.Mint)
            }
            RecapState.Empty -> Box(Modifier.fillMaxSize().background(RecapPalette.Ink), contentAlignment = Alignment.Center) {
                StatusBar(RecapPalette.Ink, light = false)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Nothing to recap for $year yet.", color = Color.White, fontSize = 18.sp, fontFamily = TrackittBodyFamily)
                    TextButton(onClick = onClose) { Text("Close", color = RecapPalette.Mint) }
                }
            }
            is RecapState.Ready -> RecapStory(current.recap, autoAdvance = !reduceMotion, onClose = onClose)
        }
    }
}

@Composable
private fun RecapStory(recap: YearRecap, autoAdvance: Boolean, onClose: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val slides = remember(recap) { recapSlides(recap, onReplay = { index = 0 }, onClose = onClose) }
    val last = slides.lastIndex
    var paused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(index) {
        progress.snapTo(0f)
        if (!autoAdvance || index >= last) return@LaunchedEffect
        // Holding a finger down stops the clock; letting go carries on from where it stopped.
        snapshotFlow { paused }.collectLatest { isPaused ->
            if (isPaused) return@collectLatest
            val remaining = ((1f - progress.value) * SLIDE_MILLIS).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            index = (index + 1).coerceAtMost(last)
        }
    }

    val slide = slides[index.coerceAtMost(last)]
    StatusBar(slide.background, light = slide.light)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RecapPalette.Ink)
            .pointerInput(last) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.3f) index = (index - 1).coerceAtLeast(0)
                        else if (index < last) index += 1
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = index.coerceAtMost(last),
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
            label = "recap slide",
        ) { shown ->
            val s = slides[shown]
            Box(
                Modifier
                    .fillMaxSize()
                    .background(s.background)
                    .navigationBarsPadding()
                    .padding(top = 56.dp),
            ) {
                s.content()
            }
        }

        StoryChrome(
            count = slides.size,
            index = index,
            progress = if (autoAdvance && index < last) progress.value else 1f,
            year = recap.year,
            onDark = !slide.light,
            onClose = onClose,
        )
    }
}

@Composable
private fun StoryChrome(count: Int, index: Int, progress: Float, year: Int, onDark: Boolean, onClose: () -> Unit) {
    val ink = if (onDark) Color.White else RecapPalette.Ink
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Slide ${index + 1} of $count" },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(count) { i ->
                val fill = when {
                    i < index -> 1f
                    i == index -> progress
                    else -> 0f
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ink.copy(alpha = 0.3f)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(ink))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                "TRACKITT · $year",
                color = ink,
                fontFamily = TrackittBodyFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close recap", tint = ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Runs the slide's [color] up behind the status bar, with icons that show on it: dark ones on a
 * [light] slide, light ones otherwise. Both go back to the app's own on leaving.
 */
@Composable
private fun StatusBar(color: Color, light: Boolean) {
    val backdrop = LocalStatusBarBackdrop.current
    val shown by animateColorAsState(color, tween(260), label = "status bar")
    // Read here, not inside the effect, so each step of the fade recomposes and reaches the bar.
    val current = shown
    SideEffect { backdrop.value = current }
    DisposableEffect(backdrop) { onDispose { backdrop.value = null } }

    val view = LocalView.current
    if (view.isInEditMode) return
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    val controller = remember(window) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val before = controller.isAppearanceLightStatusBars
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
    LaunchedEffect(light) { controller.isAppearanceLightStatusBars = light }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal fun Gap(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
internal fun HGap(width: Int) = Spacer(Modifier.width(width.dp))
