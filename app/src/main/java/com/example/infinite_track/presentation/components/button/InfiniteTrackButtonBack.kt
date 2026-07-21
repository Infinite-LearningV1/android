package com.example.infinite_track.presentation.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

/**
 * Legacy back-bar API preserved for existing call sites.
 * Internally uses the global glass navigation app bar.
 */
@Composable
fun InfiniteTracButtonBack(
    modifier: Modifier = Modifier,
    title: String,
    navigationBack: () -> Unit
) {
    InfiniteTopBar(
        title = title,
        modifier = modifier,
        onNavigationClick = navigationBack
    )
}

@Composable
@Preview(showBackground = true)
fun InfiniteTracButtonBackPreview() {
    Infinite_TrackTheme {
        InfiniteTracButtonBack(
            navigationBack = {},
            title = "Sample Title"
        )
    }
}
