package com.github.tinggalleaf.ai_quota_dashboard.data.repo

import android.content.Context
import com.github.tinggalleaf.ai_quota_dashboard.core.net.QuotaFetcher
import com.github.tinggalleaf.ai_quota_dashboard.data.datastore.ServicesDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.datastore.SettingsDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaResult
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import com.github.tinggalleaf.ai_quota_dashboard.data.preset.PresetLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository: source of truth for service configs, settings, and live
 * quota snapshots.
 *
 * Holds an in-memory cache of the last fetched [QuotaResult] per service
 * keyed by id. The UI observes this map directly.
 */
class QuotaRepository(
    private val context: Context,
    private val servicesStore: ServicesDataStore,
    private val settingsStore: SettingsDataStore,
    private val presetLoader: PresetLoader,
    val fetcher: QuotaFetcher,
) {

    /** All services the user has configured, including built-in presets the user activated. */
    val services: Flow<List<ServiceConfig>> = servicesStore.services

    val settings = settingsStore.settings

    /** id → last QuotaResult (and timestamp). */
    private val _snapshots = MutableStateFlow<Map<String, QuotaSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, QuotaSnapshot>> = _snapshots.asStateFlow()

    /** Built-in presets, loaded from assets. */
    val builtInPresets: List<ServiceConfig> by lazy { presetLoader.loadAll() }

    /** Convenience: combine services + snapshots to a UI-ready list. */
    val dashboard: Flow<List<DashboardEntry>> = combine(services, snapshots) { list, snaps ->
        list.map { svc ->
            DashboardEntry(svc, snaps[svc.id])
        }
    }

    suspend fun upsert(service: ServiceConfig) = servicesStore.upsert(service)

    suspend fun remove(id: String) {
        servicesStore.remove(id)
        clearSnapshot(id)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = servicesStore.setEnabled(id, enabled)

    fun publishSnapshot(id: String, snapshot: QuotaSnapshot) {
        _snapshots.value = _snapshots.value + (id to snapshot)
    }

    fun clearSnapshot(id: String) {
        _snapshots.value = _snapshots.value - id
    }
}

data class QuotaSnapshot(
    val result: QuotaResult,
    val fetchedAt: Long,
    val error: String? = null,
)

data class DashboardEntry(
    val service: ServiceConfig,
    val snapshot: QuotaSnapshot? = null,
)
