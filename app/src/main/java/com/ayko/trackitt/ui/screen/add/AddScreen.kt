package com.ayko.trackitt.ui.screen.add

import com.ayko.trackitt.ui.component.CATEGORY_CELL_MIN_WIDTH
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import com.ayko.trackitt.ui.component.isCompactHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
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
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.TrackittBodyFamily

private enum class AddStep { AMOUNT, CATEGORY, CONFIRM }

private val expenseCategories = TransactionCategory.entries.filter { it != TransactionCategory.INCOME }

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

    var step by remember { mutableStateOf(AddStep.AMOUNT) }

    LaunchedEffect(saved) { if (saved) onSave() }

    BackHandler(enabled = step != AddStep.AMOUNT) {
        step = when (step) {
            AddStep.CONFIRM -> AddStep.CATEGORY
            AddStep.CATEGORY -> AddStep.AMOUNT
            else -> return@BackHandler
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "addStep",
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            AddStep.AMOUNT -> "New expense"
                            AddStep.CATEGORY -> "What's it for?"
                            AddStep.CONFIRM -> ""
                        },
                        fontFamily = TrackittBodyFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp,
                    )
                },
                navigationIcon = {
                    when (currentStep) {
                        AddStep.AMOUNT -> IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                        else -> IconButton(onClick = {
                            step = if (currentStep == AddStep.CONFIRM) AddStep.CATEGORY else AddStep.AMOUNT
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            when (currentStep) {
                AddStep.AMOUNT -> AmountContent(amount, viewModel) {
                    val result = evaluateExpression(amount)
                    if (result != null && result > 0) {
                        viewModel.amount.value = formatAmount(result)
                        step = AddStep.CATEGORY
                    }
                }

                AddStep.CATEGORY -> CategoryContent(amount) { cat ->
                    viewModel.selectedCategory.value = cat
                    step = AddStep.CONFIRM
                }

                AddStep.CONFIRM -> ConfirmContent(
                    amount = amount,
                    category = selectedCategory,
                    note = note,
                    onNoteChange = { viewModel.note.value = it },
                    onSave = { viewModel.save() },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.AmountContent(
    amount: String,
    viewModel: AddViewModel,
    onNext: () -> Unit,
) {
    if (isCompactHeight()) {
        // Landscape: the keypad would squeeze the amount out of view, so they sit side by side.
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmountDisplay(amount, Modifier.weight(1f).fillMaxHeight())
            AmountPad(amount, viewModel, onNext, Modifier.weight(1.4f))
        }
    } else {
        AmountDisplay(amount, Modifier.weight(1f).fillMaxWidth())
        AmountPad(amount, viewModel, onNext)
    }
}

@Composable
private fun AmountDisplay(amount: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val hasOp = amount.contains("+") || amount.lastIndexOf("−") > 0 ||
            amount.contains("×") || amount.contains("÷")

        if (hasOp) {
            Text(
                text = "GH₵ $amount",
                fontSize = 32.sp,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.5).sp,
            )
        } else if (amount.isEmpty()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "GH₵",
                    fontSize = 18.sp,
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "0",
                    fontSize = 64.sp,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(start = 4.dp),
                )
            }
        } else {
            DisplayNumber(
                value = amount.toDoubleOrNull() ?: 0.0,
                sizeSp = 52f,
            )
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 2.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun AmountPad(
    amount: String,
    viewModel: AddViewModel,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CalculatorPad(
        expression = amount,
        buttonText = "Next",
        onDigit = { digit ->
            val current = amount
            val lastPart = current.substringAfterLast("+").substringAfterLast("−")
                .substringAfterLast("×").substringAfterLast("÷")
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
                .substringAfterLast("×").substringAfterLast("÷")
            if (!lastPart.contains(".")) {
                viewModel.amount.value =
                    if (amount.isEmpty() || amount.last() in "+−×÷") "${amount}0." else "$amount."
            }
        },
        onBackspace = {
            if (amount.isNotEmpty()) viewModel.amount.value = amount.dropLast(1)
        },
        onOperator = { op ->
            if (amount.isNotEmpty() && !amount.endsWith("+") && !amount.endsWith("−") &&
                !amount.endsWith("×") && !amount.endsWith("÷")
            ) {
                viewModel.amount.value = amount + op
            }
        },
        onEquals = {
            val result = evaluateExpression(amount)
            if (result != null && result >= 0) {
                viewModel.amount.value = formatAmount(result)
            }
        },
        onSave = onNext,
        saveEnabled = (evaluateExpression(amount) ?: 0.0) > 0,
        modifier = modifier,
    )
}

@Composable
private fun CategoryContent(
    amount: String,
    onCategorySelected: (TransactionCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                "GH₵",
                fontSize = 13.sp,
                fontFamily = TrackittBodyFamily,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                formatDisplay(amount),
                fontSize = 22.sp,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 3.dp),
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    LazyVerticalGrid(
        columns = GridCells.Adaptive(CATEGORY_CELL_MIN_WIDTH),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        items(expenseCategories) { cat ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCategorySelected(cat) }
                    .padding(vertical = 4.dp),
            ) {
                CookieIcon(
                    fillColor = cat.bgColor,
                    size = 56.dp,
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = cat.label,
                        tint = cat.fgColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = cat.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TrackittBodyFamily,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ConfirmContent(
    amount: String,
    category: TransactionCategory?,
    note: String,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        if (category != null) {
            CookieIcon(
                fillColor = category.bgColor,
                size = 72.dp,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.fgColor,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                category.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TrackittBodyFamily,
            )
        }

        Spacer(Modifier.height(8.dp))
        DisplayNumber(
            value = amount.toDoubleOrNull() ?: 0.0,
            sizeSp = 40f,
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("What was this for? (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onSave() },
    ) {
        Text(
            "Save expense",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = TrackittBodyFamily,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

private fun formatDisplay(amount: String): String {
    val value = amount.toDoubleOrNull() ?: return amount
    return if (value == value.toLong().toDouble()) {
        val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
            .format(value.toLong())
        "$formatted.00"
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

private fun evaluateExpression(expr: String): Double? {
    if (expr.isEmpty()) return null
    val clean = expr.trimEnd('+', '−', '×', '÷')
    if (clean.isEmpty()) return null

    val tokens = mutableListOf<Any>()
    var current = ""

    for (ch in clean) {
        if (ch == '+' || ch == '−' || ch == '×' || ch == '÷') {
            val num = current.toDoubleOrNull() ?: return null
            tokens.add(num)
            tokens.add(ch)
            current = ""
        } else {
            current += ch
        }
    }
    val lastNum = current.toDoubleOrNull() ?: return null
    tokens.add(lastNum)

    val simplified = mutableListOf<Any>()
    var i = 0
    while (i < tokens.size) {
        if (i + 2 < tokens.size && (tokens[i + 1] == '×' || tokens[i + 1] == '÷')) {
            var acc = tokens[i] as Double
            while (i + 2 < tokens.size && (tokens[i + 1] == '×' || tokens[i + 1] == '÷')) {
                val op = tokens[i + 1] as Char
                val right = tokens[i + 2] as Double
                acc = if (op == '×') acc * right else if (right != 0.0) acc / right else return null
                i += 2
            }
            simplified.add(acc)
        } else {
            simplified.add(tokens[i])
        }
        i++
    }

    var result = simplified[0] as Double
    var j = 1
    while (j < simplified.size) {
        val op = simplified[j] as Char
        val right = simplified[j + 1] as Double
        result = if (op == '+') result + right else result - right
        j += 2
    }
    return result
}
