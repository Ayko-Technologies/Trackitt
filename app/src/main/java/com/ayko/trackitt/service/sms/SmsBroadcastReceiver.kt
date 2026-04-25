package com.ayko.trackitt.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ayko.trackitt.TrackittApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                    val parsed = parser.parse(body)
                    if (parsed != null) {
                        repository.insertFromSms(parsed)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
