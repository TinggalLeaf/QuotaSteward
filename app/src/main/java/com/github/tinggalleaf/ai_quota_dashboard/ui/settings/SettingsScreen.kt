package com.github.tinggalleaf.ai_quota_dashboard.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tinggalleaf.ai_quota_dashboard.ServiceLocator
import com.github.tinggalleaf.ai_quota_dashboard.data.datastore.SettingsDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.model.AppSettings
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ColorPalette
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsViewModel(
    private val store: SettingsDataStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        store.update(transform)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(ServiceLocator.settingsStore) as T
        }
    }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsState()
    val cs = MiuixTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item { SmallTitle("外观") }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    DropdownPreference(
                        title = "主题",
                        summary = themeLabel(settings.themeMode),
                        options = ThemeMode.values().map { themeLabel(it) },
                        selectedIndex = settings.themeMode.ordinal,
                        onSelected = { idx -> vm.update { it.copy(themeMode = ThemeMode.values()[idx]) } },
                    )
                    DropdownPreference(
                        title = "色彩预设",
                        summary = settings.colorPalette.displayName,
                        options = ColorPalette.values().map { it.displayName },
                        selectedIndex = settings.colorPalette.ordinal,
                        onSelected = { idx -> vm.update { it.copy(colorPalette = ColorPalette.values()[idx]) } },
                    )
                    SwitchPreference(
                        title = "动态取色 (Material You)",
                        summary = "跟随系统壁纸取色,仅 Android 12+ 生效",
                        checked = settings.useDynamicColor,
                        onCheckedChange = { v -> vm.update { it.copy(useDynamicColor = v) } },
                    )
                    PalettePreviewRow(settings.colorPalette)
                }
            }
        }

        item { SmallTitle("刷新") }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    SliderPreference(
                        title = "默认刷新间隔",
                        summary = "${settings.defaultRefreshSec / 60} 分钟",
                        value = settings.defaultRefreshSec.toFloat(),
                        onValueChange = { v -> vm.update { it.copy(defaultRefreshSec = v.toLong()) } },
                        valueRange = 30f..3600f,
                    )
                    SwitchPreference(
                        title = "配额不足时通知",
                        summary = "剩余不足 ${settings.lowQuotaThresholdPct}% 时提醒",
                        checked = settings.notifyOnLowQuota,
                        onCheckedChange = { v -> vm.update { it.copy(notifyOnLowQuota = v) } },
                    )
                    SliderPreference(
                        title = "低配额阈值",
                        summary = "${settings.lowQuotaThresholdPct}%",
                        value = settings.lowQuotaThresholdPct.toFloat(),
                        onValueChange = { v -> vm.update { it.copy(lowQuotaThresholdPct = v.toInt()) } },
                        valueRange = 5f..50f,
                    )
                }
            }
        }

        item { SmallTitle("网络") }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("User-Agent", fontSize = 14.sp, color = cs.onSurface)
                            Text(
                                settings.userAgent.take(40),
                                fontSize = 12.sp,
                                color = cs.onSurfaceVariantSummary,
                            )
                        }
                    }
                    SliderPreference(
                        title = "连接超时",
                        summary = "${settings.connectTimeoutSec} 秒",
                        value = settings.connectTimeoutSec.toFloat(),
                        onValueChange = { v -> vm.update { it.copy(connectTimeoutSec = v.toLong()) } },
                        valueRange = 3f..60f,
                    )
                    SliderPreference(
                        title = "请求超时",
                        summary = "${settings.requestTimeoutSec} 秒",
                        value = settings.requestTimeoutSec.toFloat(),
                        onValueChange = { v -> vm.update { it.copy(requestTimeoutSec = v.toLong()) } },
                        valueRange = 5f..120f,
                    )
                    SwitchPreference(
                        title = "显示路由警告",
                        summary = "当服务需要外网访问时提示",
                        checked = settings.showRouteWarning,
                        onCheckedChange = { v -> vm.update { it.copy(showRouteWarning = v) } },
                    )
                }
            }
        }

        item { SmallTitle("关于") }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("版本", fontSize = 14.sp, color = cs.onSurface)
                            Text("1.0.0", fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("数据源", fontSize = 14.sp, color = cs.onSurface)
                            Text("cc-switch 兼容", fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.padding(top = 24.dp)) }
    }
}

// ──────── custom preference widgets (Miuix 0.8.8 has no preference package
//          compatible with our Kotlin version, so we render rows manually) ────────

@Composable
private fun SwitchPreference(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
            if (summary != null) {
                Text(summary, fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderPreference(
    title: String,
    summary: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val cs = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
                if (summary != null) {
                    Text(summary, fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun DropdownPreference(
    title: String,
    summary: String?,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val cs = MiuixTheme.colorScheme
    var pickerExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { pickerExpanded = !pickerExpanded }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
            if (summary != null) {
                Text(summary, fontSize = 12.sp, color = cs.onSurfaceVariantSummary)
            }
        }
        Text(if (pickerExpanded) "▲" else "▾", fontSize = 14.sp, color = cs.onSurfaceVariantSummary)
    }
    if (pickerExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                options.forEachIndexed { idx, opt ->
                    val selected = idx == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(idx)
                                pickerExpanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (selected) "✓" else " ",
                            fontSize = 14.sp,
                            color = if (selected) cs.primary else cs.onSurfaceVariantSummary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(opt, fontSize = 14.sp, color = cs.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePreviewRow(current: ColorPalette) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("预览", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val swatches = listOf(
                Color(0xFF3A3A3D), Color(0xFFFF6F00), Color(0xFF1FAA86),
                Color(0xFFFF7A59), Color(0xFF1F66D9), Color(0xFF7B5AC7),
            )
            swatches.forEachIndexed { idx, c ->
                Box(
                    Modifier
                        .size(if (idx == current.ordinal) 28.dp else 20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c),
                )
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
