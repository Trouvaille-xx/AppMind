package com.example.appmind.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appmind.viewmodel.MainViewModel

enum class Screen(val title: String, val icon: ImageVector) {
    Settings("设置", Icons.Default.Settings),
    Apps("应用管理", Icons.Default.PhoneAndroid),
    Logs("历史记录", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Settings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AppMind") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Settings -> SettingsScreen(viewModel)
                Screen.Apps -> AppManageScreen(viewModel)
                Screen.Logs -> LogScreen(viewModel)
            }
        }
    }
}
