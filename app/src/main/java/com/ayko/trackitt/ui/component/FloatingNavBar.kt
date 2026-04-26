package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val BtnSize = 52.dp
private val BtnShape = RoundedCornerShape(16.dp)
private val IconSize = 22.dp

@Composable
fun FloatingNavBar(
    onAddTap: () -> Unit = {},
    onBudgetTap: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NavButton(
                icon = Icons.Filled.Add,
                contentDescription = "Add expense",
                bg = MaterialTheme.colorScheme.primary,
                tint = MaterialTheme.colorScheme.onPrimary,
                onClick = onAddTap,
            )
            NavButton(
                icon = Icons.Filled.DonutSmall,
                contentDescription = "Budgets",
                bg = MaterialTheme.colorScheme.surfaceContainerHigh,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBudgetTap,
            )
        }
        NavButton(
            icon = Icons.Filled.Search,
            contentDescription = "Search",
            bg = MaterialTheme.colorScheme.primary,
            tint = MaterialTheme.colorScheme.onPrimary,
            onClick = onSearchTap,
        )
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    contentDescription: String,
    bg: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BtnSize)
            .clip(BtnShape)
            .background(bg)
            .clickable { onClick() },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(IconSize),
        )
    }
}
