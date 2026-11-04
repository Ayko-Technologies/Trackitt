package com.ayko.trackitt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small, non-lazy grid that fits as many columns as the width allows and spreads them evenly
 * across it, so a wide (landscape) screen gets more columns rather than empty space on the right.
 *
 * @param maxRows show only this many rows (for a collapsed preview); null shows everything.
 */
@Composable
fun <T> SpreadGrid(
    items: List<T>,
    minCellWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    maxRows: Int? = null,
    cell: @Composable (index: Int, item: T) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = ((maxWidth + spacing) / (minCellWidth + spacing)).toInt().coerceAtLeast(1)
        val rows = items.indices.chunked(columns).let { if (maxRows != null) it.take(maxRows) else it }
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { index ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            cell(index, items[index])
                        }
                    }
                    // Keep a short last row aligned to the columns above it.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
