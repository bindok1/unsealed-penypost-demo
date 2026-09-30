package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/**
 * Real Annotate Mode toolbar (compose-screen-spec.md §5), replacing the
 * static "coming soon" AnnotateModeStubBar. Single row: 5 tool buttons plus
 * one color trigger (see [AnnotateColorButton]) that opens a spectrum picker
 * popup instead of a fixed row of preset swatches. Tool buttons follow
 * motion-rules.md §3.1/§4 properly (scale feedback, no ripple) as new
 * surface area, unlike the pre-existing ComposeToolbar/FormattingBar buttons
 * which still use default IconButton ripple.
 */
@Composable
fun AnnotateToolbar(
    annotateState: AnnotateState,
    onToolSelect: (AnnotateTool) -> Unit,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ToolbarFrostedDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIconToggleButton(
            icon = Icons.Filled.Brush,
            contentDescription = stringResource(R.string.annotate_tool_pen),
            isSelected = annotateState.selectedTool == AnnotateTool.PEN,
            onClick = { onToolSelect(AnnotateTool.PEN) },
        )
        PillIconToggleButton(
            icon = Icons.Filled.Draw,
            contentDescription = stringResource(R.string.annotate_tool_fine),
            isSelected = annotateState.selectedTool == AnnotateTool.FINE,
            onClick = { onToolSelect(AnnotateTool.FINE) },
        )
        PillIconToggleButton(
            icon = Icons.Filled.Waves,
            contentDescription = stringResource(R.string.annotate_tool_wave),
            isSelected = annotateState.selectedTool == AnnotateTool.WAVE,
            onClick = { onToolSelect(AnnotateTool.WAVE) },
        )
        PillIconToggleButton(
            icon = Icons.Filled.AutoAwesome,
            contentDescription = stringResource(R.string.annotate_tool_deco),
            isSelected = annotateState.selectedTool == AnnotateTool.DECO,
            onClick = { onToolSelect(AnnotateTool.DECO) },
        )
        PillIconToggleButton(
            icon = Icons.Filled.CleaningServices,
            contentDescription = stringResource(R.string.annotate_tool_eraser),
            isSelected = annotateState.selectedTool == AnnotateTool.ERASER,
            onClick = { onToolSelect(AnnotateTool.ERASER) },
        )
        AnnotateColorButton(
            selectedColor = annotateState.inkColor,
            onColorSelect = onColorSelect,
        )
    }
}
