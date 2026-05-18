package com.videodownloader.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.videodownloader.app.ui.screens.*
import com.videodownloader.app.ui.theme.*
import com.videodownloader.app.viewmodel.MainViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle shared URL
        handleSharedIntent(intent)

        setContent {
            VideoDownloaderTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(Color.Black, darkIcons = false)
                }

                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSharedIntent(it) }
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.addLink(sharedText)
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val navItems = listOf(
        NavItem("Downloads", Icons.Outlined.Download, Icons.Filled.Download),
        NavItem("History", Icons.Outlined.History, Icons.Filled.History)
    )

    // Show player fullscreen if active
    if (uiState.showPlayer && uiState.currentPlayerPath != null) {
        VideoPlayerScreen(
            filePath = uiState.currentPlayerPath!!,
            title = uiState.currentPlayerTitle,
            onClose = { viewModel.closePlayer() }
        )
        return
    }

    // Setup / Loading screen
    if (!uiState.isReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(uiState.setupProgress, color = Color.Gray)
            }
        }
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                contentColor = Primary,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = uiState.activeTab == index,
                        onClick = { viewModel.setActiveTab(index) },
                        icon = {
                            Icon(
                                if (uiState.activeTab == index) item.selectedIcon else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.activeTab) {
                0 -> DownloadScreen(viewModel = viewModel, uiState = uiState)
                1 -> HistoryScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
