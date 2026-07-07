package com.example.infinite_track.presentation.design.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteTopBar(
    title: String,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, color = InfiniteColors.Text) },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(imageVector = navigationIcon, contentDescription = navigationContentDescription, tint = InfiniteColors.Text)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = InfiniteColors.Surface.copy(alpha = 0.72f),
            titleContentColor = InfiniteColors.Text,
            actionIconContentColor = InfiniteColors.Primary
        )
    )
}
