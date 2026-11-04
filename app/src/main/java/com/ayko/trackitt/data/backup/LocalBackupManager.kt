package com.ayko.trackitt.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Restoring from a backup file the user picks, wherever it is (Google Drive, phone storage, USB). */
class LocalBackupManager(private val context: Context) {

    fun suggestedFileName(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        return "trackitt-backup-$timestamp.db"
    }

    /** Swaps in a backup file. The app must restart after a [BackupResult.Success]. */
    suspend fun import(source: Uri): BackupResult = withContext(Dispatchers.IO) {
        val copy = File(context.cacheDir, "import.db")
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: return@withContext BackupResult.Failure("Couldn't open that file")
            input.use { inp -> copy.outputStream().use { inp.copyTo(it) } }
            DatabaseSnapshot.trimToRecordedSize(copy)
            if (!DatabaseSnapshot.isRestorable(copy)) {
                return@withContext BackupResult.Failure("That file isn't a Trackitt backup this version can read")
            }
            DatabaseSnapshot.replaceWith(context, copy)
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Import failed")
        } finally {
            copy.delete()
        }
    }
}
