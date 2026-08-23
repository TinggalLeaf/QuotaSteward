package com.github.tinggalleaf.ai_quota_dashboard.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Normalized quota result returned by every preset extractor.
 *
 * Mirrors the cc-switch `UsageData` shape so that all built-in and custom
 * providers expose the same fields to the UI layer.
 */
@Serializable
data class QuotaResult(
    val isValid: Boolean = true,
    val invalidMessage: String? = null,
    val planName: String? = null,
    val remaining: Double? = null,
    val used: Double? = null,
    val total: Double? = null,
    val unit: QuotaUnit = QuotaUnit.REQUESTS,
    /** ISO 8601 reset timestamp or relative delta string. */
    val resetTime: String? = null,
    /** Convenience: "5h 23m" relative to now. */
    val resetHumanReadable: String? = null,
    /** Tiered quotas for plans with multiple windows (5h + 7d, etc). */
    val tiers: List<QuotaTier> = emptyList(),
    val extra: Map<String, JsonElement>? = null,
)

@Serializable
enum class QuotaUnit {
    @SerialName("USD") USD,
    @SerialName("CNY") CNY,
    @SerialName("TOKENS") TOKENS,
    @SerialName("REQUESTS") REQUESTS,
}

@Serializable
data class QuotaTier(
    val name: String,
    /** 0-100, percent utilized. */
    val utilization: Double,
    val resetsAt: String? = null,
    val remaining: Double? = null,
    val used: Double? = null,
    val total: Double? = null,
    val unit: QuotaUnit? = null,
)
