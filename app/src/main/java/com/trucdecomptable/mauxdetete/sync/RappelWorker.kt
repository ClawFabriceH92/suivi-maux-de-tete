package com.trucdecomptable.mauxdetete.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trucdecomptable.mauxdetete.data.AppDb
import com.trucdecomptable.mauxdetete.util.Diag

/**
 * Réveillé le lendemain d'une crise ouverte. Vérifie en base que la crise
 * court toujours avant de notifier : si elle a été clôturée entre-temps,
 * rien n'est affiché.
 */
class RappelWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(CLE_CRISE, -1L)
        if (id <= 0) return Result.success()
        return runCatching {
            val crise = AppDb.get(applicationContext).dao()
                .toutesMaintenant()
                .firstOrNull { it.id == id }
            when {
                crise == null -> {
                    Diag.record("rappel", "crise $id supprimée, aucun rappel")
                }
                crise.finLe != null -> {
                    Diag.record("rappel", "crise $id déjà clôturée, aucun rappel")
                }
                else -> {
                    NotificationsCrise.afficher(applicationContext, crise)
                    Diag.record("rappel", "notification envoyée pour la crise $id")
                }
            }
            Result.success()
        }.getOrElse {
            Diag.record("rappel", it)
            Result.success()
        }
    }

    companion object {
        const val CLE_CRISE = "crise_id"
    }
}
