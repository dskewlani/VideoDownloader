package com.videodownloader.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object BinaryManager {
    private const val TAG = "BinaryManager"

    fun getYtDlpPath(context: Context): String {
        val file = File(context.filesDir, "yt-dlp")
        return file.absolutePath
    }

    fun getAria2cPath(context: Context): String {
        val file = File(context.filesDir, "aria2c")
        return file.absolutePath
    }

    fun getFfmpegPath(context: Context): String {
        val file = File(context.filesDir, "ffmpeg")
        return file.absolutePath
    }

    suspend fun ensureBinariesReady(context: Context, onProgress: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onProgress("Checking binaries...")
                // Copy bundled binaries from assets if not already there
                listOf("yt-dlp", "aria2c", "ffmpeg").forEach { binary ->
                    val dest = File(context.filesDir, binary)
                    if (!dest.exists() || dest.length() < 1000) {
                        onProgress("Installing $binary...")
                        copyAssetBinary(context, binary, dest)
                    }
                    if (dest.exists()) {
                        dest.setExecutable(true, false)
                    }
                }
                onProgress("Ready!")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Binary setup failed", e)
                false
            }
        }
    }

    private fun copyAssetBinary(context: Context, name: String, dest: File) {
        try {
            context.assets.open(name).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.setExecutable(true, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy $name from assets", e)
        }
    }

    fun isBinaryAvailable(context: Context, name: String): Boolean {
        val file = File(context.filesDir, name)
        return file.exists() && file.canExecute()
    }
}
