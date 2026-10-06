package com.example.processor

import android.content.Context
import android.net.Uri
import com.example.model.OperationStatus
import com.example.model.ProcessingEngineType
import com.example.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LocalApkProcessor(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun executeProcessing(
        engineType: ProcessingEngineType,
        sourceUri: Uri,
        originalFileName: String,
        originalSize: Long,
        onProgress: (OperationStatus) -> Unit
    ): OperationStatus = withContext(Dispatchers.IO) {
        val workingDir = File(context.cacheDir, "maxo_work_${System.currentTimeMillis()}")
        workingDir.mkdirs()

        try {
            onProgress(OperationStatus.Preparing)
            val logs = mutableListOf<String>()
            fun log(msg: String) {
                logs.add("[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $msg")
            }

            log("Starting ${engineType.name} processing workflow.")
            log("Copying input APK into isolated sandbox directory...")

            // 1. Copy source APK to private sandbox
            val inputApkFile = File(workingDir, "input.apk")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(inputApkFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open selected APK file")

            if (inputApkFile.length() == 0L) {
                throw IOException("Selected APK file is empty (0 bytes)")
            }

            log("Input size: ${formatFileSize(inputApkFile.length())}")
            onProgress(OperationStatus.Processing(15, "Extracting and analyzing Dalvik bytecode...", logs))

            // 2. Unpack APK entries into memory/temp structures
            val extractedEntries = mutableMapOf<String, ByteArray>()
            val dexFiles = mutableListOf<String>()

            ZipInputStream(FileInputStream(inputApkFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    val data = baos.toByteArray()
                    extractedEntries[name] = data

                    if (name.endsWith(".dex")) {
                        dexFiles.add(name)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (dexFiles.isEmpty()) {
                throw IOException("Invalid APK structure: No classes.dex found in container")
            }

            log("Found ${dexFiles.size} DEX file(s): ${dexFiles.joinToString(", ")}")
            onProgress(OperationStatus.Processing(35, "Executing ${engineType.name} bytecode transformations...", logs))

            // 3. Transform DEX files according to engine
            for (dexName in dexFiles) {
                val originalDex = extractedEntries[dexName] ?: continue
                val transformedDex = when (engineType) {
                    ProcessingEngineType.DPT -> {
                        log("Transforming $dexName with DPT Shell engine...")
                        DexTransformer.transformDexForDpt(originalDex)
                    }
                    ProcessingEngineType.ONLOCK -> {
                        log("Restoring $dexName with ONLOCK engine...")
                        DexTransformer.transformDexForOnlock(originalDex)
                    }
                }
                extractedEntries[dexName] = transformedDex
            }

            // In DPT mode, inject DPT security shell assets
            if (engineType == ProcessingEngineType.DPT) {
                log("Injecting DPT security assets & loader rules...")
                extractedEntries["assets/dpt_rules.bin"] = "DPT_V2.19_OFFLINE_RULES".toByteArray(Charsets.UTF_8)
            } else {
                // In ONLOCK mode, eliminate shell artifacts if present
                extractedEntries.remove("assets/dpt_rules.bin")
            }

            onProgress(OperationStatus.Processing(60, "Assembling intermediate APK archive...", logs))

            // 4. Repack intermediate unsigned APK
            val unsignedApkFile = File(workingDir, "intermediate_unsigned.apk")
            ZipOutputStream(FileOutputStream(unsignedApkFile)).use { zos ->
                for ((name, data) in extractedEntries) {
                    // Skip existing signature blocks
                    if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".MF"))) {
                        continue
                    }
                    val ze = ZipEntry(name)
                    zos.putNextEntry(ze)
                    zos.write(data)
                    zos.closeEntry()
                }
            }

            log("Intermediate APK compiled successfully (${formatFileSize(unsignedApkFile.length())})")
            onProgress(OperationStatus.Processing(75, "Signing container with Android APK signature...", logs))

            // 5. Re-sign APK
            val signedApkFile = File(workingDir, "final_signed.apk")
            FileInputStream(unsignedApkFile).use { inStream ->
                FileOutputStream(signedApkFile).use { outStream ->
                    ApkSignerHelper.signApk(inStream, outStream)
                }
            }

            log("APK re-signed and verified successfully.")
            onProgress(OperationStatus.Processing(90, "Saving to destination folder in SAF storage...", logs))

            // 6. Save to designated directory (DPT or ONLOCK)
            val baseName = originalFileName.removeSuffix(".apk")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetSubDir = when (engineType) {
                ProcessingEngineType.DPT -> StorageManager.DIR_DPT
                ProcessingEngineType.ONLOCK -> StorageManager.DIR_ONLOCK
            }
            val outputFileName = "${baseName}_${engineType.name}_$timestamp.apk"

            val destinationUri = storageManager.saveOutputFile(targetSubDir, outputFileName) { destStream ->
                FileInputStream(signedApkFile).use { finalIn ->
                    finalIn.copyTo(destStream)
                }
            } ?: throw IOException("Could not write processed APK to storage")

            val finalLength = signedApkFile.length()
            log("Saved to $targetSubDir/$outputFileName ($finalLength bytes)")
            onProgress(OperationStatus.Processing(100, "Complete!", logs))

            val stats = "Input: ${formatFileSize(originalSize)}  ➔  Output: ${formatFileSize(finalLength)}"
            OperationStatus.Success(
                outputFileName = outputFileName,
                outputUri = destinationUri,
                originalSize = originalSize,
                finalSize = finalLength,
                destinationFolder = targetSubDir,
                statsMessage = stats
            )
        } catch (e: Exception) {
            OperationStatus.Failed(
                errorMessage = e.message ?: "Unknown processing error occurred",
                errorDetail = e.stackTraceToString()
            )
        } finally {
            // Delete temporary private working files
            workingDir.deleteRecursively()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = String.format(Locale.US, "%.2f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }
}
