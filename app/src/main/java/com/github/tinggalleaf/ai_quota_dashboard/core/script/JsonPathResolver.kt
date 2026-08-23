package com.github.tinggalleaf.ai_quota_dashboard.core.script

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Resolves dot/bracket path expressions like `data.limits[0].percentage`
 * against a parsed JSON tree. The grammar is intentionally tiny:
 *
 *   path      := segment ( '.' segment | '[' index ']' )*
 *   segment   := identifier
 *   index     := integer | '?' filter
 *   filter    := '@.field == value'  (very small subset)
 */
object JsonPathResolver {

    /** Returns the value at [path] or null if any segment is missing. */
    fun resolve(root: JsonElement, path: String): JsonElement? {
        if (path.isEmpty()) return root
        val segments = tokenize(path)
        var current: JsonElement? = root
        for (seg in segments) {
            current = when (seg) {
                is Seg.Key -> (current as? JsonObject)?.get(seg.name)
                is Seg.Index -> (current as? JsonArray)?.getOrNull(seg.n)
                is Seg.Filter -> filterArray(current, seg)
            } ?: return null
        }
        return current
    }

    /** Convenience for primitive extraction. */
    fun resolveString(root: JsonElement, path: String): String? =
        (resolve(root, path) as? JsonPrimitive)?.contentOrNull
            ?: (resolve(root, path) as? JsonPrimitive)?.toString()

    fun resolveNumber(root: JsonElement, path: String): Double? {
        val el = resolve(root, path) as? JsonPrimitive ?: return null
        return el.doubleOrNull ?: el.longOrNull?.toDouble() ?: el.contentOrNull?.toDoubleOrNull()
    }

    fun resolveBoolean(root: JsonElement, path: String): Boolean? =
        (resolve(root, path) as? JsonPrimitive)?.booleanOrNull

    fun resolveArray(root: JsonElement, path: String): List<JsonElement>? =
        (resolve(root, path) as? JsonArray)?.toList()

    // ─── internal ────────────────────────────────────────────────────────

    private sealed interface Seg {
        data class Key(val name: String) : Seg
        data class Index(val n: Int) : Seg
        data class Filter(val field: String, val op: Op, val value: String) : Seg
    }

    private enum class Op { EQ, NEQ }

    private fun tokenize(path: String): List<Seg> {
        val out = mutableListOf<Seg>()
        val buf = StringBuilder()
        var i = 0
        while (i < path.length) {
            val c = path[i]
            when {
                c == '.' -> {
                    flushKey(buf, out)
                    i++
                }
                c == '[' -> {
                    flushKey(buf, out)
                    val end = path.indexOf(']', i)
                    if (end < 0) error("未闭合的 '[' 在: $path")
                    val inner = path.substring(i + 1, end).trim()
                    if (inner.startsWith("?")) {
                        // filter: ?(@.field op value)
                        val filterBody = inner.removePrefix("?").trim().removeSurrounding("(", ")")
                        out.add(parseFilter(filterBody))
                    } else {
                        val n = inner.toIntOrNull()
                            ?: error("非法索引 '$inner' 于: $path")
                        out.add(Seg.Index(n))
                    }
                    i = end + 1
                }
                else -> {
                    buf.append(c)
                    i++
                }
            }
        }
        flushKey(buf, out)
        return out
    }

    private fun flushKey(buf: StringBuilder, out: MutableList<Seg>) {
        if (buf.isNotEmpty()) {
            out.add(Seg.Key(buf.toString()))
            buf.clear()
        }
    }

    private fun parseFilter(body: String): Seg.Filter {
        // supported forms:
        //   @.field == "value"
        //   @.field != "value"
        //   @.field == 123
        val trimmed = body.trim()
        val eqIdx = listOf("==", "!=").mapNotNull { op -> trimmed.indexOf(op).takeIf { it > 0 }?.let { it to op } }
            .minByOrNull { it.first }
            ?: error("不支持的过滤器: $body")
        val (pos, op) = eqIdx
        val lhs = trimmed.substring(0, pos).trim()
        val rhs = trimmed.substring(pos + op.length).trim()
        val field = lhs.removePrefix("@.").removePrefix("@")
        val value = rhs.trim('"')
        return Seg.Filter(field, if (op == "==") Op.EQ else Op.NEQ, value)
    }

    private fun filterArray(current: JsonElement?, seg: Seg.Filter): JsonElement? {
        val arr = current as? JsonArray ?: return null
        val first = arr.firstOrNull { item ->
            val v = (item as? JsonObject)?.get(seg.field)
            val primitive = (v as? JsonPrimitive)?.contentOrNull
            when (seg.op) {
                Op.EQ -> primitive == seg.value
                Op.NEQ -> primitive != seg.value
            }
        }
        return first ?: JsonNull
    }
}
