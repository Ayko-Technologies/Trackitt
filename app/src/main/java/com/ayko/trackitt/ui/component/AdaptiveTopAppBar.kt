package com.ayko.trackitt.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A large title bar on tall screens; a standard one in landscape, where the large one eats the screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    if (isCompactHeight()) {
        TopAppBar(title, modifier, navigationIcon, actions, colors = colors, scrollBehavior = scrollBehavior)
    } else {
        LargeTopAppBar(title, modifier, navigationIcon, actions, colors = colors, scrollBehavior = scrollBehavior)
    }
}
