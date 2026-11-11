package com.ayko.trackitt.ui.screen.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.accounts.PayeeNumber
import com.ayko.trackitt.service.recap.Personality
import com.ayko.trackitt.service.recap.YearRecap
import com.ayko.trackitt.ui.theme.CatBillsBg
import com.ayko.trackitt.ui.theme.CatBillsFg
import com.ayko.trackitt.ui.theme.CatChurchBg
import com.ayko.trackitt.ui.theme.CatChurchFg
import com.ayko.trackitt.ui.theme.CatFeesBg
import com.ayko.trackitt.ui.theme.CatFeesFg
import com.ayko.trackitt.ui.theme.CatFoodBg
import com.ayko.trackitt.ui.theme.CatFoodFg
import com.ayko.trackitt.ui.theme.CatGroceriesBg
import com.ayko.trackitt.ui.theme.CatGroceriesFg
import com.ayko.trackitt.ui.theme.CatIncomeBg
import com.ayko.trackitt.ui.theme.CatIncomeFg
import com.ayko.trackitt.ui.theme.CatTransportBg
import com.ayko.trackitt.ui.theme.CatTransportFg
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.Primary
import com.ayko.trackitt.ui.theme.PrimaryContainer
import com.ayko.trackitt.ui.theme.Surface
import com.ayko.trackitt.ui.theme.TrackittBodyFamily
import kotlinx.coroutines.delay
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

internal object RecapPalette {
    val Ink = Color(0xFF11110F)
    val Mint = PrimaryContainer
    val Cream = Surface
}

/** One page of the story: its background, whether that background is light, and what it says. */
internal class RecapSlide(
    val background: Color,
    val light: Boolean = false,
    val content: @Composable () -> Unit,
)

/** The story for [recap]: only the slides it has something to say on, ending with the share card. */
internal fun recapSlides(recap: YearRecap, onReplay: () -> Unit, onClose: () -> Unit): List<RecapSlide> = buildList {
    add(RecapSlide(RecapPalette.Ink) { IntroSlide(recap) })
    add(RecapSlide(Primary) { SpentSlide(recap) })
    if (recap.received > 0) add(RecapSlide(CatIncomeFg) { MoneyInSlide(recap) })
    recap.topCategory?.let { top ->
        add(RecapSlide(top.category.fgColor) { TopCategorySlide(recap) })
    }
    if (recap.categories.size >= 2) add(RecapSlide(RecapPalette.Ink) { CategoriesSlide(recap) })
    if (recap.topPayees.size >= 3) add(RecapSlide(CatBillsFg) { TopPayeesSlide(recap) })
    if (recap.regular != null) add(RecapSlide(CatTransportFg) { RegularSlide(recap) })
    if (recap.biggest != null) add(RecapSlide(CatFoodFg) { BiggestSlide(recap) })
    if (recap.months.count { it.complete } >= 2) add(RecapSlide(RecapPalette.Ink) { MonthsSlide(recap) })
    if (recap.busiestWeekday != null) add(RecapSlide(CatChurchFg) { RhythmSlide(recap) })
    if (recap.noSpendDays > 0 && recap.trackedDays >= 14) add(RecapSlide(CatGroceriesFg) { NoSpendSlide(recap) })
    if (recap.fees > 0) add(RecapSlide(CatFeesFg) { FeesSlide(recap) })
    add(RecapSlide(recap.topCategory?.category?.fgColor ?: RecapPalette.Ink) { PersonalitySlide(recap) })
    add(RecapSlide(RecapPalette.Cream, light = true) { RecapSummarySlide(recap, onReplay, onClose) })
}

// ── Slides ──

@Composable
private fun IntroSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("Your year in money", RecapPalette.Mint) }
    Gap(12)
    Rise(1) {
        Text(
            recap.year.toString(),
            fontFamily = FrauncesFamily,
            fontSize = 112.sp,
            lineHeight = 112.sp,
            letterSpacing = (-4).sp,
            color = RecapPalette.Mint,
        )
    }
    Gap(16)
    Rise(2) {
        Headline(
            if (recap.coversWholeYear) "Every cedi, every payment. Here's how your year went."
            else "From ${RecapFormat.dayMonth(recap.from)} to ${RecapFormat.dayMonth(recap.to)}, here's how your year went.",
        )
    }
    Gap(40)
    Rise(3) { Body("Tap to move on. Hold to pause.", Color.White.copy(alpha = 0.6f)) }
}

