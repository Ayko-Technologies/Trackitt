package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.ui.theme.SatoshiFamily

@Composable
fun CalculatorPad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onOperator: (String) -> Unit,
    onEquals: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    expression: String,
    modifier: Modifier = Modifier,
) {
    val hasOperator = expression.contains("+") || expression.lastIndexOf("−") > 0 ||
            expression.contains("×") || expression.contains("÷")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Row 1: 1 2 3 +
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey("1", Modifier.weight(1f)) { onDigit("1") }
            CalcKey("2", Modifier.weight(1f)) { onDigit("2") }
            CalcKey("3", Modifier.weight(1f)) { onDigit("3") }
            CalcKey("+", Modifier.weight(1f), accent = true) { onOperator("+") }
        }
        // Row 2: 4 5 6 −
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey("4", Modifier.weight(1f)) { onDigit("4") }
            CalcKey("5", Modifier.weight(1f)) { onDigit("5") }
            CalcKey("6", Modifier.weight(1f)) { onDigit("6") }
            CalcKey("−", Modifier.weight(1f), accent = true) { onOperator("−") }
        }
        // Row 3: 7 8 9 ×
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey("7", Modifier.weight(1f)) { onDigit("7") }
            CalcKey("8", Modifier.weight(1f)) { onDigit("8") }
            CalcKey("9", Modifier.weight(1f)) { onDigit("9") }
            CalcKey("×", Modifier.weight(1f), accent = true) { onOperator("×") }
        }
        // Row 4: . 0 00 ÷
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey(".", Modifier.weight(1f)) { onDecimal() }
            CalcKey("0", Modifier.weight(1f)) { onDigit("0") }
            CalcKey("00", Modifier.weight(1f)) { onDigit("00") }
            CalcKey("÷", Modifier.weight(1f), accent = true) { onOperator("÷") }
        }
        // Row 5: ⌫ and = (or just ⌫)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onBackspace() },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (hasOperator) {
                CalcKey("=", Modifier.weight(1f), accent = true) { onEquals() }
            } else {
                Box(modifier = Modifier.weight(1f))
            }
        }
        // Save button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (saveEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
                .then(if (saveEnabled) Modifier.clickable { onSave() } else Modifier),
        ) {
            Text(
                "Save expense",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SatoshiFamily,
                color = if (saveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (accent) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable { onClick() },
    ) {
        Text(
            label,
            fontSize = if (accent) 20.sp else 22.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = SatoshiFamily,
            color = if (accent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}
