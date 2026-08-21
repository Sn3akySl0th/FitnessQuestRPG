package com.fitnessquest.rpg

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fitnessquest.rpg.ui.FitQuestNav
import com.fitnessquest.rpg.ui.theme.FitQuestTheme

import android.content.pm.ActivityInfo
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @SuppressLint("InvalidFragmentVersionForActivityResult")
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

        // Observe screen orientation lock preference
        container?.let { appContainer ->
            lifecycleScope.launch {
                appContainer.prefs.lockPortrait.collect { lockPortrait ->
                    requestedOrientation = if (lockPortrait) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }

        setContent {
            FitQuestTheme {
                FitQuestNav()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as? FitQuestApp)?.container
        container?.inAppUpdate?.onResume()
        container?.steps?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? FitQuestApp)?.container?.inAppUpdate?.onDestroy()
    }
}
