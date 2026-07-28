package com.example.infinite_track.presentation.screen.attendance.wfa_request.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.presentation.design.components.input.InfiniteTextField
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WfaDateField(date: LocalDate?, error: WfaRequestFieldError?, onDateChanged: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)) {
        InfiniteTextField(
            value = date?.toString().orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().testTag("wfaScheduleDate").clickable(role = Role.Button) { showPicker = true },
            label = stringResource(R.string.wfa_request_date),
            placeholder = stringResource(R.string.wfa_request_date_placeholder),
            readOnly = true
        )
        FieldError(error)
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChanged(pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    })
                    showPicker = false
                }) { Text(stringResource(R.string.wfa_request_date_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.wfa_request_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
fun WfaReasonSelector(
    reasons: List<WfaRequestReason>,
    selectedReasonId: Long?,
    error: WfaRequestFieldError?,
    onReasonSelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)) {
        Text(stringResource(R.string.wfa_request_reason), style = MaterialTheme.typography.titleSmall, color = InfiniteColors.Text)
        reasons.forEach { reason ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("wfaReason-${reason.id}").clickable { onReasonSelected(reason.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = reason.id == selectedReasonId, onClick = { onReasonSelected(reason.id) })
                Text(reason.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        FieldError(error)
    }
}

@Composable
fun WfaTextInput(
    value: String,
    label: String,
    placeholder: String,
    tag: String,
    error: WfaRequestFieldError?,
    singleLine: Boolean,
    onValueChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)) {
        InfiniteTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth().testTag(tag),
            label = label,
            placeholder = placeholder,
            singleLine = singleLine
        )
        FieldError(error)
    }
}

@Composable
private fun FieldError(error: WfaRequestFieldError?) {
    if (error != null) {
        Text(
            text = when (error) {
                WfaRequestFieldError.REQUIRED -> stringResource(R.string.wfa_request_error_required)
                WfaRequestFieldError.REASON_UNAVAILABLE -> stringResource(R.string.wfa_request_error_reason_unavailable)
                WfaRequestFieldError.OTHER_REASON_REQUIRED -> stringResource(R.string.wfa_request_error_other_required)
                WfaRequestFieldError.TOO_LONG -> stringResource(R.string.wfa_request_error_too_long)
                WfaRequestFieldError.INVALID_LOCATION -> stringResource(R.string.wfa_request_error_location)
            },
            style = MaterialTheme.typography.bodySmall,
            color = InfiniteColors.Error
        )
    }
}
