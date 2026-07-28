package com.example.infinite_track.presentation.design.components.input

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun InfiniteSupportingText(
    text: String,
    modifier: Modifier = Modifier,
    semantic: InfiniteSemantic = InfiniteSemantic.Neutral
) {
    Text(
        text = text,
        modifier = modifier,
        style = body2,
        color = infiniteSemanticColors(semantic).accent
    )
}

@Preview(showBackground = true)
@Composable
private fun InfiniteSupportingTextPreview() {
    Infinite_TrackTheme {
        InfiniteSupportingText(
            text = "Kolom ini wajib diisi",
            semantic = InfiniteSemantic.Error
        )
    }
}
