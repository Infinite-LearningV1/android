package com.example.infinite_track.presentation.screen.wfa.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.data.InfiniteFilterChips
import com.example.infinite_track.presentation.design.components.data.InfiniteFilterOption
import com.example.infinite_track.presentation.screen.wfa.WfaRequestStatusFilter

@Composable
fun WfaRequestFilterChips(
    selectedFilter: WfaRequestStatusFilter,
    onFilterSelected: (WfaRequestStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember {
        WfaRequestStatusFilter.values().map { filter ->
            InfiniteFilterOption(
                key = filter.name,
                label = filter.label
            )
        }
    }

    InfiniteFilterChips(
        options = options,
        selectedKey = selectedFilter.name,
        onSelected = { key ->
            WfaRequestStatusFilter.values()
                .firstOrNull { it.name == key }
                ?.let(onFilterSelected)
        },
        modifier = modifier
    )
}
