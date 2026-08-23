package com.github.tinggalleaf.ai_quota_dashboard.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.tinggalleaf.ai_quota_dashboard.data.model.AppSettings
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ColorPalette
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorPalette = prefs[KEY_PALETTE]?.let {
                runCatching { ColorPalette.valueOf(it) }.getOrNull()
            } ?: ColorPalette.GRAPHITE,
            defaultRefreshSec = prefs[KEY_REFRESH] ?: AppSettings().defaultRefreshSec,
            useDynamicColor = prefs[KEY_DYNAMIC] ?: AppSettings().useDynamicColor,
            userAgent = prefs[KEY_UA] ?: AppSettings().userAgent,
            connectTimeoutSec = prefs[KEY_CONNECT_TIMEOUT] ?: AppSettings().connectTimeoutSec,
            requestTimeoutSec = prefs[KEY_REQUEST_TIMEOUT] ?: AppSettings().requestTimeoutSec,
            notifyOnLowQuota = prefs[KEY_LOW_NOTIFY] ?: AppSettings().notifyOnLowQuota,
            lowQuotaThresholdPct = prefs[KEY_LOW_THRESHOLD] ?: AppSettings().lowQuotaThresholdPct,
            showRouteWarning = prefs[KEY_SHOW_ROUTE] ?: AppSettings().showRouteWarning,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs ->
            val current = AppSettings(
                themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                colorPalette = prefs[KEY_PALETTE]?.let {
                    runCatching { ColorPalette.valueOf(it) }.getOrNull()
                } ?: ColorPalette.GRAPHITE,
                defaultRefreshSec = prefs[KEY_REFRESH] ?: AppSettings().defaultRefreshSec,
                useDynamicColor = prefs[KEY_DYNAMIC] ?: AppSettings().useDynamicColor,
                userAgent = prefs[KEY_UA] ?: AppSettings().userAgent,
                connectTimeoutSec = prefs[KEY_CONNECT_TIMEOUT] ?: AppSettings().connectTimeoutSec,
                requestTimeoutSec = prefs[KEY_REQUEST_TIMEOUT] ?: AppSettings().requestTimeoutSec,
                notifyOnLowQuota = prefs[KEY_LOW_NOTIFY] ?: AppSettings().notifyOnLowQuota,
                lowQuotaThresholdPct = prefs[KEY_LOW_THRESHOLD] ?: AppSettings().lowQuotaThresholdPct,
                showRouteWarning = prefs[KEY_SHOW_ROUTE] ?: AppSettings().showRouteWarning,
            )
            val next = transform(current)
            prefs[KEY_THEME] = next.themeMode.name
            prefs[KEY_PALETTE] = next.colorPalette.name
            prefs[KEY_REFRESH] = next.defaultRefreshSec
            prefs[KEY_DYNAMIC] = next.useDynamicColor
            prefs[KEY_UA] = next.userAgent
            prefs[KEY_CONNECT_TIMEOUT] = next.connectTimeoutSec
            prefs[KEY_REQUEST_TIMEOUT] = next.requestTimeoutSec
            prefs[KEY_LOW_NOTIFY] = next.notifyOnLowQuota
            prefs[KEY_LOW_THRESHOLD] = next.lowQuotaThresholdPct
            prefs[KEY_SHOW_ROUTE] = next.showRouteWarning
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_PALETTE = stringPreferencesKey("color_palette")
        private val KEY_REFRESH = longPreferencesKey("default_refresh_sec")
        private val KEY_DYNAMIC = booleanPreferencesKey("use_dynamic_color")
        private val KEY_UA = stringPreferencesKey("user_agent")
        private val KEY_CONNECT_TIMEOUT = longPreferencesKey("connect_timeout")
        private val KEY_REQUEST_TIMEOUT = longPreferencesKey("request_timeout")
        private val KEY_LOW_NOTIFY = booleanPreferencesKey("low_notify")
        private val KEY_LOW_THRESHOLD = intPreferencesKey("low_threshold_pct")
        private val KEY_SHOW_ROUTE = booleanPreferencesKey("show_route_warning")
    }
}
