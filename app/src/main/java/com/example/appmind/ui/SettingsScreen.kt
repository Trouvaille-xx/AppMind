package com.example.appmind.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appmind.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var questionText by remember { mutableStateOf(viewModel.defaultQuestion.value) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ========================
        // 服务状态
        // ========================
        Text("服务状态", style = MaterialTheme.typography.titleMedium)

        StatusCard(
            title = "无障碍服务",
            description = "用于检测应用启动，必须在系统中开启后才能工作",
            isEnabled = viewModel.isAccessibilityEnabled,
            onEnable = { openAccessibilitySettings(context) }
        )

        StatusCard(
            title = "悬浮窗权限",
            description = "用于在其他应用上显示问题弹窗",
            isEnabled = viewModel.isOverlayPermissionGranted,
            onEnable = { openOverlaySettings(context) }
        )

        // ========================
        // 默认问题
        // ========================
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("默认问题设置", style = MaterialTheme.typography.titleMedium)
        Text(
            "当应用没有设置独立问题时，使用此默认问题。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("默认问题") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Button(
            onClick = { viewModel.setDefaultQuestion(questionText) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("保存默认问题")
        }

        // ========================
        // 使用说明
        // ========================
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("使用说明", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. 先开启「无障碍服务」和「悬浮窗权限」")
                Text("2. 进入「应用管理」选择要监控的应用")
                Text("3. 设置默认问题（或为每个应用设置独立问题）")
                Text("4. 当你打开被监控的应用时，会弹出问题窗口")
                Text("5. 输入理由点击「确认打开」进入应用，点击「取消」返回")
                Text("6. 所有操作记录在「历史记录」中查看")
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onEnable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isEnabled) {
                AssistChip(
                    onClick = {},
                    label = { Text("已开启") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            } else {
                Button(onClick = onEnable) {
                    Text("去开启")
                }
            }
        }
    }
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}
