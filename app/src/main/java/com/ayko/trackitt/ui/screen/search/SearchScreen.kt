package com.ayko.trackitt.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.SatoshiFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onBack: () -> Unit = {},
    onTransactionTap: (Long) -> Unit = {},
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val hasSearched = query.length >= 2
    val noResults = hasSearched && results.isEmpty()

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
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text("Search merchants, notes...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { if (query.isNotBlank()) viewModel.saveSearch(query) }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { viewModel.query.value = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Results
            if (results.isNotEmpty()) {
                item {
                    Text(
                        text = "${results.size} RESULT${if (results.size > 1) "S" else ""} FOR \"${query.uppercase()}\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                items(results, key = { it.id }) { tx ->
                    SearchResultRow(
                        tx = tx,
                        onClick = { onTransactionTap(tx.id) },
                    )
                }
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            // No results
            if (noResults) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp, start = 40.dp, end = 40.dp),
                    ) {
                        Text(
                            text = "¯\\_(ツ)_/¯",
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Nothing for \"$query\"",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SatoshiFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Try a different merchant name or check your spelling",
                            fontSize = 13.sp,
                            fontFamily = SatoshiFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Recent searches
            if (!hasSearched && recentSearches.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                items(recentSearches) { recent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.query.value = recent }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            recent,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SatoshiFamily,
                        )
                    }
                }
            }

            // Empty state — no recent, no query
            if (!hasSearched && recentSearches.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp, start = 40.dp, end = 40.dp),
                    ) {
                        Text(
                            text = "Search your transactions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SatoshiFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    tx: Transaction,
    onClick: () -> Unit = {},
) {
    val dateFormatted = SimpleDateFormat("MMM d", Locale.US).format(Date(tx.date))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(tx.category.bgColor, CircleShape),
        ) {
            Icon(
                imageVector = tx.category.icon,
                contentDescription = null,
                tint = tx.category.fgColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.merchant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SatoshiFamily,
            )
            Text(
                "${tx.category.label} · $dateFormatted",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SatoshiFamily,
            )
        }
        Text(
            text = "${if (tx.isIncome) "+" else "−"}₵${String.format(Locale.US, "%.2f", tx.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FrauncesFamily,
            letterSpacing = (-0.3).sp,
        )
    }
}
