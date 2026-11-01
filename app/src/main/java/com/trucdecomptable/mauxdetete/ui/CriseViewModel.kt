package com.trucdecomptable.mauxdetete.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trucdecomptable.mauxdetete.data.AppDb
import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Reglages
import com.trucdecomptable.mauxdetete.data.Stats
import com.trucdecomptable.mauxdetete.export.ExportManager
import com.trucdecomptable.mauxdetete.export.FichierExport
import com.trucdecomptable.mauxdetete.sync.Rappels
import com.trucdecomptable.mauxdetete.util.Diag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Sortie { CSV, JSON, PDF }

class CriseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDb.get(application).dao()
    val reglages = Reglages(application)

    val crises: StateFlow<List<Crise>> = dao.toutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enCours: StateFlow<Crise?> = dao.enCours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var formulaire by mutableStateOf(Formulaire())
        private set

    var message by mutableStateOf<String?>(null)
    var partage by mutableStateOf<FichierExport?>(null)
        private set

    var fichierSauvegarde by mutableStateOf<FichierExport?>(null)
        private set

    val enModification: Boolean get() = formulaire.id != null

    // ------------------------------------------------------------ saisie

    fun modifier(bloc: (Formulaire) -> Formulaire) {
        formulaire = bloc(formulaire)
    }

    fun editer(crise: Crise) {
        formulaire = Formulaire.depuis(crise)
    }

    fun reinitialiser() {
        formulaire = Formulaire()
    }

    fun annulerEdition() {
        formulaire = Formulaire()
    }

    fun enregistrer(onFini: () -> Unit = {}) {
        val f = formulaire
        viewModelScope.launch {
            if (f.id != null) {
                val existante = crises.value.firstOrNull { it.id == f.id }
                val crise = f.versCrise(creeLe = existante?.creeLe ?: System.currentTimeMillis())
                dao.mettreAJour(crise)
                if (f.enCours) Rappels.programmer(getApplication(), crise.id)
                else Rappels.annuler(getApplication(), crise.id)
                message = "Crise mise à jour"
            } else {
                val id = dao.inserer(f.versCrise())
                // Crise ouverte : on demande le lendemain si elle est terminée.
                if (f.enCours) Rappels.programmer(getApplication(), id)
                message = "Crise enregistrée · ${f.dureeHeures} h"
            }
            formulaire = Formulaire()
            onFini()
        }
    }

    /** Un seul tap : la crise démarre maintenant, à l'heure pleine courante. */
    fun demarrerMaintenant() {
        val maintenant = System.currentTimeMillis()
        viewModelScope.launch {
            val id = dao.inserer(
                Crise(
                    debutLe = maintenant - (maintenant % 3_600_000L),
                    finLe = null,
                    niveau = 5,
                    sommeil = com.trucdecomptable.mauxdetete.data.Referentiels.SOMMEIL_MOYEN,
                ),
            )
            Rappels.programmer(getApplication(), id)
            message = "Crise démarrée · rappel demain matin"
        }
    }

    fun cloturer(crise: Crise) {
        viewModelScope.launch {
            dao.mettreAJour(crise.copy(finLe = System.currentTimeMillis()))
            Rappels.annuler(getApplication(), crise.id)
            message = "Crise clôturée"
        }
    }

    fun supprimer(crise: Crise, onFini: () -> Unit = {}) {
        viewModelScope.launch {
            Rappels.annuler(getApplication(), crise.id)
            dao.supprimer(crise.id)
            message = "Crise supprimée"
            onFini()
        }
    }

    fun effacerMessage() {
        message = null
    }

    fun consommerPartage() {
        partage = null
    }

    // ----------------------------------------------------------- exports

    fun exporter(sortie: Sortie) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val liste = crises.value
            if (liste.isEmpty()) {
                message = "Aucune crise à exporter"
                return@launch
            }
            val resultat = withContext(Dispatchers.IO) {
                runCatching {
                    when (sortie) {
                        Sortie.CSV -> ExportManager.csv(app, liste)
                        Sortie.JSON -> ExportManager.json(app, liste)
                        Sortie.PDF -> ExportManager.pdf(app, liste, Stats.bilan(liste))
                    }
                }
            }
            resultat.onSuccess {
                fichierSauvegarde = it
                partage = it
                Diag.record("export", "${it.nom} (${it.description})")
            }.onFailure {
                Diag.record("export", it)
                message = "Export impossible : ${it.message}"
            }
        }
    }

    /** Vrai si une sauvegarde automatique mensuelle est due. */
    fun sauvegardeAutoDue(): Boolean {
        if (!reglages.sauvegardeAutomatique) return false
        val dernier = reglages.derniereSauvegarde
        if (dernier == 0L) return crises.value.isNotEmpty()
        val mois = 30L * 24 * 3600 * 1000
        return System.currentTimeMillis() - dernier > mois && crises.value.isNotEmpty()
    }

    fun marquerSauvegarde() {
        reglages.derniereSauvegarde = System.currentTimeMillis()
    }

    fun restaurer(texte: String) {
        viewModelScope.launch {
            val resultat = runCatching { ExportManager.lireJson(texte) }
            resultat.onSuccess { liste ->
                withContext(Dispatchers.IO) {
                    dao.toutSupprimer()
                    dao.insererTout(liste.map { it.copy(id = 0) })
                }
                message = "Sauvegarde restaurée · ${liste.size} crises"
                Diag.record("restauration", "${liste.size} crises")
            }.onFailure {
                message = "Restauration impossible : ${it.message}"
                Diag.record("restauration", it)
            }
        }
    }
}
