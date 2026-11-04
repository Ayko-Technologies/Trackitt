package com.ayko.trackitt.ui.screen.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.service.sms.SmsProvider
import com.ayko.trackitt.service.sms.SmsProviders
import com.ayko.trackitt.ui.theme.TrackittBodyFamily

private const val PREFS_NAME = "sms_sources"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSourcesScreen(onBack: () -> Unit = {}) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, 0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("SMS Sources", fontFamily = TrackittBodyFamily, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            item { SectionLabel("Mobile Money") }
            items(WALLETS) { provider -> ProviderToggle(provider, prefs) }

            item { SectionLabel("Banks") }
            items(BANKS) { provider -> ProviderToggle(provider, prefs) }

            item { SectionLabel("Everything else") }
            item {
                ToggleSourceRow(
                    name = "Other banks & wallets",
                    key = "other",
                    bgColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    prefs = prefs,
                    activeLabel = "Detected automatically from transaction alerts",
                )
            }
        }
    }
}

private val WALLETS = listOf(
    SmsProviders.MOMO, SmsProviders.TELECEL, SmsProviders.GHANAPAY,
    SmsProviders.AT_MONEY, SmsProviders.GMONEY, SmsProviders.ZEEPAY,
)

private val BANKS = SmsProviders.known - WALLETS.toSet()

private val BRAND_COLORS = mapOf(
    SmsProviders.MOMO.key to (Color(0xFFFFCC00) to Color(0xFF003366)),
    SmsProviders.TELECEL.key to (Color(0xFFE31E24) to Color.White),
    SmsProviders.GHANAPAY.key to (Color(0xFF1B5E20) to Color.White),
    SmsProviders.AT_MONEY.key to (Color(0xFF0066B3) to Color.White),
    SmsProviders.GMONEY.key to (Color(0xFF004D26) to Color.White),
    SmsProviders.ZEEPAY.key to (Color(0xFF5B2A86) to Color.White),
    SmsProviders.ECOBANK.key to (Color(0xFF003B71) to Color.White),
    SmsProviders.GCB.key to (Color(0xFF004D26) to Color.White),
    SmsProviders.STANBIC.key to (Color(0xFF0033A0) to Color.White),
    SmsProviders.FIDELITY.key to (Color(0xFF006B3F) to Color.White),
    SmsProviders.ABSA.key to (Color(0xFFAF0000) to Color.White),
    SmsProviders.ACCESS.key to (Color(0xFFF37021) to Color.White),
    SmsProviders.GTBANK.key to (Color(0xFFDD4F05) to Color.White),
)

@Composable
private fun ProviderToggle(provider: SmsProvider, prefs: android.content.SharedPreferences) {
    val (bg, fg) = BRAND_COLORS[provider.key]
        ?: (MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer)
    ToggleSourceRow(name = provider.name, key = provider.key, bgColor = bg, textColor = fg, prefs = prefs)
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = TrackittBodyFamily,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleSourceRow(
    name: String,
    key: String,
    bgColor: Color,
    textColor: Color,
    prefs: android.content.SharedPreferences,
    activeLabel: String = "Active",
) {
    var enabled by remember { mutableStateOf(prefs.getBoolean("source_$key", true)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(bgColor, CircleShape),
        ) {
            Text(name.first().toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, fontFamily = TrackittBodyFamily)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = TrackittBodyFamily)
            Text(
                if (enabled) activeLabel else "Off",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = TrackittBodyFamily,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                prefs.edit().putBoolean("source_$key", it).apply()
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
