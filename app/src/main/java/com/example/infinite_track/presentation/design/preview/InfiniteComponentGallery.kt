package com.example.infinite_track.presentation.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteAction
import com.example.infinite_track.presentation.design.components.data.InfiniteBottomActionBar
import com.example.infinite_track.presentation.design.components.data.InfiniteFilterChips
import com.example.infinite_track.presentation.design.components.data.InfiniteFilterOption
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteMetricCard
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.data.InfiniteTimelineRow
import com.example.infinite_track.presentation.design.components.data.TimelineConnectorPosition
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
fun InfiniteComponentGalleryPreview() {
    Infinite_TrackTheme {
        InfiniteComponentGallery()
    }
}

@Composable
fun InfiniteComponentGallery() {
    val selectedFilter = remember { mutableStateOf("week") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InfiniteColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfiniteSectionHeader(
            title = "Infinite Track Material 3",
            subtitle = "Reusable component foundation",
            leadingIcon = InfiniteIcons.More,
            trailingText = "Preview"
        )

        InfiniteCard(variant = InfiniteSurfaceVariant.Glass, semantic = InfiniteSemantic.Primary) {
            InfiniteInfoRow(label = "Design direction", value = "Premium academic SaaS", icon = InfiniteIcons.Info)
            InfiniteInfoRow(label = "Theme", value = "Light only", icon = InfiniteIcons.Success)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfiniteButton(text = "Primary", onClick = {}, modifier = Modifier.weight(1f), fullWidth = true)
            InfiniteButton(text = "Tonal", onClick = {}, modifier = Modifier.weight(1f), variant = InfiniteButtonVariant.Tonal, fullWidth = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfiniteButton(text = "Loading", onClick = {}, modifier = Modifier.weight(1f), state = InfiniteButtonState.Loading, fullWidth = true)
            InfiniteButton(text = "Danger", onClick = {}, modifier = Modifier.weight(1f), variant = InfiniteButtonVariant.Danger, fullWidth = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfiniteMetricCard(title = "Attendance", value = "96%", subtitle = "On track", icon = InfiniteIcons.Calendar, modifier = Modifier.weight(1f))
            InfiniteMetricCard(title = "Late", value = "2", subtitle = "This month", icon = InfiniteIcons.Time, semantic = InfiniteSemantic.Warning, modifier = Modifier.weight(1f))
        }

        InfiniteFilterChips(
            options = listOf(
                InfiniteFilterOption("week", "Week", InfiniteIcons.Calendar),
                InfiniteFilterOption("month", "Month", InfiniteIcons.Filter),
                InfiniteFilterOption("pdf", "PDF Ready", InfiniteIcons.Download)
            ),
            selectedKey = selectedFilter.value,
            onSelected = { selectedFilter.value = it }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfiniteStatusPill("Active", InfiniteStatusVariant.Active)
            InfiniteStatusPill("Pending", InfiniteStatusVariant.Pending)
            InfiniteStatusPill("Rejected", InfiniteStatusVariant.Rejected)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfiniteStatusPill("Inside", InfiniteStatusVariant.Inside)
            InfiniteStatusPill("PDF Ready", InfiniteStatusVariant.PdfReady)
            InfiniteStatusPill("Review", InfiniteStatusVariant.NeedsReview)
        }

        InfiniteTimelineRow(
            dateLabel = "Mon, 07 Jul",
            modeLabel = "Work From Office",
            timeRange = "08:00 - 17:00",
            statusLabel = "On Time",
            statusVariant = InfiniteStatusVariant.OnTime,
            connectorPosition = TimelineConnectorPosition.First
        )
        InfiniteTimelineRow(
            dateLabel = "Tue, 08 Jul",
            modeLabel = "Work From Anywhere",
            timeRange = "09:10 - 17:00",
            statusLabel = "Late",
            statusVariant = InfiniteStatusVariant.Late,
            connectorPosition = TimelineConnectorPosition.Last
        )

        InfiniteInlineAlert(title = "Success", message = "Your request has been saved.", semantic = InfiniteSemantic.Success)
        InfiniteInlineAlert(title = "Warning", message = "Please review this state before continuing.", semantic = InfiniteSemantic.Warning)
        InfiniteErrorState(title = "Unable to load", message = "Try again after checking your connection.")
        InfiniteLoadingState(message = "Preparing component preview...")
        InfiniteEmptyState(title = "No data yet", message = "Reusable empty state for report and WFA lists.")

        InfiniteBottomActionBar(
            primaryAction = InfiniteAction("Save", InfiniteIcons.Success) {},
            secondaryAction = InfiniteAction("Share", InfiniteIcons.Share, InfiniteButtonVariant.Outlined) {},
            tertiaryAction = InfiniteAction("PDF", InfiniteIcons.Download, InfiniteButtonVariant.Tonal) {}
        )
    }
}
