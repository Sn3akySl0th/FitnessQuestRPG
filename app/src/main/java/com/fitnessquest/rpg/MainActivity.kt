package com.fitnessquest.rpg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fitnessquest.rpg.ui.FitQuestNav
import com.fitnessquest.rpg.ui.theme.FitQuestTheme

class MainActivity : ComponentActivity() {

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User declined update confirmation dialog; flexible update will not start this launch
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as? FitQuestApp)?.container
        container?.inAppUpdate?.checkForUpdate(updateLauncher)

        setContent {
            FitQuestTheme {
                FitQuestNav()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as? FitQuestApp)?.container?.inAppUpdate?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? FitQuestApp)?.container?.inAppUpdate?.onDestroy()
    }
}
