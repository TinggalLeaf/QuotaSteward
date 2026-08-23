package com.github.tinggalleaf.ai_quota_dashboard.data.model

import kotlinx.serialization.Serializable

/**
 * One row in the dashboard: a single service whose quota we track.
 *
 * The fields below describe both the request template (URL with
 * `{{baseUrl}}`-style placeholders, headers, etc.) and the script
 * that pulls quota numbers out of the response.
 */
@Serializable
data class ServiceConfig(
    val id: String,
    val name: String,
    val providerKey: String? = null,
    val urlTemplate: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: List<HttpHeader> = emptyList(),
    /** Optional JSON body for POST endpoints. */
    val bodyTemplate: String? = null,
    /** Variables that the user must supply (e.g. baseUrl, apiKey, accessToken). */
    val variables: List<String> = emptyList(),
    /** User-supplied values for the variables above. */
    val variableValues: Map<String, String> = emptyMap(),
    /** Custom JS-style extractor source. */
    val scriptSource: String,
    val unit: QuotaUnit = QuotaUnit.REQUESTS,
    val enabled: Boolean = true,
    /** Auto-refresh interval, seconds. 0 == manual only. */
    val refreshIntervalSec: Long = 300,
    /** Provider icon file inside `assets/provider-icons/`, e.g. "openai.svg". */
    val iconAsset: String? = null,
    /** Free-form note shown on the row. */
    val note: String? = null,
)

@Serializable
data class HttpHeader(
    val key: String,
    val value: String,
)

@Serializable
enum class HttpMethod { GET, POST }
