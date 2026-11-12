package com.ayko.trackitt.ui.screen.recap

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ayko.trackitt.service.recap.YearRecap
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.TextStyle
import java.util.Locale

/** The story's last page: the year on one card, to keep or share, and the way out. */
@Composable
internal fun RecapSummarySlide(recap: YearRecap, onReplay: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Amounts stay private unless the user chooses to share them.
    var showAmounts by rememberSaveable { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val card = rememberGraphicsLayer()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .drawWithContent {
                    card.record { this@drawWithContent.drawContent() }
                    drawLayer(card)
                },
        ) {
            ShareCard(recap, showAmounts)
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Text("Show amounts", color = RecapPalette.Ink, fontFamily = TrackittBodyFamily, fontSize = 15.sp)
            HGap(12)
            Switch(
                checked = showAmounts,
                onCheckedChange = { showAmounts = it },
                colors = SwitchDefaults.colors(checkedTrackColor = RecapPalette.Ink),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = onReplay) {
                Icon(Icons.Filled.Replay, contentDescription = null, tint = RecapPalette.Ink, modifier = Modifier.size(18.dp))
                HGap(6)
                Text("Replay", color = RecapPalette.Ink)
            }
            HGap(8)
            Button(
                enabled = !sharing,
                onClick = {
                    sharing = true
                    scope.launch {
                        try {
                            val bitmap = card.toImageBitmap().asAndroidBitmap()
                            shareImage(context, bitmap, recap.year)
                        } finally {
                            sharing = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RecapPalette.Ink, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                HGap(8)
                Text("Share")
            }
            HGap(8)
            TextButton(onClick = onClose) { Text("Done", color = RecapPalette.Ink) }
        }
    }
}

/**
 * The card people see when it's shared: the year, the personality and where the money went, in
 * shares rather than cedis unless [showAmounts]. It never names the people the user paid.
 */
@Composable
private fun ShareCard(recap: YearRecap, showAmounts: Boolean) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(RecapPalette.Ink),
    ) {
        // Laid out for a 320dp-wide card and scaled to whatever room there is. The card is a picture,
        // so its text scales with the card, not the phone's font size: the image comes out the same
        // everywhere and nothing falls off the bottom.
        val unit = maxWidth.value / 320f
        val density = LocalDensity.current
        fun d(v: Number): Dp = (v.toFloat() * unit).dp
        // Undoes the font size setting, including Android's gentler scaling of large text.
        fun s(v: Number): TextUnit = with(density) { d(v).toSp() }
        val accent = recap.topCategory?.category?.bgColor ?: RecapPalette.Mint

        Column(Modifier.fillMaxSize().padding(d(24))) {
            Text(
                "MY YEAR IN MONEY",
                color = RecapPalette.Mint,
                fontFamily = TrackittBodyFamily,
                fontWeight = FontWeight.Bold,
                fontSize = s(11),
                letterSpacing = s(2),
            )
            Text(
                recap.year.toString(),
                color = RecapPalette.Mint,
                fontFamily = FrauncesFamily,
                fontSize = s(72),
                lineHeight = s(76),
                letterSpacing = s(-3),
            )
            // Whoever sees the card should know when it covers only part of the year.
            if (recap.ongoing || !recap.coversWholeYear) {
                val range = "${RecapFormat.dayMonth(recap.from)} – ${RecapFormat.dayMonth(recap.to)}".uppercase()
                Text(
                    if (recap.ongoing) "$range · SO FAR" else range,
                    color = RecapPalette.Mint.copy(alpha = 0.8f),
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = s(11),
                    letterSpacing = s(1.6f),
                )
            }
            Box(Modifier.height(d(8)))
            Text("I'm", color = Color.White.copy(alpha = 0.7f), fontFamily = TrackittBodyFamily, fontSize = s(15))
            FitText(recap.personality.title, s(32), accent, minSize = s(20), maxLines = 2)
            Text(
                recap.personality.line,
                color = Color.White.copy(alpha = 0.8f),
                fontFamily = TrackittBodyFamily,
                fontSize = s(13),
                lineHeight = s(18),
                modifier = Modifier.padding(top = d(4)),
            )

            Box(Modifier.weight(1f))

            if (showAmounts) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = d(14))) {
                    Text(
                        RecapFormat.money(recap.spent),
                        color = Color.White,
                        fontFamily = FrauncesFamily,
                        fontSize = s(26),
                        maxLines = 1,
                    )
                    Text(
                        " spent",
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = TrackittBodyFamily,
                        fontSize = s(14),
                        modifier = Modifier.padding(bottom = d(4)),
                    )
                }
            }
            Text(
                "WHERE IT WENT",
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = TrackittBodyFamily,
                fontWeight = FontWeight.Bold,
                fontSize = s(10),
                letterSpacing = s(1.6f),
            )
            Box(Modifier.height(d(10)))
            recap.categories.take(3).forEach { share ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = d(8))) {
                    Box(Modifier.size(d(10)).background(share.category.bgColor, CircleShape))
                    Box(Modifier.size(d(10)))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(
                                share.category.label,
                                color = Color.White,
                                fontFamily = TrackittBodyFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = s(14),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (showAmounts) RecapFormat.money(share.total) else RecapFormat.percent(share.share),
                                color = Color.White.copy(alpha = 0.8f),
                                fontFamily = TrackittBodyFamily,
                                fontSize = s(13),
                            )
                        }
                        Box(Modifier.height(d(5)))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(d(5))
                                .clip(RoundedCornerShape(d(3)))
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(share.share.toFloat().coerceIn(0.03f, 1f))
                                    .background(share.category.bgColor),
                            )
                        }
                    }
                }
            }

            Box(Modifier.height(d(10)))
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(8)),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                Stat("Payments", RecapFormat.count(recap.payments), ::d, ::s, Modifier.weight(1f))
                Stat("No-spend days", RecapFormat.count(recap.noSpendDays), ::d, ::s, Modifier.weight(1f))
                Stat(
                    "Busiest day",
                    recap.busiestWeekday?.getDisplayName(TextStyle.SHORT, Locale.US) ?: "—",
                    ::d, ::s, Modifier.weight(1f),
                )
            }
            Box(Modifier.height(d(16)))
            Text(
                "Tracked with Trackitt",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = TrackittBodyFamily,
                fontSize = s(11),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, d: (Number) -> Dp, s: (Number) -> TextUnit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(d(12)))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = d(10), vertical = d(10)),
    ) {
        Text(
            value,
            color = Color.White,
            fontFamily = FrauncesFamily,
            fontSize = s(20),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, color = Color.White.copy(alpha = 0.6f), fontFamily = TrackittBodyFamily, fontSize = s(10), lineHeight = s(12), maxLines = 2)
    }
}

/** Saves the card to the app's cache and offers it to whatever app the user picks. */
private suspend fun shareImage(context: Context, bitmap: Bitmap, year: Int) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "trackitt-$year.png").also { file ->
            val software = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            file.outputStream().use { software.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share your $year"))
}
