package io.github.digihori.pgp.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.digihori.pgp.core.ProjectInfo

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = ProjectInfo.DISPLAY_NAME,
    ) {
        App()
    }
}

@Composable
private fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                Text(
                    text = ProjectInfo.DISPLAY_NAME,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text("Project foundation is ready.")
            }
        }
    }
}
