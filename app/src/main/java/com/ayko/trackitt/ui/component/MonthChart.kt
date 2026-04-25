package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.OnSurface
import com.ayko.trackitt.ui.theme.Primary
import com.ayko.trackitt.ui.theme.Secondary
import com.ayko.trackitt.ui.theme.Surface
import com.ayko.trackitt.ui.theme.SurfaceContainerHigh
import androidx.compose.ui.graphics.compositeOver
import java.util.Locale

data class ChartCategory(
    val category: TransactionCategory,
    val total: Double,
)

@Composable
fun MonthChart(
    categories: List<ChartCategory>,
    totalFormatted: String,
    monthLabel: String = "APRIL",
    empty: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val leadCategory = categories.firstOrNull()
    val leadBg = leadCategory?.category?.bgColor ?: SurfaceContainerHigh
    val leadFg = leadCategory?.category?.fgColor ?: Primary
    val cardBg = if (empty) SurfaceContainerHigh else leadBg.copy(alpha = 0.3f).compositeOver(Surface)
    val blobFg = if (empty) Secondary else leadBg.copy(alpha = 0.6f).compositeOver(Surface)

    val maxVal = remember(categories) { categories.maxOfOrNull { it.total } ?: 1.0 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cardBg),
        ) {
            ChartHeader(
                cardBg = cardBg,
                empty = empty,
                totalFormatted = totalFormatted,
                monthLabel = monthLabel,
            )

            Box(modifier = Modifier.weight(1f)) {
                if (!empty) Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 0.dp, bottom = 40.dp)
                        .size(240.dp),
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(0.20f * w, 0.47f * h)
                            cubicTo(0.13f * w, 0.30f * h, 0.30f * w, 0.10f * h, 0.53f * w, 0.13f * h)
                            cubicTo(0.77f * w, 0.17f * h, 0.93f * w, 0.37f * h, 0.90f * w, 0.60f * h)
                            cubicTo(0.87f * w, 0.83f * h, 0.60f * w, 0.93f * h, 0.40f * w, 0.87f * h)
                            cubicTo(0.20f * w, 0.80f * h, 0.27f * w, 0.63f * h, 0.20f * w, 0.47f * h)
                            close()
                        }
                        drawPath(path, blobFg, style = Fill)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (empty) {
                        val ghostHeights = listOf(0.82f, 0.60f, 0.46f, 0.26f)
                        ghostHeights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(h)
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                    .background(Secondary),
                            )
                        }
                    } else {
                        categories.forEach { cat ->
                            val fraction = (cat.total / maxVal).toFloat().coerceIn(0f, 1f)
                            val isLead = cat == categories.first()
                            val barBg = if (isLead) cat.category.fgColor else cat.category.bgColor
                            val labelColor = if (isLead) Color.White.copy(alpha = 0.7f) else cat.category.fgColor
                            val amountColor = if (isLead) Color.White else OnSurface

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                        .background(barBg)
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Text(
                                        text = cat.category.label.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = labelColor,
                                        fontFamily = SatoshiFamily,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f", cat.total),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-0.4).sp,
                                        color = amountColor,
                                        fontFamily = FrauncesFamily,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                if (empty) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                    ) {
                        Text(
                            text = "Your transactions will show up here",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = (-0.1).sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(
    cardBg: Color,
    empty: Boolean,
    totalFormatted: String,
    monthLabel: String = "APRIL",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(Primary, RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2, 0f)
                    lineTo(size.width, size.height * 0.4f)
                    lineTo(size.width * 0.76f, size.height * 0.88f)
                    lineTo(size.width * 0.24f, size.height * 0.88f)
                    lineTo(0f, size.height * 0.4f)
                    close()
                }
                drawPath(path, cardBg, style = Fill)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "TRACKITT · ${monthLabel.uppercase()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = Primary,
        )
        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Primary, CircleShape)
                .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        ) {
            Text(
                text = if (empty) "READY" else "LIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = cardBg,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (empty) "0.00" else totalFormatted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = OnSurface,
                modifier = Modifier
                    .background(cardBg, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
