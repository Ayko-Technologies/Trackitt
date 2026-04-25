package com.ayko.trackitt.ui.screen.search

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
) {
    var query by remember { mutableStateOf("trotro") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        // Search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search transactions") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Results
        if (query.isNotEmpty()) {
            Text(
                text = "RESULTS FOR \"${query.uppercase()}\"",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SatoshiFamily,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
            )

            val results = listOf(
                Triple("Trotro to Madina", "Transport · Apr 22", "−₵15.00"),
                Triple("Trotro to Legon", "Transport · Apr 19", "−₵8.00"),
                Triple("Trotro to Circle", "Transport · Apr 14", "−₵12.00"),
            )

            results.forEach { (merchant, subtitle, amount) ->
                SearchResultRow(
                    merchant = merchant,
                    subtitle = subtitle,
                    amount = amount,
                    category = TransactionCategory.TRANSPORT,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Recent
        Text(
            text = "RECENT",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = SatoshiFamily,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
        )

        listOf("Lydia supermarket", "MTN MoMo", "DStv bills").forEach { recent ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = recent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    merchant: String,
    subtitle: String,
    amount: String,
    category: TransactionCategory,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(category.bgColor, CircleShape),
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.fgColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = amount,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FrauncesFamily,
        )
    }
}
