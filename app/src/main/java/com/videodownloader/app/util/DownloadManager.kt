package com.videodownloader.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.videodownloader.app.model.DownloadStatus
import com.videodownloader.app.model.VideoLink
import com.videodownloader.app.model.VideoQuality
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class DownloadManager(private val context: Context) {
    private val TAG = "DownloadManager"

    private val _downloads = MutableStateFlow<Map<String, VideoLink>>(emptyMap())
    val downloads: StateFlow<Map<String, VideoLink>> = _downloads

    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Max 3 concurrent downloads
    private val semaphore = kotlinx.coroutines.sync.Semaphore(3)

    companion object {
        val OUTPUT_DIR: String = "${Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )}/VideoDownloader"

        // yt-dlp progress patterns
        private val PROGRESS_PATTERN = Pattern.compile(
            "\\[download\\]\\s+(\\d+\\.?\\d*)%\\s+of\\s+~?\\s*(\\S+)\\s+at\\s+(\\S+)\\s+ETA\\s+(\\S+)"
        )
        private val DEST_PATTERN = Pattern.compile(
            "\\[download\\] Destination: (.+)"
        )
        private val MERGE_PATTERN = Pattern.compile(
            "\\[Merger\\] Merging formats into \"(.+)\""
        )
        private val ALREADY_DONE = Pattern.compile(
            "\\[download\\] (.+) has already been downloaded"
        )
    }

    fun addDownload(link: VideoLink) {
        updateDownload(link)
    }

    fun startDownload(linkId: String) {
        val link = _downloads.value[linkId] ?: return
        if (link.status == DownloadStatus.DOWNLOADING) return

        val job = scope.launch {
            semaphore.acquire()
            try {
                executeDownload(link)
            } finally {
                semaphore.release()
            }
        }
        downloadJobs[linkId] = job
    }

    fun startAllDownloads() {
        _downloads.value.keys.forEach { id ->
            val link = _downloads.value[id]
            if (link?.status == DownloadStatus.QUEUED || link?.status == DownloadStatus.FAILED) {
                startDownload(id)
            }
        }
    }

    fun pauseDownload(linkId: String) {
        activeProcesses[linkId]?.destroy()
        activeProcesses.remove(linkId)
        downloadJobs[linkId]?.cancel()
        downloadJobs.remove(linkId)
        updateStatus(linkId, DownloadStatus.PAUSED)
    }

    fun cancelDownload(linkId: String) {
        activeProcesses[linkId]?.destroyForcibly()
        activeProcesses.remove(linkId)
        downloadJobs[linkId]?.cancel()
        downloadJobs.remove(linkId)
        updateStatus(linkId, DownloadStatus.CANCELLED)
    }

    fun resumeDownload(linkId: String) {
        startDownload(linkId)
    }

    private suspend fun executeDownload(link: VideoLink) {
        withContext(Dispatchers.IO) {
            val id = link.id
            try {
                // Fetch info first
                updateStatus(id, DownloadStatus.FETCHING_INFO)
                val info = YtDlpEngine.getVideoInfo(context, link.url)

                val title = info?.title ?: "video_${link.serialNo}"
                val quality = info?.quality ?: VideoQuality.UNKNOWN
                val fileSize = info?.fileSize ?: 0L
                val thumbnail = info?.thumbnail ?: ""
                val duration = info?.duration ?: 0L

                // Sanitize filename
                val safeTitle = sanitizeFilename(title).take(100)
                val qualityTag = quality.label.replace(" ", "")
                val filename = "VD_${link.serialNo}_${safeTitle}_${qualityTag}"

                updateDownload(
                    _downloads.value[id]!!.copy(
                        status = DownloadStatus.DOWNLOADING,
                        title = title,
                        thumbnail = thumbnail,
                        quality = quality,
                        fileSize = fileSize,
                        duration = duration
                    )
                )

                // Ensure output dir exists
                File(OUTPUT_DIR).mkdirs()

                // Build command
                val cmd = YtDlpEngine.buildDownloadCommand(context, link.url, OUTPUT_DIR, filename)

                Log.d(TAG, "Starting download: ${cmd.joinToString(" ")}")

                val process = ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()

                activeProcesses[id] = process

                // Parse progress in real-time
                var currentFilePath = ""
                var lastSpeed = 0L
                var lastEta = 0L

                process.inputStream.bufferedReader().forEachLine { line ->
                    Log.d(TAG, "YT-DLP: $line")

                    // Parse download progress
                    val progressMatcher = PROGRESS_PATTERN.matcher(line)
                    if (progressMatcher.find()) {
                        val percent = progressMatcher.group(1)?.toFloatOrNull() ?: 0f
                        val sizeStr = progressMatcher.group(2) ?: ""
                        val speedStr = progressMatcher.group(3) ?: ""
                        val etaStr = progressMatcher.group(4) ?: ""

                        val totalSize = parseSize(sizeStr)
                        val downloaded = (totalSize * percent / 100).toLong()
                        val speed = parseSpeed(speedStr)
                        val eta = parseEta(etaStr)

                        lastSpeed = speed
                        lastEta = eta

                        val current = _downloads.value[id]
                        if (current != null) {
                            updateDownload(
                                current.copy(
                                    downloadedBytes = downloaded,
                                    fileSize = if (totalSize > 0) totalSize else current.fileSize,
                                    speed = speed,
                                    eta = eta
                                )
                            )
                        }
                    }

                    // Parse destination
                    val destMatcher = DEST_PATTERN.matcher(line)
                    if (destMatcher.find()) {
                        currentFilePath = destMatcher.group(1) ?: ""
                    }

                    val mergeMatcher = MERGE_PATTERN.matcher(line)
                    if (mergeMatcher.find()) {
                        currentFilePath = mergeMatcher.group(1) ?: currentFilePath
                    }
                }

                val exitCode = process.waitFor()
                activeProcesses.remove(id)

                if (exitCode == 0) {
                    // Find the actual downloaded file
                    val finalPath = findDownloadedFile(OUTPUT_DIR, filename) ?: currentFilePath
                    val actualSize = if (finalPath.isNotEmpty()) File(finalPath).length() else fileSize

                    updateDownload(
                        _downloads.value[id]!!.copy(
                            status = DownloadStatus.COMPLETED,
                            filePath = finalPath,
                            downloadedBytes = actualSize,
                            fileSize = actualSize,
                            speed = 0L,
                            eta = 0L
                        )
                    )
                    Log.d(TAG, "Download completed: $finalPath")
                } else {
                    if (_downloads.value[id]?.status != DownloadStatus.CANCELLED &&
                        _downloads.value[id]?.status != DownloadStatus.PAUSED) {
                        updateStatus(id, DownloadStatus.FAILED, "Download failed (exit: $exitCode)")
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                updateStatus(id, DownloadStatus.FAILED, e.message ?: "Unknown error")
            }
        }
    }

    private fun findDownloadedFile(dir: String, filenamePrefix: String): String? {
        return File(dir).listFiles()
            ?.filter { it.name.startsWith("VD_") && it.extension in listOf("mp4", "mkv", "webm", "mov") }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }

    private fun parseSize(sizeStr: String): Long {
        return try {
            val num = sizeStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return 0L
            when {
                sizeStr.contains("GiB", true) -> (num * 1024 * 1024 * 1024).toLong()
                sizeStr.contains("MiB", true) || sizeStr.contains("MB", true) ->
                    (num * 1024 * 1024).toLong()
                sizeStr.contains("KiB", true) || sizeStr.contains("KB", true) ->
                    (num * 1024).toLong()
                else -> num.toLong()
            }
        } catch (e: Exception) { 0L }
    }

    private fun parseSpeed(speedStr: String): Long {
        return try {
            if (speedStr == "Unknown" || speedStr.contains("N/A")) return 0L
            parseSize(speedStr)
        } catch (e: Exception) { 0L }
    }

    private fun parseEta(etaStr: String): Long {
        return try {
            if (etaStr == "Unknown" || etaStr.contains("N/A")) return 0L
            val parts = etaStr.split(":")
            when (parts.size) {
                1 -> parts[0].toLongOrNull() ?: 0L
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                else -> 0L
            }
        } catch (e: Exception) { 0L }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_', ' ')
    }

    private fun updateStatus(id: String, status: DownloadStatus, error: String = "") {
        val current = _downloads.value[id] ?: return
        updateDownload(current.copy(status = status, errorMessage = error))
    }

    private fun updateDownload(link: VideoLink) {
        val current = _downloads.value.toMutableMap()
        current[link.id] = link
        _downloads.value = current
    }

    fun removeDownload(linkId: String) {
        cancelDownload(linkId)
        val current = _downloads.value.toMutableMap()
        current.remove(linkId)
        _downloads.value = current
    }

    fun clearCompleted() {
        val current = _downloads.value.toMutableMap()
        current.entries.removeIf { it.value.status == DownloadStatus.COMPLETED }
        _downloads.value = current
    }

    fun destroy() {
        scope.cancel()
        activeProcesses.values.forEach { it.destroyForcibly() }
    }
}
