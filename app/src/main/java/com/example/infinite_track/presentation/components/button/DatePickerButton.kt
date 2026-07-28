package com.example.infinite_track.presentation.components.button

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.White
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shared controlled date selector. Date ownership stays with the caller so the
 * selected value survives navigation and always matches the submitted draft.
 */
@Composable
fun DatePickerButton(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select date",
    minimumDate: LocalDate? = null,
    calendarContentDescription: String = "Open calendar"
) {
    val context = LocalContext.current
    val initialDate = selectedDate ?: minimumDate ?: LocalDate.now()
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val datePickerDialog = DatePickerDialog(
        context,
        R.style.CustomDatePickerDialogTheme,
        { _, year, month, day -> onDateSelected(LocalDate.of(year, month + 1, day)) },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).apply {
        minimumDate?.let {
            datePicker.minDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    OutlinedButton(
        onClick = datePickerDialog::show,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, White),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = White.copy(alpha = 0.18f)),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = selectedDate?.format(formatter) ?: placeholder,
                style = body1,
                color = Blue_500
            )
            Image(
                painter = painterResource(R.drawable.ic_calender),
                contentDescription = calendarContentDescription,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
