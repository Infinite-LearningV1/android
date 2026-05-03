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
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicy
import com.example.infinite_track.presentation.navigation.model.NavigationItem
import com.example.infinite_track.presentation.navigation.model.Screen
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_500
import java.util.Locale

private val bottomBarShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
private val bottomBarHeight = 90.dp
private val bottomBarBlurColors = listOf(
    Color.White.copy(alpha = 0.25f),
    Color.White.copy(alpha = 0.15f)
)
private val bottomBarOverlayColors = listOf(
    Color.White.copy(alpha = 0.3f),
    Color.White.copy(alpha = 0.1f)
)

internal fun formatBottomBarLabel(label: String): String {
    return label
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { firstChar ->
                if (firstChar.isLowerCase()) {
                    firstChar.titlecase(Locale.getDefault())
                } else {
                    firstChar.toString()
                }
            }
        }
}

@Composable
fun MainBottomBar(navController: NavController, userRole: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val navigationItems = MainShellNavigationPolicy.bottomBarItemsForRole(userRole)

    if (navigationItems.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarHeight)
                .clip(bottomBarShape)
                .background(brush = Brush.verticalGradient(colors = bottomBarBlurColors))
                .blur(radius = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarHeight)
                .clip(bottomBarShape)
                .background(brush = Brush.verticalGradient(colors = bottomBarOverlayColors))
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    ambientShadowColor = Color.Black.copy(alpha = 0.1f)
                    spotShadowColor = Color.Black.copy(alpha = 0.1f)
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        color = Color.White.copy(alpha = 0.3f),
                        size = size.copy(height = 1.dp.toPx())
                    )
                }
        )

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarHeight)
                .padding(top = 8.dp)
                .clip(bottomBarShape),
            contentColor = Color.Transparent,
            containerColor = Color.Transparent,
        ) {
            navigationItems.forEach { item ->
                val selected = MainShellNavigationPolicy.isSelected(item, currentRoute)
                val label = formatBottomBarLabel(stringResource(id = item.labelRes))

                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = if (selected) item.selectedIcon else item.unselectedIcon),
                            contentDescription = label,
                        )
                    },
                    label = { Text(label, style = body1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Blue_500,
                        selectedTextColor = Blue_500,
                        indicatorColor = Color(0x00FFFFFF),
                        unselectedIconColor = Purple_500,
                        unselectedTextColor = Purple_500,
                    ),
                    selected = selected,
                    onClick = {
                        navigateToBottomBarDestination(
                            navController = navController,
                            item = item
                        )
                    }
                )
            }
        }
    }
}

private fun navigateToBottomBarDestination(navController: NavController, item: NavigationItem) {
    navController.navigate(item.screen.route) {
        if (item.screen.route == Screen.Home.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        } else {
            popUpTo(Screen.Home.route) {
                saveState = true
            }
        }
        restoreState = true
        launchSingleTop = true
    }
}

@Composable
@Preview
fun PreviewMainBottomBar() {
    Infinite_TrackTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue)
        ) {
            MainBottomBar(
                navController = rememberNavController(),
                userRole = "Employee"
            )
        }
    }
}
