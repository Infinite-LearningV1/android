package com.example.infinite_track.presentation.design.components.input

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    size: InfiniteSize = InfiniteSize.Medium
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        shape = InfiniteRadius.shape(size),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null) } },
        trailingIcon = trailingIcon?.let { { Icon(imageVector = it, contentDescription = null) } },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = InfiniteColors.Primary,
            unfocusedBorderColor = InfiniteColors.Neutral.copy(alpha = 0.22f),
            cursorColor = InfiniteColors.Primary,
            focusedLabelColor = InfiniteColors.Primary
        )
    )
}
