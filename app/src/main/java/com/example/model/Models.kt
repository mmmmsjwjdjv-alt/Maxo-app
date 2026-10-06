package com.example.model

import android.net.Uri

enum class ProcessingEngineType {
    DPT,
    ONLOCK
}

enum class UiStep {
    STORAGE_SETUP,
    HOME,
    DPT_SCREEN,
    ONLOCK_SCREEN
}

sealed interface OperationStatus {
    object Idle : OperationStatus
    object Preparing : OperationStatus
    data class Processing(
        val progressPercent: Int,
        val stageMessage: String,
        val detailLogs: List<String> = emptyList()
    ) : OperationStatus
    data class Success(
        val outputFileName: String,
        val outputUri: Uri,
        val originalSize: Long,
        val finalSize: Long,
        val destinationFolder: String,
        val statsMessage: String
    ) : OperationStatus
    data class Failed(
        val errorMessage: String,
        val errorDetail: String? = null
    ) : OperationStatus
}

data class SelectedApkInfo(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val readableSize: String,
    val packageName: String? = null,
    val isApkValid: Boolean = true
)

data class StorageSetupInfo(
    val treeUriString: String?,
    val displayPath: String?,
    val isReady: Boolean = false
)
