package com.ayko.trackitt.ui.screen.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayko.trackitt.data.backup.DriveBackupManager
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.entity.CustomCategoryEntity
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

private val PRESET_COLORS = listOf(
    Color(0xFFE57373) to Color(0xFFB71C1C),
    Color(0xFFF06292) to Color(0xFF880E4F),
    Color(0xFFBA68C8) to Color(0xFF4A148C),
    Color(0xFF9575CD) to Color(0xFF311B92),
    Color(0xFF7986CB) to Color(0xFF1A237E),
    Color(0xFF64B5F6) to Color(0xFF0D47A1),
    Color(0xFF4FC3F7) to Color(0xFF01579B),
    Color(0xFF4DD0E1) to Color(0xFF006064),
    Color(0xFF4DB6AC) to Color(0xFF004D40),
    Color(0xFF81C784) to Color(0xFF1B5E20),
    Color(0xFFAED581) to Color(0xFF33691E),
    Color(0xFFFFD54F) to Color(0xFFF57F17),
    Color(0xFFFFB74D) to Color(0xFFE65100),
    Color(0xFFFF8A65) to Color(0xFFBF360C),
    Color(0xFFA1887F) to Color(0xFF3E2723),
    Color(0xFF90A4AE) to Color(0xFF263238),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backupManager = remember { DriveBackupManager(context) }
    val db = remember { TrackittDatabase.getInstance(context) }
    val customCategories by db.customCategoryDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            val accountName = account?.email ?: return@rememberLauncherForActivityResult
            scope.launch {
                val success = backupManager.backup(accountName)
                snackbarHostState.showSnackbar(if (success) "Backup complete" else "Backup failed")
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontFamily = SatoshiFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            // ── Categories ──
            item { SectionHeader("Categories") }

            item {
                SettingsGroup {
                    customCategories.forEachIndexed { index, cat ->
                        SettingsRow(
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(cat.bgColor), CircleShape),
                                )
                            },
                            label = cat.name,
                            trailing = {
                                IconButton(
                                    onClick = { scope.launch { db.customCategoryDao().delete(cat.id) } },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            showDivider = index < customCategories.size - 1,
                        )
                    }
                    SettingsRow(
                        leading = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        label = "Add category",
                        labelColor = MaterialTheme.colorScheme.primary,
                        onClick = { showAddCategoryDialog = true },
                        showDivider = false,
                    )
                }
            }

            // ── Data & Backup ──
            item { SectionHeader("Data & Backup") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Filled.CloudUpload,
                        label = "Back up to Google Drive",
                        subtitle = "Saves your database to a private Drive folder",
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            signInLauncher.launch(client.signInIntent)
                        },
                        showDivider = true,
                    )
                    SettingsRow(
                        icon = Icons.Filled.History,
                        label = "Restore from backup",
                        subtitle = "Download a previous backup from Drive",
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Restore coming soon") }
                        },
                        showDivider = false,
                    )
                }
            }

            // ── SMS ──
            item { SectionHeader("SMS Parsing") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Filled.Sms,
                        label = "SMS sources",
                        subtitle = "MTN MoMo",
                        showChevron = true,
                        showDivider = true,
                    )
                    SettingsRow(
                        icon = Icons.Filled.Sell,
                        label = "Merchant dictionary",
                        subtitle = "40+ auto-categorised merchants",
                        showChevron = true,
                        showDivider = false,
                    )
                }
            }

            // ── About ──
            item { SectionHeader("About") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        label = "Version",
                        trailing = {
                            Text(
                                "0.1.0",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SatoshiFamily,
                            )
                        },
                        showDivider = true,
                    )
                    SettingsRow(
                        icon = Icons.Filled.Palette,
                        label = "Made by Ayko Technologies",
                        showDivider = false,
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, bgColor, fgColor ->
                scope.launch {
                    db.customCategoryDao().insert(
                        CustomCategoryEntity(name = name, bgColor = bgColor.toArgb(), fgColor = fgColor.toArgb())
                    )
                }
                showAddCategoryDialog = false
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = SatoshiFamily,
        modifier = Modifier.padding(start = 32.dp, top = 28.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    label: String,
    subtitle: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(14.dp))
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = labelColor,
                    fontFamily = SatoshiFamily,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SatoshiFamily,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }

            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null || leading != null) 52.dp else 16.dp)
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, bgColor: Color, fgColor: Color) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Color",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SatoshiFamily,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COLORS.size) { index ->
                        val (bg, _) = PRESET_COLORS[index]
                        val isSelected = index == selectedColorIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bg, CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorIndex = index },
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val (bg, fg) = PRESET_COLORS[selectedColorIndex]
                        onSave(name.trim(), bg, fg)
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
