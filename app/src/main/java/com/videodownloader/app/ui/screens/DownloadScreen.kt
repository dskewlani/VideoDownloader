package com.videodownloader.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.model.DownloadStatus
import com.videodownloader.app.model.VideoLink
import com.videodownloader.app.ui.theme.*
import com.videodownloader.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: MainViewModel, uiState: com.videodownloader.app.viewmodel.UiState) {
    var inputUrl by remember { mutableStateOf("") }
    var showBulkInput by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }

    // Clipboard detection
    LaunchedEffect(Unit) {
        viewModel.checkClipboard()
    }

    // Show clipboard URL snackbar
    val clipboardUrl = uiState.clipboardUrl
    var clipboardHandled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A0050), Background)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("VideoDownloader", fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("4K · 2K · 1080p60 · HDR", fontSize = 11.sp,
                                color = Primary.copy(alpha = 0.8f))
                        }
                    }
                    Row {
                        IconButton(onClick = { showBulkInput = !showBulkInput }) {
                            Icon(Icons.Outlined.List, contentDescription = "Bulk Input",
                                tint = if (showBulkInput) Primary else Color.Gray)
                        }
                        if (uiState.links.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearLinks() }) {
                                Icon(Icons.Outlined.ClearAll, contentDescription = "Clear All",
                                    tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Clipboard Banner
        AnimatedVisibility(visible = clipboardUrl.isNotEmpty() && !clipboardHandled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null,
                        tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("URL detected in clipboard",
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        viewModel.addLink(clipboardUrl)
                        clipboardHandled = true
                    }) {
                        Text("Add", color = Primary, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { clipboardHandled = true },
                        modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null,
                            tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // URL Input Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (showBulkInput) {
                    // Bulk input mode
                    Text("Paste multiple URLs (one per line)",
                        fontSize = 12.sp, color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("https://...\nhttps://...\nhttps://...",
                            color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val urls = bulkText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                            viewModel.addMultipleLinks(urls)
                            bulkText = ""
                            showBulkInput = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add ${bulkText.split("\n").count { it.isNotBlank() }} URLs",
                            fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Single URL input
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Paste video URL here...",
                                color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (inputUrl.isNotBlank()) {
                                    viewModel.addLink(inputUrl)
                                    inputUrl = ""
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = {
                                Icon(Icons.Outlined.Link, contentDescription = null,
                                    tint = Color.Gray, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (inputUrl.isNotEmpty()) {
                                    IconButton(onClick = { inputUrl = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = null,
                                            tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    viewModel.addLink(inputUrl)
                                    inputUrl = ""
                                }
                            },
                            containerColor = Primary,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add",
                                tint = Color.White)
                        }
                    }
                }
            }
        }

        // Download All Button
        AnimatedVisibility(
            visible = uiState.links.any {
                it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.FAILED
            }
        ) {
            Button(
                onClick = { viewModel.downloadAll() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                val pendingCount = uiState.links.count {
                    it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.FAILED
                }
                Icon(Icons.Filled.DownloadForOffline, contentDescription = null,
                    modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download All ($pendingCount videos)",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Stats bar
        if (uiState.links.isNotEmpty()) {
            val completed = uiState.links.count { it.status == DownloadStatus.COMPLETED }
            val downloading = uiState.links.count { it.status == DownloadStatus.DOWNLOADING }
            val failed = uiState.links.count { it.status == DownloadStatus.FAILED }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("Total: ${uiState.links.size}", Color.Gray)
                if (downloading > 0) StatChip("↓ $downloading", DownloadingColor)
                if (completed > 0) StatChip("✓ $completed", CompletedColor)
                if (failed > 0) StatChip("✗ $failed", FailedColor)

                if (completed > 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearCompleted() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Clear done", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Links List
        if (uiState.links.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.links, key = { it.id }) { link ->
                    DownloadItemCard(
                        link = link,
                        onDownload = { viewModel.startDownload(link.id) },
                        onPause = { viewModel.pauseDownload(link.id) },
                        onResume = { viewModel.resumeDownload(link.id) },
                        onCancel = { viewModel.cancelDownload(link.id) },
                        onRemove = { viewModel.removeLink(link.id) },
                        onPlay = { viewModel.playVideo(link.filePath, link.title) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DownloadItemCard(
    link: VideoLink,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit
) {
    val statusColor = when (link.status) {
        DownloadStatus.QUEUED -> QueuedColor
        DownloadStatus.FETCHING_INFO -> Primary
        DownloadStatus.DOWNLOADING -> DownloadingColor
        DownloadStatus.PAUSED -> PausedColor
        DownloadStatus.COMPLETED -> CompletedColor
        DownloadStatus.FAILED -> FailedColor
        DownloadStatus.CANCELLED -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Serial + Status + Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Serial Number Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#${link.serialNo}",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (link.title.isNotEmpty()) link.title else link.url,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge
                        StatusBadge(link.status, statusColor)

                        // Quality badge
                        if (link.quality.label != "Best" && link.status != DownloadStatus.QUEUED) {
                            QualityBadge(link.quality.label)
                        }
                    }
                }

                // Action buttons
                Row {
                    when (link.status) {
                        DownloadStatus.QUEUED, DownloadStatus.FAILED -> {
                            IconButton(onClick = onDownload,
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Download",
                                    tint = Primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        DownloadStatus.DOWNLOADING, DownloadStatus.FETCHING_INFO -> {
                            IconButton(onClick = onPause,
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Pause, contentDescription = "Pause",
                                    tint = Warning, modifier = Modifier.size(22.dp))
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume,
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Resume",
                                    tint = Primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onPlay,
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = "Play",
                                    tint = CompletedColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        else -> {}
                    }

                    IconButton(onClick = onRemove,
                        modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove",
                            tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Progress Section
            if (link.status == DownloadStatus.DOWNLOADING ||
                link.status == DownloadStatus.FETCHING_INFO ||
                (link.status == DownloadStatus.PAUSED && link.downloadedBytes > 0)) {

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { if (link.status == DownloadStatus.FETCHING_INFO) 0f else link.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (link.status == DownloadStatus.FETCHING_INFO) Secondary else Primary,
                    trackColor = SurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (link.status == DownloadStatus.FETCHING_INFO) {
                        Text("Fetching video info...", color = Secondary, fontSize = 11.sp)
                    } else {
                        Text(
                            "${link.formattedDownloaded} / ${link.formattedSize}",
                            color = Color.Gray, fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (link.formattedSpeed.isNotEmpty()) {
                                Text("⚡ ${link.formattedSpeed}",
                                    color = Secondary, fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                            if (link.formattedEta.isNotEmpty()) {
                                Text("ETA: ${link.formattedEta}",
                                    color = Color.Gray, fontSize = 11.sp)
                            }
                            Text("${link.progressPercent}%",
                                color = Primary, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Error message
            if (link.status == DownloadStatus.FAILED && link.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "⚠ ${link.errorMessage}",
                    color = FailedColor, fontSize = 11.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }

            // Completed info
            if (link.status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓ ${link.formattedSize} saved",
                        color = CompletedColor, fontSize = 11.sp)
                    Text(link.filePath.substringAfterLast("/"),
                        color = Color.Gray, fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                            .padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus, color: Color) {
    val label = when (status) {
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.FETCHING_INFO -> "Fetching..."
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }
    Text(label, color = color, fontSize = 11.sp)
}

@Composable
fun QualityBadge(quality: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Primary.copy(alpha = 0.2f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(quality, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No downloads yet", color = Color(0xFF555555),
            fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Paste a YouTube, Instagram, TikTok,\nor any video URL above to get started",
            color = Color(0xFF444444), fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Supports 4K · 2K · 1080p60 · HDR",
            color = Primary.copy(alpha = 0.6f), fontSize = 12.sp)
        Text("Powered by yt-dlp + aria2c", color = Color(0xFF333333), fontSize = 11.sp)
    }
}
