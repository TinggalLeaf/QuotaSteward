package com.github.tinggalleaf.ai_quota_dashboard.core.script

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Declarative extractor script.
 *
 * Each preset (or user-added service) ships with one of these so the UI
 * layer can pull quota numbers out of the raw HTTP response without
 * running a real scripting engine.
 *
 * Example (NewAPI):
 * {
 *   "validWhen":   "response.success == true",
 *   "planName":    { "path": "response.data.group", "default": "默认套餐" },
 *   "remaining":   { "path": "response.data.quota",        "divide": 500000 },
 *   "used":        { "path": "response.data.used_quota",  "divide": 500000 },
 *   "total":       { "expr": "remaining + used" },
 *   "unit":        "USD"
 * }
 */
@Serializable
data class ExtractorScript(
    val validWhen: String? = null,
    val planName: FieldBinding? = null,
    val remaining: FieldBinding? = null,
    val used: FieldBinding? = null,
    val total: FieldBinding? = null,
    val resetTime: FieldBinding? = null,
    val resetHumanReadable: FieldBinding? = null,
    val invalidMessage: FieldBinding? = null,
    val unit: String? = null,
    val tiers: List<TierConfig> = emptyList(),
    val extra: Map<String, FieldBinding> = emptyMap(),
)

@Serializable
data class TierConfig(
    val name: String,
    val utilization: FieldBinding? = null,
    val remaining: FieldBinding? = null,
    val used: FieldBinding? = null,
    val total: FieldBinding? = null,
    val resetTime: FieldBinding? = null,
)

@Serializable
data class FieldBinding(
    val path: String? = null,
    val expr: String? = null,
    val literal: JsonElement? = null,
    val divide: Double? = null,
    val multiply: Double? = null,
    @SerialName("default") val defaultValue: JsonElement? = null,
    val transform: String? = null,
)
