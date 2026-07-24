package com.example.infinite_track.presentation.screen.contact

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.cards.ContactCard
import com.example.infinite_track.presentation.components.empty.EmptyListAnimation
import com.example.infinite_track.presentation.components.empty.ErrorAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.search.InfiniteTrackSearchBar
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Status_Rejected

@Composable
fun ContactScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var searchValue by remember { mutableStateOf("") }

    LaunchedEffect(searchValue) {
        viewModel.onSearchQueryChanged(searchValue)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = "Employees (${filteredContacts.size})",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            InfiniteTrackSearchBar(
                modifier = Modifier.fillMaxWidth(),
                value = searchValue,
                placeholder = "Search Contacts",
                onChange = { newValue -> searchValue = newValue },
                onClear = { searchValue = "" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> LoadingAnimation()

                    errorMessage != null && errorMessage.toString().isNotEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ErrorAnimation(modifier = Modifier.size(150.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = body2,
                                color = Status_Rejected
                            )
                        }
                    }

                    filteredContacts.isEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyListAnimation(modifier = Modifier.size(150.dp))
                            Text(
                                text = "No contacts found matching \"$searchValue\"",
                                style = body2,
                                color = Purple_300
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredContacts) { contact ->
                                ContactCard(
                                    name = contact.fullName,
                                    position = contact.division,
                                    cardImage = contact.photoUrl,
                                    phone = contact.phoneNumber,
                                    message = contact.email,
                                    whatsapp = contact.whatsappNumber,
                                    onClickCard = {},
                                    messageWA = "Hello ${contact.fullName}",
                                    context = context
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
