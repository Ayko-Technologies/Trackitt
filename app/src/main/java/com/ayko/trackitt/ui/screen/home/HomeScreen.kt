package com.ayko.trackitt.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.isCompactHeight
import com.ayko.trackitt.ui.component.BalanceStrip
import com.ayko.trackitt.ui.component.CategoryBottomSheet
import com.ayko.trackitt.ui.component.DayPicker
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.component.MonthPicker
import com.ayko.trackitt.ui.component.FloatingNavBar
import com.ayko.trackitt.ui.component.MonthChart
import com.ayko.trackitt.ui.component.TrackittChip
import com.ayko.trackitt.ui.theme.OnSurface
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onAddTap: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    onBudgetTap: () -> Unit = {},
    onInsightsTap: (Int, Int) -> Unit = { _, _ -> },
    onTransactionTap: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle(initialValue = 0.0)
    val allChartCategories by viewModel.chartCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val displayPrefs = remember { context.getSharedPreferences("trackitt_display", 0) }
    val showIncome = remember { displayPrefs.getBoolean("show_income", true) }
    val chartCategories = allChartCategories
        .filter { showIncome || it.category != com.ayko.trackitt.domain.model.TransactionCategory.INCOME }
    val smsGranted by viewModel.smsPermissionGranted.collectAsStateWithLifecycle()
    val balances by viewModel.balances.collectAsStateWithLifecycle(initialValue = emptyList())

    val monthName by viewModel.currentMonthName.collectAsStateWithLifecycle(initialValue = "")
    val currentYear by viewModel.currentYear.collectAsStateWithLifecycle(initialValue = 2026)

    val dayLabel by viewModel.dayLabel.collectAsStateWithLifecycle(initialValue = "Today")

    val empty = chartCategories.isEmpty()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    // A transaction opened from a category's list only hides the list, so going back to Home
    // reopens it where it was.
    var selectedCategoryForSheet by rememberSaveable { mutableStateOf<TransactionCategory?>(null) }
    // Not saved, so it is already false on the first frame back.
    var categorySheetHidden by remember { mutableStateOf(false) }
    // How far the list was pulled up then, so it comes back already open instead of sliding up.
    var categorySheetReopenAt by rememberSaveable { mutableStateOf<SheetValue?>(null) }
    val categoryListState = rememberSaveable(selectedCategoryForSheet, saver = LazyListState.Saver) { LazyListState() }
    LaunchedEffect(selectedCategoryForSheet) { viewModel.showCategorySheet(selectedCategoryForSheet?.name) }
    val categorySheetTransactions by viewModel.categorySheetTransactions.collectAsStateWithLifecycle()
    // Null until loaded: an empty first frame would scroll the reopened list back to the top.
    val categoryTransactions = categorySheetTransactions
        ?.takeIf { (name, _) -> name == selectedCategoryForSheet?.name }
        ?.second
    LaunchedEffect(categoryTransactions) {
        // Every transaction in it was moved to another category.
        if (categoryTransactions?.isEmpty() == true) selectedCategoryForSheet = null
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            viewModel.classifyPending()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
    } else if (total % 1.0 != 0.0) {
        String.format(Locale.US, "₵%.2f", total)
    } else {
        String.format(Locale.US, "₵%.0f", total)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Landscape is too short to share between the header and the chart, so the page scrolls
        // and the chart keeps its portrait height.
        val compactHeight = isCompactHeight()
        // Scrolled into view, the chart fills the screen: as tall as it can be without cropping.
        val landscapeChartHeight = LocalConfiguration.current.screenHeightDp.dp.coerceAtLeast(360.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (compactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 22.dp, end = 16.dp, top = 10.dp),
            ) {
                Spacer(Modifier.weight(1f))
                if (compactHeight) {
                    // Landscape: the actions live up here; floating, they would cover the chart.
                    IconButton(onClick = onSearchTap) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onBudgetTap) {
                        Icon(Icons.Filled.DonutSmall, contentDescription = "Budgets", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = onAddTap) {
                        Icon(Icons.Filled.Add, contentDescription = "Add expense", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onSettingsTap) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            val totalBlock: @Composable () -> Unit = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).rotate(45f)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(OnSurface)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SPENT · $periodLabel",
                            fontFamily = TrackittBodyFamily,
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
                }
            }
            val periodChips: @Composable (Modifier) -> Unit = { chipsModifier ->
                Row(modifier = chipsModifier.horizontalScroll(rememberScrollState())) {
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
                    if (selectedPeriod == Period.MONTH) {
                        Spacer(Modifier.width(8.dp))
                        TrackittChip(
                            label = "Insights",
                            selected = false,
                            onClick = {
                                onInsightsTap(
                                    viewModel.selectedYearValue,
                                    viewModel.selectedMonthValue,
                                )
                            },
                            leadingIcon = Icons.Filled.AutoAwesome,
                        )
                    }
                }
            }
            if (compactHeight) {
                // Landscape: the period buttons sit to the right of the total instead of under it.
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp),
                ) {
                    totalBlock()
                    Spacer(Modifier.width(24.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomEnd) {
                        periodChips(Modifier.padding(bottom = 14.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                ) {
                    totalBlock()
                    periodChips(Modifier.padding(top = 14.dp))
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
                        text = "Allow SMS access to track your bank and wallet transactions",
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

            BalanceStrip(
                balances = balances,
                recentGapsSince = viewModel.recentGapsSince,
                modifier = Modifier.padding(top = 12.dp),
            )

            MonthChart(
                categories = chartCategories,
                totalFormatted = totalFormatted,
                monthLabel = monthName,
                empty = empty,
                onCategoryTap = { category ->
                    categorySheetReopenAt = null
                    selectedCategoryForSheet = category
                },
                barsBottomPadding = if (compactHeight) 24.dp else 100.dp,
                modifier = Modifier
                    .padding(top = if (smsGranted) 20.dp else 4.dp)
                    .then(if (compactHeight) Modifier.height(landscapeChartHeight) else Modifier.weight(1f)),
            )
        }

        if (!compactHeight) {
            FloatingNavBar(
                onAddTap = onAddTap,
                onBudgetTap = onBudgetTap,
                onSearchTap = onSearchTap,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

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

        val sheetCategory = selectedCategoryForSheet
        val sheetTransactions = categoryTransactions
        if (sheetCategory != null && !categorySheetHidden && !sheetTransactions.isNullOrEmpty()) {
            val density = LocalDensity.current
            val compactHeight = isCompactHeight()
            val categorySheetState = remember(sheetCategory) {
                val reopenAt = categorySheetReopenAt
                SheetState(
                    // Otherwise a fully opened list would come back half open.
                    skipPartiallyExpanded = compactHeight || reopenAt == SheetValue.Expanded,
                    density = density,
                    initialValue = reopenAt ?: SheetValue.Hidden,
                )
            }
            CategoryBottomSheet(
                category = sheetCategory,
                transactions = sheetTransactions,
                sheetState = categorySheetState,
                listState = categoryListState,
                onTransactionTap = { id ->
                    categorySheetReopenAt = categorySheetState.currentValue
                    categorySheetHidden = true
                    onTransactionTap(id)
                },
                onDismiss = {
                    categorySheetReopenAt = null
                    selectedCategoryForSheet = null
                },
                includeFees = remember { context.getSharedPreferences("trackitt_display", 0).getBoolean("show_fees", false) },
            )
        }
    }
}
