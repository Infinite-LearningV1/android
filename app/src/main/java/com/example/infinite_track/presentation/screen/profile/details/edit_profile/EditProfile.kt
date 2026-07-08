package com.example.infinite_track.presentation.screen.profile.details.edit_profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.components.status.InfiniteTrackInlineAlert
import com.example.infinite_track.presentation.components.status.StatusStateSpec
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.utils.UiState

@Composable
fun EditProfile(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfileState.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val nipNim by viewModel.nipNim.collectAsStateWithLifecycle()
    val updateProfileState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()

    val isSaving = updateProfileState is UiState.Loading

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = stringResource(R.string.edit_profile_title),
                navigationIcon = Icons.Default.KeyboardArrowLeft,
                navigationContentDescription = stringResource(R.string.cancel),
                onNavigationClick = onBackClick
            )
        },
        bottomBar = {
            EditProfileBottomActionBar(
                canSave = canSave,
                isSaving = isSaving,
                onSave = { viewModel.onSaveChangesClick() },
                onCancel = {
                    viewModel.onCancelClick()
                    onBackClick()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditProfileHeroCard(user = userProfile)
            EditProfileFormCard(
                user = userProfile,
                fullName = fullName,
                nipNim = nipNim,
                phone = phone,
                onFullNameChange = viewModel::onFullNameChange,
                onNipNimChange = viewModel::onNipNimChange,
                onPhoneChange = viewModel::onPhoneChange
            )
            EditProfileFeedback(
                updateProfileState = updateProfileState,
                onDismiss = { viewModel.resetUpdateState() }
            )
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun EditProfileHeroCard(user: UserModel?) {
    val unavailable = stringResource(R.string.account_hub_not_available)
    val fullName = user?.fullName?.ifBlank { unavailable } ?: unavailable
    val position = user?.positionName?.ifBlank { null } ?: stringResource(R.string.edit_profile_no_position)
    val role = user?.roleName?.ifBlank { unavailable } ?: unavailable

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = InfiniteColors.AccountHubHeroSurface),
        border = BorderStroke(1.dp, InfiniteColors.AccountHubHeroOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(InfiniteColors.AccountHubHeroGradient)
                .padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(InfiniteColors.AccountHubAvatarRingGradient)
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = user?.photoUrl?.takeIf { it.isNotBlank() },
                        contentDescription = null,
                        placeholder = painterResource(R.drawable.ic_profile),
                        error = painterResource(R.drawable.ic_profile),
                        fallback = painterResource(R.drawable.ic_profile),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, InfiniteColors.AccountHubOutline, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = InfiniteColors.AccountHubFloatingSurface,
                    border = BorderStroke(1.dp, InfiniteColors.AccountHubStrongOutline),
                    shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cameras),
                            contentDescription = stringResource(R.string.edit_profile_photo_action_disabled),
                            tint = InfiniteColors.AccountHubPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fullName,
                    style = headline2,
                    color = InfiniteColors.AccountHubTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = position,
                    style = headline3,
                    color = InfiniteColors.AccountHubMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                InfiniteStatusPill(
                    label = role,
                    variant = InfiniteStatusVariant.Recommended,
                    size = InfiniteSize.Small,
                    leadingIcon = null
                )
            }
        }
    }
}