@Composable
private fun SpentSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("You spent", RecapPalette.Mint) }
    Gap(8)
    Rise(1) { BigMoney(recap.spent, Color.White) }
    Gap(20)
    Rise(2) {
        Headline("across ${RecapFormat.count(recap.payments)} payments to ${RecapFormat.count(recap.payees)} people and places.")
    }
    Gap(20)
    Rise(3) { Body("That's about ${RecapFormat.money(recap.perDay)} a day.") }
    recap.perDayChange?.let { change ->
        Gap(8)
        Rise(4) {
            val more = change >= 0
            Body(
                "${RecapFormat.percent(abs(change))} ${if (more) "more" else "less"} a day than in ${recap.year - 1}.",
                RecapPalette.Mint,
            )
        }
    }
}

@Composable
private fun MoneyInSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("Money in", CatIncomeBg) }
    Gap(8)
    Rise(1) { BigMoney(recap.received, Color.White) }
    Gap(20)
    Rise(2) { Headline("came into your accounts.") }
    Gap(20)
    val kept = recap.received - recap.spent - recap.fees
    Rise(3) {
        Body(
            if (kept >= 0) "That's ${RecapFormat.money(kept)} more than went out."
            else "${RecapFormat.money(-kept)} more went out than came in.",
            CatIncomeBg,
        )
    }
    Gap(8)
    Rise(4) { Body("Only accounts that text you are counted.", Color.White.copy(alpha = 0.6f), size = 13) }
}

@Composable
private fun TopCategorySlide(recap: YearRecap) = SlideColumn {
    val top = recap.topCategory ?: return@SlideColumn
    Rise(0) { Kicker("Your number one", top.category.bgColor) }
    Gap(20)
    Rise(1) { CategoryBadge(top.category, size = 72) }
    Gap(20)
    Rise(2) { FitText(top.category.label, 60.sp, Color.White) }
    Gap(12)
    Rise(3) { BigMoney(top.total, top.category.bgColor, size = 40) }
    Gap(16)
    Rise(4) {
        Headline("${RecapFormat.percent(top.share)} of your spending, over ${RecapFormat.count(top.count)} payments.")
    }
}

@Composable
private fun CategoriesSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("Where it went", RecapPalette.Mint) }
    Gap(8)
    Rise(1) { Headline("Your top categories") }
    Gap(28)
    val shown = recap.categories.take(5)
    val max = shown.maxOf { it.total }
    shown.forEachIndexed { i, share ->
        Rise(2 + i) {
            Column(Modifier.padding(bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(share.category, size = 28)
                    HGap(12)
                    Text(
                        share.category.label,
                        color = Color.White,
                        fontFamily = TrackittBodyFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        RecapFormat.money(share.total),
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = TrackittBodyFamily,
                        fontSize = 15.sp,
                    )
                }
                Gap(8)
                GrowingBar(fraction = (share.total / max).toFloat(), color = share.category.bgColor, delayMillis = 300 + i * 160)
            }
        }
    }
}

