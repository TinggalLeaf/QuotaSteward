package com.github.tinggalleaf.ai_quota_dashboard.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.DashboardEntry
import com.github.tinggalleaf.ai_quota_dashboard.ui.components.ProviderIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ServicesScreen(
    entries: List<DashboardEntry>,
    onAdd: () -> Unit,
    onEdit: (ServiceConfig) -> Unit,
    onPickPreset: () -> Unit,
    onRefresh: (String) -> Unit,
    onToggle: (ServiceConfig, Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val cs = MiuixTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "服务管理",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                    Text(
                        "${entries.size} 个 · 点击右侧开关启用,齿轮图标编辑",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariantSummary,
                    )
                }
                androidx.compose.material3.FilledTonalButton(onClick = onPickPreset) {
                    Text("从预设添加")
                }
                androidx.compose.material3.Button(onClick = onAdd) {
                    Text("自定义")
                }
            }
        }

        items(entries, key = { it.service.id }) { entry ->
            ServiceManageRow(
                entry = entry,
                onEdit = { onEdit(entry.service) },
                onRefresh = { onRefresh(entry.service.id) },
                onToggle = { v -> onToggle(entry.service, v) },
            )
        }
    }
}

@Composable
private fun ServiceManageRow(
    entry: DashboardEntry,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val cs = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderIcon(service = entry.service, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.service.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    "${entry.service.method.name} · ${entry.service.unit.name} · ${entry.service.variables.size} 变量",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    MiuixIcons.Refresh,
                    contentDescription = "刷新",
                    tint = cs.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = entry.service.enabled,
                onCheckedChange = onToggle,
            )
            IconButton(onClick = onEdit) {
                Icon(
                    MiuixIcons.Add,
                    contentDescription = "编辑",
                    tint = cs.onSurfaceVariantSummary,
                )
            }
        }
    }
}
