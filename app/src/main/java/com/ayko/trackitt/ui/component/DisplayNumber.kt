package com.ayko.trackitt.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun DisplayNumber(
    value: Double,
    sizeSp: Float = 72f,
    showSign: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val negative = value < 0
    val absValue = abs(value)
    val parts = String.format(Locale.US, "%.2f", absValue).split(".")
    val wholeFmt = NumberFormat.getNumberInstance(Locale.US).format(parts[0].toLong())
    val frac = parts[1]

    val prefixText = buildString {
        if (showSign) append(if (negative) "−" else "+")
        append("GH₵")
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Text(
            text = prefixText,
            style = TextStyle(
                fontFamily = SatoshiFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (sizeSp * 0.3f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .alignByBaseline()
                .padding(top = (sizeSp * 0.18f).dp),
        )
        Text(
            text = wholeFmt,
            style = TextStyle(
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Normal,
                fontSize = sizeSp.sp,
                letterSpacing = (sizeSp * -0.03f).sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .alignByBaseline()
                .padding(start = 4.dp),
        )
        Text(
            text = ".$frac",
            style = TextStyle(
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (sizeSp * 0.42f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .alignByBaseline()
                .padding(start = 2.dp),
        )
    }
}
