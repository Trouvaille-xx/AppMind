package com.example.appmind.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appmind.data.entity.AppLog
import com.example.appmind.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("操作记录 (${logs.size})", style = MaterialTheme.typography.titleMedium)
            if (logs.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearLogs() }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "清空",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空全部", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有记录", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "打开被监控的应用后会在这里显示",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@Composable
fun LogCard(log: AppLog) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📱 ${log.appName}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val (actionText, actionColor) = if (log.action == "confirmed") {
                    "已确认" to Color(0xFF4CAF50)
                } else {
                    "已取消" to Color(0xFFE53935)
                }
                Text(
                    actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = actionColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (log.answer.isNotBlank()) {
                Text(
                    "💬 ${log.answer}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                dateFormat.format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
