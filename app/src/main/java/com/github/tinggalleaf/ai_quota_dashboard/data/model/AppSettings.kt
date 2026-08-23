package com.github.tinggalleaf.ai_quota_dashboard.data.model

import kotlinx.serialization.Serializable

/** Persisted across launches via DataStore. */
@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: ColorPalette = ColorPalette.GRAPHITE,
    val defaultRefreshSec: Long = 300,
    val useDynamicColor: Boolean = true,
    val userAgent: String = "AIQuotaDashboard/1.0 (Android)",
    val connectTimeoutSec: Long = 10,
    val requestTimeoutSec: Long = 30,
    val notifyOnLowQuota: Boolean = true,
    val lowQuotaThresholdPct: Int = 20,
    val showRouteWarning: Boolean = true,
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Hand-curated palettes for users who don't want dynamic color.
 * Names follow the existing CSS variables in the HTML seed.
 */
@Serializable
enum class ColorPalette(val displayName: String) {
    GRAPHITE("石墨黑 (Graphite)"),
    MIUI_ORANGE("澎湃橙 (HyperOS)"),
    MINT("薄荷绿 (Mint)"),
    SUNSET("暮色橙 (Sunset)"),
    OCEAN("深邃蓝 (Ocean)"),
    LAVENDER("薰衣草 (Lavender)"),
}
