package com.ayko.trackitt.ui.screen.budget

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.entity.BudgetEntity
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily
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
    val application = context.applicationContext as Application

    val budgets by db.budgetDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())

    val now = LocalDate.now()
    val year = now.year.toString()
    val month = now.monthValue.toString().padStart(2, '0')
    val categoryTotals by db.transactionDao().getCategoryTotals(year, month)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val totalBudget = budgets.sumOf { it.amount }
    val totalSpent = categoryTotals.sumOf { it.total }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Budgets", fontFamily = SatoshiFamily, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            FloatingActionButton(
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
            // Overview card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Spent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                            Text(
                                "GH₵${String.format(Locale.US, "%.0f", totalSpent)}",
                                fontSize = 24.sp, fontFamily = FrauncesFamily,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Budget", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                            Text(
                                "GH₵${String.format(Locale.US, "%.0f", totalBudget)}",
                                fontSize = 24.sp, fontFamily = FrauncesFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val progress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
                    val animatedProgress by animateFloatAsState(progress, label = "totalProgress")
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress > 0.9f) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% of total budget",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                    ) {
                        Text("No budgets yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = SatoshiFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap + to set a spending limit for a category", fontSize = 13.sp, fontFamily = SatoshiFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(budgets, key = { it.id }) { budget ->
                val cat = try { TransactionCategory.valueOf(budget.category) } catch (_: Exception) { null }
                val spent = categoryTotals.find { it.category == budget.category }?.total ?: 0.0
                val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                val animatedProgress by animateFloatAsState(progress, label = "catProgress")
                val overBudget = spent > budget.amount

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
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
                                fontFamily = SatoshiFamily,
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
    val availableCategories = TransactionCategory.entries.filter { it.name !in existingCategoryNames && it != TransactionCategory.INCOME }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set budget", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
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
                            Text(cat.label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = SatoshiFamily)
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
        },
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
    )
}
