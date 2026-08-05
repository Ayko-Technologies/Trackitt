package com.ayko.trackitt.ui.screen.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayko.trackitt.service.insights.InsightSource
import com.ayko.trackitt.service.insights.MonthlyInsightsSnapshot
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    year: Int,
    month: Int,
    viewModel: InsightsViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(year, month) {
        viewModel.setMonth(year, month)
    }

    val snapshot = uiState.snapshot
    val narrative = uiState.narrative

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Insights",
                    fontFamily = TrackittBodyFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = snapshot?.monthLabel ?: "",
                    fontFamily = TrackittBodyFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh insights", modifier = Modifier.size(20.dp))
            }
        }

        if (snapshot == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = narrative?.headline ?: "Loading insights",
                    fontFamily = FrauncesFamily,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            item {
                SourceBadge(
                    source = narrative?.source ?: InsightSource.RULES,
                    loading = uiState.isLoading,
                )
            }

            item {
                StatsCard(snapshot = snapshot)
            }

            if (narrative != null) {
                item {
                    InsightSectionCard(
                        title = "Where Money Went",
                        body = narrative.whereMoneyWent,
                    )
                }
                item {
                    InsightSectionCard(
                        title = "What To Cut",
                        body = narrative.cutBack,
                    )
                }
                item {
                    InsightSectionCard(
                        title = "How To Save",
                        body = narrative.saveMore,
                    )
                }
            }

            item {
                BreakdownCard(
                    title = "Top Categories",
                    rows = snapshot.topCategories.take(5).map {
                        Triple(it.category.label, money(it.amount), percent(it.shareOfSpend))
                    },
                )
            }

            item {
                BreakdownCard(
                    title = "Biggest Outflows",
                    rows = snapshot.topMerchants.take(6).map { merchant ->
                        Triple(
                            merchant.merchant,
                            money(merchant.amount),
                            percent(merchant.shareOfSpend),
                        )
                    },
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SourceBadge(
    source: InsightSource,
    loading: Boolean,
) {
    val label = when {
        loading -> "Refreshing insights…"
        source == InsightSource.ON_DEVICE_AI -> "Generated with on-device AI"
        source == InsightSource.CLOUD_AI -> "Generated with cloud AI"
        else -> "Structured from your data"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            fontFamily = TrackittBodyFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun StatsCard(snapshot: MonthlyInsightsSnapshot) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Spent",
                fontFamily = TrackittBodyFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DisplayNumber(
                value = snapshot.spend,
                sizeSp = 48f,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                StatPill("Fees", money(snapshot.fees), Modifier.weight(1f))
                StatPill("Income", money(snapshot.income), Modifier.weight(1f))
                StatPill("Net", signedMoney(snapshot.net), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                text = label,
                fontFamily = TrackittBodyFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                fontFamily = TrackittBodyFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InsightSectionCard(
    title: String,
    body: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontFamily = TrackittBodyFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                fontFamily = TrackittBodyFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    rows: List<Triple<String, String, String>>,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontFamily = TrackittBodyFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.first,
                            fontFamily = TrackittBodyFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = row.third,
                            fontFamily = TrackittBodyFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = row.second,
                        fontFamily = TrackittBodyFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun money(value: Double): String {
    return "GH₵${String.format(Locale.US, "%,.2f", value)}"
}

private fun signedMoney(value: Double): String {
    val prefix = if (value >= 0.0) "+" else "-"
    return "$prefix${money(kotlin.math.abs(value))}"
}

private fun percent(value: Double): String {
    val percentValue = value * 100
    if (percentValue <= 0.0) return "0%"
    if (percentValue < 1.0) return "<1%"
    if (percentValue < 10.0 && abs(percentValue - percentValue.roundToInt()) > 0.05) {
        return String.format(Locale.US, "%.1f%%", percentValue)
    }
    return "${percentValue.roundToInt()}%"
}