@Composable
private fun TopPayeesSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("Your top ${recap.topPayees.size}", CatBillsBg) }
    Gap(8)
    Rise(1) { Headline("The people and places you paid the most") }
    Gap(28)
    recap.topPayees.forEachIndexed { i, payee ->
        Rise(2 + i) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
                Text(
                    "${i + 1}",
                    fontFamily = FrauncesFamily,
                    fontSize = 34.sp,
                    color = CatBillsBg,
                    modifier = Modifier.width(40.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        RecapFormat.name(payee.name),
                        color = Color.White,
                        fontFamily = TrackittBodyFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${RecapFormat.money(payee.total)} · ${RecapFormat.count(payee.count)} ${if (payee.count == 1) "payment" else "payments"}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = TrackittBodyFamily,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularSlide(recap: YearRecap) = SlideColumn {
    val regular = recap.regular ?: return@SlideColumn
    Rise(0) { Kicker("Your regular", CatTransportBg) }
    Gap(16)
    Rise(1) { FitText(RecapFormat.name(regular.name), 48.sp, Color.White) }
    Gap(20)
    Rise(2) { Headline("You paid them ${RecapFormat.count(regular.count)} times, ${RecapFormat.money(regular.total)} in all.") }
    val weeks = recap.trackedDays / 7.0
    if (weeks >= 4 && regular.count >= weeks) {
        Gap(12)
        Rise(3) { Body("More than once a week. They must know you well.", CatTransportBg) }
    }
    if (PayeeNumber.of(regular.name) != null) {
        Gap(12)
        Rise(4) { Body("Give this number a name on one of its payments to see who it is here.", Color.White.copy(alpha = 0.7f), size = 14) }
    }
}

@Composable
private fun BiggestSlide(recap: YearRecap) = SlideColumn {
    val biggest = recap.biggest ?: return@SlideColumn
    val day = java.time.Instant.ofEpochMilli(biggest.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    Rise(0) { Kicker("Your biggest payment", CatFoodBg) }
    Gap(8)
    Rise(1) { BigMoney(biggest.amount, Color.White) }
    Gap(20)
    Rise(2) { Headline("to ${RecapFormat.name(PayeeNumber.of(biggest.merchant) ?: biggest.merchant.trim())}") }
    Gap(8)
    Rise(3) { Body("on ${RecapFormat.longDay(day)}.", CatFoodBg) }
    Gap(20)
    Rise(4) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(biggest.category, size = 28)
            HGap(10)
            Body(biggest.category.label)
        }
    }
}

@Composable
private fun MonthsSlide(recap: YearRecap) = SlideColumn {
    // Only whole months are compared: the one under way, or one tracked from its middle, would always look calm.
    val whole = recap.months.filter { it.complete }
    val biggest = whole.maxBy { it.total }
    val calmest = whole.minBy { it.total }
    Rise(0) { Kicker("Month by month", RecapPalette.Mint) }
    Gap(8)
    Rise(1) { Headline("${RecapFormat.month(biggest.month)} was your biggest month.") }
    Gap(6)
    Rise(2) { Body("${RecapFormat.month(calmest.month)} was your calmest.", RecapPalette.Mint) }
    Gap(32)
    Rise(3) {
        val max = recap.months.maxOf { it.total }.coerceAtLeast(1.0)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        ) {
            recap.months.forEachIndexed { i, month ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        GrowingColumn(
                            fraction = (month.total / max).toFloat().coerceAtLeast(0.02f),
                            color = when {
                                month == biggest -> RecapPalette.Mint
                                month.complete -> RecapPalette.Mint.copy(alpha = 0.35f)
                                else -> RecapPalette.Mint.copy(alpha = 0.15f)
                            },
                            delayMillis = 400 + i * 70,
                        )
                    }
                    Gap(8)
                    Text(
                        RecapFormat.monthInitial(month.month),
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = TrackittBodyFamily,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
    if (recap.months.any { !it.complete }) {
        Gap(16)
        Rise(4) { Body("Faint bars are months Trackitt only saw part of.", Color.White.copy(alpha = 0.6f), size = 13) }
    }
}

@Composable
private fun RhythmSlide(recap: YearRecap) = SlideColumn {
    val weekday = recap.busiestWeekday ?: return@SlideColumn
    Rise(0) { Kicker("Your rhythm", CatChurchBg) }
    Gap(8)
    Rise(1) { Headline("You paid for things most on") }
    Gap(8)
    Rise(2) { FitText(weekday.getDisplayName(TextStyle.FULL, Locale.US) + "s", 64.sp, Color.White) }
    Gap(24)
    Rise(3) {
        Body(
            when (recap.lateNightPayments) {
                0 -> "And not one payment between 10 pm and 5 am. Early to bed."
                1 -> "Just one payment between 10 pm and 5 am."
                else -> "And ${RecapFormat.count(recap.lateNightPayments)} payments between 10 pm and 5 am. Night owl."
            },
            CatChurchBg,
        )
    }
}

@Composable
private fun NoSpendSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("No-spend days", CatGroceriesBg) }
    Gap(8)
    Rise(1) { BigCount(recap.noSpendDays, Color.White) }
    Gap(16)
    Rise(2) { Headline("days without spending a pesewa.") }
    Gap(16)
    Rise(3) {
        Body(
            "That's ${RecapFormat.percent(recap.noSpendDays.toDouble() / recap.trackedDays)} of the ${RecapFormat.count(recap.trackedDays)} days you tracked.",
            CatGroceriesBg,
        )
    }
}

@Composable
private fun FeesSlide(recap: YearRecap) = SlideColumn {
    Rise(0) { Kicker("Fees", CatFeesBg) }
    Gap(8)
    Rise(1) { BigMoney(recap.fees, Color.White) }
    Gap(20)
    Rise(2) {
        Headline("went on fees, over ${RecapFormat.count(recap.feePayments)} ${if (recap.feePayments == 1) "payment" else "payments"}.")
    }
    Gap(16)
    Rise(3) { Body("Paying a shop's merchant number often costs nothing, where a transfer is charged.", CatFeesBg) }
}

@Composable
private fun PersonalitySlide(recap: YearRecap) = SlideColumn {
    val accent = recap.topCategory?.category?.bgColor ?: RecapPalette.Mint
    Rise(0) { Kicker("Your money personality", accent) }
    Gap(16)
    Rise(1) { Headline("You're") }
    Rise(2) { FitText(recap.personality.title, 56.sp, Color.White) }
    Gap(20)
    Rise(3) { Headline(recap.personality.line) }
    recap.topCategory?.let { top ->
        Gap(20)
        Rise(4) {
            Body(
                if (recap.personality == Personality.ALL_ROUNDER) {
                    "Even your biggest, ${top.category.label}, was only ${RecapFormat.percent(top.share)} of your spending."
                } else {
                    "${top.category.label} took ${RecapFormat.percent(top.share)} of your spending."
                },
                accent,
            )
        }
    }
}

// ── Building blocks ──

/** A slide's text, left-aligned and centred on the screen, scrolling if a small screen needs it. */
@Composable
private fun SlideColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) { content() }
    }
}

