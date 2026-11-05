package com.ayko.trackitt.ui.component

import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
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
    includeFees: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isCompactHeight()),
    listState: LazyListState = rememberLazyListState(),
) {
    val total = transactions.sumOf { it.amount + if (includeFees) it.fee else 0.0 }
    val count = transactions.size

    ModalBottomSheet(
        sheetMaxWidth = sheetMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Opened full height in landscape, the list would otherwise run under the status bar and
        // the camera cutout on the side.
        contentWindowInsets = { WindowInsets.safeDrawing },
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
                    fontFamily = TrackittBodyFamily,
                )
                Text(
                    "$count transaction${if (count != 1) "s" else ""} · GH₵ ${String.format(Locale.US, "%.2f", total)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = TrackittBodyFamily,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Transaction list
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            items(transactions, key = { it.id }) { tx ->
                val dateFormatted = SimpleDateFormat("MMM d · h:mm a", Locale.US).format(Date(tx.date))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalePressEffect()
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
                            fontFamily = TrackittBodyFamily,
                        )
                        Text(
                            dateFormatted,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = TrackittBodyFamily,
                        )
                    }
                    val displayAmount = tx.amount + if (includeFees) tx.fee else 0.0
                    Text(
                        "${if (tx.isIncome) "+" else "−"}₵${String.format(Locale.US, "%.2f", displayAmount)}",
                        fontSize = 15.sp,
                        fontFamily = FrauncesFamily,
                    )
                }
            }
        }
    }
}
