package com.trucdecomptable.mauxdetete.ui

import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import com.trucdecomptable.mauxdetete.util.Temps
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * État de l'écran de saisie. Les heures sont des heures pleines : aucune minute
 * à taper, et une fin antérieure au début bascule automatiquement au lendemain.
 */
data class Formulaire(
    val id: Long? = null,
    val jour: LocalDate = LocalDate.now(),
    val heureDebut: Int = LocalTime.now().hour,
    val heureFin: Int = (LocalTime.now().hour + 1).coerceAtMost(23),
    val enCours: Boolean = false,
    val decalageJours: Int = 1,
    val niveau: Int = 5,
    val sommeil: String = Referentiels.SOMMEIL_MOYEN,
    val reveils: Int = 0,
    val declencheurs: Set<String> = emptySet(),
    val repas: Set<String> = emptySet(),
    val symptomes: Set<String> = emptySet(),
    val localisations: Set<String> = emptySet(),
    val medicament: String? = null,
    val efficacite: String? = null,
    val commentaire: String = "",
    val efferalganPrises: Int = 0,
    val efferalganMg: Int = Referentiels.EFFERALGAN_MG_DEFAUT,
    val ibuprofenePrises: Int = 0,
    val ibuprofeneMg: Int = Referentiels.IBUPROFENE_MG_DEFAUT,
) {

    val debutMs: Long
        get() = jour.atTime(heureDebut, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val finMs: Long?
        get() = if (enCours) null
        else Temps.finReportee(
            debutMs = debutMs,
            heureFin = heureFin,
            heureDebut = heureDebut,
            decalageJours = decalageJours,
        )

    /** Vrai quand la fin est le lendemain du début. */
    val franchitMinuit: Boolean
        get() = !enCours && heureFin < heureDebut

    val dureeHeures: Long
        get() = Temps.dureeHeures(debutMs, finMs, System.currentTimeMillis())

    val depasseSoiree: Boolean
        get() = Temps.depasseSoiree(debutMs, finMs, System.currentTimeMillis())

    fun resume(): String = when {
        enCours -> "En cours depuis ${heureDebut}h · pas encore d'heure de fin"
        franchitMinuit -> "Se termine à J+$decalageJours à ${heureFin}h · " +
            Temps.libelleDuree(dureeHeures) + " · dépasse la soirée"
        else -> "Même journée · " + Temps.libelleDuree(dureeHeures) +
            if (depasseSoiree) " · dépasse la soirée" else ""
    }

    fun versCrise(creeLe: Long = System.currentTimeMillis()): Crise = Crise(
        id = id ?: 0,
        debutLe = debutMs,
        finLe = finMs,
        niveau = niveau,
        sommeil = sommeil,
        reveils = reveils,
        declencheurs = Crise.joindre(declencheurs),
        repas = Crise.joindre(repas),
        symptomes = Crise.joindre(symptomes),
        localisations = Crise.joindre(localisations),
        medicament = medicament,
        efficacite = efficacite,
        commentaire = commentaire,
        creeLe = creeLe,
        efferalganPrises = efferalganPrises,
        efferalganMg = efferalganMg,
        ibuprofenePrises = ibuprofenePrises,
        ibuprofeneMg = ibuprofeneMg,
    )

    companion object {
        fun depuis(crise: Crise): Formulaire {
            val zone = ZoneId.systemDefault()
            val debut = java.time.Instant.ofEpochMilli(crise.debutLe).atZone(zone)
            val fin = crise.finLe?.let { java.time.Instant.ofEpochMilli(it).atZone(zone) }
            // Pas de plafond à 3 : une sauvegarde importée peut porter un décalage
            // plus long, le ramener à 3 fausserait la crise à la modification.
            val decalage = if (fin == null) 1
            else (fin.toLocalDate().toEpochDay() - debut.toLocalDate().toEpochDay()).toInt().coerceAtLeast(1)
            return Formulaire(
                id = crise.id,
                jour = debut.toLocalDate(),
                heureDebut = debut.hour,
                heureFin = fin?.hour ?: (debut.hour + 1).coerceAtMost(23),
                enCours = crise.finLe == null,
                decalageJours = decalage,
                niveau = crise.niveau,
                sommeil = crise.sommeil,
                reveils = crise.reveils,
                declencheurs = Crise.decouper(crise.declencheurs).toSet(),
                repas = Crise.decouper(crise.repas).toSet(),
                symptomes = Crise.decouper(crise.symptomes).toSet(),
                localisations = Crise.decouper(crise.localisations).toSet(),
                medicament = crise.medicament,
                efficacite = crise.efficacite,
                commentaire = crise.commentaire,
                efferalganPrises = crise.efferalganPrises,
                efferalganMg = crise.efferalganMg,
                ibuprofenePrises = crise.ibuprofenePrises,
                ibuprofeneMg = crise.ibuprofeneMg,
            )
        }
    }
}
