package com.github.tinggalleaf.ai_quota_dashboard.ui.editor

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tinggalleaf.ai_quota_dashboard.data.model.HttpHeader
import com.github.tinggalleaf.ai_quota_dashboard.data.model.HttpMethod
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Add/Edit screen. The screen is local-state-only for v1 — user types the
 * fields, hits Save, and we persist a fresh ServiceConfig.
 */
@Composable
fun EditorScreen(
    initial: ServiceConfig?,
    onSave: (ServiceConfig) -> Unit,
    onCancel: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val cs = MiuixTheme.colorScheme

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var url by remember { mutableStateOf(initial?.urlTemplate ?: "") }
    var method by remember { mutableStateOf(initial?.method ?: HttpMethod.GET) }
    var iconAsset by remember { mutableStateOf(initial?.iconAsset ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var headersText by remember {
        mutableStateOf(initial?.headers?.joinToString("\n") { "${it.key}: ${it.value}" } ?: "")
    }
    var varsText by remember { mutableStateOf(initial?.variables?.joinToString(", ") ?: "") }
    var valuesText by remember {
        mutableStateOf(initial?.variableValues?.entries?.joinToString("\n") { "${it.key}=${it.value}" } ?: "")
    }
    var scriptSource by remember { mutableStateOf(initial?.scriptSource ?: DEFAULT_SCRIPT) }
    var unit by remember { mutableStateOf(initial?.unit?.name ?: "REQUESTS") }

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
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    if (initial == null) "添加服务" else "编辑服务",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    "填写 URL 模板 (支持 {{var}}) 与提取器脚本",
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariantSummary,
                )
            }
        }

        item { SmallTitle("基本信息") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "URL 模板 (支持 {{baseUrl}}, {{apiKey}})",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = iconAsset,
                        onValueChange = { iconAsset = it },
                        label = "图标文件名 (在 assets/provider-icons/)",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "备注",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = "单位 (USD / CNY / TOKENS / REQUESTS)",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = varsText,
                        onValueChange = { varsText = it },
                        label = "变量名 (逗号分隔,如 baseUrl, apiKey)",
                        useLabelAsPlaceholder = true,
                    )
                    TextField(
                        value = valuesText,
                        onValueChange = { valuesText = it },
                        label = "变量值 (每行 key=value)",
                        useLabelAsPlaceholder = true,
                    )
                }
            }
        }

        item { SmallTitle("请求头 (一行一项, key: value)") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TextField(
                        value = headersText,
                        onValueChange = { headersText = it },
                        label = "Authorization: Bearer {{apiKey}}",
                        maxLines = 8,
                    )
                }
            }
        }

        item { SmallTitle("提取器脚本 (JSON DSL)") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TextField(
                        value = scriptSource,
                        onValueChange = { scriptSource = it },
                        label = "Extractor JSON",
                        maxLines = 14,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.padding(top = 8.dp))
            RowButtons(
                onCancel = onCancel,
                onSave = {
                    val headers = headersText.lines()
                        .mapNotNull { line ->
                            val idx = line.indexOf(':')
                            if (idx > 0) HttpHeader(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
                            else if (line.isNotBlank()) HttpHeader(line.trim(), "")
                            else null
                        }
                    val variables = varsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    val variableValues = valuesText.lines()
                        .mapNotNull { line ->
                            val idx = line.indexOf('=')
                            if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                            else null
                        }.toMap()
                    onSave(
                        (initial ?: ServiceConfig(
                            id = "svc_${System.currentTimeMillis()}",
                            name = name.ifBlank { "新服务" },
                            urlTemplate = url,
                            scriptSource = scriptSource,
                        )).copy(
                            name = name.ifBlank { "新服务" },
                            urlTemplate = url,
                            method = method,
                            iconAsset = iconAsset.ifBlank { null },
                            note = note.ifBlank { null },
                            headers = headers,
                            variables = variables,
                            variableValues = variableValues,
                            scriptSource = scriptSource,
                            unit = runCatching { com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaUnit.valueOf(unit.uppercase()) }
                                .getOrDefault(com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaUnit.REQUESTS),
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun RowButtons(onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        ) { Text("取消") }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) { Text("保存") }
    }
}

private const val DEFAULT_SCRIPT = """{
  "validWhen": "response.success == true",
  "remaining": { "path": "response.data.balance" },
  "unit": "USD"
}"""