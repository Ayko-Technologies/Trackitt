package com.ayko.trackitt.ui.screen.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.CookieIcon
import com.ayko.trackitt.ui.component.DisplayNumber
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.SurfaceContainerHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    var selectedCategory by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "New expense",
                    fontFamily = SatoshiFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
            ),
        )

        // Amount — no label
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    .padding(bottom = 8.dp),
            ) {
                DisplayNumber(
                    value = 215.0,
                    sizeSp = 56f,
                )
                Spacer(Modifier.weight(1f))
                Spacer(
                    Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .drawBehind { drawRect(primaryColor) },
                )
            }
        }

        // Category grid with cookie shapes
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .height(180.dp),
        ) {
            itemsIndexed(TransactionCategory.entries.toList()) { index, cat ->
                val isSelected = index == selectedCategory
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedCategory = if (isSelected) -1 else index
                    },
                ) {
                    CookieIcon(
                        fillColor = if (isSelected) cat.bgColor else SurfaceContainerHigh,
                        strokeColor = if (isSelected) cat.fgColor else null,
                        strokeWidth = 2.dp,
                        size = 56.dp,
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.label,
                            tint = if (isSelected) cat.fgColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = cat.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        // + Add details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clickable { },
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Add details",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = SatoshiFamily,
            )
        }

        Spacer(Modifier.weight(1f))

        // Save button
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(40.dp),
        ) {
            Text(
                text = "Save expense",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SatoshiFamily,
            )
        }
    }
}
