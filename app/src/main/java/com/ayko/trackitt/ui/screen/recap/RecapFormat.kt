package com.ayko.trackitt.ui.screen.recap

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Formatting shared by the recap's slides and its share card. */
internal object RecapFormat {
    private val whole = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    private val cents = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val longDay = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.US)

    /** "193,047" for large sums, "10.20" for small ones, where the pesewas matter. */
    fun amount(value: Double): String = if (value >= 100) whole.format(value) else cents.format(value)

    fun money(value: Double): String = "GH₵" + amount(value)

    fun count(value: Int): String = whole.format(value)

    fun percent(share: Double): String = "${(share * 100).roundToInt()}%"

    fun dayMonth(date: LocalDate): String = date.format(dayMonth)

    fun longDay(date: LocalDate): String = date.format(longDay)

    fun month(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, Locale.US)

    fun monthInitial(month: Int): String = Month.of(month).getDisplayName(TextStyle.NARROW, Locale.US)

    /** "ROAD TEC DRIVING ACADEMY" reads as "Road Tec Driving Academy"; names already in mixed case stay. */
    fun name(raw: String): String {
        val letters = raw.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() }) return raw
        return raw.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(Locale.US) }
        }
    }
}
