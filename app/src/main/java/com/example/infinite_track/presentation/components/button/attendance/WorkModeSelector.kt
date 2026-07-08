package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.components.button.RadioButtonWithText
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.theme.Blue_700
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

/**
 * Komponen selector untuk memilih mode kerja (WFO, WFH, atau WFA).
 *
 * @param selectedMode Mode yang dipilih saat ini.
 * @param onModeSelected Callback yang dipanggil ketika mode dipilih
 */
@Composable
fun WorkModeSelector(
    selectedMode: WorkMode,
    onModeSelected: (WorkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WorkMode.values().forEach { mode ->
            RadioButtonWithText(
                text = mode.labelText(),
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun WorkMode.labelText(): String {
    return when (this) {
        WorkMode.WFO -> stringResource(R.string.work_from_office)
        WorkMode.WFH -> stringResource(R.string.work_from_home)
        WorkMode.WFA -> stringResource(R.string.work_from_anywhere)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkModeSelectorPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.work_mode_selector_title),
                style = headline4,
                color = Blue_700
            )

            var selectedMode by remember { mutableStateOf(WorkMode.WFH) }

            WorkModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it }
            )

            Text(
                text = "Selected: $selectedMode",
                style = headline4,
                color = Blue_700
            )
        }
    }
}
