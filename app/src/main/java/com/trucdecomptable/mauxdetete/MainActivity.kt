package com.trucdecomptable.mauxdetete

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.trucdecomptable.mauxdetete.data.Reglages
import com.trucdecomptable.mauxdetete.ui.CriseViewModel
import com.trucdecomptable.mauxdetete.ui.EcranVerrouillage
import com.trucdecomptable.mauxdetete.ui.MauxDeTeteTheme
import com.trucdecomptable.mauxdetete.ui.Racine

/**
 * FragmentActivity (et non ComponentActivity) : BiometricPrompt en a besoin
 * pour afficher sa feuille d'authentification.
 */
class MainActivity : FragmentActivity() {

    private val vm: CriseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        demanderPermissionNotifications()
        val reglages = Reglages(this)
        setContent {
            MauxDeTeteTheme {
                var verrouille by remember { mutableStateOf(reglages.verrouillageBiometrique) }
                if (verrouille) {
                    EcranVerrouillage(this) { verrouille = false }
                } else {
                    Racine(vm)
                }
            }
        }
    }

    /**
     * Le rappel du lendemain passe par une notification : sur Android 13 et plus
     * la permission doit être demandée. Refusée, l'app reste utilisable, seul le
     * rappel est muet.
     */
    private fun demanderPermissionNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val accordee = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (accordee) return
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            DEMANDE_NOTIFICATIONS,
        )
    }

    companion object {
        private const val DEMANDE_NOTIFICATIONS = 1001
    }
}
