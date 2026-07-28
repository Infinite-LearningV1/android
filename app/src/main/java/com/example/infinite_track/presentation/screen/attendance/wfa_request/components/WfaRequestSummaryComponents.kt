package com.example.infinite_track.presentation.screen.attendance.wfa_request.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaEmployeeSummary

@Composable
fun WfaEmployeeCard(employee: WfaEmployeeSummary, modifier: Modifier = Modifier) {
    InfiniteCard(modifier = modifier.fillMaxWidth().testTag("wfaEmployeeCard")) {
        SummaryHeader(stringResource(R.string.wfa_request_employee), Icons.Outlined.Person)
        SummaryValue(employee.fullName, employee.division)
    }
}

@Composable
fun WfaLocationCard(
    location: WfaCandidateLocation,
    radiusMeters: Int,
    modifier: Modifier = Modifier
) {
    InfiniteCard(modifier = modifier.fillMaxWidth().testTag("wfaLocationCard")) {
        SummaryHeader(stringResource(R.string.wfa_request_location), Icons.Outlined.LocationOn)
        SummaryValue(location.displayName, location.formattedAddress)
        Text(
            text = stringResource(R.string.wfa_request_radius_policy, radiusMeters),
            style = MaterialTheme.typography.labelLarge,
            color = InfiniteColors.Primary,
            modifier = Modifier.testTag("wfaRadiusReadOnly")
        )
    }
}

@Composable
fun WfaReviewRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InfiniteColors.AttendanceReportMutedText,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = InfiniteColors.Text,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Composable
fun WfaPolicyNotice(radiusMeters: Int, modifier: Modifier = Modifier) {
    InfiniteCard(
        modifier = modifier.fillMaxWidth(),
        semantic = InfiniteSemantic.Info,
        showShadow = false
    ) {
        Text(stringResource(R.string.wfa_request_policy_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.wfa_request_policy_body, radiusMeters),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SummaryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = InfiniteSpacing.Default.sm)
    ) {
        Icon(icon, contentDescription = null, tint = InfiniteColors.Primary)
        Text(title, style = MaterialTheme.typography.titleMedium, color = InfiniteColors.Text)
    }
}

@Composable
private fun SummaryValue(primary: String, secondary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)) {
        Text(primary, style = MaterialTheme.typography.bodyLarge, color = InfiniteColors.Text)
        Text(secondary, style = MaterialTheme.typography.bodySmall, color = InfiniteColors.AttendanceReportMutedText)
    }
}
