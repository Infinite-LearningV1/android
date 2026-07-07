package com.example.infinite_track.presentation.design.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

enum class InfiniteButtonVariant {
    Primary,
    Secondary,
    Tonal,
    Outlined,
    Ghost,
    Glass,
    Danger,
    Success,
    Warning
}

enum class InfiniteButtonState {
    Enabled,
    Disabled,
    Loading,
    Selected
}

@Composable
fun InfiniteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: InfiniteButtonVariant = InfiniteButtonVariant.Primary,
    size: InfiniteSize = InfiniteSize.Medium,
    state: InfiniteButtonState = InfiniteButtonState.Enabled,
    fullWidth: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val enabled = state != InfiniteButtonState.Disabled && state != InfiniteButtonState.Loading
    val colors = infiniteButtonColors(variant, selected = state == InfiniteButtonState.Selected)
    val height = when (size) {
        InfiniteSize.Small -> 40.dp
        InfiniteSize.Medium -> 48.dp
        InfiniteSize.Large -> 56.dp
    }
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth().height(height) else modifier.height(height)
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == InfiniteButtonState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.content
                )
            } else {
                leadingIcon?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp)) }
            }
            Text(text = text)
            trailingIcon?.let { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }

    if (variant == InfiniteButtonVariant.Outlined || variant == InfiniteButtonVariant.Ghost) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = InfiniteRadius.shape(size),
            border = if (variant == InfiniteButtonVariant.Ghost) null else BorderStroke(1.dp, colors.container),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.container,
                disabledContentColor = colors.container.copy(alpha = 0.42f)
            )
        ) { content() }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = InfiniteRadius.shape(size),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.container,
                contentColor = colors.content,
                disabledContainerColor = colors.container.copy(alpha = 0.42f),
                disabledContentColor = colors.content.copy(alpha = 0.56f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (variant == InfiniteButtonVariant.Glass) 0.dp else 3.dp)
        ) { content() }
    }
}

private data class InfiniteButtonColorSet(
    val container: Color,
    val content: Color
)

private fun infiniteButtonColors(
    variant: InfiniteButtonVariant,
    selected: Boolean
): InfiniteButtonColorSet {
    return when (variant) {
        InfiniteButtonVariant.Primary -> InfiniteButtonColorSet(InfiniteColors.Primary, InfiniteColors.Surface)
        InfiniteButtonVariant.Secondary -> InfiniteButtonColorSet(InfiniteColors.Secondary, InfiniteColors.Text)
        InfiniteButtonVariant.Tonal -> InfiniteButtonColorSet(InfiniteColors.Primary.copy(alpha = if (selected) 0.24f else 0.14f), InfiniteColors.Primary)
        InfiniteButtonVariant.Outlined -> InfiniteButtonColorSet(InfiniteColors.Primary, InfiniteColors.Primary)
        InfiniteButtonVariant.Ghost -> InfiniteButtonColorSet(InfiniteColors.Primary, InfiniteColors.Primary)
        InfiniteButtonVariant.Glass -> InfiniteButtonColorSet(InfiniteColors.Surface.copy(alpha = 0.62f), InfiniteColors.Primary)
        InfiniteButtonVariant.Danger -> InfiniteButtonColorSet(InfiniteColors.SoftAlert, InfiniteColors.Surface)
        InfiniteButtonVariant.Success -> InfiniteButtonColorSet(InfiniteColors.Success, InfiniteColors.Text)
        InfiniteButtonVariant.Warning -> InfiniteButtonColorSet(InfiniteColors.Warning, InfiniteColors.Text)
    }
}
