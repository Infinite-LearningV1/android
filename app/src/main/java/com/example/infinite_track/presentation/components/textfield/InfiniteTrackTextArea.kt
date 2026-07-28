package com.example.infinite_track.presentation.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.input.InfiniteSupportingText
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteTrackTextArea(
    modifier: Modifier = Modifier,
    label: String = "Description",
    value: String = "",
    placeholder: String = "",
    enabled: Boolean = true,
    onValueChange: (String) -> Unit = {},
    maxLength: Int? = null,
    showCharacterCount: Boolean = false
) {
    val colors = infiniteSemanticColors(InfiniteSemantic.Neutral)
    val shape = MaterialTheme.shapes.medium

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = body1,
            color = colors.content,
            modifier = Modifier.padding(bottom = InfiniteSpacing.Default.xs)
        )
        TextField(
            value = value,
            onValueChange = { newValue ->
                val acceptedValue = if (maxLength == null) newValue else newValue.take(maxLength)
                onValueChange(acceptedValue)
            },
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        style = body2,
                        color = colors.content.copy(alpha = 0.62f)
                    )
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 120.dp)
                .clip(shape)
                .background(colors.container, shape),
            shape = shape,
            enabled = enabled,
            singleLine = false,
            textStyle = body2,
            colors = TextFieldDefaults.textFieldColors(
                containerColor = colors.container,
                focusedIndicatorColor = colors.container,
                unfocusedIndicatorColor = colors.container,
                disabledIndicatorColor = colors.container,
                focusedTextColor = colors.content,
                unfocusedTextColor = colors.content,
                disabledTextColor = colors.content.copy(alpha = 0.56f)
            )
        )
        if (showCharacterCount && maxLength != null) {
            InfiniteSupportingText(
                text = "${value.length}/$maxLength",
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = InfiniteSpacing.Default.xs)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfiniteTrackTextAreaPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(InfiniteSpacing.Default.lg),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
        ) {
            InfiniteTrackTextArea(
                label = "Description",
                value = "",
                placeholder = "Enter your description here...",
                onValueChange = {}
            )
            InfiniteTrackTextArea(
                label = "Notes",
                value = "Sample text content",
                maxLength = 250,
                showCharacterCount = true
            )
        }
    }
}
