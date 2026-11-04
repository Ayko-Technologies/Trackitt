package com.ayko.trackitt.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.ayko.trackitt.data.local.TrackittDatabase
import java.io.File

/** Copies the Room database out for a backup, and swaps a validated backup back in. */
object DatabaseSnapshot {

    private const val DB_NAME = "trackitt.db"
    private const val SQLITE_HEADER = "SQLite format 3\u0000"

    /** A consistent single-file copy of the database, written to the cache directory. */
    fun create(context: Context): File {
        val db = TrackittDatabase.getInstance(context).openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").close()
        val snapshot = File(context.cacheDir, "trackitt-snapshot.db")
        context.getDatabasePath(DB_NAME).copyTo(snapshot, overwrite = true)
        return snapshot
    }

    /**
     * Cuts a backup back to the size SQLite's header records. A storage app that didn't truncate on
     * overwrite (Google Drive sometimes doesn't) can leave an older, longer backup's tail behind.
     */
    fun trimToRecordedSize(file: File) {
        if (file.length() < 100) return
        java.io.RandomAccessFile(file, "rw").use { raf ->
            val header = ByteArray(100).also { raf.readFully(it) }
            fun int(at: Int) = java.nio.ByteBuffer.wrap(header, at, 4).int.toLong() and 0xffffffffL
            val pageSize = ((header[16].toInt() and 0xff) shl 8 or (header[17].toInt() and 0xff))
                .let { if (it == 1) 65536 else it }
            val pageCount = int(28)
            // The page count is only trustworthy when it was written alongside the change counter.
            if (pageSize == 0 || pageCount == 0L || int(92) != int(24)) return
            val size = pageSize * pageCount
            if (raf.length() > size) raf.setLength(size)
        }
    }

    /** True when the file is a Trackitt database this version of the app can open. */
    fun isRestorable(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_HEADER.length) return false
        val header = file.inputStream().use { input -> ByteArray(SQLITE_HEADER.length).also { input.read(it) } }
        if (String(header, Charsets.US_ASCII) != SQLITE_HEADER) return false

        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val version = db.version
                val hasTransactions = db.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'transactions'", null,
                ).use { it.moveToFirst() }
                // Trackitt never creates triggers or views; a backup that has them was tampered with.
                val hasCode = db.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type IN ('trigger', 'view')", null,
                ).use { it.moveToFirst() }
                hasTransactions && !hasCode && version in TrackittDatabase.MIN_MIGRATABLE_VERSION..TrackittDatabase.VERSION
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Replaces the live database with [file]. The current database is kept as trackitt.db.before-restore
     * so a bad restore can be undone. The app must restart afterwards.
     */
    fun replaceWith(context: Context, file: File) {
        require(isRestorable(file)) { "Not a Trackitt backup" }
        val dbFile = context.getDatabasePath(DB_NAME)
        TrackittDatabase.getInstance(context).openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)").close()
        TrackittDatabase.closeInstance()

        if (dbFile.exists()) dbFile.copyTo(File(dbFile.path + ".before-restore"), overwrite = true)
        file.copyTo(dbFile, overwrite = true)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }
}
