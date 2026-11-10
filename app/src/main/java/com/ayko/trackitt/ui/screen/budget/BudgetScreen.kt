package com.ayko.trackitt.ui.screen.budget

import androidx.compose.material3.FilledIconButton
import com.ayko.trackitt.ui.component.isCompactHeight
import com.ayko.trackitt.ui.component.CATEGORY_CELL_MIN_WIDTH
import com.ayko.trackitt.ui.component.sheetMaxWidth
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.entity.BudgetEntity
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.budget.BudgetEstimate
import com.ayko.trackitt.service.budget.BudgetEstimateOrigin
import com.ayko.trackitt.service.budget.BudgetPlanParser
import com.ayko.trackitt.service.budget.BudgetPlanLine
import com.ayko.trackitt.service.budget.groupBudgetLines
import com.ayko.trackitt.ui.component.CookieIcon
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { TrackittDatabase.getInstance(context) }
    val planParser = remember { BudgetPlanParser() }
    val appContainer = remember { (context.applicationContext as TrackittApplication).container }
    val budgetEstimateService = remember { appContainer.budgetEstimateService }

    val budgets by db.budgetDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())

    val now = LocalDate.now()
    val year = now.year.toString()
    val month = now.monthValue.toString().padStart(2, '0')
    val previousMonth = remember(now) { now.minusMonths(1) }
    val previousYear = previousMonth.year.toString()
    val previousMonthValue = previousMonth.monthValue.toString().padStart(2, '0')
    val displayPrefs = remember { context.getSharedPreferences("trackitt_display", 0) }
    val showFees = remember { displayPrefs.getBoolean("show_fees", false) }
    val categoryTotals by db.transactionDao().getCategoryTotals(year, month, showFees)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val previousCategoryTotals by db.transactionDao().getCategoryTotals(previousYear, previousMonthValue, showFees)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val showIncome = remember { displayPrefs.getBoolean("show_income", true) }
    val visibleBudgets = budgets
    val visibleCategoryTotals = categoryTotals
    val totalBudget = visibleBudgets.sumOf { it.amount }
    val expenseTotals = visibleCategoryTotals.filter { it.category != TransactionCategory.INCOME.name }
    val totalSpent = expenseTotals.sumOf { it.total }
    val totalIncome = visibleCategoryTotals.find { it.category == TransactionCategory.INCOME.name }?.total ?: 0.0
    val budgetEstimate by produceState<BudgetEstimate?>(
        initialValue = null,
        budgets,
        previousCategoryTotals,
        showFees,
        now.year,
        now.monthValue,
    ) {
        value = budgetEstimateService.estimateForMonth(now)
    }

    val compactHeight = isCompactHeight()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            com.ayko.trackitt.ui.component.AdaptiveTopAppBar(
                title = {
                    Text("Budgets", fontFamily = TrackittBodyFamily, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                    // Landscape: a floating button would cover the short list, so it lives up here.
                    if (compactHeight) {
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add budget")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (!compactHeight) FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add budget")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(16.dp),
                    ) {
                        Text("Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = TrackittBodyFamily)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "GH₵${String.format(Locale.US, "%.0f", totalSpent)}",
                            fontSize = 22.sp, fontFamily = FrauncesFamily,
                        )
                        if (totalBudget > 0) {
                            Spacer(Modifier.height(8.dp))
                            val progress = (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(progress, label = "expenseProgress")
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (progress > 0.9f) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Text(
                                "of GH₵${String.format(Locale.US, "%.0f", totalBudget)} budget",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = TrackittBodyFamily, modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (showIncome) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(16.dp),
                        ) {
                            Text("Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = TrackittBodyFamily)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "GH₵${String.format(Locale.US, "%.0f", totalIncome)}",
                                fontSize = 22.sp, fontFamily = FrauncesFamily,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            budgetEstimate?.let { estimate ->
                item {
                    EstimatedBudgetCard(
                        estimate = estimate,
                        monthLabel = now.month.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                QuickImportCard(
                    onImportClick = { showImportDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (visibleBudgets.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                    ) {
                        Text("No budgets yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = TrackittBodyFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Add one budget manually or import a full monthly plan.", fontSize = 13.sp, fontFamily = TrackittBodyFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(visibleBudgets, key = { it.id }) { budget ->
                val cat = try { TransactionCategory.valueOf(budget.category) } catch (_: Exception) { null }
                val spent = visibleCategoryTotals.find { it.category == budget.category }?.total ?: 0.0
                val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                val animatedProgress by animateFloatAsState(progress, label = "catProgress")
                val overBudget = spent > budget.amount

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                ) {
                    if (cat != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(cat.bgColor, CircleShape),
                        ) {
                            Icon(cat.icon, contentDescription = null, tint = cat.fgColor, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                cat?.label ?: budget.category,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = TrackittBodyFamily,
                            )
                            Text(
                                "GH₵${String.format(Locale.US, "%.0f", spent)} / ${String.format(Locale.US, "%.0f", budget.amount)}",
                                fontSize = 13.sp,
                                fontFamily = FrauncesFamily,
                                color = if (overBudget) Color(0xFFB3261E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                overBudget -> Color(0xFFB3261E)
                                progress > 0.8f -> Color(0xFFE6A817)
                                else -> cat?.fgColor ?: MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    IconButton(
                        onClick = { scope.launch { db.budgetDao().delete(budget.id) } },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            existingBudgets = budgets,
            onDismiss = { showAddDialog = false },
            onSave = { category, amount ->
                scope.launch {
                    db.budgetDao().upsert(BudgetEntity(category = category, amount = amount))
                }
                showAddDialog = false
            },
        )
    }

    if (showImportDialog) {
        ImportBudgetPlanDialog(
            parser = planParser,
            onDismiss = { showImportDialog = false },
            onImport = { groupedBudgets ->
                scope.launch {
                    groupedBudgets.forEach { (category, amount) ->
                        val existing = db.budgetDao().getByCategory(category.name)
                        db.budgetDao().upsert(
                            BudgetEntity(
                                id = existing?.id ?: 0,
                                category = category.name,
                                amount = amount,
                            )
                        )
                    }
                }
                showImportDialog = false
            },
        )
    }
}

@Composable
private fun EstimatedBudgetCard(
    estimate: BudgetEstimate,
    monthLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Estimated for $monthLabel",
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "GH₵${String.format(Locale.US, "%,.0f", estimate.amount)}",
                    fontFamily = FrauncesFamily,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                if (estimate.origin == BudgetEstimateOrigin.ON_DEVICE_AI) "Gemini Nano" else "Quick estimate",
                fontFamily = TrackittBodyFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            estimate.summary,
            fontFamily = TrackittBodyFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun QuickImportCard(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onImportClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Filled.UploadFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Quick import",
                fontFamily = TrackittBodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Paste a rough monthly list and we'll turn it into budgets automatically.",
                fontFamily = TrackittBodyFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Open",
            fontFamily = TrackittBodyFamily,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AddBudgetDialog(
    existingBudgets: List<BudgetEntity>,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    val existingCategoryNames = existingBudgets.map { it.category }.toSet()
    val availableCategories = TransactionCategory.assignable.filter { it.name !in existingCategoryNames && it != TransactionCategory.INCOME }

    BudgetDialogScaffold(
        title = "Set budget",
        onDismiss = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory ?: return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (amt > 0) onSave(cat.name, amt)
                },
                enabled = selectedCategory != null && (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = TrackittBodyFamily)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                availableCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) cat.bgColor else Color.Transparent)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(cat.icon, contentDescription = null, tint = if (isSelected) cat.fgColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(cat.label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = TrackittBodyFamily)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monthly limit (GH₵)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ImportBudgetPlanDialog(
    parser: BudgetPlanParser,
    onDismiss: () -> Unit,
    onImport: (List<Pair<TransactionCategory, Double>>) -> Unit,
) {
    var planText by remember { mutableStateOf("") }
    var categoryPickerIndex by remember { mutableStateOf<Int?>(null) }
    val parseResult = remember(planText) { parser.parse(planText) }
    val selectedCategories = remember(planText) {
        parseResult.parsedLines.map { it.category }.toMutableStateList()
    }
    val reviewedLines = parseResult.parsedLines.mapIndexed { index, line ->
        line.copy(category = selectedCategories.getOrElse(index) { line.category })
    }
    val groupedBudgets = groupBudgetLines(reviewedLines)

    BudgetDialogScaffold(
        title = "Paste budget plan",
        onDismiss = onDismiss,
        confirmButton = {
            Button(
                onClick = { onImport(groupedBudgets) },
                enabled = groupedBudgets.isNotEmpty(),
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Text(
                "Paste a rough monthly plan. Use one line per item and simple math when you need it. We'll total everything and map it into budget categories.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = TrackittBodyFamily,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = planText,
                onValueChange = { planText = it },
                label = { Text("Budget plan") },
                placeholder = {
                    Text(
                        "Rent ~ 1200\nTransport ~ 300 + 150\nOffering ~ 70 x 4",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                },
                minLines = 5,
                maxLines = 6,
                colors = TextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (reviewedLines.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Review categories",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TrackittBodyFamily,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap any row to move it to a different category.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TrackittBodyFamily,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .heightIn(min = 180.dp, max = 320.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        reviewedLines.forEachIndexed { index, line ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { categoryPickerIndex = index }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(line.category.bgColor, CircleShape),
                                ) {
                                    Icon(
                                        imageVector = line.category.icon,
                                        contentDescription = null,
                                        tint = line.category.fgColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        line.label,
                                        fontFamily = TrackittBodyFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "In ${line.category.label}",
                                        fontFamily = TrackittBodyFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "GH₵${String.format(Locale.US, "%.0f", line.amount)}",
                                        fontFamily = FrauncesFamily,
                                        fontSize = 15.sp,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Change",
                                        fontFamily = TrackittBodyFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (groupedBudgets.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Will create",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TrackittBodyFamily,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 140.dp, max = 220.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        groupedBudgets.forEach { (category, amount) ->
                            val groupedLabels = reviewedLines
                                .filter { it.category == category }
                                .joinToString(", ") { it.label }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(category.bgColor, CircleShape),
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = category.fgColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        category.label,
                                        fontFamily = TrackittBodyFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Includes: $groupedLabels",
                                        fontFamily = TrackittBodyFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "GH₵${String.format(Locale.US, "%.0f", amount)}",
                                    fontFamily = FrauncesFamily,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }
            }

            if (parseResult.invalidLines.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Skipped lines",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB3261E),
                    fontFamily = TrackittBodyFamily,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        parseResult.invalidLines.forEach { line ->
                            Text(
                                "• $line",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = TrackittBodyFamily,
                            )
                        }
                    }
                }
            }
        }
    }

    val selectedLineIndex = categoryPickerIndex
    if (selectedLineIndex != null) {
        CategoryPickerSheet(
            selectedCategory = selectedCategories.getOrElse(selectedLineIndex) {
                parseResult.parsedLines.getOrNull(selectedLineIndex)?.category ?: TransactionCategory.OTHER
            },
            onSelect = { category ->
                if (selectedLineIndex in selectedCategories.indices) {
                    selectedCategories[selectedLineIndex] = category
                }
                categoryPickerIndex = null
            },
            onDismiss = { categoryPickerIndex = null },
        )
    }
}

@Composable
private fun BudgetDialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = title,
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                ) {
                    content()
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    selectedCategory: TransactionCategory,
    onSelect: (TransactionCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        sheetMaxWidth = sheetMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isCompactHeight()),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            "Change category",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = TrackittBodyFamily,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CATEGORY_CELL_MIN_WIDTH),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .heightIn(max = 320.dp),
        ) {
            items(TransactionCategory.assignable.filter { it != TransactionCategory.INCOME }) { category ->
                val isSelected = category == selectedCategory
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(category) },
                ) {
                    CookieIcon(
                        fillColor = if (isSelected) category.bgColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeColor = if (isSelected) category.fgColor else null,
                        size = 48.dp,
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = category.label,
                            tint = if (isSelected) category.fgColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        category.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = TrackittBodyFamily,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
