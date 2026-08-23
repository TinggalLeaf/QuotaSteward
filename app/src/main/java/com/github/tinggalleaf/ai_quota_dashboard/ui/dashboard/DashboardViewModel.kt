package com.github.tinggalleaf.ai_quota_dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.tinggalleaf.ai_quota_dashboard.ServiceLocator
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.DashboardEntry
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.QuotaRepository
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.QuotaSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repo: QuotaRepository,
) : ViewModel() {

    /** id → currently-refreshing flag. */
    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing: StateFlow<Set<String>> = _refreshing.asStateFlow()

    val entries: StateFlow<List<DashboardEntry>> = repo.dashboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val builtInPresets: List<ServiceConfig> get() = repo.builtInPresets

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun refreshOne(id: String) = viewModelScope.launch {
        val entry = repo.dashboard.first().firstOrNull { it.service.id == id } ?: return@launch
        fetchOne(entry.service)
    }

    fun refreshAll() = viewModelScope.launch {
        val list = repo.dashboard.first()
        list.forEach { fetchOne(it.service) }
    }

    private suspend fun fetchOne(svc: ServiceConfig) {
        _refreshing.value = _refreshing.value + svc.id
        try {
            val result = repo.fetcher.fetch(svc, svc.variableValues)
            repo.publishSnapshot(svc.id, QuotaSnapshot(result = result, fetchedAt = System.currentTimeMillis()))
        } catch (e: Throwable) {
            repo.publishSnapshot(
                svc.id,
                QuotaSnapshot(
                    result = com.github.tinggalleaf.ai_quota_dashboard.data.model.QuotaResult(
                        isValid = false, invalidMessage = e.message
                    ),
                    fetchedAt = System.currentTimeMillis(),
                    error = e.message,
                ),
            )
        } finally {
            _refreshing.value = _refreshing.value - svc.id
        }
    }

    fun toggleEnabled(svc: ServiceConfig, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(svc.id, enabled)
        if (enabled) refreshOne(svc.id)
    }

    fun remove(svc: ServiceConfig) = viewModelScope.launch { repo.remove(svc.id) }

    fun addPreset(preset: ServiceConfig) = viewModelScope.launch {
        repo.upsert(preset.copy(id = preset.id + "_${System.currentTimeMillis()}"))
        refreshOne(repo.dashboard.first().last().service.id)
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                val settings = repo.settings.first()
                val intervalSec = settings.defaultRefreshSec.coerceAtLeast(30)
                val list = repo.dashboard.first().filter { it.service.enabled }
                list.forEach { entry ->
                    val snapshot = repo.snapshots.value[entry.service.id]
                    val due = snapshot == null ||
                        System.currentTimeMillis() - snapshot.fetchedAt > intervalSec * 1000
                    if (due) fetchOne(entry.service)
                }
                delay(intervalSec * 1000L)
            }
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(ServiceLocator.repository) as T
        }
    }
}
