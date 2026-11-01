package com.trucdecomptable.mauxdetete.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trucdecomptable.mauxdetete.MainActivity
import com.trucdecomptable.mauxdetete.R
import com.trucdecomptable.mauxdetete.data.Crise
import com.trucdecomptable.mauxdetete.util.Diag
import com.trucdecomptable.mauxdetete.util.Temps

/**
 * Le lendemain d'une crise encore ouverte, l'app demande si c'est terminé.
 * Deux actions directement dans la notification : clôturer, ou repousser au
 * lendemain suivant. Aucune autre notification n'est envoyée.
 */
object NotificationsCrise {

    const val CANAL = "crise-en-cours"
    const val ACTION_TERMINER = "com.trucdecomptable.mauxdetete.TERMINER"
    const val ACTION_REPLANIFIER = "com.trucdecomptable.mauxdetete.REPLANIFIER"
    const val EXTRA_ID = "crise_id"
    private const val BASE_ID = 4200

    fun idNotification(criseId: Long): Int = BASE_ID + (criseId % 1000).toInt()

    fun creerCanal(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CANAL) != null) return
        val canal = NotificationChannel(
            CANAL,
            "Crise en cours",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Demande si la crise d'hier est terminée"
            enableVibration(true)
        }
        manager.createNotificationChannel(canal)
    }

    fun afficher(context: Context, crise: Crise) {
        creerCanal(context)
        val heures = Temps.dureeHeures(crise.debutLe, crise.finLe, System.currentTimeMillis())
        val ouverture = PendingIntent.getActivity(
            context,
            idNotification(crise.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("La crise est-elle terminée ?")
            .setContentText(
                "Crise ouverte depuis ${Temps.libelleDuree(heures)} " +
                    "(intensité ${crise.niveau}/10). Touche une action pour la clôturer.",
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Crise ouverte depuis ${Temps.libelleDuree(heures)} " +
                        "(intensité ${crise.niveau}/10). Si elle est finie, l'app enregistre " +
                        "l'heure de fin ; sinon elle te redemandera demain.",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(ouverture)
            .addAction(0, "C'est terminé", action(context, crise.id, ACTION_TERMINER, 1))
            .addAction(0, "Toujours en cours", action(context, crise.id, ACTION_REPLANIFIER, 2))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(idNotification(crise.id), notification)
        }.onFailure { Diag.record("notification", it) }
            .onSuccess {
                if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    Diag.record("notification", "notifications désactivées par l'utilisateur")
                }
            }
    }

    private fun action(context: Context, criseId: Long, action: String, code: Int): PendingIntent {
        val intent = Intent(context, RappelActions::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, criseId)
        }
        return PendingIntent.getBroadcast(
            context,
            idNotification(criseId) * 10 + code,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun annuler(context: Context, criseId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(idNotification(criseId))
        }
    }
}
