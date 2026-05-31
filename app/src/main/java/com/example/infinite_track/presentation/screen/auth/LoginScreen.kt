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
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.infinite_track.R
import com.example.infinite_track.presentation.components.button.InfiniteTrackButton
import com.example.infinite_track.presentation.components.textfield.ThriveInInputText
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.utils.DialogHelper
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

    val context = LocalContext.current

    var dialog by remember { mutableStateOf<SweetAlertDialog?>(null) }


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
                dialog?.dismissWithAnimation()
                dialog = DialogHelper.showDialogLoading(
                    context = context,
                    textContent = "Please wait"
                )
            }

            is UiState.Success -> {
                dialog?.dismissWithAnimation()
                dialog = DialogHelper.showDialogSuccess(
                    context = context,
                    title = "Complete your Profile",
                    textContent = "Please head to Setting and complete your profile",
                    imageRes = R.drawable.img_login,
                    onConfirm = {
                        // First navigate to home
                        navigateToHome()
                        // Then reset the state to prevent dialog from showing again if user comes back
                        loginViewModel.resetState()
                    }
                )
            }

            is UiState.Error -> {
                dialog?.dismissWithAnimation()
                dialog = DialogHelper.showDialogError(
                    context = context,
                    title = "Failed",
                    textContent = (loginState as UiState.Error).errorMessage
                )
            }
        }
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