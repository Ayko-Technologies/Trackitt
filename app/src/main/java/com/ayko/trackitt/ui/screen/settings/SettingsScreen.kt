package com.ayko.trackitt.ui.screen.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.data.backup.DriveBackupManager
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backupManager = remember { DriveBackupManager(context) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            val accountName = account?.email ?: return@rememberLauncherForActivityResult
            scope.launch {
                val success = backupManager.backup(accountName)
                snackbarHostState.showSnackbar(
                    if (success) "Backup complete" else "Backup failed"
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                fontFamily = SatoshiFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "DATA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = SatoshiFamily,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
        )

        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                signInLauncher.launch(client.signInIntent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Back up to Google Drive", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("Restore coming soon")
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
        ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Restore from backup", fontFamily = SatoshiFamily, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp),
        )
    }
}
