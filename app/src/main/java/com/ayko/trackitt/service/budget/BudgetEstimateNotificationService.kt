package com.ayko.trackitt.service.budget

import com.ayko.trackitt.service.security.privateOnLockScreen
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ayko.trackitt.MainActivity
import com.ayko.trackitt.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class BudgetEstimateNotificationService(
    private val context: Context,
    private val estimateService: BudgetEstimateService,
    private val prefs: android.content.SharedPreferences,
) {

    suspend fun notifyForTodayIfNeeded(date: LocalDate = LocalDate.now()) {
        if (date.dayOfMonth != 1) return

        val monthKey = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
        if (prefs.getString(KEY_LAST_NOTIFIED_MONTH, null) == monthKey) return

        val estimate = estimateService.estimateForMonth(date = date, forceShow = true) ?: return
        if (showNotification(date, estimate)) {
            prefs.edit().putString(KEY_LAST_NOTIFIED_MONTH, monthKey).apply()
        }
    }

    private fun showNotification(date: LocalDate, estimate: BudgetEstimate): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget estimates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Starting budget estimates for each month"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Estimated budget for $monthName")
            .setContentText("Start ${monthName.lowercase(Locale.getDefault())} with ${money(estimate.amount)}. ${estimate.summary}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your starting budget estimate for $monthName ${date.year} is ${money(estimate.amount)}. ${estimate.summary}"
                )
            )
            .setContentIntent(buildLaunchAppPendingIntent())
            .setAutoCancel(true)
            .privateOnLockScreen(context, CHANNEL_ID, "Budget estimate ready")
            .build()

        notificationManager.notify(BUDGET_ESTIMATE_NOTIFICATION_ID, notification)
        return true
    }

    private fun buildLaunchAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun money(amount: Double): String {
        return "GH₵${String.format(Locale.US, "%,.2f", amount)}"
    }

    companion object {
        private const val CHANNEL_ID = "trackitt_budget_estimates"
        private const val BUDGET_ESTIMATE_NOTIFICATION_ID = 2105
        private const val KEY_LAST_NOTIFIED_MONTH = "last_notified_budget_estimate_month"
    }
}
