package com.ayko.trackitt.ui.screen.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import com.ayko.trackitt.ui.component.isCompactHeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayko.trackitt.ui.theme.TrackittBodyFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppLockScreen(
    showBiometric: Boolean,
    biometricPromptNonce: Int,
    onUnlockWithPin: (String) -> Boolean,
    onRequestBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {}

    LaunchedEffect(showBiometric, biometricPromptNonce) {
        if (showBiometric) {
            onRequestBiometric()
        }
    }

    val submit: () -> Unit = {
        if (onUnlockWithPin(pin)) {
            pin = ""
        } else {
            error = "Wrong PIN. Try again."
        }
    }
    // Landscape: the PIN field shares a row with its buttons so the card fits the short screen.
    // With the keyboard up as well, only that row is left.
    val compact = isCompactHeight()
    val condensed = compact && WindowInsets.isImeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = if (condensed) 4.dp else 12.dp)
                // Landscape uses the full width; portrait keeps a comfortable card width.
                .then(if (compact) Modifier else Modifier.widthIn(max = 480.dp))
                .fillMaxWidth(),
        ) {
            // Scrolls when the screen is too short for the whole card, such as in landscape.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = if (condensed) 8.dp else if (compact) 20.dp else 28.dp),
            ) {
                if (compact && !condensed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Unlock Trackitt",
                            fontFamily = TrackittBodyFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (showBiometric) "Use your fingerprint or enter your PIN." else "Enter your PIN to open the app.",
                        fontFamily = TrackittBodyFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                } else if (!condensed) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Unlock Trackitt",
                    fontFamily = TrackittBodyFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (showBiometric) "Use your fingerprint or enter your PIN." else "Enter your PIN to open the app.",
                    fontFamily = TrackittBodyFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                }
                val pinField: @Composable (Modifier) -> Unit = { fieldModifier ->
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(4)
                        error = null
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (pin.length == 4) submit() }),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = fieldModifier,
                )
                }
                val unlockButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                    Button(onClick = submit, enabled = pin.length == 4, modifier = buttonModifier) {
                        Text("Unlock")
                    }
                }
                // The field keeps the same place in every layout, so it keeps focus and the keyboard
                // stays open when the layout changes around it.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    pinField(Modifier.weight(1f))
                    if (compact) {
                        Spacer(Modifier.width(12.dp))
                        unlockButton(Modifier)
                        if (showBiometric) {
                            Spacer(Modifier.width(8.dp))
                            FilledTonalIconButton(onClick = onRequestBiometric, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = "Use biometrics")
                            }
                        }
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error!!,
                        fontFamily = TrackittBodyFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Start),
                    )
                }
                if (!compact) {
                    Spacer(Modifier.height(16.dp))
                    unlockButton(Modifier.fillMaxWidth())
                }
                if (showBiometric && !compact) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRequestBiometric,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use biometrics")
                    }
                }
            }
        }
    }
}
