package com.github.tinggalleaf.ai_quota_dashboard.core.net

import com.github.tinggalleaf.ai_quota_dashboard.core.script.ExtractorInterpreter
import com.github.tinggalleaf.ai_quota_dashboard.core.script.ExtractorScript
import com.github.tinggalleaf.ai_quota_dashboard.core.template.TemplateEngine
import com.github.tinggalleaf.ai_quota_dashboard.data.model.HttpMethod
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaResult
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Performs an HTTP quota query and runs the extractor script against the
 * JSON response.
 */
class QuotaFetcher(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    },
) {

    suspend fun fetch(service: ServiceConfig, vars: Map<String, String>): QuotaResult {
        // 1. Substitute placeholders.
        val merged = service.variableValues + vars
        val url = try {
            TemplateEngine.render(service.urlTemplate, merged)
        } catch (e: Throwable) {
            return QuotaResult(isValid = false, invalidMessage = "URL 模板错误: ${e.message}")
        }
        if (url.isBlank()) {
            return QuotaResult(isValid = false, invalidMessage = "未配置 URL")
        }

        val renderedHeaders = service.headers.associate { (k, v) ->
            k to TemplateEngine.renderOrEmpty(v, merged)
        }

        val body = service.bodyTemplate?.let { TemplateEngine.renderOrEmpty(it, merged) }

        val builder = Request.Builder().url(url)
        renderedHeaders.forEach { (k, v) -> builder.addHeader(k, v) }

        when (service.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> {
                val rb = (body ?: "{}").toRequestBody("application/json".toMediaType())
                builder.post(rb)
            }
        }

        val request = builder.build()

        // 2. Execute.
        return runCatching {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@use QuotaResult(
                        isValid = false,
                        invalidMessage = "HTTP ${resp.code} ${resp.message}"
                    )
                }
                val element = runCatching { json.parseToJsonElement(raw) }.getOrElse {
                    return@use QuotaResult(
                        isValid = false,
                        invalidMessage = "响应不是合法 JSON: ${it.message?.take(120)}"
                    )
                }
                // 3. Run extractor.
                val script = runCatching {
                    json.decodeFromString(ExtractorScript.serializer(), service.scriptSource)
                }.getOrElse { e ->
                    return@use QuotaResult(
                        isValid = false,
                        invalidMessage = "提取器脚本错误: ${e.message?.take(120)}"
                    )
                }
                ExtractorInterpreter.run(script, element)
            }
        }.getOrElse { e ->
            QuotaResult(isValid = false, invalidMessage = "网络错误: ${e.message?.take(120)}")
        }
    }

    companion object {
        fun defaultClient(connectSec: Long = 10, requestSec: Long = 30): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(connectSec, TimeUnit.SECONDS)
                .readTimeout(requestSec, TimeUnit.SECONDS)
                .writeTimeout(requestSec, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
    }
}
