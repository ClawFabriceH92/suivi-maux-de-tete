package com.trucdecomptable.mauxdetete

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
}
