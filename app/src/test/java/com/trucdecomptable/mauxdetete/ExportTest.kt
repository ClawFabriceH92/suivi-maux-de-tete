package com.trucdecomptable.mauxdetete

import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.export.ExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ExportTest {

    private val zone = ZoneId.of("Europe/Paris")

    private fun ms(jour: Int, heure: Int): Long =
        LocalDateTime.of(2026, 11, jour, heure, 0).atZone(zone).toInstant().toEpochMilli()

    private val crise = Crise(
        id = 7,
        debutLe = ms(1, 20),
        finLe = ms(2, 9),
        niveau = 8,
        sommeil = "mauvais",
        reveils = 2,
        declencheurs = "Stress|Écran",
        repas = "Fromage",
        symptomes = "Nausée",
        localisations = "Tempes",
        medicament = "Triptan",
        efficacite = "Soulagé",
        commentaire = "Soirée difficile",
        creeLe = ms(2, 10),
    )

    @Test
    fun entete_csv_avec_point_virgule() {
        val csv = ExportManager.construireCsv(listOf(crise), zone)
        val entete = csv.lineSequence().first()
        assertTrue(entete.startsWith("date_debut;heure_debut;date_fin;heure_fin;duree_heures;depasse_soiree"))
        assertTrue(entete.contains("medicament"))
    }

    @Test
    fun ligne_csv_porte_les_valeurs_attendues() {
        val ligne = ExportManager.construireCsv(listOf(crise), zone).lineSequence().toList()[1]
        val champs = ligne.split(";")
        assertEquals("2026-11-01", champs[0])
        assertEquals("20:00", champs[1])
        assertEquals("2026-11-02", champs[2])
        assertEquals("13", champs[4])
        assertEquals("oui", champs[5])
        assertEquals("8", champs[6])
        assertEquals("Triptan", champs[13])
        assertTrue(champs[9].contains("Stress"))
    }

    @Test
    fun csv_ne_casse_pas_une_liste_vide() {
        val vierge = crise.copy(repas = "", medicament = null, efficacite = null)
        val champs = ExportManager.construireCsv(listOf(vierge), zone).lineSequence().toList()[1].split(";")
        assertEquals("", champs[10])
        assertEquals("", champs[13])
    }

    @Test
    fun csv_porte_les_quantites_de_medicaments() {
        val avec = crise.copy(efferalganPrises = 2, efferalganMg = 1000, ibuprofenePrises = 1, ibuprofeneMg = 400)
        val champs = ExportManager.construireCsv(listOf(avec), zone).lineSequence().toList()[1].split(";")
        assertEquals("2", champs[16])
        assertEquals("1000", champs[17])
        assertEquals("1", champs[18])
        assertEquals("400", champs[19])
    }

    @Test
    fun json_aller_retour_conserve_les_champs() {
        val texte = """{
            "application": "suivi-maux-de-tete",
            "format": 1,
            "crises": [
              {"debut_le": 1762033200000, "fin_le": 1762074000000, "niveau": 8, "sommeil": "mauvais",
               "reveils": 2, "declencheurs": "Stress|Écran", "repas": "Fromage", "symptomes": "Nausée",
               "localisations": "Tempes", "medicament": "Triptan", "efficacite": "Soulagé",
               "commentaire": "Soirée difficile", "cree_le": 1762077600000}
            ]
        }""".trimIndent()
        val relues = ExportManager.lireJson(texte)
        assertEquals(1, relues.size)
        val c = relues.first()
        assertEquals(8, c.niveau)
        assertEquals("mauvais", c.sommeil)
        assertEquals(2, c.reveils)
        assertEquals(listOf("Stress", "Écran"), Crise.decouper(c.declencheurs))
        assertEquals("Triptan", c.medicament)
        assertNull(c.copy(finLe = null).finLe)
    }

    @Test(expected = IllegalArgumentException::class)
    fun json_dune_autre_application_est_refuse() {
        ExportManager.lireJson("""{"application":"notes-de-frais","crises":[]}""")
    }
}
