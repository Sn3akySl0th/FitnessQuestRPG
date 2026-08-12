package com.fitnessquest.rpg.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.ui.theme.FitQuestTheme

/**
 * Shown when the user taps the privacy-policy link on the Health Connect permissions screen.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitQuestTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "FitnessRPG & Health Connect",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "FitnessRPG reads your height and weight from Health Connect so it can:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Estimate heart-rate zones with a max HR based on your age\n" +
                            "• Prefill bodyweight for bodyweight exercises\n" +
                            "• Keep workout coaching grounded in your real body metrics",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "We do not write health data back to Health Connect. " +
                            "Watch calorie estimates still use the Fitbit / system profile " +
                            "on your Pixel Watch. You can revoke access anytime in Health Connect settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { finish() }) { Text("Close") }
                }
            }
        }
    }
}