@Composable
private fun EditProfileFormCard(
    user: UserModel?,
    fullName: String,
    nipNim: String,
    phone: String,
    onFullNameChange: (String) -> Unit,
    onNipNimChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    val noDivision = stringResource(R.string.edit_profile_no_division)
    val noPosition = stringResource(R.string.edit_profile_no_position)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = InfiniteColors.AccountHubHeroSurface),
        border = BorderStroke(1.dp, InfiniteColors.AccountHubHeroOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditProfileField(
                label = stringResource(R.string.edit_profile_full_name),
                value = fullName,
                onValueChange = onFullNameChange,
                editable = true,
                icon = R.drawable.ic_pencil
            )
            EditProfileField(
                label = stringResource(R.string.edit_profile_nip_nim),
                value = nipNim,
                onValueChange = onNipNimChange,
                editable = true,
                icon = R.drawable.ic_pencil
            )
            EditProfileField(
                label = stringResource(R.string.edit_profile_phone_number),
                value = phone,
                onValueChange = onPhoneChange,
                editable = true,
                icon = R.drawable.ic_pencil,
                keyboardType = KeyboardType.Phone
            )
            EditProfileField(
                label = stringResource(R.string.edit_profile_division),
                value = user?.divisionName?.ifBlank { null } ?: noDivision,
                onValueChange = {},
                editable = false
            )
            EditProfileField(
                label = stringResource(R.string.edit_profile_position),
                value = user?.positionName?.ifBlank { null } ?: noPosition,
                onValueChange = {},
                editable = false
            )
            EditProfileField(
                label = stringResource(R.string.edit_profile_email),
                value = user?.email.orEmpty(),
                onValueChange = {},
                editable = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    editable: Boolean,
    @DrawableRes icon: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        focused && editable -> InfiniteColors.AccountHubPrimary
        editable -> InfiniteColors.AccountHubOutline
        else -> InfiniteColors.AccountHubDividerColor
    }
    val fieldValue = value.ifBlank {
        if (editable) "" else stringResource(R.string.account_hub_not_available)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = headline4,
            color = InfiniteColors.AccountHubMutedText,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = fieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            enabled = editable,
            readOnly = !editable,
            singleLine = true,
            textStyle = body1.copy(color = InfiniteColors.AccountHubTitle),
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                if (editable && icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = label,
                        tint = InfiniteColors.AccountHubPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (!editable) {
                    ReadOnlyBadge()
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InfiniteColors.AccountHubIconSurface,
                unfocusedContainerColor = InfiniteColors.AccountHubIconSurface,
                disabledContainerColor = InfiniteColors.AccountHubIconSurface,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
                cursorColor = InfiniteColors.AccountHubPrimary,
                focusedTextColor = InfiniteColors.AccountHubTitle,
                unfocusedTextColor = InfiniteColors.AccountHubTitle,
                disabledTextColor = InfiniteColors.AccountHubBodyText,
                disabledTrailingIconColor = InfiniteColors.AccountHubMutedText
            )
        )
    }
}

@Composable
private fun ReadOnlyBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = InfiniteColors.AccountHubFloatingSurface,
        border = BorderStroke(1.dp, InfiniteColors.AccountHubDividerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = InfiniteColors.AccountHubMutedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.edit_profile_read_only),
                style = headline4,
                color = InfiniteColors.AccountHubMutedText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditProfileFeedback(
    updateProfileState: UiState<UserModel>,
    onDismiss: () -> Unit
) {
    when (updateProfileState) {
        is UiState.Success -> {
            InfiniteTrackInlineAlert(
                status = StatusStateSpec("success", "Success"),
                title = stringResource(R.string.edit_profile_success_title),
                message = stringResource(R.string.edit_profile_success_message),
                onDismiss = onDismiss
            )
        }

        is UiState.Error -> {
            InfiniteTrackInlineAlert(
                status = StatusStateSpec("error", "Error"),
                title = stringResource(R.string.edit_profile_error_title),
                message = updateProfileState.errorMessage.ifBlank {
                    stringResource(R.string.edit_profile_error_message)
                },
                onDismiss = onDismiss
            )
        }

        else -> Unit
    }
}

@Composable
private fun EditProfileBottomActionBar(
    canSave: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val primaryState = when {
        isSaving -> InfiniteButtonState.Loading
        canSave -> InfiniteButtonState.Enabled
        else -> InfiniteButtonState.Disabled
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = InfiniteColors.AccountHubHeroSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteButton(
                text = if (isSaving) {
                    stringResource(R.string.edit_profile_saving)
                } else {
                    stringResource(R.string.edit_profile_save_changes)
                },
                onClick = onSave,
                modifier = Modifier.weight(1f),
                variant = InfiniteButtonVariant.Primary,
                state = primaryState,
                fullWidth = true
            )
            InfiniteButton(
                text = stringResource(R.string.edit_profile_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                variant = InfiniteButtonVariant.Outlined,
                state = if (isSaving) InfiniteButtonState.Disabled else InfiniteButtonState.Enabled,
                fullWidth = true
            )
        }
    }
}
