package com.trucdecomptable.mauxdetete.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import com.trucdecomptable.mauxdetete.util.Temps
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EcranSaisie(
    vm: CriseViewModel,
    criseEnCours: Crise?,
    nombreCrises: Int,
    onVoirHistorique: () -> Unit,
) {
    val f = vm.formulaire
    var details by remember { mutableStateOf(false) }
    val zone = java.time.ZoneId.systemDefault()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Titre(
            if (vm.enModification) "Modifier la crise" else "Nouvelle crise",
            if (vm.enModification) "Corrige ce qui doit l'être" else "Heures pleines, gros boutons, 30 secondes",
        )

        // ---- crise en cours : clôture en un tap
        if (criseEnCours != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33F0A52B))
                    .padding(14.dp),
            ) {
                val heures = Temps.dureeHeures(
                    criseEnCours.debutLe,
                    criseEnCours.finLe,
                    System.currentTimeMillis(),
                )
                Text(
                    "Crise en cours depuis " +
                        java.time.Instant.ofEpochMilli(criseEnCours.debutLe).atZone(zone).hour + "h",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    "durée ${Temps.libelleDuree(heures)} · intensité ${criseEnCours.niveau}/10",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.cloturer(criseEnCours) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A52B)),
                ) {
                    Text("Clôturer maintenant", color = Color(0xFF20160A), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        } else {
            BoutonPrincipal("Démarrer une crise maintenant") { vm.demarrerMaintenant() }
            Spacer(Modifier.height(10.dp))
        }

        // ---- quand
        Section("Quand", "heures pleines") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.modifier { it.copy(jour = it.jour.minusDays(1)) } }) {
                    Text("−", fontSize = 18.sp)
                }
                Text(
                    f.jour.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRANCE)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.modifier { it.copy(jour = it.jour.plusDays(1)) } }) {
                    Text("+", fontSize = 18.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelecteurHeure("Début", f.heureDebut) { h -> vm.modifier { it.copy(heureDebut = h) } }
                Text("→", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelecteurHeure("Fin", f.heureFin, actif = !f.enCours) { h ->
                    vm.modifier { it.copy(heureFin = h) }
                }
            }
            if (f.franchitMinuit) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Se termine à", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    (1..3).forEach { n ->
                        TextButton(onClick = { vm.modifier { it.copy(decalageJours = n) } }) {
                            Text(
                                "J+$n",
                                fontWeight = if (f.decalageJours == n) FontWeight.Black else FontWeight.Normal,
                                color = if (f.decalageJours == n) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Crise en cours", fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = f.enCours,
                    onCheckedChange = { actif -> vm.modifier { it.copy(enCours = actif) } },
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(10.dp),
            ) {
                Text(f.resume(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ---- douleur
        Section("Niveau de douleur", "0 · 10") {
            BarreDouleur(f.niveau) { n -> vm.modifier { it.copy(niveau = n) } }
        }

        // ---- sommeil
        Section("Sommeil la veille") {
            ChoixUnique(
                listOf(Referentiels.SOMMEIL_BON, Referentiels.SOMMEIL_MOYEN, Referentiels.SOMMEIL_MAUVAIS),
                f.sommeil,
            ) { valeur -> vm.modifier { it.copy(sommeil = valeur) } }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Réveils nocturnes", fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    vm.modifier { it.copy(reveils = (it.reveils - 1).coerceAtLeast(0)) }
                }) { Text("−", fontSize = 18.sp) }
                Text(f.reveils.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = {
                    vm.modifier { it.copy(reveils = (it.reveils + 1).coerceAtMost(9)) }
                }) { Text("+", fontSize = 18.sp) }
            }
        }

        if (!details) {
            Spacer(Modifier.height(4.dp))
            BoutonSecondaire("Ajouter déclencheurs, médicament, détails") { details = true }
        } else {
            Section("Déclencheurs suspectés", "${f.declencheurs.size} choisi(s)") {
                NuagePuces(Referentiels.DECLENCHEURS, f.declencheurs) { v ->
                    vm.modifier {
                        it.copy(
                            declencheurs = if (v in it.declencheurs) it.declencheurs - v
                            else it.declencheurs + v,
                        )
                    }
                }
            }

            Section("Médicament pris") {
                ChoixUnique(Referentiels.MEDICAMENTS, f.medicament) { v ->
                    vm.modifier { it.copy(medicament = if (it.medicament == v) null else v) }
                }
                if (f.medicament != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Efficacité", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    ChoixUnique(Referentiels.EFFICACITES, f.efficacite) { v ->
                        vm.modifier { it.copy(efficacite = if (it.efficacite == v) null else v) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Seuil d'alerte : ${Referentiels.SEUIL_MEDICAMENT} jours par mois pour un antalgique simple, " +
                        "${Referentiels.SEUIL_TRIPTAN} jours pour un triptan.",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section("Repas d'avant") {
                NuagePuces(Referentiels.REPAS, f.repas) { v ->
                    vm.modifier { it.copy(repas = if (v in it.repas) it.repas - v else it.repas + v) }
                }
            }

            Section("Symptômes") {
                NuagePuces(Referentiels.SYMPTOMES, f.symptomes) { v ->
                    vm.modifier { it.copy(symptomes = if (v in it.symptomes) it.symptomes - v else it.symptomes + v) }
                }
            }

            Section("Localisation") {
                NuagePuces(Referentiels.LOCALISATIONS, f.localisations) { v ->
                    vm.modifier {
                        it.copy(
                            localisations = if (v in it.localisations) it.localisations - v
                            else it.localisations + v,
                        )
                    }
                }
            }

            Section("Commentaire") {
                OutlinedTextField(
                    value = f.commentaire,
                    onValueChange = { v -> vm.modifier { it.copy(commentaire = v) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp),
                    placeholder = { Text("Contexte, ce qui a aidé...", fontSize = 12.sp) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        BoutonPrincipal(if (vm.enModification) "Mettre à jour" else "Enregistrer") {
            vm.enregistrer()
        }
        if (vm.enModification) {
            Spacer(Modifier.height(8.dp))
            BoutonSecondaire("Annuler la modification") { vm.annulerEdition() }
        }
        Spacer(Modifier.height(8.dp))
        BoutonSecondaire("Réinitialiser") { vm.reinitialiser() }
        Spacer(Modifier.height(12.dp))
        Text(
            "$nombreCrises crises · stockage local",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onVoirHistorique) { Text("Voir l'historique", fontSize = 12.sp) }
    }
}

@Composable
fun SelecteurHeure(libelle: String, heure: Int, actif: Boolean = true, onChoix: (Int) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { if (actif) ouvert = true },
            enabled = actif,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text("$heure" + "h", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text = { Text("$h" + "h") },
                    onClick = {
                        onChoix(h)
                        ouvert = false
                    },
                )
            }
        }
    }
}
