package com.example.infinite_track.presentation.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Immutable
data class InfiniteTrackDropDownOption<Key : Any>(
    val key: Key,
    val label: String,
    val testTag: String? = null
)

@Composable
fun InfiniteTrackDropDown(
    selectedValue: String?,
    onSelected: (String) -> Unit,
    items: List<String>,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true
) {
    InfiniteTrackDropDownContent(
        displayedValue = selectedValue,
        onSelected = onSelected,
        options = items.map { item ->
            InfiniteTrackDropDownOption(key = item, label = item)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled
    )
}

@Composable
fun <Key : Any> InfiniteTrackDropDown(
    selectedKey: Key?,
    onSelected: (Key) -> Unit,
    options: List<InfiniteTrackDropDownOption<Key>>,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true
) {
    InfiniteTrackDropDownContent(
        displayedValue = options.firstOrNull { it.key == selectedKey }?.label,
        onSelected = onSelected,
        options = options,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled
    )
}

@Composable
private fun <Key : Any> InfiniteTrackDropDownContent(
    displayedValue: String?,
    onSelected: (Key) -> Unit,
    options: List<InfiniteTrackDropDownOption<Key>>,
    modifier: Modifier,
    label: String?,
    placeholder: String,
    enabled: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) isExpanded = false
    }
    val colors = infiniteSemanticColors(InfiniteSemantic.Neutral)
    val shape = MaterialTheme.shapes.medium

    Column {
        label?.let {
            Text(
                text = it,
                style = body2,
                color = colors.content,
                modifier = Modifier.padding(bottom = InfiniteSpacing.Default.xs)
            )
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(colors.container, shape)
                .border(1.dp, colors.border, shape)
                .clickable(enabled = enabled) { isExpanded = !isExpanded }
                .padding(
                    horizontal = InfiniteSpacing.Default.lg,
                    vertical = InfiniteSpacing.Default.sm
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayedValue ?: placeholder,
                    style = body1,
                    color = if (enabled) colors.content else colors.content.copy(alpha = 0.56f)
                )
                Icon(
                    painter = painterResource(if (isExpanded) R.drawable.arrow_up else R.drawable.arrow_down),
                    tint = if (enabled) colors.accent else colors.accent.copy(alpha = 0.56f),
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = isExpanded && enabled,
                onDismissRequest = { isExpanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label, style = body1) },
                        enabled = enabled,
                        modifier = option.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                        onClick = {
                            isExpanded = false
                            if (enabled) onSelected(option.key)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun InfiniteTrackDropDownPreview() {
    Infinite_TrackTheme {
        InfiniteTrackDropDown(
            selectedValue = null,
            onSelected = {},
            placeholder = "Pilih Head Program",
            items = listOf("Items", "Item")
        )
    }
}
