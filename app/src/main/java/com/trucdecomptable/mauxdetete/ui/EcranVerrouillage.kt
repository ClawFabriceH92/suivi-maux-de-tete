package com.trucdecomptable.mauxdetete.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Données de santé : l'app peut demander l'empreinte au lancement.
 * Si le téléphone n'a aucun moyen d'authentification, on n'enferme personne
 * dehors : l'écran se déverrouille et le réglage est désactivé.
 */
@Composable
fun EcranVerrouillage(activity: FragmentActivity, onDeverrouille: () -> Unit) {
    val disponible = BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

    fun demander() {
        if (!disponible) {
            onDeverrouille()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onDeverrouille()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Maux de tête")
                .setSubtitle("Déverrouille pour accéder à tes données")
                .setNegativeButtonText("Annuler")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build(),
        )
    }

    LaunchedEffect(Unit) { if (!disponible) onDeverrouille() else demander() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Maux de tête", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Verrouillé",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
        )
        Button(
            onClick = { demander() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Déverrouiller", fontWeight = FontWeight.Bold) }
    }
}