/**
 * Big display text that shrinks, down to [minSize], until its longest word fits the width, so a
 * long word like "Wednesdays" or "Electronics" never breaks in the middle.
 */
@Composable
internal fun FitText(text: String, maxSize: TextUnit, color: Color, minSize: TextUnit = 28.sp, maxLines: Int = 3) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val width = constraints.maxWidth
        fun style(size: Float) = androidx.compose.ui.text.TextStyle(
            fontFamily = FrauncesFamily,
            fontSize = size.sp,
            lineHeight = (size * 1.08f).sp,
            letterSpacing = (-size * 0.025f).sp,
        )
        val size = remember(text, width, maxSize) {
            var size = maxSize.value
            val words = text.split(" ").filter { it.isNotEmpty() }
            while (size > minSize.value &&
                words.any { measurer.measure(it, style(size), softWrap = false, maxLines = 1).size.width > width }
            ) size -= 2f
            size
        }
        Text(text, style = style(size), color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Fades and lifts into place, one after another by [order]. */
@Composable
internal fun Rise(order: Int, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(150L + order * 170L)
        shown.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    Box(Modifier.graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * 28.dp.toPx()
    }) { content() }
}

/** Counts up to [target], so a big number lands rather than just appears. */
@Composable
private fun countUp(target: Double, delayMillis: Int = 450): Double {
    val reduce = LocalReduceMotion.current
    val value = remember(target) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(target) {
        if (!reduce) value.animateTo(1f, tween(1400, delayMillis, FastOutSlowInEasing))
    }
    return target * value.value
}

@Composable
private fun BigMoney(value: Double, color: Color, size: Int = 64) {
    val shown = countUp(value)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "GH₵",
            color = color.copy(alpha = 0.75f),
            fontFamily = TrackittBodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (size * 0.32f).sp,
            modifier = Modifier.padding(bottom = (size * 0.16f).dp, end = 4.dp),
        )
        Text(
            RecapFormat.amount(if (shown >= value) value else shown),
            color = color,
            fontFamily = FrauncesFamily,
            fontSize = size.sp,
            lineHeight = (size * 1.05f).sp,
            letterSpacing = (-size * 0.03f).sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun BigCount(value: Int, color: Color) {
    val shown = countUp(value.toDouble())
    Text(
        RecapFormat.count(shown.toInt()),
        color = color,
        fontFamily = FrauncesFamily,
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-3).sp,
    )
}

@Composable
private fun GrowingBar(fraction: Float, color: Color, delayMillis: Int) {
    val reduce = LocalReduceMotion.current
    val grown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) grown.animateTo(1f, tween(900, delayMillis, FastOutSlowInEasing)) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth((fraction * grown.value).coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(color),
        )
    }
}

@Composable
private fun GrowingColumn(fraction: Float, color: Color, delayMillis: Int) {
    val reduce = LocalReduceMotion.current
    val grown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) grown.animateTo(1f, tween(800, delayMillis, FastOutSlowInEasing)) }
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight((fraction * grown.value).coerceIn(0.02f, 1f))
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(color),
    )
}

@Composable
internal fun CategoryBadge(category: TransactionCategory, size: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp).background(category.bgColor, CircleShape),
    ) {
        Icon(category.icon, contentDescription = null, tint = category.fgColor, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
internal fun Kicker(text: String, color: Color) {
    Text(
        text.uppercase(),
        color = color,
        fontFamily = TrackittBodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
    )
}

@Composable
private fun Headline(text: String, color: Color = Color.White) {
    Text(
        text,
        color = color,
        fontFamily = TrackittBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    )
}

@Composable
private fun Body(text: String, color: Color = Color.White, size: Int = 17) {
    Text(
        text,
        color = color,
        fontFamily = TrackittBodyFamily,
        fontSize = size.sp,
        lineHeight = (size * 1.35f).sp,
    )
}
