package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.UiStep
import com.example.ui.components.SubtleMonochromeParticles
import com.example.ui.screens.EngineProcessingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StorageSetupScreen
import com.example.ui.theme.MaxoDeepBlack
import com.example.ui.theme.MaxoTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaxoTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaxoDeepBlack)
                ) {
                    // Subtle background particles
                    SubtleMonochromeParticles()

                    when (state.currentStep) {
                        UiStep.STORAGE_SETUP -> {
                            StorageSetupScreen(
                                storageReady = state.storageReady,
                                displayPath = state.workspaceDisplayPath,
                                errorMessage = state.errorMessage,
                                onFolderSelected = { uri -> viewModel.onFolderSelected(uri) },
                                onContinue = { viewModel.confirmStorageSetup() }
                            )
                        }
                        UiStep.HOME -> {
                            HomeScreen(
                                workspaceDisplayPath = state.workspaceDisplayPath,
                                onNavigateEngine = { engine -> viewModel.navigateToEngine(engine) },
                                onChangeFolder = { viewModel.checkInitialStorage() }
                            )
                        }
                        UiStep.DPT_SCREEN, UiStep.UNLOCK_SCREEN -> {
                            EngineProcessingScreen(
                                engineType = state.activeEngine,
                                selectedApk = state.selectedApk,
                                operationStatus = state.operationStatus,
                                errorMessage = state.errorMessage,
                                onBack = { viewModel.navigateToHome() },
                                onSelectApk = { uri -> viewModel.onApkSelected(uri) },
                                onStartProcessing = { viewModel.startProcessing() },
                                onReset = { viewModel.resetProcessing() }
                            )
                        }
                    }
                }
            }
        }
    }
}
