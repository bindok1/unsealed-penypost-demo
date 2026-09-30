package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stub sheet shared by Font Picker and Overflow Menu entry points, which
 * aren't built yet this pass. Demonstrates the tap -> state flip -> UI
 * reacts wiring end-to-end without building the real sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Fitur ini belum tersedia di build ini.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
