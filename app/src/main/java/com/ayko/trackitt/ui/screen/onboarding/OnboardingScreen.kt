package com.ayko.trackitt.ui.screen.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.entity.BudgetEntity
import com.ayko.trackitt.service.sms.SmsBackfillWorker
import com.ayko.trackitt.ui.component.CookieIcon
import com.ayko.trackitt.ui.theme.CatFoodBg
import com.ayko.trackitt.ui.theme.CatFoodFg
import com.ayko.trackitt.ui.theme.CatGroceriesFg
import com.ayko.trackitt.ui.theme.CatShoppingBg
import com.ayko.trackitt.ui.theme.CatShoppingFg
import com.ayko.trackitt.ui.theme.CatTransportBg
import com.ayko.trackitt.ui.theme.CatTransportFg
import com.ayko.trackitt.ui.theme.CatBillsFg
import com.ayko.trackitt.ui.theme.FrauncesFamily
import com.ayko.trackitt.ui.theme.OnSurface
import com.ayko.trackitt.ui.theme.Primary
import com.ayko.trackitt.ui.theme.PrimaryContainer
import com.ayko.trackitt.ui.theme.SatoshiFamily
import com.ayko.trackitt.ui.theme.Surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Step {
    WELCOME, HOW_IT_WORKS, SMS_ACCESS, NOTIFICATIONS,
    PICK_SOURCES, SET_BUDGET, BACKFILLING, READY
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.WELCOME) }
    var hasSources by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            TrackittDatabase.getInstance(context).clearAllTables()
        }
    }

    val finishOnboarding: () -> Unit = {
        context.getSharedPreferences("trackitt", 0)
            .edit().putBoolean("onboarding_completed", true).apply()
        onComplete()
    }

    BackHandler(enabled = step != Step.WELCOME && step != Step.BACKFILLING && step != Step.READY) {
        step = Step.entries[step.ordinal - 1]
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "onboarding",
    ) { current ->
        when (current) {
            Step.WELCOME -> WelcomeStep(
                onGetStarted = { step = Step.HOW_IT_WORKS },
            )
            Step.HOW_IT_WORKS -> HowItWorksStep(
                onBack = { step = Step.WELCOME },
                onContinue = { step = Step.SMS_ACCESS },
            )
            Step.SMS_ACCESS -> SmsAccessStep(
                onBack = { step = Step.HOW_IT_WORKS },
                onGranted = { step = Step.NOTIFICATIONS },
                onSkip = { step = Step.NOTIFICATIONS },
            )
            Step.NOTIFICATIONS -> NotificationsStep(
                onBack = { step = Step.SMS_ACCESS },
                onGranted = { step = Step.PICK_SOURCES },
                onSkip = { step = Step.PICK_SOURCES },
            )
            Step.PICK_SOURCES -> PickSourcesStep(
                onBack = { step = Step.NOTIFICATIONS },
                onContinue = { enabled ->
                    hasSources = enabled
                    step = Step.SET_BUDGET
                },
            )
            Step.SET_BUDGET -> SetBudgetStep(
                onBack = { step = Step.PICK_SOURCES },
                onSet = { step = if (hasSources) Step.BACKFILLING else Step.READY },
                onSkip = { step = if (hasSources) Step.BACKFILLING else Step.READY },
            )
            Step.BACKFILLING -> BackfillingStep(
                onDone = { step = Step.READY },
            )
            Step.READY -> ReadyStep(onOpen = finishOnboarding)
        }
    }
}

// ── Shared components ───────────────────────────────────────

@Composable
private fun StepDots(current: Int, total: Int = 7) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == current) 22.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i == current) OnSurface else OnSurface.copy(alpha = 0.18f)),
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(stepIndex: Int, onBack: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
            }
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            StepDots(current = stepIndex)
        }
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun MicroLabel(text: String, color: Color = OnSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(6.dp).rotate(45f).background(color))
        Text(
            text.uppercase(),
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp, color = color, fontFamily = SatoshiFamily,
        )
    }
}

