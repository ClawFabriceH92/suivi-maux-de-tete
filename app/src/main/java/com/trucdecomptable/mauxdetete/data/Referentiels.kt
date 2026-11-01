package com.trucdecomptable.mauxdetete.data

/** Listes fixes proposées en cases à cocher. Aucune saisie libre, aucune requête réseau. */
object Referentiels {

    val DECLENCHEURS = listOf(
        "Stress", "Fatigue", "Écran", "Chaleur", "Règles", "Sport",
        "Bruit", "Parfum", "Saut de repas", "Sommeil court",
    )

    val REPAS = listOf(
        "Sucré", "Alcool", "Fromage", "Caféine", "Charcuterie",
        "Friture", "Viande rouge", "Repas sauté",
    )

    val SYMPTOMES = listOf(
        "Aura visuelle", "Nausée", "Gêne à la lumière", "Gêne au bruit",
        "Vertiges", "Odeurs",
    )

    val LOCALISATIONS = listOf(
        "Tempes", "Front", "Arrière", "Nuque", "Un seul côté",
        "Les deux côtés", "Autour des yeux",
    )

    val MEDICAMENTS = listOf("Paracétamol", "AINS / ibuprofène", "Triptan", "Autre")

    val EFFICACITES = listOf("Soulagé", "Un peu", "Pas du tout")

    const val SOMMEIL_BON = "bon"
    const val SOMMEIL_MOYEN = "moyen"
    const val SOMMEIL_MAUVAIS = "mauvais"

    /** Seuils de céphalée par abus médicamenteux (ICHD-3), en jours par mois. */
    const val SEUIL_MEDICAMENT = 15
    const val SEUIL_TRIPTAN = 10

    /** Sous ce nombre de cas, aucune corrélation n'est affichée. */
    const val MIN_CAS = 5

    fun libelleSommeil(v: String): String = when (v) {
        SOMMEIL_BON -> "Bon"
        SOMMEIL_MOYEN -> "Moyen"
        else -> "Mauvais"
    }

    fun libelleNiveau(n: Int): String = when {
        n <= 0 -> "aucune"
        n <= 2 -> "très légère"
        n <= 4 -> "gênante"
        n <= 6 -> "modérée"
        n <= 8 -> "forte"
        else -> "invalidante"
    }
}
