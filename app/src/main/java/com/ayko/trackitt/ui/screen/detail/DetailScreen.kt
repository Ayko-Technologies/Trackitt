package com.ayko.trackitt.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.CookieIcon
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.RobotoMonoFamily
import com.ayko.trackitt.ui.theme.TertiaryContainer

@Composable
fun DetailScreen(
    onBack: () -> Unit = {},
    onDelete: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onMarkTransfer: () -> Unit = {},
) {
    val category = TransactionCategory.SHOPPING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        // Toolbar
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
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", modifier = Modifier.size(22.dp))
            }
        }

        // SMS badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 20.dp, top = 4.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Icon(Icons.Filled.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Parsed from MoMo SMS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }

        // Amount
        DisplayNumber(
            value = -215.0,
            sizeSp = 56f,
            showSign = true,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp),
        )

        Text(
            text = "Today · 2:14 PM · Ref 8827104",
            fontFamily = RobotoMonoFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp),
        )

        // Category card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            CookieIcon(
                fillColor = category.bgColor,
                strokeColor = category.fgColor,
                size = 40.dp,
            ) {
                Icon(category.icon, contentDescription = null, tint = category.fgColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Lydia — supermarket", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }

        // AI suggestion
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(TertiaryContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF002023), modifier = Modifier.size(18.dp))
            Text(
                text = buildAnnotatedString {
                    append("We guessed ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Shopping") }
                    append(" from \"LYDIA ENTERP\". Tap to change — we'll learn your preference.")
                },
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF002023),
            )
        }

        // Meta info
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp)) {
            Text("MTN MoMo · 0244 ··· 892", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Today · 2:14 PM · Ref 8827104", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.weight(1f))

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onMarkTransfer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f).height(40.dp),
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Transfer", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = SatoshiFamily)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1.2f).height(40.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = SatoshiFamily)
            }
        }
    }
}
