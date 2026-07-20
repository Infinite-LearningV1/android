package com.example.infinite_track.presentation.screen.profile

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.popUp.LanguagePopUp
import com.example.infinite_track.presentation.components.status.InfiniteTrackConfirmDialog
import com.example.infinite_track.presentation.components.status.InfiniteTrackStatusDialog
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.utils.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProfileScreen(
    navigateToEditProfile: () -> Unit,
    navigateToContactUs: () -> Unit,
    navigateToContacts: () -> Unit,
//    navigateToFAQ: () -> Unit,
    navigateToMyDocument: () -> Unit,
    navigateToPaySlip: () -> Unit,
    navigateToAbout: () -> Unit,
    navHostController: NavHostController,
    rootNavController: NavHostController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutLoadingDialog by remember { mutableStateOf(false) }

    // Collect all states from the ViewModel
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val language by profileViewModel.languageState.collectAsStateWithLifecycle()
    val showLanguageDialog by profileViewModel.showLanguageDialog.collectAsStateWithLifecycle()
    val logoutState by profileViewModel.logoutState.collectAsStateWithLifecycle()

    // Show language selection dialog using LanguagePopUp
    LanguagePopUp(
        showDialog = showLanguageDialog,
        selectedLanguage = language,
        onDismiss = { profileViewModel.onLanguageDialogDismiss() },
        onLanguageChange = { newLanguage ->
            // Update temporary selection in ViewModel state
            // We do not persist here; persistence happens on confirm
            profileViewModel.onUpdateLanguage(newLanguage)
        },
        onConfirm = { _ ->
            // Persist already done in onUpdateLanguage; ensure dialog closed
            // LanguagePopUp will call updateAppLanguage to apply runtime locale
            profileViewModel.onLanguageDialogDismiss()
        }
    )


    InfiniteTrackConfirmDialog(
        status = StatusStates.Warning,
        title = "Log out",
        message = "Are you sure you want to log out?",
        showDialog = showLogoutConfirmDialog,
        confirmText = "Log out",
        cancelText = "Cancel",
        isDestructive = true,
        onDismiss = { showLogoutConfirmDialog = false },
        onConfirm = {
            showLogoutConfirmDialog = false
            showLogoutLoadingDialog = true
            scope.launch {
                delay(2000)
                showLogoutLoadingDialog = false
                profileViewModel.onConfirmLogout()
            }
        }
    )

    ProfileLoadingDialog(showDialog = showLogoutLoadingDialog || logoutState is UiState.Loading)

    if (logoutState is UiState.Success) {
        InfiniteTrackStatusDialog(
            status = StatusStates.Success,
            title = "Log out berhasil",
            message = "Sampai jumpa kembali.",
            showDialog = true,
            onDismiss = {
                profileViewModel.resetLogoutState()
                rootNavController.navigate("auth_graph") {
                    popUpTo(rootNavController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onConfirm = {
                profileViewModel.resetLogoutState()
                rootNavController.navigate("auth_graph") {
                    popUpTo(rootNavController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }

    if (logoutState is UiState.Error) {
        InfiniteTrackStatusDialog(
            status = StatusStates.Warning,
            title = "Log out selesai dengan peringatan",
            message = (logoutState as UiState.Error).errorMessage,
            showDialog = true,
            onDismiss = {
                profileViewModel.resetLogoutState()
                rootNavController.navigate("auth_graph") {
                    popUpTo(rootNavController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onConfirm = {
                profileViewModel.resetLogoutState()
                rootNavController.navigate("auth_graph") {
                    popUpTo(rootNavController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }

    // MainScreen already applies scaffold/bottom-bar padding.
    // Match Home/History content insets exactly.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (profileState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = InfiniteColors.AccountHubPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            EmployeesAccessShortcut(onClick = navigateToContacts)
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = (profileState as UiState.Error).errorMessage,
                                style = headline3,
                                color = InfiniteColors.AccountHubDestructive
                            )
                            EmployeesAccessShortcut(onClick = navigateToContacts)
                        }
                    }
                }

                is UiState.Success -> {
                    val user = (profileState as UiState.Success<UserModel>).data
                    AccountHubContent(
                        user = user,
                        language = language,
                        onEditProfile = navigateToEditProfile,
                        onLanguageClick = { profileViewModel.onLanguageSettingsClicked() },
                        onMyDocument = navigateToMyDocument,
                        onPaySlip = navigateToPaySlip,
                        onContactSupport = navigateToContactUs,
                        onEmployees = navigateToContacts,
                        onAbout = navigateToAbout,
                        onLogout = { showLogoutConfirmDialog = true }
                    )
                }

                else -> { /* Idle state - do nothing */
                }
            }
        }
    }
}

@Composable
private fun AccountHubContent(
    user: UserModel,
    language: String,
    onEditProfile: () -> Unit,
    onLanguageClick: () -> Unit,
    onMyDocument: () -> Unit,
    onPaySlip: () -> Unit,
    onContactSupport: () -> Unit,
    onEmployees: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val unavailable = stringResource(R.string.account_hub_not_available)
    val role = user.roleName.ifBlank { unavailable }
    val position = user.positionName.orEmpty().ifBlank { unavailable }
    val division = user.divisionName.orEmpty().ifBlank {
        user.programName.orEmpty().ifBlank { unavailable }
    }
    val contact = user.phone.orEmpty().ifBlank { unavailable }
    val shouldShowPaySlip = remember(user.roleName) { user.roleName.shouldShowPayrollAccess() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IdentityHeroCard(
            user = user,
            role = role,
            position = position,
            division = division
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountSummaryCard(
                label = stringResource(R.string.account_hub_role),
                value = role,
                icon = R.drawable.ic_profile,
                modifier = Modifier.weight(1f)
            )
            AccountSummaryCard(
                label = stringResource(R.string.account_hub_division),
                value = division,
                icon = R.drawable.ic_division,
                modifier = Modifier.weight(1f)
            )
            AccountSummaryCard(
                label = stringResource(R.string.account_hub_contact),
                value = contact,
                icon = R.drawable.ic_phone,
                modifier = Modifier.weight(1f)
            )
        }

        AccountHubSection(title = stringResource(R.string.account_hub_account_identity)) {
            AccountHubMenuRow(
                title = stringResource(R.string.edit_profile),
                description = stringResource(R.string.account_hub_edit_profile_description),
                icon = R.drawable.ic_pencil,
                onClick = onEditProfile
            )
            AccountHubDivider()
            AccountHubMenuRow(
                title = stringResource(R.string.language),
                description = currentLanguageLabel(language),
                icon = R.drawable.ic_global,
                onClick = onLanguageClick
            )
        }

        AccountHubSection(title = stringResource(R.string.account_hub_company_access)) {
            AccountHubMenuRow(
                title = stringResource(R.string.my_document),
                description = stringResource(R.string.account_hub_my_document_description),
                icon = R.drawable.ic_mydocument,
                onClick = onMyDocument
            )
            if (shouldShowPaySlip) {
                AccountHubDivider()
                AccountHubMenuRow(
                    title = stringResource(R.string.pay_slip),
                    description = stringResource(R.string.account_hub_pay_slip_description),
                    icon = R.drawable.ic_payslip,
                    onClick = onPaySlip
                )
            }
            AccountHubDivider()
            AccountHubMenuRow(
                title = stringResource(R.string.employees),
                description = stringResource(R.string.account_hub_contact_support_description),
                icon = R.drawable.ic_contactus,
                onClick = onEmployees
            )
        }

        AccountHubSection(title = stringResource(R.string.account_hub_help_information)) {
            AccountHubMenuRow(
                title = stringResource(R.string.account_hub_contact_support),
                description = stringResource(R.string.account_hub_contact_support_description),
                icon = R.drawable.ic_contactus,
                onClick = onContactSupport
            )
            AccountHubDivider()
            AccountHubMenuRow(
                title = stringResource(R.string.account_hub_about_title),
                description = stringResource(R.string.account_hub_about_description),
                icon = R.drawable.ic_community,
                onClick = onAbout
            )
        }

        AccountHubSection(title = stringResource(R.string.account_hub_security)) {
            AccountHubMenuRow(
                title = stringResource(R.string.logOut),
                description = stringResource(R.string.account_hub_logout_description),
                icon = R.drawable.ic_logout,
                onClick = onLogout,
                destructive = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderColor: Color = InfiniteColors.AttendanceReportGlassBorder,
    shadowColor: Color = InfiniteColors.Primary.copy(alpha = 0.10f),
    backgroundBrush: Brush = Brush.linearGradient(
        colors = listOf(
            InfiniteColors.AttendanceReportGlassSurface,
            InfiniteColors.AttendanceReportGlassSurface
        )
    ),
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = InfiniteColors.Accent.copy(alpha = 0.08f),
            clip = false
        ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = InfiniteColors.Transparent),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(contentPadding)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun IdentityHeroCard(
    user: UserModel,
    role: String,
    position: String,
    division: String
) {
    val unavailable = stringResource(R.string.account_hub_not_available)
    val fullName = user.fullName.ifBlank { unavailable }

    ProfileGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl?.takeIf { it.isNotBlank() },
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_profile),
                error = painterResource(R.drawable.ic_profile),
                fallback = painterResource(R.drawable.ic_profile),
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(1.dp, InfiniteColors.AttendanceReportGlassBorder, CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = fullName,
                    style = headline4,
                    color = Purple_500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = position,
                    style = body1,
                    color = Purple_300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    style = body2,
                    color = Blue_500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = division,
                    style = body2,
                    color = Purple_300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.account_hub_identifier, user.nipNim),
                    style = body2,
                    color = Purple_300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier
) {
    ProfileGlassCard(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Blue_500,
                modifier = Modifier.size(16.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = label,
                    style = body3,
                    color = Purple_300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = body2,
                    color = Purple_500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AccountHubSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ProfileGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = body1,
                color = Purple_500
            )
            content()
        }
    }
}

@Composable
private fun AccountHubMenuRow(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    val accent = if (destructive) InfiniteColors.AccountHubDestructive else Blue_500
    val rowBackground = if (destructive) InfiniteColors.AccountHubDestructiveContainer else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = InfiniteColors.AttendanceReportGlassSurface,
            border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
            shadowElevation = 0.dp
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = body1,
                color = if (destructive) InfiniteColors.AccountHubDestructive else InfiniteColors.AccountHubTitle,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = body2,
                color = InfiniteColors.AccountHubMutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(R.drawable.right_arrow),
            contentDescription = null,
            tint = if (destructive) InfiniteColors.AccountHubDestructiveArrow else InfiniteColors.AccountHubTitle,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AccountHubDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = InfiniteColors.AttendanceReportGlassBorder
    )
}

@Composable
private fun SoftPill(
    text: String,
    @DrawableRes icon: Int
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = InfiniteColors.AccountHubPillContainer,
        border = BorderStroke(1.dp, InfiniteColors.AccountHubPillBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = InfiniteColors.AccountHubPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = body2,
                color = InfiniteColors.AccountHubSectionAccent,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IdentityInfoRow(
    @DrawableRes icon: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = InfiniteColors.AccountHubIconText,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = body2,
            color = InfiniteColors.AccountHubBodyText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmployeesAccessShortcut(onClick: () -> Unit) {
    AccountHubMenuRow(
        title = stringResource(R.string.employees),
        description = stringResource(R.string.account_hub_contact_support_description),
        onClick = onClick,
        icon = R.drawable.ic_contactus
    )
}

@Composable
fun ProfileBar(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Int,
) {
    AccountHubMenuRow(
        title = label,
        description = "",
        onClick = onClick,
        icon = icon,
        modifier = modifier
    )
}

@Composable
private fun currentLanguageLabel(language: String): String {
    return if (language == "en") {
        stringResource(R.string.language_english)
    } else {
        stringResource(R.string.language_indonesia)
    }
}

private fun String.shouldShowPayrollAccess(): Boolean {
    val normalized = lowercase()
    return !normalized.contains("intern") &&
        !normalized.contains("internship") &&
        !normalized.contains("magang")
}

@Composable
private fun ProfileLoadingDialog(showDialog: Boolean) {
    if (!showDialog) return

    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = InfiniteColors.AccountHubSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingAnimation()
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Please wait",
                    style = headline4
                )
            }
        }
    }
}
