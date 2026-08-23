package com.github.tinggalleaf.ai_quota_dashboard.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import com.github.tinggalleaf.ai_quota_dashboard.ui.components.ProviderIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PresetPickerScreen(
    presets: List<ServiceConfig>,
    onPick: (ServiceConfig) -> Unit,
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
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "选择预设",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    "点击添加,稍后填写变量值",
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariantSummary,
                )
            }
        }

        items(presets, key = { it.id }) { preset ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onPick(preset) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProviderIcon(service = preset, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            preset.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                        )
                        Text(
                            preset.note ?: preset.urlTemplate,
                            fontSize = 12.sp,
                            color = cs.onSurfaceVariantSummary,
                            maxLines = 2,
                        )
                    }
                    Box(Modifier.padding(start = 8.dp)) {
                        Text(
                            "+",
                            fontSize = 22.sp,
                            color = cs.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
