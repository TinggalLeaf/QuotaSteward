package com.github.tinggalleaf.ai_quota_dashboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import com.github.tinggalleaf.ai_quota_dashboard.ServiceLocator
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ColorPalette
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** Hand-curated palettes keyed by [ColorPalette]. */
object BrandPalettes {
    fun lightColors(p: ColorPalette): Colors = base(p, dark = false)
    fun darkColors(p: ColorPalette): Colors = base(p, dark = true)

    private fun base(p: ColorPalette, dark: Boolean): Colors {
        val primary = primaryOf(p, dark)
        return if (dark) darkColorScheme(primary = primary) else lightColorScheme(primary = primary)
    }

    private fun primaryOf(p: ColorPalette, dark: Boolean): Color = when (p) {
        ColorPalette.GRAPHITE -> if (dark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E)
        ColorPalette.MIUI_ORANGE -> if (dark) Color(0xFFFFB74D) else Color(0xFFFF6F00)
        ColorPalette.MINT -> if (dark) Color(0xFF7FE3C8) else Color(0xFF1FAA86)
        ColorPalette.SUNSET -> if (dark) Color(0xFFFFB199) else Color(0xFFFF7A59)
        ColorPalette.OCEAN -> if (dark) Color(0xFF7CB6FF) else Color(0xFF1F66D9)
        ColorPalette.LAVENDER -> if (dark) Color(0xFFCBA8FF) else Color(0xFF7B5AC7)
    }
}

@Composable
fun AIQuotaTheme(content: @Composable () -> Unit) {
    val settings by ServiceLocator.settingsStore.settings.collectAsState(initial = com.github.tinggalleaf.ai_quota_dashboard.data.model.AppSettings())
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val controller = remember(settings, isDark) {
        ThemeController(
            colorSchemeMode = if (settings.useDynamicColor) ColorSchemeMode.MonetSystem else ColorSchemeMode.System,
            lightColors = BrandPalettes.lightColors(settings.colorPalette),
            darkColors = BrandPalettes.darkColors(settings.colorPalette),
            isDark = isDark,
        )
    }

    MiuixTheme(controller = controller) {
        content()
    }
}
