package com.github.tinggalleaf.ai_quota_dashboard.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaResult
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaTier
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.DashboardEntry
import com.github.tinggalleaf.ai_quota_dashboard.ui.components.ProviderIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val OkColor = Color(0xFF16A34A)
private val WarnColor = Color(0xFFEAB308)
private val ErrColor = Color(0xFFDC2626)

@Composable
fun DashboardScreen(
    onAdd: () -> Unit,
    onEdit: (ServiceConfig) -> Unit,
    onPickPreset: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val entries by vm.entries.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val cs = MiuixTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "服务列表",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                    Text(
                        text = "${entries.size} 个服务 · ${entries.count { it.service.enabled }} 项已启用",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariantSummary,
                    )
                }
                Row {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(MiuixIcons.Refresh, contentDescription = "刷新全部", tint = cs.onSurface)
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item { EmptyState(onPickPreset = onPickPreset, onAdd = onAdd) }
        }

        items(entries, key = { it.service.id }) { entry ->
            ServiceRow(
                entry = entry,
                refreshing = entry.service.id in refreshing,
                onRefresh = { vm.refreshOne(entry.service.id) },
                onEdit = { onEdit(entry.service) },
                onToggle = { on -> vm.toggleEnabled(entry.service, on) },
            )
        }
    }
}

@Composable
private fun EmptyState(onPickPreset: () -> Unit, onAdd: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("还没有服务", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            Text(
                "从预设中选择常见 AI 提供商,或自定义一个模板。",
                fontSize = 13.sp,
                color = cs.onSurfaceVariantSummary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(label = "选择预设", onClick = onPickPreset)
                ActionButton(label = "自定义", onClick = onAdd, primary = true)
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, primary: Boolean = false) {
    val cs = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (primary) cs.primary else cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (primary) cs.onPrimary else cs.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ServiceRow(
    entry: DashboardEntry,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val cs = MiuixTheme.colorScheme
    val result = entry.snapshot?.result
    val disabled = !entry.service.enabled

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderIcon(service = entry.service, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.service.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (disabled) cs.onSurfaceVariantSummary else cs.onSurface,
                    )
                    Text(
                        entry.service.urlTemplate.take(64).ifBlank { "(未配置 URL)" },
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(MiuixIcons.Refresh, contentDescription = "刷新", tint = cs.onSurfaceVariantSummary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            QuotaDisplay(entry.service, result)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        disabled -> "未启用"
                        entry.snapshot == null -> "尚未刷新"
                        else -> "更新于 " + formatRelative(entry.snapshot!!.fetchedAt)
                    },
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariantSummary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (entry.service.enabled) "停用" else "启用",
                    fontSize = 12.sp,
                    color = cs.primary,
                    modifier = Modifier.clickable { onToggle(!entry.service.enabled) }.padding(4.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "编辑",
                    fontSize = 12.sp,
                    color = cs.primary,
                    modifier = Modifier.clickable { onEdit() }.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun QuotaDisplay(svc: ServiceConfig, result: QuotaResult?) {
    val cs = MiuixTheme.colorScheme
    val onSurface = cs.onSurface
    val onSurfaceVariant = cs.onSurfaceVariantSummary

    if (result == null) {
        Text("—", fontSize = 13.sp, color = onSurfaceVariant)
        return
    }

    if (!result.isValid) {
        Text(result.invalidMessage ?: "查询失败", fontSize = 13.sp, color = ErrColor)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (result.planName != null) {
            Text(result.planName, fontSize = 12.sp, color = onSurfaceVariant)
        }
        if (result.tiers.isNotEmpty()) {
            result.tiers.take(2).forEach { tier -> TierLine(tier) }
        } else {
            val remaining = result.remaining
            val total = result.total
            val used = result.used
            if (remaining != null || total != null || used != null) {
                val pct = when {
                    total != null && total > 0 -> ((used ?: (total - (remaining ?: 0.0))) / total).coerceIn(0.0, 1.0)
                    else -> null
                }
                val pctText = if (pct != null) "%.0f%%".format(pct * 100) else "—"
                val tone = when {
                    pct == null -> onSurfaceVariant
                    pct >= 0.85 -> ErrColor
                    pct >= 0.6 -> WarnColor
                    else -> OkColor
                }
                LinearProgressIndicator(
                    progress = { (pct ?: 0.0).toFloat() },
                    color = tone,
                    trackColor = cs.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = listOfNotNull(remaining, total).joinToString(" / ") { "%.2f".format(it) } + " " + result.unit.name,
                        fontSize = 12.sp,
                        color = onSurface,
                    )
                    Text(pctText, fontSize = 12.sp, color = tone, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TierLine(tier: QuotaTier) {
    val cs = MiuixTheme.colorScheme
    val pct = tier.utilization.coerceIn(0.0, 100.0) / 100.0
    val tone = when {
        pct >= 0.85 -> ErrColor
        pct >= 0.6 -> WarnColor
        else -> OkColor
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tierLabel(tier.name), fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
            Text("%.0f%%".format(pct * 100), fontSize = 12.sp, color = tone, fontWeight = FontWeight.Medium)
        }
        LinearProgressIndicator(
            progress = { pct.toFloat() },
            color = tone,
            trackColor = cs.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
}

private fun tierLabel(name: String): String = when (name) {
    "five_hour" -> "5h 窗口"
    "seven_day" -> "7d 窗口"
    "weekly_limit" -> "周限额"
    "monthly" -> "月限额"
    "30_day" -> "30 天"
    "credits" -> "积分"
    "secondary" -> "次要窗口"
    "gemini_pro" -> "Gemini Pro"
    "gemini_flash" -> "Gemini Flash"
    "gemini_flash_lite" -> "Gemini Flash Lite"
    else -> name
}

private fun formatRelative(ts: Long): String {
    val diff = (System.currentTimeMillis() - ts).coerceAtLeast(0)
    val mins = diff / 60_000
    return when {
        mins < 1 -> "刚刚"
        mins < 60 -> "${mins} 分钟前"
        mins < 1440 -> "${mins / 60} 小时前"
        else -> "${mins / 1440} 天前"
    }
}
