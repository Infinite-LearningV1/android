package com.example.infinite_track.presentation.screen.profile

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cn.pedant.SweetAlert.SweetAlertDialog
import coil.compose.AsyncImage
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.components.popUp.LanguagePopUp
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.utils.DialogHelper
import com.example.infinite_track.utils.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProfileScreen(
    navigateToEditProfile: () -> Unit,
    navigateToContactUs: () -> Unit,
//    navigateToFAQ: () -> Unit,
    navigateToMyDocument: () -> Unit,
    navigateToPaySlip: () -> Unit,
    navHostController: NavHostController,
    rootNavController: NavHostController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<SweetAlertDialog?>(null) }

    // Collect all states from the ViewModel
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val language by profileViewModel.languageState.collectAsStateWithLifecycle()
    val showLanguageDialog by profileViewModel.showLanguageDialog.collectAsStateWithLifecycle()

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
        onConfirm = { confirmedLanguage ->
            // Persist already done in onUpdateLanguage; ensure dialog closed
            // LanguagePopUp will call updateAppLanguage to apply runtime locale
            profileViewModel.onLanguageDialogDismiss()
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (profileState) {
                is UiState.Loading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Purple_500,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                is UiState.Error -> {
                    // Show error message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profileState as UiState.Error).errorMessage,
                            style = headline3,
                            color = Color.Red
                        )
                    }
                }

                is UiState.Success -> {
                    // Show profile content
                    val user = (profileState as UiState.Success<UserModel>).data
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.width(280.dp)
                            ) {
                                Text(
                                    text = user.fullName ?: "N/A",
                                    style = headline2,
                                    color = Purple_500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = user.positionName ?: "N/A",
                                    style = headline4,
                                    color = Purple_300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AsyncImage(
                                model = user.photoUrl
                                    ?: "https://w7.pngwing.com/pngs/177/551/png-transparent-user-interface-design-computer-icons-default-stephen-salazar-graphy-user-interface-design-computer-wallpaper-sphere-thumbnail.png",
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .height(60.dp)
                                    .width(60.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.account_information),
                                style = headline3,
                                fontWeight = FontWeight.Medium
                            )
                            ProfileBar(
                                label = stringResource(R.string.edit_profile),
                                onClick = navigateToEditProfile,
                                icon = R.drawable.ic_pencil
                            )
                            ProfileBar(
                                label = stringResource(R.string.pay_slip),
                                onClick = navigateToPaySlip,
                                icon = R.drawable.ic_payslip
                            )
                            ProfileBar(
                                label = stringResource(R.string.my_document),
                                onClick = navigateToMyDocument,
                                icon = R.drawable.ic_mydocument
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Settings Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.setting),
                                style = headline3,
                                fontWeight = FontWeight.Medium
                            )

                            // Language Selector Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable { profileViewModel.onLanguageSettingsClicked() },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_global),
                                    contentDescription = stringResource(R.string.language),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.language),
                                    style = headline4,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (language == "en") "English" else "Indonesia",
                                    style = headline4,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    painter = painterResource(R.drawable.right_arrow),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Other Settings Options
                            ProfileBar(
                                label = stringResource(R.string.contact_us),
                                onClick = navigateToContactUs,
                                icon = R.drawable.ic_contactus
                            )
//                            ProfileBar(
//                                label = stringResource(R.string.faq),
//                                onClick = navigateToFAQ,
//                                icon = R.drawable.ic_faq
//                            )
                            ProfileBar(
                                label = stringResource(R.string.about_us),
                                onClick = {},
                                icon = R.drawable.ic_community
                            )
                            ProfileBar(
                                label = stringResource(R.string.logOut),
                                icon = R.drawable.ic_logout,
                                onClick = {
                                    dialog = DialogHelper.showDialogWarning(
                                        context = context,
                                        title = "Log out",
                                        textContent = "Are you sure you want to log out?",
                                        onDismis = { dialog?.dismissWithAnimation() },
                                        onConfirm = {
                                            dialog?.dismissWithAnimation()
                                            dialog = DialogHelper.showDialogLoading(
                                                context = context,
                                                textContent = "Please wait"
                                            )
                                            scope.launch {
                                                delay(2000)
                                                dialog?.dismissWithAnimation()
                                                profileViewModel.onConfirmLogout()
                                                rootNavController.navigate("auth_graph") {
                                                    popUpTo(rootNavController.graph.startDestinationId) {
                                                        inclusive = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "Log out success",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                else -> { /* Idle state - do nothing */
                }
            }
        }
    }
}

@Composable
fun ProfileBar(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = headline4,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.right_arrow),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}
