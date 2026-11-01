package com.trucdecomptable.mauxdetete

import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.data.Referentiels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicamentsTest {

    private fun crise() = Crise(
        debutLe = 1_762_000_000_000,
        niveau = 6,
        sommeil = Referentiels.SOMMEIL_MOYEN,
    )

    @Test
    fun dose_totale_somme_les_deux_molecules() {
        val c = crise().copy(
            efferalganPrises = 2, efferalganMg = 1000,
            ibuprofenePrises = 1, ibuprofeneMg = 400,
        )
        assertEquals(2400, c.mgTotal)
    }

    @Test
    fun rien_pris_donne_un_resume_vide() {
        val c = crise()
        assertEquals(0, c.mgTotal)
        assertFalse(c.aPrisQuelqueChose)
        assertEquals("", c.resumeMedicaments())
    }

    @Test
    fun resume_efferalgan_seul() {
        val c = crise().copy(efferalganPrises = 1, efferalganMg = 500)
        assertEquals("Efferalgan 500 mg ×1", c.resumeMedicaments())
    }

    @Test
    fun resume_avec_triptan_et_efficacite() {
        val c = crise().copy(
            ibuprofenePrises = 2, ibuprofeneMg = 200,
            medicament = "Triptan", efficacite = "Soulagé",
        )
        assertTrue(c.aPrisQuelqueChose)
        assertTrue(c.resumeMedicaments().contains("Ibuprofène 200 mg ×2"))
        assertTrue(c.resumeMedicaments().contains("Triptan (Soulagé)"))
    }

    @Test
    fun une_seule_molecule_suffit_a_compter_un_jour_de_prise() {
        val c = crise().copy(ibuprofenePrises = 1)
        assertTrue(c.aPrisQuelqueChose)
    }

    @Test
    fun dosages_par_defaut() {
        val c = crise()
        assertEquals(Referentiels.EFFERALGAN_MG_DEFAUT, c.efferalganMg)
        assertEquals(Referentiels.IBUPROFENE_MG_DEFAUT, c.ibuprofeneMg)
    }
}
