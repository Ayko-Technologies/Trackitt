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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.MERCHANT_DICTIONARY
import com.ayko.trackitt.ui.theme.SatoshiFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDictionaryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { TrackittDatabase.getInstance(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val userMappings by db.merchantMappingDao().let {
        kotlinx.coroutines.flow.flow {
            emit(emptyList<Pair<String, String>>())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val sortedDictionary = remember {
        MERCHANT_DICTIONARY.entries
            .sortedBy { it.key }
            .groupBy { it.value }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Merchant Dictionary", fontFamily = SatoshiFamily, fontWeight = FontWeight.SemiBold) },
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
            sortedDictionary.forEach { (category, entries) ->
                item {
                    Text(
                        category.label.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        entries.forEachIndexed { index, (keyword, cat) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(cat.bgColor, CircleShape),
                                ) {
                                    Icon(cat.icon, contentDescription = null, tint = cat.fgColor, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    keyword,
                                    fontSize = 14.sp,
                                    fontFamily = SatoshiFamily,
                                )
                            }
                            if (index < entries.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 58.dp)
                                        .height(0.5.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
