package com.example.infinite_track.presentation.components.calendar

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_400
import com.example.infinite_track.presentation.theme.Violet_50
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun DatePickerComponent(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    minimumDate: LocalDate? = null,
    enabled: Boolean = true,
    textColor: Color = Color.Gray,
    calendarContentDescription: String? = null,
    dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
) {
    val context = LocalContext.current
    val initialDate = (selectedDate ?: minimumDate ?: LocalDate.now()).let { date ->
        if (minimumDate != null && date < minimumDate) minimumDate else date
    }
    val datePickerDialog = DatePickerDialog(
        context,
        R.style.CustomDatePickerDialogTheme,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).apply {
        minimumDate?.let { minimum ->
            datePicker.minDate = Calendar.getInstance().apply {
                clear()
                set(minimum.year, minimum.monthValue - 1, minimum.dayOfMonth)
            }.timeInMillis
        }
    }

    DatePickerField(
        text = selectedDate?.format(dateFormatter) ?: label,
        enabled = enabled,
        textColor = textColor,
        calendarContentDescription = calendarContentDescription,
        onClick = datePickerDialog::show,
        modifier = modifier
    )
}

@Composable
fun DatePickerComponent(
    modifier: Modifier = Modifier,
    label: String,
    initialDate: String,
    onDateSelected: (String) -> Unit,
    enabled: Boolean = true,
    textColor: Color = Color.Gray
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var selectedDate by remember(initialDate) {
        mutableStateOf(
            runCatching { LocalDate.parse(initialDate, formatter) }
                .getOrElse { LocalDate.now() }
        )
    }

    DatePickerComponent(
        selectedDate = selectedDate,
        onDateSelected = { date ->
            selectedDate = date
            onDateSelected(date.format(formatter))
        },
        modifier = modifier,
        label = label,
        enabled = enabled,
        textColor = textColor,
        dateFormatter = formatter
    )
}

@Composable
private fun DatePickerField(
    text: String,
    enabled: Boolean,
    textColor: Color,
    calendarContentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Violet_50.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .clickable(enabled = enabled) {
                if (enabled) onClick()
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = body1.copy(color = textColor)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                tint = Purple_300,
                contentDescription = calendarContentDescription
            )
        }
    }
}

@Composable
fun DatePickerComponentWithLabel(
    modifier: Modifier = Modifier,
    label: String,
    initialDate: String,
    onDateSelected: (String) -> Unit,
    enabled: Boolean = true,
    textColor: Color = Color.Gray
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            style = body1.copy(color = Purple_400),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        DatePickerComponent(
            modifier = Modifier.fillMaxWidth(),
            label = label,
            initialDate = initialDate,
            onDateSelected = onDateSelected,
            enabled = enabled,
            textColor = textColor // Pass the textColor to DatePickerComponent
        )
    }
}


@Composable
fun ContractDatePicker() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DatePickerComponentWithLabel(
            modifier = Modifier.weight(1f),
            label = "Start Contract",
            initialDate = "09/09/2024",
            onDateSelected = { startDate -> },
            enabled = true
        )

        Spacer(modifier = Modifier.width(16.dp))

        DatePickerComponentWithLabel(
            modifier = Modifier.weight(1f),
            label = "End Contract",
            initialDate = "09/03/2024",
            onDateSelected = { endDate -> },
            enabled = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContractDatePickerPreview() {
    ContractDatePicker()
}
