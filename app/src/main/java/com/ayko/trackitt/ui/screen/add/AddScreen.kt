package com.ayko.trackitt.ui.screen.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.CalculatorPad
import com.ayko.trackitt.ui.component.CookieIcon
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.SurfaceContainerHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel = viewModel(),
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()

    var detailsExpanded by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(saved) {
        if (saved) onSave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "New expense",
                    fontFamily = SatoshiFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
            ),
        )

        // Amount display — tap to show calculator
        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    showCalculator = true
                    focusManager.clearFocus()
                },
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    .padding(bottom = 8.dp),
            ) {
                val hasOp = amount.contains("+") || (amount.lastIndexOf("−") > 0)
                if (hasOp) {
                    Text(
                        text = "GH₵ $amount",
                        fontSize = 32.sp,
                        fontFamily = com.ayko.trackitt.ui.theme.FrauncesFamily,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp,
                    )
                } else {
                    DisplayNumber(
                        value = amount.toDoubleOrNull() ?: 0.0,
                        sizeSp = 52f,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (amount.isEmpty()) {
                    Spacer(
                        Modifier
                            .width(2.dp)
                            .height(36.dp)
                            .drawBehind { drawRect(primaryColor) },
                    )
                }
            }
        }

        // Category grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .height(180.dp),
        ) {
            itemsIndexed(TransactionCategory.entries.toList()) { _, cat ->
                val isSelected = selectedCategory == cat
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        viewModel.selectedCategory.value = if (isSelected) null else cat
                        showCalculator = false
                        focusManager.clearFocus()
                    },
                ) {
                    CookieIcon(
                        fillColor = if (isSelected) cat.bgColor else SurfaceContainerHigh,
                        strokeColor = if (isSelected) cat.fgColor else null,
                        strokeWidth = 2.dp,
                        size = 56.dp,
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.label,
                            tint = if (isSelected) cat.fgColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = cat.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        // Add details
        if (!detailsExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable { detailsExpanded = true; showCalculator = false },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add details", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontFamily = SatoshiFamily)
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.note.value = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) showCalculator = false },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = source,
                    onValueChange = { viewModel.source.value = it },
                    label = { Text("Source") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) showCalculator = false },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (showCalculator) CalculatorPad(
            expression = amount,
            onDigit = { digit ->
                val current = amount
                val lastPart = current.substringAfterLast("+").substringAfterLast("−")
                if (lastPart == "0" && digit == "0") return@CalculatorPad
                if (lastPart == "0" && digit != "00") {
                    viewModel.amount.value = current.dropLast(1) + digit
                } else if (lastPart.contains(".") && lastPart.substringAfter(".").length >= 2) {
                    return@CalculatorPad
                } else {
                    viewModel.amount.value = current + digit
                }
            },
            onDecimal = {
                val lastPart = amount.substringAfterLast("+").substringAfterLast("−")
                if (!lastPart.contains(".")) {
                    viewModel.amount.value = if (amount.isEmpty() || amount.endsWith("+") || amount.endsWith("−")) "${amount}0." else "$amount."
                }
            },
            onBackspace = {
                if (amount.isNotEmpty()) {
                    viewModel.amount.value = amount.dropLast(1)
                }
            },
            onOperator = { op ->
                if (amount.isNotEmpty() && !amount.endsWith("+") && !amount.endsWith("−")) {
                    viewModel.amount.value = amount + op
                }
            },
            onEquals = {
                val result = evaluateExpression(amount)
                if (result != null && result >= 0) {
                    viewModel.amount.value = if (result == result.toLong().toDouble()) {
                        result.toLong().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.2f", result)
                    }
                }
            },
            onSave = { viewModel.save() },
            saveEnabled = (evaluateExpression(amount) ?: 0.0) > 0 && selectedCategory != null,
        )
    }
}

private fun evaluateExpression(expr: String): Double? {
    if (expr.isEmpty()) return null
    val clean = expr.trimEnd('+', '−')
    if (clean.isEmpty()) return null

    var result = 0.0
    var current = ""
    var op = '+'

    for (ch in "$clean+") {
        if (ch == '+' || ch == '−') {
            val num = current.toDoubleOrNull() ?: return null
            result = if (op == '+') result + num else result - num
            current = ""
            op = ch
        } else {
            current += ch
        }
    }
    return result
}
