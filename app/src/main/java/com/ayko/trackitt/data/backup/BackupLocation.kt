package com.ayko.trackitt.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.security.MessageDigest

/**
 * The one backup file the user picked with Android's file picker: in Google Drive, on the phone, on
 * an SD card, anywhere a storage app offers. Trackitt keeps permission to that file and rewrites it
 * for each backup. Choosing Google Drive needs no Google sign-in in Trackitt; the Drive app uploads it.
 */
object BackupLocation {

    private const val PREFS = "backup"
    private const val KEY_URI = "location_uri"
    private const val KEY_NAME = "location_name"
    private const val KEY_LAST_SUCCESS = "last_success"
    private const val KEY_LAST_ERROR = "last_error"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, 0)

    fun uri(context: Context): Uri? = prefs(context).getString(KEY_URI, null)?.let(Uri::parse)

    fun name(context: Context): String? = prefs(context).getString(KEY_NAME, null)

    fun lastSuccess(context: Context): Long = prefs(context).getLong(KEY_LAST_SUCCESS, 0L)

    fun lastError(context: Context): String? = prefs(context).getString(KEY_LAST_ERROR, null)

    /** Remembers a file the user just created with the picker, keeping access to it across restarts. */
    fun choose(context: Context, uri: Uri) {
        val resolver = context.contentResolver
        uri(context)?.takeIf { it != uri }?.let { old ->
            try {
                resolver.releasePersistableUriPermission(old, READ_WRITE)
            } catch (_: SecurityException) {
                // Already gone.
            }
        }
        try {
            resolver.takePersistableUriPermission(uri, READ_WRITE)
        } catch (_: SecurityException) {
            // Some storage apps only allow this one write; automatic backups will ask to choose again.
        }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        prefs(context).edit()
            .putString(KEY_URI, uri.toString())
            .putString(KEY_NAME, name ?: "your backup file")
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    /** Writes a fresh backup to the chosen file and checks it reads back intact. */
    suspend fun backUp(context: Context): BackupResult = withContext(Dispatchers.IO) {
        val target = uri(context) ?: return@withContext BackupResult.Failure("Choose where to keep your backup first")
        val result = try {
            writeVerified(context, target)
            BackupResult.Success
        } catch (_: SecurityException) {
            BackupResult.Failure(LOST_ACCESS)
        } catch (_: FileNotFoundException) {
            BackupResult.Failure(LOST_ACCESS)
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Backup failed")
        }
        record(context, result)
        result
    }

    /** One-off copy to a file the user picked, without changing the backup location. */
    suspend fun saveCopy(context: Context, destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            writeVerified(context, destination)
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Couldn't save the backup there")
        }
    }

    private fun writeVerified(context: Context, target: Uri) {
        val snapshot = DatabaseSnapshot.create(context)
        try {
            val bytes = snapshot.readBytes()
            val resolver = context.contentResolver
            // "wt" truncates; plain "w" may leave an older, longer backup's tail in place.
            val output = resolver.openOutputStream(target, "wt") ?: throw FileNotFoundException()
            output.use { it.write(bytes) }

            // Read it back: some storage apps don't truncate even with "wt". Extra trailing bytes are
            // harmless (restores trim them), but the backup itself must be byte-for-byte correct.
            val written = resolver.openInputStream(target)?.use { it.readBytes() } ?: throw FileNotFoundException()
            if (written.size < bytes.size || !sha256(written, bytes.size).contentEquals(sha256(bytes, bytes.size))) {
                throw IllegalStateException("The backup didn't save correctly. Try again, or choose another place.")
            }
        } finally {
            snapshot.delete()
        }
    }

    private fun record(context: Context, result: BackupResult) {
        val editor = prefs(context).edit()
        when (result) {
            BackupResult.Success -> editor.putLong(KEY_LAST_SUCCESS, System.currentTimeMillis()).remove(KEY_LAST_ERROR)
            is BackupResult.Failure -> editor.putString(KEY_LAST_ERROR, result.message)
        }
        editor.apply()
    }

    private fun sha256(bytes: ByteArray, length: Int): ByteArray =
        MessageDigest.getInstance("SHA-256").apply { update(bytes, 0, length) }.digest()

    const val LOST_ACCESS = "Trackitt can no longer reach your backup file. Choose where to keep it again."

    private const val READ_WRITE = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}
