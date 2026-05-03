package com.example.infinite_track.presentation.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun BottomBarInternship(navController: NavController) {
    MainBottomBar(
        navController = navController,
        userRole = "Internship"
    )
}

@Composable
@Preview
fun PreviewBottomBarIntership() {
    Infinite_TrackTheme {
        BottomBarInternship(rememberNavController())
    }
}
