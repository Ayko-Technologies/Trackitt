package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBottomSheet(
    category: TransactionCategory,
    transactions: List<Transaction>,
    onTransactionTap: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val total = transactions.sumOf { it.amount }
    val count = transactions.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(category.bgColor, CircleShape),
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = category.fgColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    category.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SatoshiFamily,
                )
                Text(
                    "$count transaction${if (count != 1) "s" else ""} · GH₵ ${String.format(Locale.US, "%.2f", total)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SatoshiFamily,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Transaction list
        LazyColumn(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            items(transactions, key = { it.id }) { tx ->
                val dateFormatted = SimpleDateFormat("MMM d · h:mm a", Locale.US).format(Date(tx.date))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTransactionTap(tx.id) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(category.bgColor, CircleShape),
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = null,
                            tint = category.fgColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.merchant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SatoshiFamily,
                        )
                        Text(
                            dateFormatted,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SatoshiFamily,
                        )
                    }
                    Text(
                        "${if (tx.isIncome) "+" else "−"}₵${String.format(Locale.US, "%.2f", tx.amount)}",
                        fontSize = 15.sp,
                        fontFamily = FrauncesFamily,
                    )
                }
            }
        }
    }
}
