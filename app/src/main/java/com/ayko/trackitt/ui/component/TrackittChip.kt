package com.ayko.trackitt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.ui.theme.SatoshiFamily

@Composable
fun TrackittChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit = {},
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val borderMod = if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    val textColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .height(36.dp)
            .scalePressEffect()
            .clip(shape)
            .then(borderMod)
            .background(bg, shape)
            .clickable { onClick() }
            .padding(
                start = if (leadingIcon != null) 10.dp else 14.dp,
                end = 14.dp,
            ),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            fontFamily = SatoshiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = textColor,
        )
    }
}
