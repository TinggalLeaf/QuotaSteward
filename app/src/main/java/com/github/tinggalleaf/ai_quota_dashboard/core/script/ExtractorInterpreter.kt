package com.github.tinggalleaf.ai_quota_dashboard.core.script

import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaResult
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaTier
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Executes an [ExtractorScript] against the JSON tree returned by the
 * HTTP layer and produces a normalized [QuotaResult].
 *
 * The interpreter handles:
 *   - simple JSON path reads
 *   - arithmetic over previously-bound fields (+, -, *, /, parentheses)
 *   - named transforms (first, sum, min, max, toBool, flip100)
 *   - default fallbacks when reads fail
 *
 * NOTE: This is deliberately NOT a full expression language. The grammar
 * for [FieldBinding.expr] is:
 *
 *   expr := term ( ('+' | '-') term )*
 *   term := factor ( ('*' | '/') factor )*
 *   factor := NUMBER | IDENT | '(' expr ')' | '-' factor
 *
 * Variable names must already be present in [bindings] when the [FieldBinding]
 * is evaluated, so order matters: declare [planName] before [total] etc.
 */
object ExtractorInterpreter {

    fun run(script: ExtractorScript, response: JsonElement): QuotaResult {
        val bindings = mutableMapOf<String, JsonElement>()

        fun binding(name: String, b: FieldBinding?) {
            if (b == null) return
            val v = evaluate(b, response, bindings)
            if (v != null && v != JsonNull) bindings[name] = v
        }

        // pre-evaluate simple path bindings first
        binding("planName", script.planName)
        binding("remaining", script.remaining)
        binding("used", script.used)
        binding("total", script.total)
        binding("resetTime", script.resetTime)
        binding("resetHumanReadable", script.resetHumanReadable)
        binding("invalidMessage", script.invalidMessage)

        val isValid = evaluateValid(script.validWhen, response, bindings)
        val invalidMessage = (bindings["invalidMessage"] as? JsonPrimitive)?.contentOrNull
        val planName = (bindings["planName"] as? JsonPrimitive)?.contentOrNull
        val remaining = numericValue(bindings["remaining"])
        val used = numericValue(bindings["used"])
        val total = numericValue(bindings["total"])
        val resetTime = (bindings["resetTime"] as? JsonPrimitive)?.contentOrNull
        val resetHuman = (bindings["resetHumanReadable"] as? JsonPrimitive)?.contentOrNull

        val unit = script.unit
            ?.let { runCatching { QuotaUnit.valueOf(it.uppercase()) }.getOrNull() }
            ?: QuotaUnit.REQUESTS

        val tiers = script.tiers.mapNotNull { tier ->
            val tBindings = mutableMapOf<String, JsonElement>()
            val util = tier.utilization?.let { evaluate(it, response, bindings + tBindings) }
                ?.let(::numericValue)
                ?: return@mapNotNull null
            tBindings["remaining"] = tier.remaining
                ?.let { evaluate(it, response, bindings + tBindings) }
                ?: JsonNull
            tBindings["used"] = tier.used
                ?.let { evaluate(it, response, bindings + tBindings) }
                ?: JsonNull
            tBindings["total"] = tier.total
                ?.let { evaluate(it, response, bindings + tBindings) }
                ?: JsonNull
            QuotaTier(
                name = tier.name,
                utilization = util,
                resetsAt = (tier.resetTime?.let { evaluate(it, response, bindings + tBindings) } as? JsonPrimitive)?.contentOrNull,
                remaining = numericValue(tBindings["remaining"]),
                used = numericValue(tBindings["used"]),
                total = numericValue(tBindings["total"]),
            )
        }

        val extra = script.extra.mapValues { (_, b) ->
            evaluate(b, response, bindings) ?: JsonNull
        }.filterValues { it != JsonNull }

        return QuotaResult(
            isValid = isValid,
            invalidMessage = invalidMessage,
            planName = planName,
            remaining = remaining,
            used = used,
            total = total,
            unit = unit,
            resetTime = resetTime,
            resetHumanReadable = resetHuman,
            tiers = tiers,
            extra = extra,
        )
    }

    // ─── evaluation primitives ──────────────────────────────────────────

    private fun evaluateValid(
        expr: String?,
        response: JsonElement,
        bindings: Map<String, JsonElement>,
    ): Boolean {
        if (expr.isNullOrBlank()) return true
        return try {
            val r = ArithmeticParser(expr).eval { ident ->
                bindings[ident] ?: response.let { JsonPathResolver.resolve(it, ident.removePrefix("response.").let { p -> if (ident.startsWith("response.")) "response.${p}" else ident }) }
            }
            (r as? JsonPrimitive)?.booleanOrNull ?: true
        } catch (_: Throwable) {
            true
        }
    }

    fun evaluate(
        b: FieldBinding,
        response: JsonElement,
        bindings: Map<String, JsonElement>,
    ): JsonElement? {
        var raw: JsonElement? = null

        // 1. start from path
        if (b.path != null) raw = JsonPathResolver.resolve(response, b.path)

        // 2. or from expr over already-bound names
        if (raw == null && b.expr != null) {
            raw = try {
                ArithmeticParser(b.expr).eval { ident ->
                    if (ident == "response") response
                    else bindings[ident] ?: JsonPathResolver.resolve(response, ident)
                }
            } catch (_: Throwable) {
                null
            }
        }

        // 3. or from literal
        if (raw == null && b.literal != null) raw = b.literal

        // 4. transforms
        if (raw != null && raw !is JsonNull && b.transform != null) {
            raw = applyTransform(raw, b.transform)
        }

        // 5. arithmetic
        if (raw != null && raw !is JsonNull) {
            if (b.divide != null && b.divide != 0.0) {
                val n = numericValue(raw)
                if (n != null) raw = JsonPrimitive(n / b.divide)
            }
            if (b.multiply != null) {
                val n = numericValue(raw)
                if (n != null) raw = JsonPrimitive(n * b.multiply)
            }
        }

        // 6. default fallback
        if ((raw == null || raw is JsonNull) && b.defaultValue != null) raw = b.defaultValue
        return raw
    }

    private fun applyTransform(el: JsonElement, name: String): JsonElement? = when (name) {
        "first" -> when (el) {
            is JsonArray -> el.firstOrNull() ?: JsonNull
            else -> el
        }
        "sum" -> JsonPrimitive(sum(el))
        "min" -> JsonPrimitive(minMax(el, pickMax = false))
        "max" -> JsonPrimitive(minMax(el, pickMax = true))
        "toBool" -> JsonPrimitive((el as? JsonPrimitive)?.booleanOrNull ?: false)
        "flip100" -> JsonPrimitive(((el as? JsonPrimitive)?.doubleOrNull ?: 0.0).let { 100.0 - it })
        else -> el
    }

    private fun sum(el: JsonElement): Double {
        val arr = el as? JsonArray ?: return 0.0
        return arr.sumOf { (it as? JsonPrimitive)?.doubleOrNull ?: 0.0 }
    }

    private fun minMax(el: JsonElement, pickMax: Boolean): Double {
        val arr = el as? JsonArray ?: return 0.0
        val nums = arr.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
        return if (pickMax) nums.maxOrNull() ?: 0.0 else nums.minOrNull() ?: 0.0
    }

    fun numericValue(el: JsonElement?): Double? = when (el) {
        null, JsonNull -> null
        is JsonPrimitive -> el.doubleOrNull
            ?: el.longOrNull?.toDouble()
            ?: el.contentOrNull?.toDoubleOrNull()
        else -> null
    }
}
