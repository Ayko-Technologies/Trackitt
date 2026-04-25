package com.ayko.trackitt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.R

val FrauncesFamily = FontFamily(
    Font(R.font.fraunces, weight = FontWeight.Light),
    Font(R.font.fraunces, weight = FontWeight.Normal),
    Font(R.font.fraunces, weight = FontWeight.Medium),
    Font(R.font.fraunces, weight = FontWeight.SemiBold),
    Font(R.font.fraunces, weight = FontWeight.Bold),
)

val SatoshiFamily = FontFamily(
    Font(R.font.satoshi_light, weight = FontWeight.Light),
    Font(R.font.satoshi_regular, weight = FontWeight.Normal),
    Font(R.font.satoshi_medium, weight = FontWeight.Medium),
    Font(R.font.satoshi_bold, weight = FontWeight.SemiBold),
    Font(R.font.satoshi_bold, weight = FontWeight.Bold),
)

val RobotoMonoFamily = FontFamily(
    Font(R.font.roboto_mono, weight = FontWeight.Normal),
    Font(R.font.roboto_mono, weight = FontWeight.Medium),
)

val TrackittTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        letterSpacing = (-2.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 60.sp,
        letterSpacing = (-1.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SatoshiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.2.sp,
    ),
)
