package com.ayko.trackitt.service.security

import android.content.Context
import androidx.core.app.NotificationCompat
import com.ayko.trackitt.R

/**
 * Keeps amounts and names off the lock screen: there the notification shows only [publicTitle];
 * the full text appears once the phone is unlocked (when the user hides sensitive lock-screen content).
 */
fun NotificationCompat.Builder.privateOnLockScreen(
    context: Context,
    channelId: String,
    publicTitle: String,
): NotificationCompat.Builder =
    setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(publicTitle)
            .setContentText("Unlock to see details")
            .build()
    )
