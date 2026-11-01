package com.trucdecomptable.mauxdetete.data

import com.trucdecomptable.mauxdetete.util.Temps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Résultat d'une corrélation, avec son effectif. Rien n'est affiché sous [Referentiels.MIN_CAS]. */
data class Correlation(val moyenne: Double?, val effectif: Int) {
    val concluable: Boolean get() = effectif >= Referentiels.MIN_CAS
}

data class Frequence(val libelle: String, val effectif: Int, val pourcentage: Int)

data class Bilan(
    val crisesMois: Int,
    val intensiteMoyenne: Double,
    val dureeMoyenneHeures: Double,
    val joursSansDouleur: Int,
    val crisesLongues: Int,
    val soireesDepassees: Int,
    val dureeMaxHeures: Long,
    val parHeure: List<Int>,
    val parJourSemaine: List<Int>,
    val semaineCourante: Int,
    val semainePrecedente: Int,
    val sommeilBon: Correlation,
    val sommeilMauvais: Correlation,
    val medicamentsJours: Int,
    val prisesMois: Int,
    val mgMois: Int,
    val seuilMedicaments: Int,
    val declencheurs: List<Frequence>,
    val repas: List<Frequence>,
)

/**
 * Calculs statistiques, sans Android : testables en JVM pure.
 * Les crises sont passées en entier, le bilan ne retient que les 30 derniers jours
 * pour les moyennes et laisse les totaux du mois à part.
 */
object Stats {

    fun bilan(
        crises: List<Crise>,
        maintenant: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Bilan {
        val aujourdHui = Instant.ofEpochMilli(maintenant).atZone(zone).toLocalDate()
        fun jourDe(c: Crise): LocalDate = Temps.jourLocal(c.debutLe, zone)

        val mois = crises.filter {
            val j = jourDe(it)
            j.year == aujourdHui.year && j.monthValue == aujourdHui.monthValue
        }
        val debut30 = aujourdHui.minusDays(29)
        val sur30 = crises.filter { !jourDe(it).isBefore(debut30) }

        val intensite = if (sur30.isEmpty()) 0.0 else sur30.sumOf { it.niveau }.toDouble() / sur30.size
        val duree = if (sur30.isEmpty()) 0.0 else
            sur30.sumOf { Temps.dureeHeures(it.debutLe, it.finLe, maintenant) }.toDouble() / sur30.size

        val joursAvecDouleur = sur30.map { jourDe(it) }.distinct().size
        val longues = sur30.count { Temps.estLongue(it.debutLe, it.finLe, maintenant) }
        val soirees = sur30.count { Temps.depasseSoiree(it.debutLe, it.finLe, maintenant, zone) }
        val dureeMax = sur30.maxOfOrNull { Temps.dureeHeures(it.debutLe, it.finLe, maintenant) } ?: 0L

        val parHeure = MutableList(24) { 0 }
        sur30.forEach { parHeure[Instant.ofEpochMilli(it.debutLe).atZone(zone).hour] += 1 }

        val parJour = MutableList(7) { 0 }
        sur30.forEach { parJour[jourDe(it).dayOfWeek.value % 7] += 1 }

        val ilYa7 = aujourdHui.minusDays(6)
        val ilYa14 = aujourdHui.minusDays(13)
        val semaineCourante = crises.count { !jourDe(it).isBefore(ilYa7) }
        val semainePrecedente = crises.count {
            !jourDe(it).isBefore(ilYa14) && jourDe(it).isBefore(ilYa7)
        }

        fun correlation(valeur: String): Correlation {
            val lot = crises.filter { it.sommeil == valeur }
            return Correlation(
                moyenne = if (lot.isEmpty()) null else lot.sumOf { it.niveau }.toDouble() / lot.size,
                effectif = lot.size,
            )
        }

        val joursMedicament = mois.filter { it.aPrisQuelqueChose }.map { jourDe(it) }.distinct().size
        val prises = mois.sumOf { it.efferalganPrises + it.ibuprofenePrises }
        val mg = mois.sumOf { it.mgTotal }
        val seuil = if (mois.any { it.medicament == "Triptan" }) Referentiels.SEUIL_TRIPTAN
        else Referentiels.SEUIL_MEDICAMENT

        return Bilan(
            crisesMois = mois.size,
            intensiteMoyenne = intensite,
            dureeMoyenneHeures = duree,
            joursSansDouleur = 30 - joursAvecDouleur,
            crisesLongues = longues,
            soireesDepassees = soirees,
            dureeMaxHeures = dureeMax,
            parHeure = parHeure,
            parJourSemaine = parJour,
            semaineCourante = semaineCourante,
            semainePrecedente = semainePrecedente,
            sommeilBon = correlation(Referentiels.SOMMEIL_BON),
            sommeilMauvais = correlation(Referentiels.SOMMEIL_MAUVAIS),
            medicamentsJours = joursMedicament,
            prisesMois = prises,
            mgMois = mg,
            seuilMedicaments = seuil,
            declencheurs = frequences(crises) { Crise.decouper(it.declencheurs) },
            repas = frequences(crises) { Crise.decouper(it.repas) },
        )
    }

    private fun frequences(crises: List<Crise>, extraire: (Crise) -> List<String>): List<Frequence> {
        if (crises.isEmpty()) return emptyList()
        val comptes = mutableMapOf<String, Int>()
        crises.forEach { crise -> extraire(crise).forEach { comptes[it] = (comptes[it] ?: 0) + 1 } }
        return comptes.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { Frequence(it.key, it.value, Math.round(it.value * 100f / crises.size)) }
    }
}
