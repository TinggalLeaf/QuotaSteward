package com.github.tinggalleaf.ai_quota_dashboard.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.servicesStore: DataStore<Preferences> by preferencesDataStore(name = "services")

/**
 * DataStore-backed repository for the user's configured services.
 *
 * Stores the entire list as a single JSON string under one key. At the
 * expected scale (~100 services) this is simpler and faster than Proto.
 */
class ServicesDataStore(
    private val context: Context,
    private val json: Json = defaultJson,
) {

    val services: Flow<List<ServiceConfig>> = context.servicesStore.data.map { prefs ->
        decode(prefs[KEY_SERVICES])
    }

    suspend fun replace(list: List<ServiceConfig>) {
        val text = json.encodeToString(ListSerializer(ServiceConfig.serializer()), list)
        context.servicesStore.edit { it[KEY_SERVICES] = text }
    }

    suspend fun upsert(service: ServiceConfig) {
        val current = readDecoded()
        val next = current.filterNot { it.id == service.id } + service
        replace(next)
    }

    suspend fun remove(id: String) {
        val current = readDecoded()
        replace(current.filterNot { it.id == id })
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val current = readDecoded()
        replace(current.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    private suspend fun readDecoded(): List<ServiceConfig> {
        val prefs = context.servicesStore.data.first()
        return decode(prefs[KEY_SERVICES])
    }

    private fun decode(raw: String?): List<ServiceConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ServiceConfig.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val KEY_SERVICES = stringPreferencesKey("services_json")

        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
}
