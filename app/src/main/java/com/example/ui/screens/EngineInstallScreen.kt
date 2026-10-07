package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineInstallStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun EngineInstallScreen(
    status: EngineInstallStatus,
    onStartInstall: () -> Unit,
    onContinue: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = MaxoTextWhite,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "CORE ENGINE SETUP",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaxoTextWhite
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "نصب و آماده‌سازی فایل‌های موتور DPT و UNLOCK برای استفاده دائمی و آفلاین (Termux Environment)",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaxoTextSecondary,
                lineHeight = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (status) {
                    is EngineInstallStatus.NotInstalled -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaxoTextWhite,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "پکیج‌های ابزار DPT و آنپکر هنوز نصب نشده‌اند.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaxoTextWhite,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "برای شروع کار با یک کلیک فایل‌ها را راه‌اندازی کنید.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaxoTextMuted)
                                )
                            }
                        }
                    }
                    is EngineInstallStatus.Installing -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = status.currentStep,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaxoTextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { status.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaxoTextWhite,
                                trackColor = MaxoBorderGlass,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "TERMINAL LOGS:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaxoTextMuted,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(state = listState) {
                                    items(status.logs) { log ->
                                        Text(
                                            text = "> $log",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaxoTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is EngineInstallStatus.Ready -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "✓ تمام فایل‌ها نصب شدند!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaxoTextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "محیط آفلاین DPT و UNLOCK آماده پردازش است.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaxoTextSecondary)
                                )
                            }
                        }
                    }
                    is EngineInstallStatus.Failed -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "خطا در نصب فایل‌ها:",
                                    style = MaterialTheme.typography.titleMedium.copy(color = MaxoAccentRed)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = status.error,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaxoTextWhite)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (status) {
            is EngineInstallStatus.NotInstalled, is EngineInstallStatus.Failed -> {
                Button(
                    onClick = onStartInstall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("install_engine_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaxoTextWhite,
                        contentColor = MaxoPureBlack
                    )
                ) {
                    Text(
                        text = "نصب و راه‌اندازی ابزارها (INSTALL)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
            is EngineInstallStatus.Installing -> {
                // In progress
            }
            is EngineInstallStatus.Ready -> {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_to_home_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaxoTextWhite,
                        contentColor = MaxoPureBlack
                    )
                ) {
                    Text(
                        text = "ورود به محیط برنامه (CONTINUE)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
