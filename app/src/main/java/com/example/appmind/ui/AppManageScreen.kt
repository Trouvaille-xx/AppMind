package com.example.appmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appmind.viewmodel.AppWithStatus
import com.example.appmind.viewmodel.MainViewModel

@Composable
fun AppManageScreen(viewModel: MainViewModel) {
    val apps by viewModel.installedApps.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val monitoredApps = apps.filter { it.isMonitored }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索应用...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        // 已监控标签
        if (monitoredApps.isNotEmpty()) {
            Text(
                "已监控 (${monitoredApps.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(horizontal = 16.dp)
            ) {
                items(monitoredApps) { app ->
                    AppCard(app, viewModel, showCustomQuestion = true)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // 所有应用列表
        Text(
            "所有应用",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        val filteredApps = if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appInfo.appName.contains(searchQuery, ignoreCase = true) ||
                    it.appInfo.packageName.contains(searchQuery, ignoreCase = true)
        }

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("没有找到匹配的应用", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    AppCard(app, viewModel, showCustomQuestion = false)
                }
            }
        }
    }
}

@Composable
fun AppCard(app: AppWithStatus, viewModel: MainViewModel, showCustomQuestion: Boolean) {
    var dialogVisible by remember { mutableStateOf(false) }
    var questionText by remember(app.customQuestion) { mutableStateOf(app.customQuestion ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (showCustomQuestion) {
                OutlinedButton(
                    onClick = { dialogVisible = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("自定义问题", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Switch(
                checked = app.isMonitored,
                onCheckedChange = { viewModel.toggleMonitor(app.appInfo.packageName, it) }
            )

            // Custom question dialog
            if (dialogVisible) {
                AlertDialog(
                    onDismissRequest = { dialogVisible = false },
                    title = { Text("为 ${app.appInfo.appName} 设置问题") },
                    text = {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text("自定义问题（留空使用默认）") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val q = questionText.takeIf { it.isNotBlank() }
                            viewModel.setCustomQuestion(app.appInfo.packageName, q)
                            dialogVisible = false
                        }) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogVisible = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}
