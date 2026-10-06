package com.example.validation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.SelectedApkInfo
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object ApkValidator {

    fun validateAndExtractInfo(context: Context, uri: Uri): SelectedApkInfo? {
        var fileName = "unknown.apk"
        var fileSize = 0L

        // Query metadata from ContentResolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "selected.apk"
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        // Verify if stream can be opened
        var hasClassesDex = false
        var hasAndroidManifest = false

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Inspect zip stream
                val zis = ZipInputStream(inputStream)
                var entry = zis.nextEntry
                var count = 0
                while (entry != null && count < 100) {
                    val name = entry.name
                    if (name == "AndroidManifest.xml") hasAndroidManifest = true
                    if (name.startsWith("classes") && name.endsWith(".dex")) hasClassesDex = true
                    zis.closeEntry()
                    entry = zis.nextEntry
                    count++
                }
            }
        } catch (e: Exception) {
            return null
        }

        val isValid = hasAndroidManifest || hasClassesDex || fileName.endsWith(".apk", ignoreCase = true)
        if (!isValid) return null

        val readableSize = formatSize(fileSize)
        return SelectedApkInfo(
            uri = uri,
            fileName = fileName,
            sizeBytes = fileSize,
            readableSize = readableSize,
            packageName = null,
            isApkValid = true
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = String.format(Locale.US, "%.2f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }
}
