package com.ayko.trackitt.data.backup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.ayko.trackitt.data.local.TrackittDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriveBackupManager(private val context: Context) {

    private fun getDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccountName = accountName }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Trackitt").build()
    }

    suspend fun backup(accountName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("trackitt.db")
            if (!dbFile.exists()) return@withContext false

            TrackittDatabase.getInstance(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)").close()

            val timestamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
            val fileName = "trackitt-backup-$timestamp.db"

            val drive = getDriveService(accountName)
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }
            val mediaContent = FileContent("application/octet-stream", dbFile)
            drive.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listBackups(accountName: String): List<BackupFile> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(accountName)
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, size, createdTime)")
                .setOrderBy("createdTime desc")
                .execute()
            result.files?.map { file ->
                BackupFile(file.id, file.name, file.getSize()?.toLong() ?: 0, file.createdTime?.value ?: 0)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun restore(accountName: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(accountName)
            val dbFile = context.getDatabasePath("trackitt.db")

            TrackittDatabase.getInstance(context).close()

            val tempFile = File(context.cacheDir, "restore.db")
            FileOutputStream(tempFile).use { out ->
                drive.files().get(fileId).executeMediaAndDownloadTo(out)
            }

            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            true
        } catch (_: Exception) {
            false
        }
    }
}

data class BackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: Long,
)