@Composable
private fun DisplayText(text: String, size: Int = 56) {
    Text(
        text, fontFamily = FrauncesFamily, fontSize = size.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (size * -0.025f).sp,
        lineHeight = (size * 1.05f).sp,
    )
}

@Composable
private fun PrimaryCta(text: String, icon: ImageVector? = Icons.Filled.ArrowForward, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(OnSurface)
            .clickable { onClick() },
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Surface, fontFamily = SatoshiFamily, letterSpacing = 0.2.sp)
        if (icon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(icon, contentDescription = null, tint = Surface, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GhostCta(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
    }
}

@Composable
private fun OrganicBlob(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0.20f * w, 0.47f * h)
            cubicTo(0.13f * w, 0.30f * h, 0.30f * w, 0.10f * h, 0.53f * w, 0.13f * h)
            cubicTo(0.77f * w, 0.17f * h, 0.93f * w, 0.37f * h, 0.90f * w, 0.60f * h)
            cubicTo(0.87f * w, 0.83f * h, 0.60f * w, 0.93f * h, 0.40f * w, 0.87f * h)
            cubicTo(0.20f * w, 0.80f * h, 0.27f * w, 0.63f * h, 0.20f * w, 0.47f * h)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}

@Composable
private fun BulletPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = SatoshiFamily)
    }
}

// ── 01 Welcome ──────────────────────────────────────────────

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(PrimaryContainer),
        ) {
            OrganicBlob(
                color = Color(0xFFA6E098),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 0.dp)
                    .size(340.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 24.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CookieIcon(fillColor = OnSurface, size = 120.dp)
            }

            Spacer(Modifier.height(80.dp))
            MicroLabel("Welcome")
            Spacer(Modifier.height(14.dp))
            DisplayText("Track every", size = 56)
            DisplayText("cedi, quietly.", size = 56)
            Spacer(Modifier.height(18.dp))
            Text(
                "Trackitt reads your MoMo and bank SMS on-device, and shows you where the month went. No logins. No bookkeeping.",
                fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp, fontFamily = SatoshiFamily,
            )

            Spacer(Modifier.weight(1f))

            PrimaryCta("Get started", onClick = onGetStarted)
        }
    }
}

// ── 02 How it works ─────────────────────────────────────────

@Composable
private fun HowItWorksStep(onBack: () -> Unit, onContinue: () -> Unit) {
    val steps = listOf(
        Triple(Icons.Filled.Sms, "New SMS comes in.", "A MoMo or bank message lands. Trackitt reads it on your phone; nothing leaves the device."),
        Triple(Icons.Filled.AutoAwesome, "It picks a category.", "Lydia Enterp goes to Shopping. Trotro to Transport. Wrong guess? Tap once to fix it."),
        Triple(Icons.Filled.BarChart, "You see where it went.", "Open the app and your month is right there; one total, the things you spent on most."),
    )
    val tints = listOf(PrimaryContainer, Color(0xFFFFE8C5), Color(0xFFBCEBEF))
    val inks = listOf(Color(0xFF1F4D1B), Color(0xFF7A4A0F), Color(0xFF0E4A4D))

    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        OnboardingTopBar(stepIndex = 0, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("How it works")
            Spacer(Modifier.height(10.dp))
            DisplayText("No spreadsheets.", size = 40)
            DisplayText("No typing.", size = 40)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            steps.forEachIndexed { i, (icon, title, body) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(tints[i])
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OnSurface),
                    ) {
                        Icon(icon, contentDescription = null, tint = tints[i], modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontFamily = FrauncesFamily, fontSize = 22.sp, color = inks[i], fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp, lineHeight = 24.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(body, fontSize = 14.sp, color = inks[i].copy(alpha = 0.78f), lineHeight = 20.sp, fontFamily = SatoshiFamily)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            PrimaryCta("Continue", icon = null, onClick = onContinue)
        }
    }
}

// ── 03 SMS access ───────────────────────────────────────────

