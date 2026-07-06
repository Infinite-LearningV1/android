//package com.example.infinite_track.presentation.screen.leave_request.leave
//
//import android.net.Uri
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.systemBarsPadding
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.zIndex
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import coil.compose.rememberImagePainter
//import com.example.infinite_track.R
//import com.example.infinite_track.data.soucre.NetworkResponse
//import com.example.infinite_track.data.soucre.network.response.LeaveRequestResponse
//import com.example.infinite_track.presentation.components.address.AddressInputField
//import com.example.infinite_track.presentation.components.button.InfiniteTracButtonBack
//import com.example.infinite_track.presentation.components.calendar.Date
//import com.example.infinite_track.presentation.components.cameras.CameraCapture
//import com.example.infinite_track.presentation.components.leave_balance_view.LeaveBalanceView
//import com.example.infinite_track.presentation.components.loading.LoadingAnimation
//import com.example.infinite_track.presentation.components.textfield.InfiniteTrackDropDown
//import com.example.infinite_track.presentation.components.textfield.PhoneNumberInputText
//import com.example.infinite_track.presentation.components.textfield.ThriveInInputText
//import com.example.infinite_track.presentation.core.body1
//import com.example.infinite_track.presentation.core.headline3
//import com.example.infinite_track.presentation.core.headline4
//import com.example.infinite_track.presentation.theme.Purple_300
//import com.example.infinite_track.utils.toMultipartBodyPart
//import kotlinx.coroutines.launch
//
//@Composable
//fun LeaveRequestScreen(
//    leaveRequestViewModel: LeaveRequestViewModel = hiltViewModel(),
//    onBackClick: () -> Unit,
//    user: UserModel
//) {
//    var desc by remember { mutableStateOf("") }
//    var startDate by remember { mutableStateOf("") }
//    var endDate by remember { mutableStateOf("") }
//    val leaveRequestState by leaveRequestViewModel.leaveRequestState.collectAsState()
//    val coroutineScope = rememberCoroutineScope()
//
//    var selectedLeaveType by remember { mutableStateOf("") }
//
//    // Camera states
//    var showCamera by remember { mutableStateOf(false) }
//    var imageUri by remember { mutableStateOf<Uri?>(null) }
//    var isImageUploaded by remember { mutableStateOf(false) }
//
//    val context = LocalContext.current
//
//    // Permission launcher for Camera
//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission(),
//        onResult = { isGranted ->
//            if (isGranted) {
//                showCamera = true
//            } else {
//                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
//    )
//
//    val leaveBalance = (user.annualBalance!! - user.annualUsed!!)
//
//    Scaffold(
//        modifier = Modifier
//            .fillMaxSize(),
//        containerColor = Color.Transparent,
//        topBar = {
//            InfiniteTracButtonBack(
//                title = "Leave Application",
//                navigationBack = onBackClick,
//                modifier = Modifier.padding(top = 12.dp)
//            )
//        }
//    ) { innerPadding ->
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp)
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding)
//                    .verticalScroll(rememberScrollState()),
//                verticalArrangement = Arrangement.Center,
//            ) {
//                ThriveInInputText(
//                    value = user.userName,
//                    placeholder = "Full Name",
//                    onChange = {},
//                    leadingIcon = painterResource(id = R.drawable.ic_name),
//                    isObsecure = false
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                ThriveInInputText(
//                    value = user.headprogramname ?: "",
//                    placeholder = "Head Program",
//                    onChange = {},
//                    leadingIcon = painterResource(id = R.drawable.ic_headprogram),
//                    isObsecure = false
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                ThriveInInputText(
//                    value = user.division ?: "",
//                    placeholder = "Division",
//                    onChange = {},
//                    leadingIcon = painterResource(id = R.drawable.ic_division),
//                    isObsecure = false
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Date Selection for Start Date and End Date
//                Date(
//                    label = "Start Date",
//                    initialDate = startDate,
//                    onDateSelected = { date -> startDate = date },
//                    leadingIcon = painterResource(id = R.drawable.ic_calendar)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Date(
//                    label = "End Date",
//                    initialDate = endDate,
//                    onDateSelected = { date -> endDate = date },
//                    leadingIcon = painterResource(id = R.drawable.ic_calendar)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Leave Balance
//                LeaveBalanceView(leaveBalance = leaveBalance)
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Dropdown for Leave Type
//                InfiniteTrackDropDown(
//                    placeholder = if (selectedLeaveType.isEmpty()) "Leave Type" else selectedLeaveType,
//                    leadingIcon = painterResource(id = R.drawable.ic_leavetype),
//                    items = listOf(
//                        "Sick Leave",
//                        "Maternity Leave",
//                        "Marriage Leave",
//                        "Annual Leave"
//                    ),
//                    onSelected = { selectedItem ->
//                        selectedLeaveType = selectedItem
//                    }
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Description Input Field
//                ThriveInInputText(
//                    value = desc,
//                    placeholder = "Description (Optional)",
//                    onChange = { desc = it },
//                    leadingIcon = painterResource(id = R.drawable.ic_description),
//                    isObsecure = false
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Phone Number Input Field
//                PhoneNumberInputText(
//                    value = user.phone_number ?: "",
//                    placeholder = "Phone Number",
//                    onChange = {},
//                    leadingIcon = painterResource(id = R.drawable.ic_phone)
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Address Input Field
//                AddressInputField(
//                    address = user.address ?: "",
//                    onAddressChange = {}
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Upload Image Section
//                Text(
//                    text = "Upload Image",
//                    style = headline4,
//                    color = Color.Gray,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                if (!isImageUploaded) {
//                    Button(
//                        onClick = {
//                            launcher.launch(android.Manifest.permission.CAMERA)
//                        },
//                        colors = ButtonDefaults.buttonColors(Color.Transparent),
//                        elevation = null,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .border(
//                                BorderStroke(2.dp, Color.White),
//                                RoundedCornerShape(10.dp)
//                            )
//                            .background(Color(0x80F3ECFF), RoundedCornerShape(10.dp))
//                            .size(175.dp),
//                        shape = RoundedCornerShape(10.dp)
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 15.dp),
//                            verticalArrangement = Arrangement.Center,
//                            horizontalAlignment = Alignment.CenterHorizontally
//
//                        ) {
//                            Image(
//                                painter = painterResource(id = R.drawable.cameras2),
//                                contentDescription = "Camera Icon",
//                                modifier = Modifier.size(50.dp),
//                                contentScale = ContentScale.Fit,
//                            )
//                            Spacer(modifier = Modifier.padding(7.dp))
//
//                            Text(
//                                text = stringResource(R.string.a_photo_of_you),
//                                style = headline3,
//                                color = Color.Black
//                            )
//
//                            Spacer(modifier = Modifier.padding(5.dp))
//
//                            Text(
//                                text = stringResource(R.string.photo_face_instruction),
//                                style = body1,
//                                color = Purple_300
//                            )
//                        }
//                    }
//                }
//                imageUri?.let { uri ->
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Image(
//                        painter = rememberImagePainter(data = uri),
//                        contentDescription = "Captured Image",
//                        modifier = Modifier.size(200.dp)
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Send Button
//                Button(
//                    onClick = {
//                        coroutineScope.launch {
//                            leaveRequestViewModel.handleLeave(
//                                name = user.userName,
//                                headProgramName = user.headprogramname ?: "",
//                                division = user.division ?: "",
//                                startDate = startDate,
//                                endDate = endDate,
//                                leaveType = selectedLeaveType,
//                                desc = desc,
//                                phone = user.phone_number ?: "",
//                                address = user.address ?: "",
//                                uploadImage = imageUri?.toMultipartBodyPart(
//                                    "upload_image",
//                                    context
//                                )
//                            )
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(48.dp),
//                    shape = RoundedCornerShape(12.dp),
//                    enabled = startDate.isNotEmpty() && endDate.isNotEmpty() && selectedLeaveType.isNotEmpty() && isImageUploaded
//                ) {
//                    Text(
//                        text = "Send",
//                        style = MaterialTheme.typography.bodyLarge,
//                        color = Color.White
//                    )
//                }
//            }
//        }
//
//        val lifecycleOwner = LocalLifecycleOwner.current
//        val currentScreen = lifecycleOwner.lifecycle.currentState
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            if (currentScreen == Lifecycle.State.RESUMED) {
//                when (leaveRequestState) {
//                    is NetworkResponse.Loading ->
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .background(Color.Black.copy(alpha = 0.5f))
//                                .zIndex(1f),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            LoadingAnimation()
//                        }
//
//                    is NetworkResponse.Success -> {
//                        val message =
//                            (leaveRequestState as NetworkResponse.Success<LeaveRequestResponse>).data.message
//                        Legacy success dialog removed during status dialog migration.
//                    }
//
//                    is NetworkResponse.Error -> {
//                        val errorMessage = (leaveRequestState as NetworkResponse.Error).message
//                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
//                    }
//
//                    else -> {}
//                }
//            }
//        }
//
//        if (showCamera) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.8f))
//                    .zIndex(1f)
//                    .systemBarsPadding()
//            ) {
//                CameraCapture(
//                    modifier = Modifier.fillMaxSize(),
//                    onImageCaptured = { uri ->
//                        if (uri != null) {
//                            imageUri = uri
//                            isImageUploaded = true
//                        }
//                        showCamera = false
//                    },
//                    onError = { /* Error Handling */ }
//                )
//            }
//        }
//    }
//}