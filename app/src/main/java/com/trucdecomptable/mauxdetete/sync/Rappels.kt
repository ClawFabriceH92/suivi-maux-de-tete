package com.trucdecomptable.mauxdetete.sync

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.trucdecomptable.mauxdetete.util.Diag
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Programme le rappel du lendemain matin pour une crise encore ouverte.
 * WorkManager plutôt qu'AlarmManager : le rappel survit au redémarrage du
 * téléphone et ne demande aucune permission d'alarme exacte.
 */
object Rappels {

    const val HEURE_RAPPEL = 9
    private const val PREFIXE = "rappel-crise-"

    fun delaiJusquAuProchainRappel(
        maintenant: LocalDateTime = LocalDateTime.now(),
        heure: Int = HEURE_RAPPEL,
    ): Duration {
        val cible = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(heure, 0))
        val delai = Duration.between(maintenant, cible)
        return if (delai.isNegative) Duration.ZERO else delai
    }

    fun programmer(context: Context, criseId: Long, depuisNotification: Boolean = false) {
        val delai = delaiJusquAuProchainRappel()
        val travail = OneTimeWorkRequestBuilder<RappelWorker>()
            .setInitialDelay(delai.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(RappelWorker.CLE_CRISE, criseId).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PREFIXE + criseId,
            ExistingWorkPolicy.REPLACE,
            travail,
        )
        Diag.record(
            "rappel",
            "rappel programmé pour la crise $criseId dans ${delai.toHours()} h" +
                if (depuisNotification) " (repoussé)" else "",
        )
    }

    fun annuler(context: Context, criseId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(PREFIXE + criseId)
        NotificationsCrise.annuler(context, criseId)
    }
}
