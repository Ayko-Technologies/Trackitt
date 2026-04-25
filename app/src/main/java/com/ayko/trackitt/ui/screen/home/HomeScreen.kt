package com.ayko.trackitt.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.component.FloatingNavBar
import com.ayko.trackitt.ui.component.MonthChart
import com.ayko.trackitt.ui.component.TrackittChip
import com.ayko.trackitt.ui.theme.OnSurface
import com.ayko.trackitt.ui.theme.SatoshiFamily
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onAddTap: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    onBudgetTap: () -> Unit = {},
    onTransactionTap: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle(initialValue = 0.0)
    val chartCategories by viewModel.chartCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val smsGranted by viewModel.smsPermissionGranted.collectAsStateWithLifecycle()

    val empty = chartCategories.isEmpty()

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.smsPermissionGranted.value = granted
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onSmsPermissionResult(granted, context)
    }

    val periodLabel = if (selectedPeriod == Period.TODAY) "TODAY" else viewModel.currentMonthName.uppercase()
    val totalFormatted = if (total >= 1000) {
        String.format(Locale.US, "₵%.1fk", total / 1000)
    } else {
        String.format(Locale.US, "₵%.0f", total)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        text = "SPENT · $periodLabel",
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

                Row(modifier = Modifier.padding(top = 14.dp)) {
                    TrackittChip(
                        label = "Today",
                        selected = selectedPeriod == Period.TODAY,
                        onClick = { viewModel.selectPeriod(Period.TODAY) },
                        leadingIcon = Icons.Filled.Today,
                    )
                    Spacer(Modifier.width(8.dp))
                    TrackittChip(
                        label = "${viewModel.currentMonthName} ${viewModel.currentYear}",
                        selected = selectedPeriod == Period.MONTH,
                        onClick = { viewModel.selectPeriod(Period.MONTH) },
                        leadingIcon = Icons.Filled.CalendarMonth,
                    )
                }
            }

            // SMS permission card
            if (!smsGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Allow SMS access to track MoMo transactions",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Grant", fontSize = 12.sp)
                    }
                }
            }

            MonthChart(
                categories = chartCategories,
                totalFormatted = totalFormatted,
                monthLabel = viewModel.currentMonthName,
                empty = empty,
                modifier = Modifier
                    .padding(top = if (smsGranted) 20.dp else 4.dp)
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
