package com.videodownloader.app.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videodownloader.app.model.DownloadHistory
import com.videodownloader.app.model.DownloadStatus
import com.videodownloader.app.model.VideoLink
import com.videodownloader.app.util.AppDatabase
import com.videodownloader.app.util.BinaryManager
import com.videodownloader.app.util.DownloadManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class UiState(
    val isReady: Boolean = false,
    val setupProgress: String = "Initializing...",
    val links: List<VideoLink> = emptyList(),
    val currentPlayerPath: String? = null,
    val currentPlayerTitle: String = "",
    val showPlayer: Boolean = false,
    val history: List<DownloadHistory> = emptyList(),
    val activeTab: Int = 0, // 0=Download, 1=History
    val isDownloading: Boolean = false,
    val clipboardUrl: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val downloadManager = DownloadManager(context)
    private val db = AppDatabase.getInstance(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        setupBinaries()
        observeDownloads()
        observeHistory()
    }

    private fun setupBinaries() {
        viewModelScope.launch {
            BinaryManager.ensureBinariesReady(context) { msg ->
                _uiState.update { it.copy(setupProgress = msg) }
            }
            _uiState.update { it.copy(isReady = true) }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadManager.downloads.collect { dlMap ->
                val links = dlMap.values.sortedBy { it.serialNo }
                val isDownloading = links.any { it.status == DownloadStatus.DOWNLOADING }

                // Save completed downloads to history
                links.filter { it.status == DownloadStatus.COMPLETED && it.filePath.isNotEmpty() }
                    .forEach { link ->
                        val existingHistory = db.historyDao().getAllHistory().first()
                        val alreadySaved = existingHistory.any { it.url == link.url }
                        if (!alreadySaved) {
                            db.historyDao().insert(
                                DownloadHistory(
                                    title = link.title.ifEmpty { "Video ${link.serialNo}" },
                                    url = link.url,
                                    filePath = link.filePath,
                                    thumbnail = link.thumbnail,
                                    quality = link.quality.label,
                                    fileSize = link.fileSize,
                                    duration = link.duration
                                )
                            )
                        }
                    }

                _uiState.update { it.copy(links = links, isDownloading = isDownloading) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            db.historyDao().getAllHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun addLink(url: String) {
        if (url.isBlank()) return
        val serialNo = (_uiState.value.links.maxOfOrNull { it.serialNo } ?: 0) + 1
        val link = VideoLink(
            id = UUID.randomUUID().toString(),
            serialNo = serialNo,
            url = url.trim()
        )
        downloadManager.addDownload(link)
    }

    fun addMultipleLinks(urls: List<String>) {
        urls.filter { it.isNotBlank() }.forEach { addLink(it) }
    }

    fun removeLink(id: String) {
        downloadManager.removeDownload(id)
    }

    fun startDownload(id: String) {
        downloadManager.startDownload(id)
    }

    fun downloadAll() {
        downloadManager.startAllDownloads()
    }

    fun pauseDownload(id: String) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
    }

    fun clearCompleted() {
        downloadManager.clearCompleted()
    }

    fun clearLinks() {
        _uiState.value.links.forEach { downloadManager.removeDownload(it.id) }
    }

    fun playVideo(path: String, title: String) {
        _uiState.update { it.copy(currentPlayerPath = path, currentPlayerTitle = title, showPlayer = true) }
    }

    fun closePlayer() {
        _uiState.update { it.copy(showPlayer = false, currentPlayerPath = null) }
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun checkClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (text.startsWith("http") && (text.contains("youtube") || text.contains("youtu.be") ||
            text.contains("instagram") || text.contains("tiktok") || text.contains("twitter") ||
            text.contains("facebook") || text.contains("vimeo") || text.contains("reddit") ||
            text.contains(".mp4") || text.contains(".m3u8"))) {
            _uiState.update { it.copy(clipboardUrl = text) }
        }
    }

    fun deleteHistory(id: String) {
        viewModelScope.launch {
            db.historyDao().deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            db.historyDao().clearAll()
        }
    }

    fun updateLinkUrl(id: String, newUrl: String) {
        // handled via re-add
    }

    override fun onCleared() {
        super.onCleared()
        downloadManager.destroy()
    }
}
