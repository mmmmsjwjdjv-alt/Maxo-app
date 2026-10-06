package com.example.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

class StorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "maxo_storage_prefs"
        private const val KEY_TREE_URI = "key_storage_tree_uri"
        const val DIR_DPT = "DPT"
        const val DIR_UNLOCK = "UNLOCK"
    }

    fun getStoredTreeUri(): Uri? {
        val uriStr = prefs.getString(KEY_TREE_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (_: Exception) {
            null
        }
    }

    fun isStorageInitialized(): Boolean {
        val uri = getStoredTreeUri() ?: return false
        return verifyAndInitDirectories(uri).first
    }

    fun takePersistablePermissions(treeUri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveTreeUri(treeUri: Uri) {
        prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
    }

    fun clearSavedTreeUri() {
        prefs.edit().remove(KEY_TREE_URI).apply()
    }

    fun verifyAndInitDirectories(treeUri: Uri): Pair<Boolean, String> {
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Pair(false, "Cannot resolve DocumentFile from URI")

        if (!documentFile.exists() || !documentFile.canRead() || !documentFile.canWrite()) {
            return Pair(false, "Selected directory is not accessible or not writable")
        }

        try {
            ensureDirectory(documentFile, DIR_DPT)
            ensureDirectory(documentFile, DIR_UNLOCK)
            return Pair(true, documentFile.name ?: "MAXO Workspace")
        } catch (e: Exception) {
            return Pair(false, "Failed to initialize directories: ${e.message}")
        }
    }

    private fun ensureDirectory(parent: DocumentFile, dirName: String): DocumentFile {
        val existing = parent.findFile(dirName)
        if (existing != null && existing.isDirectory) {
            return existing
        }
        val created = parent.createDirectory(dirName)
            ?: throw IllegalStateException("Failed to create $dirName directory in selected folder")
        return created
    }

    fun getTargetDirectory(subDir: String): DocumentFile? {
        val treeUri = getStoredTreeUri() ?: return null
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        if (!root.exists() || !root.canWrite()) return null
        return ensureDirectory(root, subDir)
    }

    fun saveOutputFile(
        subDir: String,
        targetFileName: String,
        writeBlock: (OutputStream) -> Unit
    ): Uri? {
        val targetDir = getTargetDirectory(subDir)
            ?: throw IllegalStateException("Target directory $subDir is not accessible")

        // Overwrite or create unique
        val existing = targetDir.findFile(targetFileName)
        if (existing != null) {
            try {
                existing.delete()
            } catch (_: Exception) { }
        }

        val createdFile = targetDir.createFile("application/vnd.android.package-archive", targetFileName)
            ?: throw IllegalStateException("Could not create destination file: $targetFileName")

        context.contentResolver.openOutputStream(createdFile.uri)?.use { output ->
            writeBlock(output)
        } ?: throw IllegalStateException("Could not open output stream to ${createdFile.uri}")

        return createdFile.uri
    }

    fun openInput(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    fun getDisplayPath(): String {
        val uri = getStoredTreeUri() ?: return "Not configured"
        val doc = DocumentFile.fromTreeUri(context, uri)
        return doc?.name ?: uri.lastPathSegment ?: "MAXO Workspace"
    }
}
