package com.example.infinite_track.presentation.screen.home.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.components.empty.ComingSoonAnimation
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.theme.Purple_500

@Composable
fun CompanyServiceComingSoonScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = "Company Services",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = (-50).dp)
            ) {
                ComingSoonAnimation(modifier = Modifier.size(350.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coming Soon",
                    style = headline4,
                    color = Purple_500,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
