package com.example.infinite_track.presentation.design.components.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

data class InfiniteBottomBarItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
)

@Composable
fun InfiniteBottomBar(
    items: List<InfiniteBottomBarItem>,
    selectedKey: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(containerColor = InfiniteColors.Surface.copy(alpha = 0.72f)) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item.key) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = InfiniteColors.Primary,
                    selectedTextColor = InfiniteColors.Primary,
                    indicatorColor = InfiniteColors.Primary.copy(alpha = 0.12f),
                    unselectedIconColor = InfiniteColors.Text.copy(alpha = 0.62f),
                    unselectedTextColor = InfiniteColors.Text.copy(alpha = 0.62f)
                )
            )
        }
    }
}
