package com.trucdecomptable.mauxdetete

import com.trucdecomptable.mauxdetete.util.Temps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TempsTest {

    private val zone = ZoneId.of("Europe/Paris")

    private fun ms(annee: Int, mois: Int, jour: Int, heure: Int): Long =
        LocalDateTime.of(annee, mois, jour, heure, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun duree_entre_deux_heures_pleines() {
        val debut = ms(2026, 11, 1, 8)
        val fin = ms(2026, 11, 1, 9)
        assertEquals(1L, Temps.dureeHeures(debut, fin, fin))
    }

    @Test
    fun duree_nuit_complete_13h() {
        val debut = ms(2026, 11, 1, 20)
        val fin = ms(2026, 11, 2, 9)
        assertEquals(13L, Temps.dureeHeures(debut, fin, fin))
    }

    @Test
    fun crise_en_cours_utilise_maintenant() {
        val debut = ms(2026, 11, 1, 20)
        val maintenant = ms(2026, 11, 1, 23)
        assertEquals(3L, Temps.dureeHeures(debut, null, maintenant))
    }

    @Test
    fun fin_avant_debut_ne_donne_pas_de_duree_negative() {
        val debut = ms(2026, 11, 1, 20)
        val fin = ms(2026, 11, 1, 9)
        assertEquals(0L, Temps.dureeHeures(debut, fin, fin))
    }

    @Test
    fun franchit_minuit_seulement_si_jour_change() {
        assertTrue(Temps.franchitMinuit(ms(2026, 11, 1, 20), ms(2026, 11, 2, 9), zone))
        assertFalse(Temps.franchitMinuit(ms(2026, 11, 1, 8), ms(2026, 11, 1, 9), zone))
        assertFalse(Temps.franchitMinuit(ms(2026, 11, 1, 8), null, zone))
    }

    @Test
    fun depasse_soiree_si_franchit_minuit_ou_six_heures() {
        assertTrue(Temps.depasseSoiree(ms(2026, 11, 1, 20), ms(2026, 11, 2, 9), ms(2026, 11, 2, 10), zone))
        assertTrue(Temps.depasseSoiree(ms(2026, 11, 1, 14), ms(2026, 11, 1, 20), ms(2026, 11, 1, 21), zone))
        assertFalse(Temps.depasseSoiree(ms(2026, 11, 1, 8), ms(2026, 11, 1, 9), ms(2026, 11, 1, 10), zone))
    }

    @Test
    fun crise_en_cours_depasse_toujours_la_soiree() {
        assertTrue(Temps.depasseSoiree(ms(2026, 11, 1, 20), null, ms(2026, 11, 1, 21), zone))
    }

    @Test
    fun report_au_lendemain_quand_fin_avant_debut() {
        val debut = ms(2026, 11, 1, 20)
        val fin = Temps.finReportee(debut, heureFin = 9, heureDebut = 20, decalageJours = 1, zone = zone)
        assertEquals(ms(2026, 11, 2, 9), fin)
        assertEquals(13L, Temps.dureeHeures(debut, fin, fin))
    }

    @Test
    fun report_a_deux_et_trois_jours() {
        val debut = ms(2026, 11, 1, 20)
        val fin2 = Temps.finReportee(debut, 9, 20, 2, zone)
        val fin3 = Temps.finReportee(debut, 9, 20, 3, zone)
        assertEquals(37L, Temps.dureeHeures(debut, fin2, fin2))
        assertEquals(61L, Temps.dureeHeures(debut, fin3, fin3))
    }

    @Test
    fun pas_de_report_quand_fin_apres_debut() {
        val debut = ms(2026, 11, 1, 8)
        val fin = Temps.finReportee(debut, 9, 8, 1, zone)
        assertEquals(ms(2026, 11, 1, 9), fin)
    }

    @Test
    fun libelle_duree() {
        assertEquals("moins d'1 h", Temps.libelleDuree(0))
        assertEquals("13 h", Temps.libelleDuree(13))
        assertEquals("3 h (en cours)", Temps.libelleDuree(3, enCours = true))
    }
}