@Composable
private fun SmsAccessStep(onBack: () -> Unit, onGranted: () -> Unit, onSkip: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onGranted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingTopBar(stepIndex = 1, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("Permission · 1 of 2")
            Spacer(Modifier.height(10.dp))
            DisplayText("Let trackitt", size = 44)
            DisplayText("read your SMS.", size = 44)
            Spacer(Modifier.height(14.dp))
            Text(
                "It's how we catch every MoMo payment automatically. Your messages stay on your phone; we don't see them, ever.",
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp, fontFamily = SatoshiFamily,
            )
        }

        // Faux SMS preview
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFD96E)),
                ) {
                    Text("M", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2A00), fontFamily = SatoshiFamily)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("MTN MoMo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFamily)
                    Text("Today · 2:14 PM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Payment of GHS215.00 to LYDIA ENTERP. Available bal: GHS1,847.50. Ref 8827104.",
                fontSize = 13.sp, fontFamily = SatoshiFamily, lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface)
                    .padding(14.dp),
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(CatShoppingBg),
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = CatShoppingFg, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lydia Enterp", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFamily)
                    Text("Shopping · MTN MoMo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                }
                Text("−215.00", fontFamily = FrauncesFamily, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BulletPoint(Icons.Filled.Lock, "Everything happens on your phone, not in the cloud")
            BulletPoint(Icons.Filled.VisibilityOff, "We skip anything that isn't a transaction")
            BulletPoint(Icons.Filled.Delete, "Turn it off in Settings whenever you like")
        }

        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp, top = 12.dp)) {
            PrimaryCta("Allow SMS access", icon = Icons.Filled.Check) {
                permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
            }
            Spacer(Modifier.height(6.dp))
            GhostCta("Not now, manual only", onClick = onSkip)
        }
    }
}

// ── 04 Notifications ────────────────────────────────────────

@Composable
private fun NotificationsStep(onBack: () -> Unit, onGranted: () -> Unit, onSkip: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onGranted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingTopBar(stepIndex = 2, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("Permission · 2 of 2")
            Spacer(Modifier.height(10.dp))
            DisplayText("And ping you", size = 44)
            DisplayText("when it matters.", size = 44)
            Spacer(Modifier.height(14.dp))
            Text(
                "Only when it actually matters: going over budget, an unusual charge, your weekly recap. That's it.",
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp, fontFamily = SatoshiFamily,
            )
        }

        // Faux notification preview
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(OnSurface),
                ) {
                    CookieIcon(fillColor = PrimaryContainer, size = 22.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("TRACKITT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.3.sp, fontFamily = SatoshiFamily)
                    Spacer(Modifier.height(4.dp))
                    Text("You're close to your Food budget", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFamily)
                    Spacer(Modifier.height(2.dp))
                    Text("GH₵612 of GH₵700 used; 87%. 5 days left.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp, fontFamily = SatoshiFamily)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BulletPoint(Icons.Filled.Warning, "A heads-up if you're about to blow your budget")
            BulletPoint(Icons.Filled.PriorityHigh, "A flag if something looks off")
            BulletPoint(Icons.Filled.Nightlight, "Nothing between 10pm and 7am")
        }

        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp, top = 12.dp)) {
            PrimaryCta("Allow notifications", icon = Icons.Filled.Check) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onGranted()
                }
            }
            Spacer(Modifier.height(6.dp))
            GhostCta("Maybe later", onClick = onSkip)
        }
    }
}

// ── 05 Pick sources ─────────────────────────────────────────

