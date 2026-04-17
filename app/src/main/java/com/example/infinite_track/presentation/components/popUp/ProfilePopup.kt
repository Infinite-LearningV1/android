package com.example.infinite_track.presentation.components.popUp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.theme.Blue_200
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Violet_50
import com.example.infinite_track.presentation.theme.Violet_500
import com.example.infinite_track.utils.updateAppLanguage

@Composable
fun LanguagePopUp(
	showDialog: Boolean,
	selectedLanguage: String,
	onDismiss: () -> Unit,
	onLanguageChange: (String) -> Unit,
	onConfirm: (String) -> Unit
) {
	val context = LocalContext.current

	if (showDialog) {
		AlertDialog(
			modifier = Modifier
				.width(325.dp)
				.height(300.dp),
			onDismissRequest = { onDismiss() },
			title = { Text(text = stringResource(R.string.select_language)) },
			text = {
				Column {
					Row(
						modifier = Modifier.height(36.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						RadioButton(
							selected = selectedLanguage == "en",
							onClick = { onLanguageChange("en") }
						)
						Text(text = stringResource(R.string.language_english))
					}
					Row(
						modifier = Modifier.height(36.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						RadioButton(
							selected = selectedLanguage == "id",
							onClick = { onLanguageChange("id") }
						)
						Text(text = stringResource(R.string.language_indonesia))
					}
					Spacer(modifier = Modifier.height(16.dp))

					ButtonPop(
						modifier = Modifier,
						onClick = {
							// Apply language immediately and ask host to persist via ViewModel
							updateAppLanguage(context, selectedLanguage)
							onConfirm(selectedLanguage)
						},
						label = stringResource(R.string.save),
						isOutline = false
					)
					Spacer(modifier = Modifier.height(8.dp))
					ButtonPop(
						modifier = Modifier,
						onClick = onDismiss,
						label = stringResource(R.string.cancel),
						isOutline = true
					)
				}
			},
			confirmButton = {},
			dismissButton = {},
		)
	}
}


@Composable
fun ButtonPop(
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	label: String,
	enabled: Boolean = true,
	isOutline: Boolean,
) {
	Button(
		onClick = {
			onClick()
		},
		modifier = modifier
			.fillMaxWidth()
			.height(39.dp),
		shape = RoundedCornerShape(12.dp),
		colors = if (isOutline) {
			ButtonDefaults.outlinedButtonColors(
				containerColor = Color.Transparent,
				contentColor = Color.White,
				disabledContentColor = Blue_200,
				disabledContainerColor = Violet_500
			)
		} else {
			ButtonDefaults.buttonColors(
				containerColor = Blue_500,
				contentColor = Color.White,
				disabledContentColor = Blue_200,
				disabledContainerColor = Violet_500
			)
		},
		border = if (isOutline) BorderStroke(1.dp, Blue_500) else null,
		enabled = enabled,
		elevation = if (isOutline) ButtonDefaults.elevatedButtonElevation(0.dp) else ButtonDefaults.buttonElevation(
			4.dp
		),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = label,
				style = body1,
				color = if (!isOutline) Violet_50 else Blue_500
			)
		}
	}
}


//@Preview
//@Composable
//fun LanguagePopUpPreview() {
//    LanguagePopUp(showDialog = true, onDismiss = {},)
//}
