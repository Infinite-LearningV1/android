// File: presentation/components/selection/RadioButtonWithText.kt

package com.example.infinite_track.presentation.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_500

/**
 * Komponen yang menggabungkan RadioButton dengan Text label.
 * Seluruh baris dapat diklik untuk memilih.
 *
 * @param text Label yang akan ditampilkan di samping radio button.
 * @param selected Apakah radio button ini sedang dalam keadaan terpilih.
 * @param onClick Lambda yang akan dipanggil saat komponen diklik.
 */
@Composable
fun RadioButtonWithText(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Blue_500,
                unselectedColor = Blue_500.copy(alpha = 0.6f)
            )
        )
        Text(
            text = text,
            style = body1,
            color = Purple_500
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun RadioButtonWithTextPreview() {
    Infinite_TrackTheme {
        // Contoh penggunaan dalam sebuah grup
        val options = listOf("Work From Home", "Work From Anywhere")
        var selectedOption by remember { mutableStateOf(options[0]) }

        Column {
            options.forEach { text ->
                RadioButtonWithText(
                    text = text,
                    selected = (text == selectedOption),
                    onClick = { selectedOption = text }
                )
            }
        }
    }
}
