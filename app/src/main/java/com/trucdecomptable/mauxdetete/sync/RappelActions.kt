package com.trucdecomptable.mauxdetete.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trucdecomptable.mauxdetete.data.AppDb
import com.trucdecomptable.mauxdetete.util.Diag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Boutons de la notification du lendemain : clôturer, ou repousser d'un jour. */
class RappelActions : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(NotificationsCrise.EXTRA_ID, -1L)
        if (id <= 0) return
        val action = intent.action ?: return
        val attente = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.get(context).dao()
                val crise = dao.toutesMaintenant().firstOrNull { it.id == id }
                if (crise != null) {
                    when (action) {
                        NotificationsCrise.ACTION_TERMINER -> {
                            dao.mettreAJour(crise.copy(finLe = System.currentTimeMillis()))
                            Rappels.annuler(context, id)
                            Diag.record("rappel", "crise $id clôturée depuis la notification")
                        }
                        NotificationsCrise.ACTION_REPLANIFIER -> {
                            Rappels.programmer(context, id, depuisNotification = true)
                            Diag.record("rappel", "crise $id repoussée au lendemain")
                        }
                    }
                }
                NotificationsCrise.annuler(context, id)
            } catch (e: Exception) {
                Diag.record("rappel/action", e)
            } finally {
                attente.finish()
            }
        }
    }
}
