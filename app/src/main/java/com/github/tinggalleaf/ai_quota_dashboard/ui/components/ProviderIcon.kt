package com.github.tinggalleaf.ai_quota_dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Renders the provider logo. Falls back to a colored letter chip when
 * the asset is missing, fails to load, or is not a PNG (Coil without
 * coil-svg cannot decode SVG here).
 */
@Composable
fun ProviderIcon(
    service: ServiceConfig,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val cs = MiuixTheme.colorScheme
    val context = LocalContext.current
    val asset = service.iconAsset
    var failed by remember(asset) { mutableStateOf(false) }

    val shape = RoundedCornerShape(14.dp)
    val isPng = asset?.endsWith(".png", ignoreCase = true) == true

    // Fallback chip for missing/non-PNG/failed assets.
    if (asset.isNullOrBlank() || !isPng || failed) {
        val seed = service.name.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
        val (bg, fg) = chipColors(service.id, cs.surfaceVariant, cs.onSurface)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = seed,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
        return
    }

    // Coil asset URI scheme: "file:///android_asset/..."
    val assetUri = "file:///android_asset/provider-icons/$asset"
    val request = remember(assetUri) {
        ImageRequest.Builder(context)
            .data(assetUri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(cs.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = service.name,
            modifier = Modifier.size(size),
            onError = { failed = true },
        )
    }
}

/** Deterministic color per provider id so the chip looks consistent. */
private fun chipColors(seed: String, fallbackBg: Color, fallbackFg: Color): Pair<Color, Color> {
    if (seed.isEmpty()) return fallbackBg to fallbackFg
    val palette = listOf(
        Color(0xFF3A3A3D) to Color(0xFFE5E5EA),
        Color(0xFF1F6FEB) to Color(0xFFFFFFFF),
        Color(0xFF1FAA86) to Color(0xFFFFFFFF),
        Color(0xFFFF7A59) to Color(0xFFFFFFFF),
        Color(0xFF7B5AC7) to Color(0xFFFFFFFF),
        Color(0xFFC44569) to Color(0xFFFFFFFF),
        Color(0xFFEAB308) to Color(0xFF1C1C1E),
        Color(0xFF2563EB) to Color(0xFFFFFFFF),
        Color(0xFF0EA5E9) to Color(0xFFFFFFFF),
        Color(0xFF16A34A) to Color(0xFFFFFFFF),
        Color(0xFFDC2626) to Color(0xFFFFFFFF),
        Color(0xFF7C3AED) to Color(0xFFFFFFFF),
    )
    val idx = (seed.hashCode().rem(palette.size) + palette.size) % palette.size
    return palette[idx]
}
