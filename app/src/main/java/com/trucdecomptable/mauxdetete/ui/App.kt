package com.trucdecomptable.mauxdetete.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trucdecomptable.mauxdetete.export.ExportManager
import com.trucdecomptable.mauxdetete.update.UpdateManager
import kotlinx.coroutines.delay

private val ONGLETS = listOf("Saisir", "Historique", "Stats", "Export")

@Composable
fun Racine(vm: CriseViewModel) {
    val crises by vm.crises.collectAsState()
    val enCours by vm.enCours.collectAsState()
    var onglet by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val contexte = LocalContext.current

    LaunchedEffect(vm.message) {
        val texte = vm.message
        if (texte != null) {
            snackbar.showSnackbar(texte)
            vm.effacerMessage()
        }
    }

    // Partage du fichier exporté : le système choisit l'application de destination.
    LaunchedEffect(vm.partage) {
        vm.partage?.let { fichier ->
            ExportManager.partager(contexte, fichier.fichier, ExportManager.mimeDe(fichier.nom), fichier.nom)
            vm.consommerPartage()
        }
    }

    // Un contrôle au lancement, puis toutes les 30 minutes : le plancher horaire
    // est tenu dans le manager, donc les réveils fréquents ne coûtent qu'un appel.
    LaunchedEffect(Unit) {
        while (true) {
            UpdateManager.checkAtLaunch(contexte, vm.reglages.versionIgnoree)
            delay(30 * 60_000L)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                ONGLETS.forEachIndexed { index, libelle ->
                    NavigationBarItem(
                        selected = onglet == index,
                        onClick = { onglet = index },
                        icon = { Text(iconeDe(index), fontSize = 16.sp) },
                        label = { Text(libelle, fontSize = 10.5.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (onglet) {
                0 -> EcranSaisie(
                    vm = vm,
                    criseEnCours = enCours,
                    nombreCrises = crises.size,
                    onVoirHistorique = { onglet = 1 },
                )
                1 -> EcranHistorique(
                    vm = vm,
                    crises = crises,
                    onEditer = { crise ->
                        vm.editer(crise)
                        onglet = 0
                    },
                )
                2 -> EcranStats(crises)
                else -> EcranExport(vm, crises)
            }
        }
    }

    DialogueMiseAJour()
}

private fun iconeDe(index: Int): String = when (index) {
    0 -> "+"
    1 -> "≡"
    2 -> "▤"
    else -> "⇩"
}

/**
 * Disponibilité d'une mise à jour, visible quel que soit l'onglet ouvert.
 * Un échec n'apparaît jamais ici : il reste dans l'écran Export, au bouton manuel.
 */
@Composable
fun DialogueMiseAJour() {
    val etat by UpdateManager.state.collectAsState()
    val contexte = LocalContext.current

    when (val courant = etat) {
        is UpdateManager.State.Available -> AlertDialog(
            onDismissRequest = { UpdateManager.dismiss() },
            title = { Text("Mise à jour ${courant.version}") },
            text = {
                Column {
                    Text("Une nouvelle version est disponible.", fontSize = 13.sp)
                    if (courant.notes.isNotBlank()) {
                        Text(
                            courant.notes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        "L'installation demandera une confirmation Android.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { UpdateManager.download(contexte) }) { Text("Télécharger") }
            },
            dismissButton = {
                TextButton(onClick = { UpdateManager.dismiss() }) { Text("Plus tard") }
            },
        )

        is UpdateManager.State.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Téléchargement ${courant.version}") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { courant.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text("${(courant.progress * 100).toInt()} %", fontSize = 12.sp)
                }
            },
            confirmButton = {},
        )

        is UpdateManager.State.ReadyToInstall -> AlertDialog(
            onDismissRequest = { UpdateManager.dismiss() },
            title = { Text("Version ${courant.version} prête") },
            text = { Text("Android va demander la confirmation d'installation.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { UpdateManager.install(contexte) }) {
                    Text("Installer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { UpdateManager.dismiss() }) { Text("Plus tard") }
            },
        )

        else -> Unit
    }
}
