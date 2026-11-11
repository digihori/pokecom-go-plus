package com.digihori.pgp.player.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** ROM-free host for testing the production save dialog on constrained screens. */
class MemorySaveProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val density = LocalDensity.current
            val fontScale = intent.getFloatExtra("fontScale", 1f)
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    var result by remember { mutableStateOf("") }
                    var open by remember { mutableStateOf(true) }
                    Text(result)
                    if (open) MemorySaveDialog(
                        onDismiss = { result = "CANCELLED"; open = false },
                        onSave = { start, end -> result = "SAVED $start $end"; open = false },
                    )
                }
            }
        }
    }
}
