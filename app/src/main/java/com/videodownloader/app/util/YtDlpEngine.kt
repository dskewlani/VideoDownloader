package com.videodownloader.app.util

import android.content.Context
import android.util.Log
import com.videodownloader.app.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class VideoInfo(
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val quality: VideoQuality,
    val fileSize: Long,
    val formats: List<FormatInfo>
)

data class FormatInfo(
    val formatId: String,
    val ext: String,
    val width: Int,
    val height: Int,
    val fps: Double,
    val filesize: Long,
    val vcodec: String,
    val acodec: String,
    val tbr: Double,
    val isHdr: Boolean
)

object YtDlpEngine {
    private const val TAG = "YtDlpEngine"

    // Best quality format selector - prioritizes 4K60, 2K60, 1080p60, etc.
    private const val FORMAT_SELECTOR = """
        bestvideo[ext=mp4][height>=2160][fps>=60]+bestaudio[ext=m4a]/
        bestvideo[ext=mp4][height>=2160]+bestaudio[ext=m4a]/
        bestvideo[ext=webm][height>=2160][fps>=60]+bestaudio[ext=webm]/
        bestvideo[height>=2160][fps>=60]+bestaudio/
        bestvideo[height>=2160]+bestaudio/
        bestvideo[ext=mp4][height>=1440][fps>=60]+bestaudio[ext=m4a]/
        bestvideo[ext=mp4][height>=1440]+bestaudio[ext=m4a]/
        bestvideo[height>=1440][fps>=60]+bestaudio/
        bestvideo[height>=1440]+bestaudio/
        bestvideo[ext=mp4][height>=1080][fps>=60]+bestaudio[ext=m4a]/
        bestvideo[ext=mp4][height>=1080]+bestaudio[ext=m4a]/
        bestvideo[height>=1080][fps>=60]+bestaudio/
        bestvideo[height>=1080]+bestaudio/
        bestvideo[height>=720]+bestaudio/
        bestvideo+bestaudio/
        best
    """.trimIndent().replace("\n", "")

    suspend fun getVideoInfo(context: Context, url: String): VideoInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val ytDlp = BinaryManager.getYtDlpPath(context)
                if (!File(ytDlp).exists()) return@withContext getFallbackInfo(url)

                val cmd = arrayOf(
                    ytDlp,
                    "--dump-json",
                    "--no-playlist",
                    "--no-warnings",
                    "--socket-timeout", "30",
                    url
                )

                val process = ProcessBuilder(*cmd)
                    .redirectErrorStream(false)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                if (output.isBlank()) return@withContext getFallbackInfo(url)

                parseVideoInfo(output)
            } catch (e: Exception) {
                Log.e(TAG, "getVideoInfo failed", e)
                getFallbackInfo(url)
            }
        }
    }

    private fun parseVideoInfo(json: String): VideoInfo? {
        return try {
            val obj = JSONObject(json)
            val title = obj.optString("title", "Unknown Video")
            val thumbnail = obj.optString("thumbnail", "")
            val duration = obj.optLong("duration", 0)

            val formats = mutableListOf<FormatInfo>()
            val formatsArray = obj.optJSONArray("formats")
            formatsArray?.let {
                for (i in 0 until it.length()) {
                    val f = it.getJSONObject(i)
                    val height = f.optInt("height", 0)
                    val width = f.optInt("width", 0)
                    val fps = f.optDouble("fps", 0.0)
                    val vcodec = f.optString("vcodec", "none")
                    val acodec = f.optString("acodec", "none")
                    if (vcodec != "none" && height > 0) {
                        formats.add(
                            FormatInfo(
                                formatId = f.optString("format_id", ""),
                                ext = f.optString("ext", "mp4"),
                                width = width,
                                height = height,
                                fps = fps,
                                filesize = f.optLong("filesize", f.optLong("filesize_approx", 0)),
                                vcodec = vcodec,
                                acodec = acodec,
                                tbr = f.optDouble("tbr", 0.0),
                                isHdr = f.optString("dynamic_range", "").contains("HDR")
                            )
                        )
                    }
                }
            }

            val bestFormat = formats.maxByOrNull { it.height * 1000 + it.fps.toInt() }
            val quality = when {
                bestFormat == null -> VideoQuality.UNKNOWN
                bestFormat.height >= 2160 && bestFormat.fps >= 60 -> VideoQuality.UHD_4K_60
                bestFormat.height >= 2160 -> VideoQuality.UHD_4K
                bestFormat.height >= 1440 && bestFormat.fps >= 60 -> VideoQuality.QHD_2K_60
                bestFormat.height >= 1440 -> VideoQuality.QHD_2K
                bestFormat.height >= 1080 && bestFormat.fps >= 60 -> VideoQuality.FHD_60
                bestFormat.height >= 1080 -> VideoQuality.FHD
                bestFormat.height >= 720 && bestFormat.fps >= 60 -> VideoQuality.HD_60
                bestFormat.height >= 720 -> VideoQuality.HD
                bestFormat.height >= 480 -> VideoQuality.SD
                else -> VideoQuality.LOW
            }

            VideoInfo(
                title = title,
                thumbnail = thumbnail,
                duration = duration,
                quality = quality,
                fileSize = bestFormat?.filesize ?: 0L,
                formats = formats
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseVideoInfo failed", e)
            null
        }
    }

    private fun getFallbackInfo(url: String): VideoInfo {
        return VideoInfo(
            title = extractTitleFromUrl(url),
            thumbnail = "",
            duration = 0L,
            quality = VideoQuality.UNKNOWN,
            fileSize = 0L,
            formats = emptyList()
        )
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            URL(url).host?.replace("www.", "") ?: "Video"
        } catch (e: Exception) {
            "Video"
        }
    }

    fun buildDownloadCommand(
        context: Context,
        url: String,
        outputDir: String,
        filename: String
    ): Array<String> {
        val ytDlp = BinaryManager.getYtDlpPath(context)
        val ffmpeg = BinaryManager.getFfmpegPath(context)
        val aria2c = BinaryManager.getAria2cPath(context)

        val useAria2c = File(aria2c).exists()
        val useFfmpeg = File(ffmpeg).exists()

        val cmd = mutableListOf(
            ytDlp,
            "--format", FORMAT_SELECTOR.replace(" ", ""),
            "--output", "$outputDir/${filename}.%(ext)s",
            "--no-playlist",
            "--no-warnings",
            "--progress",
            "--newline",
            "--socket-timeout", "30",
            "--retries", "10",
            "--fragment-retries", "10",
            "--concurrent-fragments", "16", // Max parallel fragment downloads
            "--throttled-rate", "0",
            "--http-chunk-size", "10M",
        )

        if (useFfmpeg) {
            cmd.addAll(listOf("--ffmpeg-location", ffmpeg))
            cmd.addAll(listOf("--merge-output-format", "mp4"))
        }

        if (useAria2c) {
            cmd.addAll(listOf(
                "--downloader", "aria2c",
                "--downloader-args", "aria2c:-x16 -s16 -k1M --min-split-size=1M --max-connection-per-server=16 --max-concurrent-downloads=16 --auto-file-renaming=false --allow-overwrite=true"
            ))
        }

        cmd.add("--add-metadata")
        cmd.add(url)

        return cmd.toTypedArray()
    }
}
