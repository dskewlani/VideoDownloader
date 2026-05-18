package com.videodownloader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videodownloader.app.model.DownloadHistory
import com.videodownloader.app.ui.theme.*
import com.videodownloader.app.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, uiState: com.videodownloader.app.viewmodel.UiState) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Download History", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
            if (uiState.history.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear All",
                        tint = FailedColor)
                }
            }
        }

        if (uiState.history.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.History, contentDescription = null,
                    tint = Color(0xFF333333), modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No history yet", color = Color(0xFF555555),
                    fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your completed downloads will appear here",
                    color = Color(0xFF444444), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.history, key = { it.id }) { item ->
                    HistoryCard(
                        item = item,
                        onPlay = { viewModel.playVideo(item.filePath, item.title) },
                        onDelete = { viewModel.deleteHistory(item.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = SurfaceVariant,
            title = { Text("Clear History?", color = Color.White) },
            text = { Text("This will remove all download history records. Downloaded files will remain on your device.",
                color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text("Clear All", color = FailedColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun HistoryCard(
    item: DownloadHistory,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val fileExists = remember(item.filePath) { File(item.filePath).exists() }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp, 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.VideoFile, contentDescription = null,
                        tint = Color(0xFF444444), modifier = Modifier.size(28.dp))
                }
                if (fileExists) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.quality.isNotEmpty()) {
                        Text(item.quality, color = Primary, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Text(formatSize(item.fileSize), color = Color.Gray, fontSize = 11.sp)
                }
                Text(
                    dateFormat.format(Date(item.downloadedAt)),
                    color = Color(0xFF555555), fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (fileExists) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play",
                            tint = CompletedColor, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Icon(Icons.Filled.BrokenImage, contentDescription = "File missing",
                        tint = Color(0xFF444444), modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = Color(0xFF555555), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes <= 0 -> ""
        bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
