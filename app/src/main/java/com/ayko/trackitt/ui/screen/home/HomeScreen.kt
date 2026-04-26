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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ayko.trackitt.ui.component.CategoryBottomSheet
import com.ayko.trackitt.ui.component.DayPicker
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.component.MonthPicker
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

    val monthName by viewModel.currentMonthName.collectAsStateWithLifecycle(initialValue = "")
    val currentYear by viewModel.currentYear.collectAsStateWithLifecycle(initialValue = 2026)

    val dayLabel by viewModel.dayLabel.collectAsStateWithLifecycle(initialValue = "Today")

    val empty = chartCategories.isEmpty()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var selectedCategoryForSheet by remember { mutableStateOf<com.ayko.trackitt.domain.model.TransactionCategory?>(null) }
    val categoryTransactions by viewModel.getCategoryTransactions(selectedCategoryForSheet?.name).collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val bothGranted = receiveGranted && readGranted
        viewModel.smsPermissionGranted.value = bothGranted

        if (bothGranted) {
            val prefs = context.getSharedPreferences("trackitt", 0)
            if (!prefs.getBoolean("backfill_completed", false)) {
                viewModel.onSmsPermissionResult(true, context)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onSmsPermissionResult(allGranted, context)
    }

    val periodLabel = if (selectedPeriod == Period.DAY) dayLabel.uppercase() else monthName.uppercase()
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
                        label = dayLabel,
                        selected = selectedPeriod == Period.DAY,
                        onClick = {
                            if (selectedPeriod == Period.DAY) showDayPicker = true
                            else viewModel.selectPeriod(Period.DAY)
                        },
                        leadingIcon = Icons.Filled.Today,
                    )
                    Spacer(Modifier.width(8.dp))
                    TrackittChip(
                        label = "$monthName $currentYear",
                        selected = selectedPeriod == Period.MONTH,
                        onClick = {
                            if (selectedPeriod == Period.MONTH) showMonthPicker = true
                            else viewModel.selectPeriod(Period.MONTH)
                        },
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
                        onClick = {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS,
                            ))
                        },
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
                monthLabel = monthName,
                empty = empty,
                onCategoryTap = { category -> selectedCategoryForSheet = category },
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

        if (showMonthPicker) {
            MonthPicker(
                selectedYear = viewModel.selectedYearValue,
                selectedMonth = viewModel.selectedMonthValue,
                onMonthSelected = { year, month ->
                    viewModel.setMonth(year, month)
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false },
            )
        }

        if (showDayPicker) {
            DayPicker(
                year = viewModel.selectedYearValue,
                month = viewModel.selectedMonthValue,
                selectedDay = viewModel.selectedDayValue,
                onDaySelected = { day ->
                    viewModel.setDay(day)
                    showDayPicker = false
                },
                onDismiss = { showDayPicker = false },
            )
        }

        if (selectedCategoryForSheet != null) {
            CategoryBottomSheet(
                category = selectedCategoryForSheet!!,
                transactions = categoryTransactions,
                onTransactionTap = { id ->
                    selectedCategoryForSheet = null
                    onTransactionTap(id)
                },
                onDismiss = { selectedCategoryForSheet = null },
            )
        }
    }
}
