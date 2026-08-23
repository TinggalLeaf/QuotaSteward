package com.github.tinggalleaf.ai_quota_dashboard.core.script

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Tiny arithmetic evaluator supporting:
 *   - numbers
 *   - identifiers resolved through [lookup] (returning a [JsonElement])
 *   - + - * / and parentheses
 *   - unary minus
 *   - comparison ==, != (returns JsonPrimitive boolean)
 *
 * Grammar:
 *   expr     := term ((+|-) term)*
 *   term     := factor ((*|/) factor)*
 *   factor   := number | ident | (expr) | -factor | ==expr | !=expr
 */
class ArithmeticParser(
    private val src: String,
    private val lookup: (String) -> JsonElement? = { null },
) {

    private var pos = 0

    fun eval(lookupFn: (String) -> JsonElement? = lookup): JsonElement {
        pos = 0
        val v = parseExpr(lookupFn)
        skipWs()
        if (pos < src.length) throw IllegalArgumentException("解析器未消费完: ${src.substring(pos)}")
        return v
    }

    // ── grammar ────────────────────────────────────────────────────────────

    private fun parseExpr(lk: (String) -> JsonElement?): JsonElement {
        var left = parseTerm(lk)
        while (true) {
            skipWs()
            if (pos >= src.length) return left
            val op = src[pos]
            if (op != '+' && op != '-') return left
            pos++
            val right = parseTerm(lk)
            left = apply(left, op, right)
        }
    }

    private fun parseTerm(lk: (String) -> JsonElement?): JsonElement {
        var left = parseFactor(lk)
        while (true) {
            skipWs()
            if (pos >= src.length) return left
            val op = src[pos]
            if (op != '*' && op != '/') return left
            pos++
            val right = parseFactor(lk)
            left = apply(left, op, right)
        }
    }

    private fun parseFactor(lk: (String) -> JsonElement?): JsonElement {
        skipWs()
        if (pos >= src.length) throw IllegalArgumentException("表达式在位置 $pos 处截断")
        val c = src[pos]
        return when {
            c == '(' -> {
                pos++
                val v = parseExpr(lk)
                skipWs()
                if (pos >= src.length || src[pos] != ')') throw IllegalArgumentException("缺少 ')'")
                pos++
                v
            }
            c == '-' -> {
                pos++
                val v = parseFactor(lk)
                negate(v)
            }
            c == '+' -> {
                pos++
                parseFactor(lk)
            }
            c.isDigit() || c == '.' -> parseNumber()
            c.isLetter() || c == '_' -> parseIdent(lk)
            else -> throw IllegalArgumentException("意外字符 '$c' @ $pos in: $src")
        }
    }

    private fun parseNumber(): JsonElement {
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        val txt = src.substring(start, pos)
        val d = txt.toDoubleOrNull() ?: throw IllegalArgumentException("非法数字 '$txt'")
        return JsonPrimitive(d)
    }

    private fun parseIdent(lk: (String) -> JsonElement?): JsonElement {
        val start = pos
        while (pos < src.length && (src[pos].isLetterOrDigit() || src[pos] == '_' || src[pos] == '.')) pos++
        val name = src.substring(start, pos)
        skipWs()
        // comparison: name == expr  /  name != expr
        if (pos < src.length && (src[pos] == '=' || src[pos] == '!')) {
            val isEq = src[pos] == '='
            pos++
            if (pos >= src.length || src[pos] != '=') throw IllegalArgumentException("需要 '==' 或 '!='")
            pos++
            val lhs = lk(name) ?: JsonNull
            val rhs = parseExpr(lk)
            return JsonPrimitive(
                if (isEq) jsonEq(lhs, rhs) else !jsonEq(lhs, rhs)
            )
        }
        return lk(name) ?: JsonNull
    }

    private fun jsonEq(a: JsonElement, b: JsonElement): Boolean {
        if (a is JsonNull || b is JsonNull) return a === b
        val pa = a as? JsonPrimitive
        val pb = b as? JsonPrimitive
        if (pa != null && pb != null) {
            return pa.contentOrNull == pb.contentOrNull
        }
        return a.toString() == b.toString()
    }

    private fun apply(a: JsonElement, op: Char, b: JsonElement): JsonElement {
        val x = toDouble(a)
        val y = toDouble(b)
        if (x == null || y == null) return JsonNull
        val r = when (op) {
            '+' -> x + y
            '-' -> x - y
            '*' -> x * y
            '/' -> if (y == 0.0) 0.0 else x / y
            else -> error("未知算符 '$op'")
        }
        return JsonPrimitive(r)
    }

    private fun negate(v: JsonElement): JsonElement {
        val d = toDouble(v) ?: return JsonNull
        return JsonPrimitive(-d)
    }

    private fun toDouble(el: JsonElement?): Double? = when (el) {
        null, JsonNull -> null
        is JsonPrimitive -> el.doubleOrNull
            ?: el.longOrNull?.toDouble()
            ?: el.contentOrNull?.toDoubleOrNull()
            ?: if (el.booleanOrNull == true) 1.0 else if (el.booleanOrNull == false) 0.0 else null
        else -> null
    }

    private fun skipWs() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }
}
