package com.trucdecomptable.mauxdetete.data

import android.content.Context
import android.content.SharedPreferences

/** Réglages persistants : verrouillage, sauvegarde automatique, version ignorée. */
class Reglages(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reglages", Context.MODE_PRIVATE)

    var verrouillageBiometrique: Boolean
        get() = prefs.getBoolean(CLE_BIO, true)
        set(valeur) = prefs.edit().putBoolean(CLE_BIO, valeur).apply()

    var sauvegardeAutomatique: Boolean
        get() = prefs.getBoolean(CLE_SAUVEGARDE, true)
        set(valeur) = prefs.edit().putBoolean(CLE_SAUVEGARDE, valeur).apply()

    var versionIgnoree: String
        get() = prefs.getString(CLE_VERSION_IGNOREE, "").orEmpty()
        set(valeur) = prefs.edit().putString(CLE_VERSION_IGNOREE, valeur).apply()

    /** Horodatage de la dernière sauvegarde automatique mensuelle. */
    var derniereSauvegarde: Long
        get() = prefs.getLong(CLE_DERNIERE_SAUVEGARDE, 0L)
        set(valeur) = prefs.edit().putLong(CLE_DERNIERE_SAUVEGARDE, valeur).apply()

    companion object {
        private const val CLE_BIO = "verrouillage_biometrique"
        private const val CLE_SAUVEGARDE = "sauvegarde_automatique"
        private const val CLE_VERSION_IGNOREE = "version_ignoree"
        private const val CLE_DERNIERE_SAUVEGARDE = "derniere_sauvegarde"
    }
}
