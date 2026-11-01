package com.trucdecomptable.mauxdetete.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trucdecomptable.mauxdetete.BuildConfig
import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import com.trucdecomptable.mauxdetete.util.Temps
import com.trucdecomptable.mauxdetete.update.UpdateManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMAT_MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE)
private val FORMAT_JOUR = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRANCE)
private val FORMAT_HEURE = DateTimeFormatter.ofPattern("HH'h'", Locale.FRANCE)

// ------------------------------------------------------------- historique

@Composable
fun EcranHistorique(vm: CriseViewModel, crises: List<Crise>, onEditer: (Crise) -> Unit) {
    var aSupprimer by remember { mutableStateOf<Crise?>(null) }
    val zone = ZoneId.systemDefault()

    if (crises.isEmpty()) {
        Column(Modifier.padding(24.dp)) {
            Titre("Historique")
            Text(
                "Aucune crise enregistrée. Commence par l'onglet Saisir.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Titre("Historique", "${crises.size} crises · touche une ligne pour modifier")
        }
        val groupes = crises.sortedByDescending { it.debutLe }
            .groupBy { Instant.ofEpochMilli(it.debutLe).atZone(zone).toLocalDate().withDayOfMonth(1) }
        groupes.forEach { (mois,liste) ->
            item(key = "mois-$mois") {
                Text(
                    mois.format(FORMAT_MOIS),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
            }
            items(liste, key = { it.id }) { crise ->
                LigneCrise(crise, onClick = { onEditer(crise) }, onSupprimer = { aSupprimer = crise })
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    aSupprimer?.let { crise ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer cette crise ?") },
            text = {
                Text(
                    "La crise du " + Instant.ofEpochMilli(crise.debutLe).atZone(zone)
                        .toLocalDate().format(FORMAT_JOUR) + " sera définitivement supprimée.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.supprimer(crise)
                    aSupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun LigneCrise(crise: Crise, onClick: () -> Unit, onSupprimer: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val debut = Instant.ofEpochMilli(crise.debutLe).atZone(zone)
    val fin = crise.finLe?.let { Instant.ofEpochMilli(it).atZone(zone) }
    val heures = Temps.dureeHeures(crise.debutLe, crise.finLe, System.currentTimeMillis())
    val lendemain = fin != null && fin.toLocalDate() != debut.toLocalDate()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PointCouleur(couleurNiveau(crise.niveau))
            Spacer(Modifier.width(10.dp))
            Text(
                debut.toLocalDate().format(FORMAT_JOUR),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${crise.niveau}",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = couleurNiveau(crise.niveau),
            )
            Text("/10", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        val morceaux = mutableListOf(
            debut.toLocalTime().format(FORMAT_HEURE) + " → " +
                (fin?.toLocalTime()?.format(FORMAT_HEURE) ?: "en cours"),
            Temps.libelleDuree(heures),
            "sommeil " + Referentiels.libelleSommeil(crise.sommeil).lowercase(Locale.FRANCE),
        )
        if (lendemain) morceaux.add("lendemain")
        if (Temps.depasseSoiree(crise.debutLe, crise.finLe, System.currentTimeMillis(), zone)) {
            morceaux.add("soirée dépassée")
        }
        Text(
            morceaux.joinToString(" · "),
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val extras = mutableListOf<String>()
        if (!crise.medicament.isNullOrBlank()) extras.add(crise.medicament!!)
        Crise.decouper(crise.declencheurs).take(2).forEach { extras.add(it.lowercase(Locale.FRANCE)) }
        Crise.decouper(crise.symptomes).take(2).forEach { extras.add(it.lowercase(Locale.FRANCE)) }
        if (extras.isNotEmpty()) {
            Text(
                extras.joinToString(" · "),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TextButton(onClick = onSupprimer, modifier = Modifier.align(Alignment.End)) {
            Text("Supprimer", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

// ------------------------------------------------------------------ stats

@Composable
fun EcranStats(crises: List<Crise>) {
    val bilan = remember(crises) {
        com.trucdecomptable.mauxdetete.data.Stats.bilan(crises)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Titre("Statistiques", "Mois en cours et 30 derniers jours")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { Kpi(bilan.crisesMois.toString(), "crises ce mois") }
            Box(Modifier.weight(1f)) {
                Kpi(
                    String.format(Locale.FRANCE, "%.1f", bilan.intensiteMoyenne),
                    "intensité moyenne",
                    couleurNiveau(Math.round(bilan.intensiteMoyenne).toInt()),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                Kpi(String.format(Locale.FRANCE, "%.1f h", bilan.dureeMoyenneHeures), "durée moyenne")
            }
            Box(Modifier.weight(1f)) { Kpi(bilan.joursSansDouleur.toString(), "jours sans douleur") }
        }

        Section("Tendance", "7 jours") {
            LigneStat("Cette semaine", bilan.semaineCourante.toString())
            LigneStat("Semaine précédente", bilan.semainePrecedente.toString())
            val delta = bilan.semaineCourante - bilan.semainePrecedente
            LigneStat(
                "Évolution",
                when {
                    delta == 0 -> "stable"
                    delta > 0 -> "+$delta"
                    else -> delta.toString()
                },
                couleur = when {
                    delta > 0 -> MaterialTheme.colorScheme.error
                    delta < 0 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Section("À quelle heure", "début de crise") {
            Graphique(bilan.parHeure)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0h", "6h", "12h", "18h", "23h").forEach {
                    Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Section("Quel jour") {
            Graphique(bilan.parJourSemaine)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim").forEach {
                    Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Section("Crises longues") {
            LigneStat("Ayant dépassé la soirée", "${bilan.crisesLongues}", couleur = couleurAlerte())
            LigneStat("Durée la plus longue", Temps.libelleDuree(bilan.dureeMaxHeures))
        }

        Section("Médicaments ce mois") {
            LigneStat(
                "Jours de prise",
                "${bilan.medicamentsJours} / ${bilan.seuilMedicaments} jours",
                detail = "seuil ICHD-3 : ${Referentiels.SEUIL_MEDICAMENT} j (antalgique), " +
                    "${Referentiels.SEUIL_TRIPTAN} j (triptan)",
                couleur = if (bilan.medicamentsJours >= bilan.seuilMedicaments * 0.8) couleurAlerte() else null,
            )
            if (bilan.medicamentsJours >= bilan.seuilMedicaments * 0.8) {
                Text(
                    "Proche du seuil de céphalée par abus médicamenteux : à évoquer avec ton médecin.",
                    fontSize = 11.sp,
                    color = couleurAlerte(),
                )
            }
        }

        Section("Effet du sommeil", "min. ${Referentiels.MIN_CAS} cas") {
            val bon = bilan.sommeilBon
            val mauvais = bilan.sommeilMauvais
            LigneStat(
                "Après une bonne nuit",
                if (bon.concluable) String.format(Locale.FRANCE, "%.1f / 10", bon.moyenne ?: 0.0) else "trop peu",
                detail = "n=${bon.effectif}",
                couleur = if (bon.concluable) MaterialTheme.colorScheme.secondary else null,
            )
            LigneStat(
                "Après une mauvaise nuit",
                if (mauvais.concluable) String.format(Locale.FRANCE, "%.1f / 10", mauvais.moyenne ?: 0.0) else "trop peu",
                detail = "n=${mauvais.effectif}",
                couleur = if (mauvais.concluable) MaterialTheme.colorScheme.error else null,
            )
            if (bon.concluable && mauvais.concluable) {
                LigneStat(
                    "Écart",
                    String.format(Locale.FRANCE, "+%.1f", (mauvais.moyenne ?: 0.0) - (bon.moyenne ?: 0.0)),
                )
            } else {
                Text(
                    "Moins de ${Referentiels.MIN_CAS} crises des deux côtés : aucun écart affiché pour ne pas induire en erreur.",
                    fontSize = 11.sp,
                    color = couleurAlerte(),
                )
            }
        }

        Section("Déclencheurs les plus fréquents") {
            if (bilan.declencheurs.isEmpty()) {
                Text("Rien de saisi pour le moment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                bilan.declencheurs.forEach {
                    LigneStat(it.libelle, "${it.pourcentage} %", detail = "n=${it.effectif}")
                }
                Text(
                    "Pourcentage calculé sur ${crises.size} crises saisies.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Section("Aliments les plus fréquents") {
            if (bilan.repas.isEmpty()) {
                Text("Rien de saisi pour le moment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                bilan.repas.forEach { LigneStat(it.libelle, "${it.pourcentage} %", detail = "n=${it.effectif}") }
                Text(
                    "Pourcentage calculé sur ${crises.size} crises saisies.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ----------------------------------------------------------------- export

@Composable
fun EcranExport(vm: CriseViewModel, crises: List<Crise>) {
    val contexte = LocalContext.current
    var verrouillage by remember { mutableStateOf(vm.reglages.verrouillageBiometrique) }
    var auto by remember { mutableStateOf(vm.reglages.sauvegardeAutomatique) }
    val etatMaj by UpdateManager.state.collectAsState()

    val lecteur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val texte = runCatching {
                contexte.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (texte != null) vm.restaurer(texte) else vm.message = "Fichier illisible"
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Titre("Export et réglages", "Tout reste sur le téléphone, l'export partage le fichier où tu veux")

        Section("Exporter") {
            BoutonSecondaire("CSV pour Excel") { vm.exporter(Sortie.CSV) }
            Spacer(Modifier.height(8.dp))
            BoutonSecondaire("Sauvegarde complète (JSON)") { vm.exporter(Sortie.JSON) }
            Spacer(Modifier.height(8.dp))
            BoutonSecondaire("Synthèse PDF pour le médecin") { vm.exporter(Sortie.PDF) }
            Spacer(Modifier.height(8.dp))
            BoutonSecondaire("Restaurer une sauvegarde") { lecteur.launch(arrayOf("application/json", "*/*")) }
            vm.fichierSauvegarde?.let { fichier ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "${fichier.nom} · ${fichier.description}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Section("Réglages") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Verrouillage biométrique", fontSize = 12.5.sp)
                    Text(
                        "empreinte à l'ouverture de l'app",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = verrouillage,
                    onCheckedChange = {
                        verrouillage = it
                        vm.reglages.verrouillageBiometrique = it
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sauvegarde automatique", fontSize = 12.5.sp)
                    Text(
                        "un JSON sauvegardé une fois par mois dans le dossier de l'app",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = auto,
                    onCheckedChange = {
                        auto = it
                        vm.reglages.sauvegardeAutomatique = it
                    },
                )
            }
        }

        Section("Application", "v${BuildConfig.VERSION_NAME}") {
            val libelle = when (val e = etatMaj) {
                is UpdateManager.State.Idle -> "jamais vérifié"
                is UpdateManager.State.Available -> "v${e.version} disponible"
                is UpdateManager.State.Downloading -> "téléchargement ${(e.progress * 100).toInt()} %"
                is UpdateManager.State.ReadyToInstall -> "prêt à installer"
                is UpdateManager.State.UpToDate -> "à jour"
                is UpdateManager.State.Failed -> "erreur : ${e.detail ?: "inconnue"}"
            }
            LigneStat("Mise à jour GitHub", libelle)
            Spacer(Modifier.height(8.dp))
            BoutonSecondaire("Vérifier maintenant") { UpdateManager.checkNow(contexte) }
            Spacer(Modifier.height(10.dp))
            Text(
                "Aucun compte, aucun serveur de santé. Seule connexion sortante : la vérification de version sur GitHub.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("${crises.size} crises enregistrées", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