@Composable
private fun PickSourcesStep(onBack: () -> Unit, onContinue: (hasAnySources: Boolean) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_sources", 0) }

    data class Source(val key: String, val name: String, val sub: String, val tint: Color, val glyph: String, val textColor: Color = OnSurface)

    val sources = listOf(
        Source("MobileMoney", "MTN MoMo", "Mobile money SMS", Color(0xFFFFD96E), "M"),
        Source("T-CASH", "Telecel Cash", "Mobile money SMS", Color(0xFFE12C2C), "T", textColor = Color.White),
        Source("ECOBANK", "Ecobank", "Bank alerts", Color(0xFF003D7A), "E", textColor = Color.White),
    )

    var enabledMap by remember {
        mutableStateOf(sources.associate { it.key to prefs.getBoolean("source_${it.key}", true) })
    }
    val enabledCount = enabledMap.values.count { it }

    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        OnboardingTopBar(stepIndex = 3, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("Sources")
            Spacer(Modifier.height(10.dp))
            DisplayText("Where do you", size = 44)
            DisplayText("spend from?", size = 44)
            Spacer(Modifier.height(14.dp))
            Text(
                "Untick anything you don't want tracked.",
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp, fontFamily = SatoshiFamily,
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            sources.forEach { s ->
                val on = enabledMap[s.key] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            val newVal = !on
                            enabledMap = enabledMap.toMutableMap().apply { put(s.key, newVal) }
                            prefs.edit().putBoolean("source_${s.key}", newVal).apply()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(s.tint),
                    ) {
                        Text(s.glyph, fontFamily = FrauncesFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = s.textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFamily)
                        Text(s.sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (on) Primary else Color.Transparent)
                            .then(if (!on) Modifier.background(Color.Transparent).clip(CircleShape) else Modifier),
                    ) {
                        if (on) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Transparent)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = Color(0xFFC5C8BB), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            PrimaryCta(
                if (enabledCount > 0) "Continue with $enabledCount" else "Continue without SMS",
                icon = null,
                onClick = { onContinue(enabledCount > 0) },
            )
        }
    }
}

// ── 06 Set budget ───────────────────────────────────────────

private val BUDGET_SPLITS = listOf(
    Triple("FOOD", "Food", 0.34),
    Triple("GROCERIES", "Groceries", 0.24),
    Triple("TRANSPORT", "Transport", 0.18),
    Triple("BILLS", "Bills", 0.12),
    Triple("OTHER", "Other", 0.12),
)

private val SPLIT_COLORS = listOf(CatFoodFg, CatGroceriesFg, CatTransportFg, CatBillsFg, Color(0xFFC5C8BB))

