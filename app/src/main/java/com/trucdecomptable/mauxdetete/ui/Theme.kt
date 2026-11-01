package com.trucdecomptable.mauxdetete.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Violet = Color(0xFF5B6CFF)
private val VioletClair = Color(0xFF8C97FF)
private val Vert = Color(0xFF2FBF71)
private val Orange = Color(0xFFF0A52B)
private val Rouge = Color(0xFFEF4D5A)

private val Sombre = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF232B4A),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Vert,
    background = Color(0xFF0B0D13),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF141824),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF1B2130),
    onSurfaceVariant = Color(0xFFA9B4C9),
    outline = Color(0xFF2A3244),
    error = Rouge,
)

private val Clair = lightColorScheme(
    primary = Color(0xFF4353E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E7FF),
    onPrimaryContainer = Color(0xFF12173A),
    secondary = Vert,
    background = Color(0xFFEEF1F8),
    onBackground = Color(0xFF131A2B),
    surface = Color.White,
    onSurface = Color(0xFF131A2B),
    surfaceVariant = Color(0xFFF5F7FD),
    onSurfaceVariant = Color(0xFF68738C),
    outline = Color(0xFFDDE3F0),
    error = Rouge,
)

private val Typographie = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = t.labelSmall.copy(fontSize = 11.sp),
    )
}

/** Couleur de l'échelle de douleur, du vert au rouge. */
fun couleurNiveau(niveau: Int): Color = when {
    niveau <= 1 -> Vert
    niveau <= 3 -> Color(0xFF79C94A)
    niveau <= 4 -> Color(0xFFD7C62C)
    niveau <= 6 -> Orange
    niveau <= 8 -> Color(0xFFF28C28)
    else -> Rouge
}

fun couleurAlerte(): Color = Orange

/**
 * Thème sombre par défaut : on consulte cette app en pleine crise, souvent
 * dans le noir. Le clair reste disponible quand le téléphone est en clair.
 */
@Composable
fun MauxDeTeteTheme(sombre: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (sombre) Sombre else Clair,
        typography = Typographie,
        content = content,
    )
}
