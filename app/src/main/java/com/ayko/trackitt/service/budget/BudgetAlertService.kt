package com.ayko.trackitt.service.budget

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
import com.ayko.trackitt.data.local.dao.BudgetDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.TransactionCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class BudgetAlertService(
    private val context: Context,
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val evaluator: BudgetAlertEvaluator = BudgetAlertEvaluator(),
) {

    suspend fun onInserted(transaction: TransactionEntity) {
        evaluateChange(before = null, after = transaction.toContribution())
    }

    suspend fun onUpdated(before: TransactionEntity?, after: TransactionEntity?) {
        evaluateChange(before = before.toContribution(), after = after.toContribution())
    }

    private suspend fun evaluateChange(
        before: BudgetAlertContribution?,
        after: BudgetAlertContribution?,
    ) {
        evaluator.impactedKeys(before, after)
            .filter(::isCurrentMonth)
            .forEach { key ->
                val budget = budgetDao.getByCategory(key.category) ?: return@forEach
                if (budget.amount <= 0.0) return@forEach

                val currentTotal = transactionDao.getCategoryMonthTotal(
                    category = key.category,
                    year = key.year.toString(),
                    month = key.month.toString().padStart(2, '0'),
                )
                val previousTotal = evaluator.previousTotalForKey(key, currentTotal, before, after)

                if (evaluator.crossedBudget(previousTotal, currentTotal, budget.amount)) {
                    showNotification(
                        key = key,
                        budgetAmount = budget.amount,
                        currentTotal = currentTotal,
                    )
                }
            }
    }

    private fun showNotification(
        key: BudgetAlertKey,
        budgetAmount: Double,
        currentTotal: Double,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when you go over a budget"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val category = runCatching { TransactionCategory.valueOf(key.category) }.getOrDefault(TransactionCategory.OTHER)
        val monthName = LocalDate.of(key.year, key.month, 1)
            .month
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
        val overBy = currentTotal - budgetAmount

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentTitle("${category.label} budget exceeded")
            .setContentText(
                "Spent ${money(currentTotal)} of ${money(budgetAmount)} in $monthName. Over by ${money(overBy)}."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${category.label} has gone over budget for $monthName. " +
                        "You've spent ${money(currentTotal)} out of ${money(budgetAmount)}, " +
                        "which is ${money(overBy)} over your limit."
                )
            )
            .setContentIntent(buildLaunchAppPendingIntent())
            .setAutoCancel(true)
            .build()

        notificationManager.notify("${key.year}-${key.month}-${key.category}".hashCode(), notification)
    }

    private fun buildLaunchAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun TransactionEntity?.toContribution(): BudgetAlertContribution? {
        if (this == null || isIncome) return null
        val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
        return BudgetAlertContribution(
            key = BudgetAlertKey(
                year = localDate.year,
                month = localDate.monthValue,
                category = category,
            ),
            amount = amount,
        )
    }

    private fun isCurrentMonth(key: BudgetAlertKey): Boolean {
        val now = LocalDate.now()
        return now.year == key.year && now.monthValue == key.month
    }

    private fun money(amount: Double): String {
        return "GH₵${String.format(Locale.US, "%.2f", amount)}"
    }

    companion object {
        private const val CHANNEL_ID = "trackitt_budget_alerts"
    }
}
