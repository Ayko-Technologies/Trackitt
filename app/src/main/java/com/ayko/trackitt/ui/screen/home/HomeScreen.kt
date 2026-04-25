package com.ayko.trackitt.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.component.FloatingNavBar
import com.ayko.trackitt.ui.component.MonthChart
import com.ayko.trackitt.ui.component.TrackittChip
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.OnSurface

@Composable
fun HomeScreen(
    onAddTap: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    onBudgetTap: () -> Unit = {},
    onTransactionTap: (String) -> Unit = {},
) {
    val empty = fakeTransactions.isEmpty()
    val total = if (empty) 0.0 else fakeTotalSpent

    var selectedChip by remember { mutableIntStateOf(1) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — settings only, no logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 22.dp, end = 16.dp, top = 10.dp),
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettingsTap) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Total spent
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).rotate(45f)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(OnSurface)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SPENT · APRIL",
                        fontFamily = SatoshiFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DisplayNumber(
                    value = total,
                    sizeSp = 72f,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Chips — Today and month
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    TrackittChip(
                        label = "Today",
                        selected = selectedChip == 0,
                        onClick = { selectedChip = 0 },
                        leadingIcon = Icons.Filled.Today,
                    )
                    Spacer(Modifier.width(8.dp))
                    TrackittChip(
                        label = "April 2026",
                        selected = selectedChip == 1,
                        onClick = { selectedChip = 1 },
                        leadingIcon = Icons.Filled.CalendarMonth,
                    )
                }
            }

            // Chart
            MonthChart(
                categories = if (empty) emptyList() else fakeChartCategories,
                totalFormatted = "₵2.3k",
                empty = empty,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .weight(1f),
            )
        }

        FloatingNavBar(
            onAddTap = onAddTap,
            onBudgetTap = onBudgetTap,
            onSearchTap = onSearchTap,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
