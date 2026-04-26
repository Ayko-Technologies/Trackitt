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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    onNavigateToSmsSources: () -> Unit = {},
    onNavigateToMerchantDictionary: () -> Unit = {},
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
                        onClick = onNavigateToSmsSources,
                    )
                    SettingsRow(
                        icon = Icons.Filled.Sell,
                        label = "Merchant dictionary",
                        subtitle = "40+ auto-categorised merchants",
                        showChevron = true,
                        showDivider = true,
                        onClick = onNavigateToMerchantDictionary,
                    )
                    SettingsRow(
                        icon = Icons.Filled.Refresh,
                        label = "Re-scan SMS",
                        subtitle = run {
                            val days = context.getSharedPreferences("trackitt", 0).getInt("backfill_days", 90)
                            "Re-import MoMo messages from the last $days days"
                        },
                        showDivider = true,
                        onClick = {
                            val prefs = context.getSharedPreferences("trackitt", 0)
                            val days = prefs.getInt("backfill_days", 90)
                            prefs.edit().putBoolean("backfill_completed", false).apply()
                            val request = androidx.work.OneTimeWorkRequestBuilder<
                                com.ayko.trackitt.service.sms.SmsBackfillWorker>()
                                .setInputData(androidx.work.workDataOf("lookback_days" to days))
                                .build()
                            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                                "sms_backfill",
                                androidx.work.ExistingWorkPolicy.REPLACE,
                                request,
                            )
                            scope.launch { snackbarHostState.showSnackbar("Scanning last $days days of SMS...") }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Filled.DateRange,
                        label = "SMS lookback period",
                        trailing = {
                            var showPicker by remember { mutableStateOf(false) }
                            val prefs = remember { context.getSharedPreferences("trackitt", 0) }
                            var currentDays by remember { mutableIntStateOf(prefs.getInt("backfill_days", 90)) }

                            Text(
                                "$currentDays days",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SatoshiFamily,
                                modifier = Modifier.clickable { showPicker = true },
                            )

                            if (showPicker) {
                                AlertDialog(
                                    onDismissRequest = { showPicker = false },
                                    title = { Text("Lookback period", fontFamily = SatoshiFamily) },
                                    text = {
                                        Column {
                                            listOf(1, 7, 30, 60, 90, 180, 365).forEach { days ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (days == currentDays) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                                        .clickable {
                                                            currentDays = days
                                                            prefs.edit().putInt("backfill_days", days).apply()
                                                            showPicker = false
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                ) {
                                                    Text(
                                                        when (days) {
                                                            1 -> "Today only"
                                                            7 -> "7 days (1 week)"
                                                            30 -> "30 days (1 month)"
                                                            60 -> "60 days (2 months)"
                                                            90 -> "90 days (3 months)"
                                                            180 -> "180 days (6 months)"
                                                            365 -> "365 days (1 year)"
                                                            else -> "$days days"
                                                        },
                                                        fontFamily = SatoshiFamily,
                                                        fontWeight = if (days == currentDays) FontWeight.SemiBold else FontWeight.Normal,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                )
                            }
                        },
                        showDivider = false,
                    )
                }
            }

            // ── Danger Zone ──
            item { SectionHeader("Danger Zone") }

            item {
                var showConfirm by remember { mutableStateOf(false) }

                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Filled.Delete,
                        label = "Reset all data",
                        subtitle = "Delete all transactions, categories, and merchant mappings",
                        labelColor = Color(0xFFB3261E),
                        onClick = { showConfirm = true },
                        showDivider = false,
                    )
                }

                if (showConfirm) {
                    AlertDialog(
                        onDismissRequest = { showConfirm = false },
                        title = { Text("Reset all data?", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium) },
                        text = { Text("This will permanently delete all your transactions, custom categories, and merchant mappings. This cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        db.clearAllTables()
                                        context.getSharedPreferences("trackitt", 0).edit().clear().apply()
                                        context.getSharedPreferences("search", 0).edit().clear().apply()
                                        snackbarHostState.showSnackbar("All data cleared")
                                    }
                                    showConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                            ) { Text("Reset") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                        },
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
            onSave = { name, bgColor, fgColor, iconName ->
                scope.launch {
                    db.customCategoryDao().insert(
                        CustomCategoryEntity(name = name, bgColor = bgColor.toArgb(), fgColor = fgColor.toArgb(), iconName = iconName)
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
    onSave: (name: String, bgColor: Color, fgColor: Color, iconName: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedIconIndex by remember { mutableIntStateOf(0) }
    var showAllIcons by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var hexInput by remember { mutableStateOf("") }
    var customBg by remember { mutableStateOf<Color?>(null) }
    var customFg by remember { mutableStateOf<Color?>(null) }

    val allIcons = remember {
        listOf<androidx.compose.ui.graphics.vector.ImageVector>(
            Icons.Filled.Restaurant,
            Icons.Filled.LocalGroceryStore,
            Icons.Filled.DirectionsBus,
            Icons.Filled.ShoppingBag,
            Icons.Filled.Category,
            Icons.Filled.SwapHoriz,
            Icons.Filled.ArrowDownward,
            Icons.Filled.Favorite,
            Icons.Filled.Star,
            Icons.Filled.Home,
            Icons.Filled.Work,
            Icons.Filled.School,
            Icons.Filled.FitnessCenter,
            Icons.Filled.LocalHospital,
            Icons.Filled.Pets,
            Icons.Filled.ChildCare,
            Icons.Filled.Flight,
            Icons.Filled.Hotel,
            Icons.Filled.LocalCafe,
            Icons.Filled.LocalBar,
            Icons.Filled.MusicNote,
            Icons.Filled.SportsEsports,
            Icons.Filled.Movie,
            Icons.Filled.MenuBook,
            Icons.Filled.Checkroom,
            Icons.Filled.Build,
            Icons.Filled.LocalGasStation,
            Icons.Filled.LocalParking,
            Icons.Filled.Wifi,
            Icons.Filled.Phone,
            Icons.Filled.Savings,
            Icons.Filled.CardGiftcard,
            Icons.Filled.Handshake,
            Icons.Filled.VolunteerActivism,
            Icons.Filled.Spa,
            Icons.Filled.Park,
        )
    }

    val iconNames = remember {
        listOf(
            "restaurant", "local_grocery_store", "directions_bus", "shopping_bag",
            "category", "swap_horiz", "arrow_downward", "favorite",
            "star", "home", "work", "school",
            "fitness_center", "local_hospital", "pets", "child_care",
            "flight", "hotel", "local_cafe", "local_bar",
            "music_note", "sports_esports", "movie", "menu_book",
            "checkroom", "build", "local_gas_station", "local_parking",
            "wifi", "phone", "savings", "card_giftcard",
            "handshake", "volunteer_activism", "spa", "park",
        )
    }

    val displayedIcons = if (showAllIcons) allIcons else allIcons.take(8)
    val displayedNames = if (showAllIcons) iconNames else iconNames.take(8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Icon section
                Spacer(Modifier.height(16.dp))
                Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                Spacer(Modifier.height(8.dp))

                // Icon grid — 4 columns when expanded, scrollable row when collapsed
                if (showAllIcons) {
                    // Full grid
                    Column {
                        for (row in displayedIcons.indices step 6) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (col in 0 until 6) {
                                    val idx = row + col
                                    if (idx < displayedIcons.size) {
                                        val isSelected = idx == selectedIconIndex
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .clickable { selectedIconIndex = idx },
                                        ) {
                                            Icon(displayedIcons[idx], contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        TextButton(onClick = { showAllIcons = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Show less", fontFamily = SatoshiFamily, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(displayedIcons.size) { index ->
                            val isSelected = index == selectedIconIndex
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { selectedIconIndex = index },
                            ) {
                                Icon(displayedIcons[index], contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    TextButton(onClick = { showAllIcons = true }) {
                        Text("More icons →", fontFamily = SatoshiFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Color section
                Spacer(Modifier.height(16.dp))
                Text("Color", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COLORS.size) { index ->
                        val (bg, _) = PRESET_COLORS[index]
                        val isSelected = customBg == null && index == selectedColorIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bg, CircleShape)
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { selectedColorIndex = index; customBg = null; customFg = null; showCustomColor = false },
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = { showCustomColor = !showCustomColor }) {
                    Text(
                        if (showCustomColor) "Hide hex input" else "Custom hex →",
                        fontFamily = SatoshiFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (customBg != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(customBg!!, CircleShape),
                        )
                    }
                }

                // Custom hex color input
                if (showCustomColor) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it in "0123456789abcdefABCDEF" }.take(6)
                                hexInput = filtered
                                if (filtered.length == 6) {
                                    try {
                                        val color = Color(android.graphics.Color.parseColor("#$filtered"))
                                        customBg = color
                                        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue).toFloat()
                                        customFg = if (luminance > 0.5f) Color(
                                            red = (color.red * 0.3f).coerceIn(0f, 1f),
                                            green = (color.green * 0.3f).coerceIn(0f, 1f),
                                            blue = (color.blue * 0.3f).coerceIn(0f, 1f),
                                        ) else Color.White
                                    } catch (_: Exception) {}
                                }
                            },
                            placeholder = { Text("FF5722") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        if (customBg != null) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(customBg!!, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val bg = customBg ?: PRESET_COLORS[selectedColorIndex].first
                        val fg = customFg ?: PRESET_COLORS[selectedColorIndex].second
                        val safeIndex = selectedIconIndex.coerceIn(0, iconNames.lastIndex)
                        onSave(name.trim(), bg, fg, iconNames[safeIndex])
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
