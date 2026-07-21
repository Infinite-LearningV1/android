package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutFooter(
    note: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = InfiniteColors.AccountHubMutedText
        )
    }
}
