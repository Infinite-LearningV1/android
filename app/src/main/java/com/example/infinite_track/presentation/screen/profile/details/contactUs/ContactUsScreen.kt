package com.example.infinite_track.presentation.screen.profile.details.contactUs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500

@Composable
fun ContactUsScreen(
    onBackClick: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = "Contact Support",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.contact_us_description),
                style = body1,
                color = Purple_500
            )

            SupportInfoCard(
                title = stringResource(R.string.customer_support),
                rows = listOf(
                    Triple("Contact Number", "(0778) 123456", R.drawable.ic_phone),
                    Triple(stringResource(R.string.email_address), "help@infinitetrack.com", R.drawable.ic_mail)
                )
            )

            SupportInfoCard(
                title = stringResource(R.string.social_media),
                rows = listOf(
                    Triple("Instagram", "@InfiniteTrack", R.drawable.ic_instagram),
                    Triple("X", "@InfiniteTrack", R.drawable.ic_mail),
                    Triple("Facebook", "@InfiniteTrack", R.drawable.ic_facebook)
                )
            )
        }
    }
}

@Composable
private fun SupportInfoCard(
    title: String,
    rows: List<Triple<String, String, Int>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.10f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.08f)
            )
            .background(InfiniteColors.AttendanceReportGlassSurface, RoundedCornerShape(24.dp))
            .border(
                BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
                RoundedCornerShape(24.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = headline4,
                color = Purple_500
            )
            rows.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(row.third),
                        contentDescription = row.first,
                        tint = Blue_500,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = row.first,
                            style = body2,
                            color = Purple_300
                        )
                        Text(
                            text = row.second,
                            style = body1,
                            color = Purple_500
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactUsPreview() {
    ContactUsScreen(onBackClick = {})
}
