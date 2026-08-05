package com.ayko.trackitt.service.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.ayko.trackitt.R
import com.ayko.trackitt.MainActivity
import com.ayko.trackitt.TrackittApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val grouped = messages
            .groupBy { it.originatingAddress }
            .map { (sender, parts) ->
                Triple(
                    sender,
                    parts.joinToString("") { it.messageBody ?: "" },
                    parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis(),
                )
            }

        val app = context.applicationContext as? TrackittApplication ?: return
        val container = app.container

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                grouped.forEach { (sender, body, smsTimestamp) ->
                    val parsed = container.parseSms(sender, body, smsTimestamp) ?: return@forEach
                    container.repository.insertFromSms(parsed)
                    showNotification(context, parsed)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, parsed: ParsedTransaction) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transactions",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "New transactions from SMS" }
            notificationManager.createNotificationChannel(channel)
        }

        val amountFormatted = String.format(Locale.US, "%.2f", parsed.amount)
        val title = if (parsed.isIncome) "GH₵$amountFormatted received" else "GH₵$amountFormatted spent"
        val text = "${if (parsed.isIncome) "From" else "To"} ${parsed.merchant}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(buildLaunchAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(parsed.transactionId.hashCode(), notification)
    }

    private fun buildLaunchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val CHANNEL_ID = "trackitt_transactions"
    }
}
