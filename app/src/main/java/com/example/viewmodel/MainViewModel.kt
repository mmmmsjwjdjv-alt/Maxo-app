package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.processor.LocalApkProcessor
import com.example.storage.StorageManager
import com.example.validation.ApkValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val currentStep: UiStep = UiStep.STORAGE_SETUP,
    val storageReady: Boolean = false,
    val workspaceDisplayPath: String = "",
    val activeEngine: ProcessingEngineType = ProcessingEngineType.DPT,
    val selectedApk: SelectedApkInfo? = null,
    val operationStatus: OperationStatus = OperationStatus.Idle,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = StorageManager(application)
    private val localProcessor = LocalApkProcessor(application, storageManager)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkInitialStorage()
    }

    fun checkInitialStorage() {
        val isReady = storageManager.isStorageInitialized()
        val displayPath = storageManager.getDisplayPath()
        _uiState.update {
            it.copy(
                storageReady = isReady,
                workspaceDisplayPath = displayPath,
                currentStep = if (isReady) UiStep.HOME else UiStep.STORAGE_SETUP
            )
        }
    }

    fun onFolderSelected(uri: Uri) {
        val permGranted = storageManager.takePersistablePermissions(uri)
        if (permGranted) {
            val (initSuccess, pathName) = storageManager.verifyAndInitDirectories(uri)
            if (initSuccess) {
                storageManager.saveTreeUri(uri)
                _uiState.update {
                    it.copy(
                        storageReady = true,
                        workspaceDisplayPath = pathName,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        storageReady = false,
                        errorMessage = "Cannot initialize MAXO directories in selected folder: $pathName"
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    storageReady = false,
                    errorMessage = "Failed to secure read/write permission for selected directory."
                )
            }
        }
    }

    fun confirmStorageSetup() {
        if (_uiState.value.storageReady) {
            _uiState.update { it.copy(currentStep = UiStep.HOME) }
        }
    }

    fun navigateToEngine(engine: ProcessingEngineType) {
        _uiState.update {
            it.copy(
                activeEngine = engine,
                currentStep = if (engine == ProcessingEngineType.DPT) UiStep.DPT_SCREEN else UiStep.ONLOCK_SCREEN,
                selectedApk = null,
                operationStatus = OperationStatus.Idle,
                errorMessage = null
            )
        }
    }

    fun navigateToHome() {
        _uiState.update {
            it.copy(
                currentStep = UiStep.HOME,
                selectedApk = null,
                operationStatus = OperationStatus.Idle,
                errorMessage = null
            )
        }
    }

    fun onApkSelected(uri: Uri) {
        val info = ApkValidator.validateAndExtractInfo(getApplication(), uri)
        if (info != null) {
            _uiState.update {
                it.copy(
                    selectedApk = info,
                    operationStatus = OperationStatus.Idle,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedApk = null,
                    errorMessage = "The selected file is not a valid APK."
                )
            }
        }
    }

    fun startProcessing() {
        val apk = _uiState.value.selectedApk ?: return
        val engine = _uiState.value.activeEngine

        viewModelScope.launch {
            _uiState.update { it.copy(operationStatus = OperationStatus.Preparing) }
            val finalStatus = localProcessor.executeProcessing(
                engineType = engine,
                sourceUri = apk.uri,
                originalFileName = apk.fileName,
                originalSize = apk.sizeBytes,
                onProgress = { progressStatus ->
                    _uiState.update { it.copy(operationStatus = progressStatus) }
                }
            )
            _uiState.update { it.copy(operationStatus = finalStatus) }
        }
    }

    fun resetProcessing() {
        _uiState.update {
            it.copy(
                selectedApk = null,
                operationStatus = OperationStatus.Idle,
                errorMessage = null
            )
        }
    }
}
