package com.trucdecomptable.mauxdetete

import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import com.trucdecomptable.mauxdetete.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StatsTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val maintenant = LocalDateTime.of(2026, 11, 1, 18, 0).atZone(zone).toInstant().toEpochMilli()

    private fun ms(mois: Int, jour: Int, heure: Int): Long =
        LocalDateTime.of(2026, mois, jour, heure, 0).atZone(zone).toInstant().toEpochMilli()

    private fun crise(
        debut: Long,
        fin: Long?,
        niveau: Int = 5,
        sommeil: String = Referentiels.SOMMEIL_MOYEN,
        medicament: String? = null,
        declencheurs: String = "",
    ) = Crise(
        debutLe = debut,
        finLe = fin,
        niveau = niveau,
        sommeil = sommeil,
        medicament = medicament,
        declencheurs = declencheurs,
    )

    @Test
    fun compte_les_crises_du_mois_courant() {
        val crises = listOf(
            crise(ms(11, 1, 7), ms(11, 1, 9)),
            crise(ms(10, 20, 7), ms(10, 20, 9)),
        )
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertEquals(1, bilan.crisesMois)
    }

    @Test
    fun jours_sans_douleur_sur_trente_jours() {
        val crises = listOf(crise(ms(11, 1, 7), ms(11, 1, 9)))
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertEquals(29, bilan.joursSansDouleur)
    }

    @Test
    fun intensite_moyenne() {
        val crises = listOf(
            crise(ms(11, 1, 7), ms(11, 1, 9), niveau = 4),
            crise(ms(10, 30, 7), ms(10, 30, 9), niveau = 8),
        )
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertEquals(6.0, bilan.intensiteMoyenne, 0.001)
    }

    @Test
    fun correlation_non_concluable_sous_cinq_cas() {
        val crises = (1..3).map { crise(ms(10, it + 10, 7), ms(10, it + 10, 9), sommeil = Referentiels.SOMMEIL_BON) }
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertEquals(3, bilan.sommeilBon.effectif)
        assertFalse(bilan.sommeilBon.concluable)
    }

    @Test
    fun correlation_concluable_a_partir_de_cinq_cas() {
        val crises = (1..5).map {
            crise(ms(10, it + 10, 7), ms(10, it + 10, 9), niveau = 7, sommeil = Referentiels.SOMMEIL_MAUVAIS)
        }
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertTrue(bilan.sommeilMauvais.concluable)
        assertEquals(7.0, bilan.sommeilMauvais.moyenne!!, 0.001)
    }

    @Test
    fun seuil_medicament_descend_a_dix_jours_avec_un_triptan() {
        val avecTriptan = listOf(crise(ms(11, 1, 7), ms(11, 1, 9), medicament = "Triptan"))
        val sansTriptan = listOf(crise(ms(11, 1, 7), ms(11, 1, 9), medicament = "Paracétamol"))
        assertEquals(Referentiels.SEUIL_TRIPTAN, Stats.bilan(avecTriptan, maintenant, zone).seuilMedicaments)
        assertEquals(Referentiels.SEUIL_MEDICAMENT, Stats.bilan(sansTriptan, maintenant, zone).seuilMedicaments)
    }

    @Test
    fun jours_de_medicament_comptes_une_fois_par_jour() {
        val crises = listOf(
            crise(ms(11, 1, 7), ms(11, 1, 9), medicament = "Paracétamol"),
            crise(ms(11, 1, 15), ms(11, 1, 16), medicament = "Paracétamol"),
            crise(ms(10, 31, 7), ms(10, 31, 9), medicament = "Paracétamol"),
        )
        assertEquals(1, Stats.bilan(crises, maintenant, zone).medicamentsJours)
    }

    @Test
    fun declencheurs_tries_par_frequence() {
        val crises = listOf(
            crise(ms(11, 1, 7), ms(11, 1, 9), declencheurs = "Stress|Écran"),
            crise(ms(10, 30, 7), ms(10, 30, 9), declencheurs = "Stress"),
        )
        val bilan = Stats.bilan(crises, maintenant, zone)
        assertEquals("Stress", bilan.declencheurs.first().libelle)
        assertEquals(2, bilan.declencheurs.first().effectif)
        assertEquals(100, bilan.declencheurs.first().pourcentage)
    }

    @Test
    fun liste_vide_ne_plante_pas() {
        val bilan = Stats.bilan(emptyList(), maintenant, zone)
        assertEquals(0, bilan.crisesMois)
        assertEquals(0.0, bilan.intensiteMoyenne, 0.001)
        assertEquals(30, bilan.joursSansDouleur)
        assertTrue(bilan.declencheurs.isEmpty())
    }
}
