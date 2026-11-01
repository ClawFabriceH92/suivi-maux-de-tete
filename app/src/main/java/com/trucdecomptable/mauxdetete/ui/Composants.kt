package com.trucdecomptable.mauxdetete.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Section(titre: String?, hint: String? = null, contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        if (titre != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titre, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (hint != null) Text(
                    hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        contenu()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuagePuces(
    valeurs: List<String>,
    selection: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        valeurs.forEach { valeur ->
            FilterChip(
                selected = valeur in selection,
                onClick = { onToggle(valeur) },
                label = { Text(valeur, fontSize = 12.5.sp) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoixUnique(
    valeurs: List<String>,
    selection: String?,
    onChoix: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        valeurs.forEach { valeur ->
            FilterChip(
                selected = valeur == selection,
                onClick = { onChoix(valeur) },
                label = { Text(valeur, fontSize = 12.5.sp) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
fun BarreDouleur(niveau: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                niveau.toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = couleurNiveau(niveau),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                com.trucdecomptable.mauxdetete.data.Referentiels.libelleNiveau(niveau),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = couleurNiveau(niveau),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Slider(
            value = niveau.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = couleurNiveau(niveau),
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0 rien", "5 gênant", "10 invalidant").forEach {
                Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Graphique(valeurs: List<Int>, modifier: Modifier = Modifier, couleur: Color = MaterialTheme.colorScheme.primary) {
    val max = (valeurs.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        valeurs.forEach { v ->
            val hauteur = if (v == 0) 0.04f else 0.12f + (v.toFloat() / max) * 0.88f
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height((96 * hauteur).dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(if (v == 0) MaterialTheme.colorScheme.outline else couleur),
            )
        }
    }
}

@Composable
fun LigneStat(libelle: String, valeur: String, detail: String? = null, couleur: Color? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(libelle, fontSize = 12.5.sp)
            if (detail != null) Text(
                detail,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(valeur, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = couleur ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun Kpi(valeur: String, libelle: String, couleur: Color? = null) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(13.dp),
    ) {
        Text(valeur, fontSize = 22.sp, fontWeight = FontWeight.Black, color = couleur ?: MaterialTheme.colorScheme.onSurface)
        Text(libelle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BoutonPrincipal(texte: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) { Text(texte, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun BoutonSecondaire(texte: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) { Text(texte, fontSize = 13.5.sp) }
}

@Composable
fun Titre(texte: String, sousTitre: String? = null) {
    Column(Modifier.padding(top = 14.dp, bottom = 8.dp)) {
        Text(texte, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (sousTitre != null) Text(
            sousTitre,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun Pastille(texte: String, couleur: Color, fond: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fond)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text(texte, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = couleur) }
}

@Composable
fun Espace(h: Int = 8) = Spacer(Modifier.height(h.dp))

@Composable
fun PointCouleur(couleur: Color) {
    Box(
        Modifier
            .size(9.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(couleur),
    )
}
