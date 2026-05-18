package com.videodownloader.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED, FETCHING_INFO, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

enum class VideoQuality(val label: String, val priority: Int) {
    UHD_4K_60("4K 60fps", 1),
    UHD_4K("4K", 2),
    QHD_2K_60("2K 60fps", 3),
    QHD_2K("2K", 4),
    FHD_60("1080p 60fps", 5),
    FHD("1080p", 6),
    HD_60("720p 60fps", 7),
    HD("720p", 8),
    SD("480p", 9),
    LOW("360p", 10),
    UNKNOWN("Best", 11)
}

data class VideoLink(
    val id: String = java.util.UUID.randomUUID().toString(),
    val serialNo: Int,
    val url: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val title: String = "",
    val thumbnail: String = "",
    val quality: VideoQuality = VideoQuality.UNKNOWN,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speed: Long = 0L, // bytes per second
    val eta: Long = 0L,   // seconds
    val filePath: String = "",
    val errorMessage: String = "",
    val duration: Long = 0L // seconds
) {
    val progress: Float
        get() = if (fileSize > 0) (downloadedBytes.toFloat() / fileSize) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedSize: String
        get() = formatBytes(fileSize)

    val formattedDownloaded: String
        get() = formatBytes(downloadedBytes)

    val formattedSpeed: String
        get() = if (speed > 0) "${formatBytes(speed)}/s" else ""

    val formattedEta: String
        get() = if (eta > 0) formatTime(eta) else ""

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}

@Entity(tableName = "download_history")
data class DownloadHistory(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val filePath: String,
    val thumbnail: String = "",
    val quality: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
)
