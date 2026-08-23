package com.github.tinggalleaf.ai_quota_dashboard.core.template

/**
 * Tiny `{{var}}` substitution engine.
 *
 * No external dependency — the entire template language is just one regex
 * and a map lookup. We pre-compile by extracting variables so the UI can
 * prompt the user for missing values.
 */
object TemplateEngine {

    private val PLACEHOLDER = Regex("""\{\{\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\}\}""")

    fun render(template: String, vars: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match ->
            val key = match.groupValues[1]
            vars[key] ?: throw IllegalArgumentException(
                "未提供变量: {{${key}}}  (已提供: ${vars.keys})"
            )
        }

    fun renderOrEmpty(template: String, vars: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match ->
            val key = match.groupValues[1]
            vars[key].orEmpty()
        }

    fun extractVariables(template: String): List<String> =
        PLACEHOLDER.findAll(template).map { it.groupValues[1] }.distinct().toList()
}
