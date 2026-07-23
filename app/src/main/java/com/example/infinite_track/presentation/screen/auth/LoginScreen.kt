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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.R
import com.example.infinite_track.presentation.components.status.InfiniteTrackInlineAlert
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.components.textfield.ThriveInInputText
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToHome: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }

    var password by rememberSaveable { mutableStateOf("") }

    var validationFailure by remember { mutableStateOf<String?>(null) }

    val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val reauthBannerMessage by loginViewModel.reauthBannerMessage.collectAsStateWithLifecycle()
    val currentNavigateToHome = rememberUpdatedState(navigateToHome)
    val isLoading = loginState == LoginUiState.Loading
    val failureMessage = validationFailure ?: (loginState as? LoginUiState.Failure)?.message

    LaunchedEffect(loginViewModel) {
        loginViewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateHome -> currentNavigateToHome.value()
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
                        InfiniteTrackInlineAlert(
                            status = StatusStates.Error,
                            title = "Sesi perlu login ulang",
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            onDismiss = { loginViewModel.dismissReauthBanner() }
                        )
                    }

                    failureMessage?.let { message ->
                        InfiniteTrackInlineAlert(
                            status = StatusStates.Error,
                            title = "Failed",
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            onDismiss = {
                                if (validationFailure == message) {
                                    validationFailure = null
                                }
                                loginViewModel.dismissFailure(message)
                            }
                        )
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
                                onChange = { if (!isLoading) email = it },
                                leadingIcon = painterResource(id = R.drawable.ic_message),
                                placeholder = stringResource(R.string.email_placeholder)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ThriveInInputText(
                                value = password,
                                onChange = { if (!isLoading) password = it },
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

                    InfiniteButton(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                validationFailure = "Data is invalid!"
                            } else {
                                validationFailure = null
                                loginViewModel.login(email, password)
                            }
                        },
                        text = stringResource(id = R.string.log_in),
                        state = if (isLoading) {
                            InfiniteButtonState.Loading
                        } else {
                            InfiniteButtonState.Enabled
                        },
                        fullWidth = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}
