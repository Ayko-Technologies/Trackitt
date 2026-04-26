package com.ayko.trackitt.service.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.ayko.trackitt.R
import com.ayko.trackitt.TrackittApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages
            .groupBy { it.originatingAddress }
            .map { (_, parts) -> parts.joinToString("") { it.messageBody ?: "" } }

        val app = context.applicationContext as? TrackittApplication ?: return
        val parser = app.container.smsParser
        val repository = app.container.repository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fullBody.forEach { body ->
                    val parsed = parser.parse(body) ?: return@forEach
                    repository.insertFromSms(parsed)
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
            ).apply { description = "New transactions from MoMo SMS" }
            notificationManager.createNotificationChannel(channel)
        }

        val amountFormatted = String.format(Locale.US, "%.2f", parsed.amount)
        val direction = if (parsed.isIncome) "received from" else "to"
        val title = if (parsed.isIncome) "GH₵$amountFormatted received" else "GH₵$amountFormatted spent"
        val text = "${if (parsed.isIncome) "From" else "To"} ${parsed.merchant}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(parsed.transactionId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "trackitt_transactions"
    }
}