@Composable
private fun SetBudgetStep(onBack: () -> Unit, onSet: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var budgetText by remember { mutableStateOf("") }
    var useSplit by remember { mutableStateOf(true) }
    val presets = listOf("1,500", "2,000", "3,000", "5,000")

    Column(modifier = Modifier.fillMaxSize().background(Surface).verticalScroll(rememberScrollState())) {
        OnboardingTopBar(stepIndex = 4, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("Optional · skip if unsure")
            Spacer(Modifier.height(10.dp))
            DisplayText("What's a fair", size = 44)
            DisplayText("month for you?", size = 44)
        }

        // Budget input card
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(PrimaryContainer)
                .padding(28.dp),
        ) {
            OrganicBlob(
                color = Color(0xFFA6E098),
                modifier = Modifier.align(Alignment.TopEnd).size(240.dp),
            )
            Column {
                MicroLabel("Monthly cap", color = Color(0xFF1F4D1B))
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GH₵", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F4D1B), fontFamily = SatoshiFamily)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        budgetText.ifEmpty { "0" },
                        fontFamily = FrauncesFamily, fontSize = 72.sp, fontWeight = FontWeight.Medium,
                        letterSpacing = (-2).sp, color = Color(0xFF0A2407),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { v ->
                        val selected = budgetText == v.replace(",", "")
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(if (selected) OnSurface else Color.White.copy(alpha = 0.55f))
                                .clickable { budgetText = v.replace(",", "") }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(v, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Surface else Color(0xFF1F4D1B), fontFamily = SatoshiFamily)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Or type your own") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val budgetAmount = budgetText.toDoubleOrNull() ?: 0.0
        if (budgetAmount > 0) {
            // Split toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { useSplit = !useSplit }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Split across categories", fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = SatoshiFamily)
                    Text("We'll create budgets for Food, Transport, etc.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = SatoshiFamily)
                }
                Switch(
                    checked = useSplit,
                    onCheckedChange = { useSplit = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary),
                )
            }

            // Suggested split breakdown
            if (useSplit) {
                Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        BUDGET_SPLITS.forEachIndexed { i, (_, _, pct) ->
                            Box(modifier = Modifier.weight(pct.toFloat()).height(14.dp).background(SPLIT_COLORS[i]))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BUDGET_SPLITS.forEachIndexed { i, (_, name, pct) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SPLIT_COLORS[i]))
                                    Text(name, fontSize = 13.sp, fontFamily = SatoshiFamily)
                                }
                                Text(
                                    "GH₵${String.format(java.util.Locale.US, "%.0f", budgetAmount * pct)}",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFamily,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            PrimaryCta("Set budget", icon = null) {
                if (budgetAmount > 0 && useSplit) {
                    scope.launch {
                        val db = TrackittDatabase.getInstance(context)
                        BUDGET_SPLITS.forEach { (category, _, _) ->
                            db.budgetDao().deleteByCategory(category)
                        }
                        BUDGET_SPLITS.forEach { (category, _, pct) ->
                            db.budgetDao().upsert(BudgetEntity(category = category, amount = budgetAmount * pct))
                        }
                    }
                }
                onSet()
            }
            Spacer(Modifier.height(6.dp))
            GhostCta("Skip; I'll decide later", onClick = onSkip)
        }
    }
}

// ── 07 Backfilling ──────────────────────────────────────────

@Composable
private fun BackfillingStep(onDone: () -> Unit) {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("trackitt", 0)
        prefs.edit()
            .putBoolean("backfill_completed", false)
            .putInt("backfill_days", 30)
            .apply()
        val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>()
            .setInputData(workDataOf("lookback_days" to 30))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_backfill", ExistingWorkPolicy.REPLACE, request,
        )

        while (!finished) {
            delay(1000)
            val infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("sms_backfill").get()
            val info = infos.firstOrNull()
            if (info?.state == WorkInfo.State.SUCCEEDED || info?.state == WorkInfo.State.FAILED) {
                finished = true
            }
            count++
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            delay(1500)
            onDone()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        OnboardingTopBar(stepIndex = 5, onBack = null)
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            MicroLabel("Reading your SMS")
            Spacer(Modifier.height(10.dp))
            DisplayText("Catching up on", size = 44)
            DisplayText("last 30 days…", size = 44)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CookieIcon(fillColor = PrimaryContainer, size = 120.dp)
                if (finished) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = OnSurface, modifier = Modifier.size(36.dp))
                } else {
                    Text(
                        if (count > 0) "…" else "",
                        fontFamily = FrauncesFamily, fontSize = 36.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (finished) "Done!" else "Scanning messages…",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SatoshiFamily,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (finished) 1f else (count * 0.08f).coerceAtMost(0.9f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OnSurface),
                )
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            Text(
                "Keep the app open until this finishes. It only happens once.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SatoshiFamily, lineHeight = 18.sp,
            )
        }
    }
}

// ── 08 Ready ────────────────────────────────────────────────

@Composable
private fun ReadyStep(onOpen: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .background(PrimaryContainer),
        ) {
            OrganicBlob(
                color = Color(0xFFA6E098),
                modifier = Modifier.align(Alignment.BottomEnd).size(380.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 24.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Box {
                    CookieIcon(fillColor = OnSurface, size = 120.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Surface),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
                    }
                }
            }

            Spacer(Modifier.height(64.dp))
            MicroLabel("All set", color = Color(0xFF0A2407))
            Spacer(Modifier.height(14.dp))
            DisplayText("You're", size = 52)
            DisplayText("tracking.", size = 52)
            Spacer(Modifier.height(16.dp))
            Text(
                "From now on, every new MoMo SMS lands in trackitt automatically.",
                fontSize = 15.sp, color = Color(0xFF1F4D1B), lineHeight = 21.sp, fontFamily = SatoshiFamily,
            )

            Spacer(Modifier.weight(1f))
            PrimaryCta("Open trackitt", icon = Icons.Filled.ArrowForward, onClick = onOpen)
        }
    }
}
