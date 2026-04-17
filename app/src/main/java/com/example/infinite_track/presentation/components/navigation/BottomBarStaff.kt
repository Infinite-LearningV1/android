package com.example.infinite_track.presentation.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.navigation.NavigationItem
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_500


@Composable
fun BottomBarStaff(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (currentRoute != Screen.Attendance.route) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            // Background blur layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .blur(radius = 10.dp)
            )

            // Glass effect overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.1f)
                        spotShadowColor = Color.Black.copy(alpha = 0.1f)
                    }
                    .drawWithContent {
                        drawContent()
                        // Add subtle border effect
                        drawRect(
                            color = Color.White.copy(alpha = 0.3f),
                            size = size.copy(height = 1.dp.toPx())
                        )
                    }
            )

            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                contentColor = Color.Transparent,
                containerColor = Color.Transparent,
            ) {
                val navigationItems = listOf<NavigationItem>(
                    NavigationItem(
                        tittle = stringResource(R.string.bottom_menu_home),
                        selectedIcon = R.drawable.ic_menu_home_selected,
                        unselectedIcon = R.drawable.ic_menu_home,
                        screen = Screen.Home
                    ),
                    NavigationItem(
                        tittle = stringResource(R.string.bottom_menu_contact),
                        selectedIcon = R.drawable.ic_contact_selected,
                        unselectedIcon = R.drawable.ic_contact,
                        screen = Screen.Contact
                    ),
                    NavigationItem(
                        tittle = stringResource(R.string.bottom_menu_MyLeave),
                        selectedIcon = R.drawable.ic_myleave,
                        unselectedIcon = R.drawable.ic_myleave,
                        screen = Screen.MyLeave
                    ),
                    NavigationItem(
                        tittle = stringResource(R.string.bottom_menu_profile),
                        selectedIcon = R.drawable.ic_profile_selected,
                        unselectedIcon = R.drawable.ic_profile,
                        screen = Screen.Profile
                    ),
                )
                navigationItems.map { item ->
                    val selected = currentRoute == item.screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = if (selected) item.selectedIcon else item.unselectedIcon),
                                contentDescription = item.tittle,
                            )
                        },
                        label = { Text(item.tittle, style = body1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue_500,
                            selectedTextColor = Blue_500,
                            indicatorColor = Color(0x00FFFFFF),
                            unselectedIconColor = Purple_500,
                            unselectedTextColor = Purple_500,
                        ),
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                if (item.screen.route == Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    restoreState = true
                                    launchSingleTop = true
                                } else {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }

                                    restoreState = true
                                    launchSingleTop = true
                                }
                            }
                        }

                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun PreviewBottomBar() {
    Infinite_TrackTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue)
        ) {
            BottomBarStaff(rememberNavController())
        }
    }
}