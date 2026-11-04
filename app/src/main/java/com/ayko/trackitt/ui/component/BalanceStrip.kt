package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.service.accounts.AccountBalance
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Latest balance per wallet or bank, as their own SMS last reported it. */
@Composable
fun BalanceStrip(
    balances: List<AccountBalance>,
    recentGapsSince: Long,
    modifier: Modifier = Modifier,
) {
    if (balances.isEmpty()) return
    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    BoxWithConstraints(modifier = modifier) {
    // On a wide screen the cards share the full width instead of bunching up on the left.
    val fits = balances.size * MIN_CARD_WIDTH.value + (balances.size - 1) * 8 + 40 <= maxWidth.value
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fits) Modifier else Modifier.horizontalScroll(rememberScrollState()))
            .padding(horizontal = 20.dp),
    ) {
        balances.forEach { account ->
            val recentGaps = account.gaps.count { it.before >= recentGapsSince }
            Column(
                modifier = Modifier
                    .then(if (fits) Modifier.weight(1f) else Modifier)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    account.source,
                    fontFamily = TrackittBodyFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    String.format(Locale.US, "₵%,.2f", account.balance),
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Text(
                    if (recentGaps > 0) {
                        "$recentGaps possible missed SMS"
                    } else {
                        "as of ${dateFormat.format(Date(account.asOf))}"
                    },
                    fontFamily = TrackittBodyFamily,
                    fontSize = 11.sp,
                    color = if (recentGaps > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    }
}

/** Wide enough for a label like "2 possible missed SMS". */
private val MIN_CARD_WIDTH = 140.dp
