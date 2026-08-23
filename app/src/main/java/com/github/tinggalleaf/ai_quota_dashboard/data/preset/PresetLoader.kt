package com.github.tinggalleaf.ai_quota_dashboard.data.preset

import android.content.Context
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import kotlinx.serialization.json.Json

/**
 * Loads built-in provider presets from the assets/presets directory.
 *
 * The asset folder is bundled into the APK at build time, so we just
 * enumerate it via the AssetManager and parse every .json file we find.
 */
class PresetLoader(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {

    fun loadAll(): List<ServiceConfig> {
        val manager = context.assets
        val files = runCatching {
            manager.list(ASSET_DIR)?.toList().orEmpty()
        }.getOrDefault(emptyList())

        return files.filter { it.endsWith(".json") }.mapNotNull { name ->
            runCatching {
                manager.open("$ASSET_DIR/$name").bufferedReader().use { it.readText() }
            }.getOrNull()?.let { text ->
                runCatching { json.decodeFromString(ServiceConfig.serializer(), text) }.getOrNull()
            }
        }
    }

    fun loadOne(id: String): ServiceConfig? =
        loadAll().firstOrNull { it.id == id }

    companion object {
        const val ASSET_DIR = "presets"
    }
}
