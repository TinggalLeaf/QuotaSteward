package com.github.tinggalleaf.ai_quota_dashboard

import android.content.Context
import com.github.tinggalleaf.ai_quota_dashboard.core.net.QuotaFetcher
import com.github.tinggalleaf.ai_quota_dashboard.data.datastore.ServicesDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.datastore.SettingsDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.preset.PresetLoader
import com.github.tinggalleaf.ai_quota_dashboard.data.repo.QuotaRepository

/**
 * Minimal service locator. We keep a single instance per process so all
 * screens share the same repository / data stores.
 */
object ServiceLocator {

    @Volatile private var initialized = false

    lateinit var servicesStore: ServicesDataStore private set
    lateinit var settingsStore: SettingsDataStore private set
    lateinit var presetLoader: PresetLoader private set
    lateinit var fetcher: QuotaFetcher private set
    lateinit var repository: QuotaRepository private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            servicesStore = ServicesDataStore(app)
            settingsStore = SettingsDataStore(app)
            presetLoader = PresetLoader(app)
            fetcher = QuotaFetcher()
            repository = QuotaRepository(
                context = app,
                servicesStore = servicesStore,
                settingsStore = settingsStore,
                presetLoader = presetLoader,
                fetcher = fetcher,
            )
            initialized = true
        }
    }
}
