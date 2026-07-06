package com.example.infinite_track.presentation.screen.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.R
import com.example.infinite_track.presentation.components.button.InfiniteTrackButton
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.status.InfiniteTrackStatusDialog
import com.example.infinite_track.presentation.components.status.StatusStateSpec
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.components.textfield.ThriveInInputText
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.utils.UiState
import kotlinx.coroutines.launch


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToHome: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }

    var password by rememberSaveable { mutableStateOf("") }

    var showLoadingDialog by remember { mutableStateOf(false) }
    var statusDialog by remember { mutableStateOf<LoginStatusDialog?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect login state using collectAsStateWithLifecycle for lifecycle awareness
    val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
    val reauthBannerMessage by loginViewModel.reauthBannerMessage.collectAsStateWithLifecycle()

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {

            is UiState.Idle -> {
                // No action needed for idle state
            }

            is UiState.Loading -> {
                statusDialog = null
                showLoadingDialog = true
            }

            is UiState.Success -> {
                showLoadingDialog = false
                statusDialog = LoginStatusDialog(
                    status = StatusStates.Success,
                    title = "Complete your Profile",
                    message = "Please head to Setting and complete your profile",
                    imageRes = R.drawable.img_login,
                    onConfirm = {
                        navigateToHome()
                        loginViewModel.resetState()
                        statusDialog = null
                    }
                )
            }

            is UiState.Error -> {
                showLoadingDialog = false
                statusDialog = LoginStatusDialog(
                    status = StatusStates.Error,
                    title = "Failed",
                    message = (loginState as UiState.Error).errorMessage,
                    onConfirm = { statusDialog = null }
                )
            }
        }
    }


    LoginLoadingDialog(showDialog = showLoadingDialog)

    statusDialog?.let { dialog ->
        InfiniteTrackStatusDialog(
            status = dialog.status,
            title = dialog.title,
            message = dialog.message,
            showDialog = true,
            imageRes = dialog.imageRes,
            onDismiss = { statusDialog = null },
            onConfirm = dialog.onConfirm
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.Transparent,
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    reauthBannerMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color.Red,
                            style = body1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        Button(onClick = { loginViewModel.dismissReauthBanner() }) {
                            Text(text = "Tutup")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Image(
                        painter = painterResource(id = R.drawable.image_login),
                        contentDescription = "Login Image",

                        )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.hello_again),
                                fontSize = 28.sp,
                                style = body1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.sign_in_prompt),
                                fontSize = 16.sp,
                                style = body1
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            ThriveInInputText(
                                value = email,
                                onChange = { email = it },
                                leadingIcon = painterResource(id = R.drawable.ic_message),
                                placeholder = stringResource(R.string.email_placeholder)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ThriveInInputText(
                                value = password,
                                onChange = { password = it },
                                leadingIcon = painterResource(id = R.drawable.ic_password),
                                placeholder = stringResource(R.string.password_placeholder),
                                isObsecure = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.forgot_password),
                            fontSize = 12.sp,
                            style = body1
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    InfiniteTrackButton(
                        onClick = {
                            if (email == "" || password == "") {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Data is invalid!",
                                        withDismissAction = true,
                                    )
                                }
                            } else {
                                // Call the login method with email and password directly
                                loginViewModel.login(email, password)
                            }
                        },
                        label = stringResource(id = R.string.log_in),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

private data class LoginStatusDialog(
    val status: StatusStateSpec,
    val title: String,
    val message: String,
    val imageRes: Int? = null,
    val onConfirm: () -> Unit
)

@Composable
private fun LoginLoadingDialog(showDialog: Boolean) {
    if (!showDialog) return

    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    style = body1
                )
            }
        }
    }
}
