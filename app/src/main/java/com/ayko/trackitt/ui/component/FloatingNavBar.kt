package com.ayko.trackitt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ButtonSize = 52.dp
private val ButtonShape = RoundedCornerShape(16.dp)
private val IconSize = 22.dp

@Composable
fun FloatingNavBar(
    onAddTap: () -> Unit = {},
    onBudgetTap: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
    ) {
        FloatingActionButton(
            onClick = onAddTap,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            shape = ButtonShape,
            modifier = Modifier.size(ButtonSize),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add expense", modifier = Modifier.size(IconSize))
        }

        Spacer(Modifier.size(12.dp))

        FloatingActionButton(
            onClick = onBudgetTap,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            shape = ButtonShape,
            modifier = Modifier.size(ButtonSize),
        ) {
            Icon(Icons.Filled.DonutSmall, contentDescription = "Budgets", modifier = Modifier.size(IconSize))
        }

        Spacer(Modifier.weight(1f))

        FloatingActionButton(
            onClick = onSearchTap,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            shape = ButtonShape,
            modifier = Modifier.size(ButtonSize),
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(IconSize))
        }
    }
}
