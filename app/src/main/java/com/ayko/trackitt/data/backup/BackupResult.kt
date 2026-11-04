package com.ayko.trackitt.data.backup

sealed interface BackupResult {
    data object Success : BackupResult
    data class Failure(val message: String) : BackupResult
}
