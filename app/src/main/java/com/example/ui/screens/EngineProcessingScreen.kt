package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.OperationStatus
import com.example.model.ProcessingEngineType
import com.example.model.SelectedApkInfo
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun EngineProcessingScreen(
    engineType: ProcessingEngineType,
    selectedApk: SelectedApkInfo?,
    operationStatus: OperationStatus,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelectApk: (Uri) -> Unit,
    onStartProcessing: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    // APK document picker
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectApk(uri)
        }
    }

    val title = if (engineType == ProcessingEngineType.DPT) {
        stringResource(R.string.dpt_title)
    } else {
        stringResource(R.string.onlock_title)
    }

    val desc = if (engineType == ProcessingEngineType.DPT) {
        stringResource(R.string.dpt_screen_desc)
    } else {
        stringResource(R.string.onlock_screen_desc)
    }

    val destinationFolder = if (engineType == ProcessingEngineType.DPT) "DPT/" else "ONLOCK/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaxoTextWhite
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaxoTextWhite
                    )
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaxoTextMuted,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic content based on operation status
        when (operationStatus) {
            is OperationStatus.Idle -> {
                IdleFileSelectionSection(
                    selectedApk = selectedApk,
                    destinationFolder = destinationFolder,
                    errorMessage = errorMessage,
                    onPickFile = { apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) },
                    onProcess = onStartProcessing
                )
            }
            is OperationStatus.Preparing -> {
                ProcessingProgressSection(progress = 5, message = stringResource(R.string.step_preparing), logs = emptyList())
            }
            is OperationStatus.Processing -> {
                ProcessingProgressSection(
                    progress = operationStatus.progressPercent,
                    message = operationStatus.stageMessage,
                    logs = operationStatus.detailLogs
                )
            }
            is OperationStatus.Success -> {
                SuccessSection(
                    success = operationStatus,
                    onShareOutput = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.android.package-archive"
                            putExtra(Intent.EXTRA_STREAM, operationStatus.outputUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Processed APK"))
                    },
                    onProcessAnother = onReset
                )
            }
            is OperationStatus.Failed -> {
                FailedSection(
                    errorMessage = operationStatus.errorMessage,
                    errorDetail = operationStatus.errorDetail,
                    onRetry = onReset
                )
            }
        }
    }
}

@Composable
private fun IdleFileSelectionSection(
    selectedApk: SelectedApkInfo?,
    destinationFolder: String,
    errorMessage: String?,
    onPickFile: () -> Unit,
    onProcess: () -> Unit
) {
    Column {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("selection_glass_card")
        ) {
            Text(
                text = "INPUT PACKAGE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaxoTextMuted
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedApk == null) {
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("select_apk_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaxoBorderGlass)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaxoTextWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Select File",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.select_apk_button),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            } else {
                // File info box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaxoElevatedBlack)
                        .border(1.dp, MaxoBorderGlass, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedApk.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaxoTextWhite,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedApk.readableSize,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaxoAccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Destination:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaxoTextMuted)
                        )
                        Text(
                            text = destinationFolder,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaxoTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFile,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("change_apk_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaxoBorderGlass)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaxoTextSecondary
                        )
                    ) {
                        Text("CHANGE FILE", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onProcess,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("process_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaxoTextWhite,
                            contentColor = MaxoPureBlack
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.process_button),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaxoAccentRed)
                )
            }
        }
    }
}

@Composable
private fun ProcessingProgressSection(
    progress: Int,
    message: String,
    logs: List<String>
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("processing_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.processing_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaxoTextWhite
                )
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaxoTextWhite
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaxoTextWhite,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaxoTextSecondary
            )
        )

        if (logs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaxoElevatedBlack)
                    .padding(12.dp)
            ) {
                LazyColumn {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaxoTextMuted,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessSection(
    success: OperationStatus.Success,
    onShareOutput: () -> Unit,
    onProcessAnother: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("success_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaxoAccentGreen,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.process_complete_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaxoAccentGreen
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaxoElevatedBlack)
                .padding(14.dp)
        ) {
            Text(
                text = "Output File:",
                style = MaterialTheme.typography.labelSmall.copy(color = MaxoTextMuted)
            )
            Text(
                text = success.outputFileName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaxoTextWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Saved To:",
                style = MaterialTheme.typography.labelSmall.copy(color = MaxoTextMuted)
            )
            Text(
                text = "${success.destinationFolder}/${success.outputFileName}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaxoTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = success.statsMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaxoAccentGreen,
                    fontFamily = FontFamily.Monospace
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onShareOutput,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_output_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaxoTextWhite,
                contentColor = MaxoPureBlack
            )
        ) {
            Text(
                text = stringResource(R.string.open_output_button),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onProcessAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("process_another_button"),
            shape = RoundedCornerShape(10.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaxoBorderGlass)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaxoTextSecondary
            )
        ) {
            Text(
                text = stringResource(R.string.process_another_button),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun FailedSection(
    errorMessage: String,
    errorDetail: String?,
    onRetry: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("failed_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Failed",
                tint = MaxoAccentRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.error_processing_failed),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaxoAccentRed
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaxoTextWhite
            )
        )

        if (errorDetail != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaxoElevatedBlack)
                    .padding(10.dp)
            ) {
                Text(
                    text = errorDetail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaxoTextMuted
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("retry_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaxoTextWhite,
                contentColor = MaxoPureBlack
            )
        ) {
            Text(
                text = stringResource(R.string.retry_button),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
