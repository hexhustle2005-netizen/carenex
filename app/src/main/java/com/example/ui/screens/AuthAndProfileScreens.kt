package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.remote.onGoogleSignInClicked
import com.example.ui.components.SectionCard
import com.example.ui.theme.CareTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MedicalBlueDark
import com.example.ui.theme.MedicalBlueLight

data class PendingPatientSignUp(
    val fullName: String,
    val mobile: String,
    val email: String
)

private enum class AuthPortalMode {
    SIGN_UP,
    SIGN_IN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (PendingPatientSignUp?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var authMode by remember { mutableStateOf(AuthPortalMode.SIGN_UP) }
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MedicalBlue, MedicalBlueDark, Color(0xFF062C63))
                    )
                )
                .padding(innerPadding)
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = "CareNex",
                        tint = MedicalBlue,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "CareNex Healthcare",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Unified Patient Care, Hospital Operations & Telehealth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(20.dp))

                Image(
                    painter = painterResource(R.drawable.img_hero_care),
                    contentDescription = "CareNex medical care platform",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = authMode == AuthPortalMode.SIGN_UP,
                                onClick = {
                                    authMode = AuthPortalMode.SIGN_UP
                                    errorMsg = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("signup_tab")
                            ) {
                                Text("Patient Sign Up")
                            }
                            SegmentedButton(
                                selected = authMode == AuthPortalMode.SIGN_IN,
                                onClick = {
                                    authMode = AuthPortalMode.SIGN_IN
                                    errorMsg = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("signin_tab")
                            ) {
                                Text("Sign In")
                            }
                        }

                        if (authMode == AuthPortalMode.SIGN_UP) {
                            Text(
                                text = "Create Patient Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter your name, mobile number, and email, then verify with Google to activate your CareNex patient account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    errorMsg = null
                                },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input")
                            )
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = {
                                    mobile = it
                                    errorMsg = null
                                },
                                label = { Text("Phone Number *") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_phone_input")
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMsg = null
                                },
                                label = { Text("Email Address *") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MedicalBlueLight.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = MedicalBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Google Sign-In secures your session without storing a separate password. For standalone Email/Password login, enable the Email/Password provider in Firebase Console.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MedicalBlueDark
                                    )
                                }
                            }

                            if (errorMsg != null) {
                                Text(
                                    text = errorMsg!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmergencyRed
                                )
                            }

                            Button(
                                onClick = {
                                    val trimmedName = fullName.trim()
                                    val trimmedPhone = mobile.trim()
                                    val digitCount = trimmedPhone.count { it.isDigit() }
                                    val trimmedEmail = email.trim()

                                    when {
                                        trimmedName.length < 2 -> {
                                            errorMsg = "Please enter your full name (at least 2 characters)."
                                        }
                                        digitCount < 7 -> {
                                            errorMsg = "Please enter a valid mobile number (at least 7 digits)."
                                        }
                                        !trimmedEmail.contains("@") || !trimmedEmail.contains(".") -> {
                                            errorMsg = "Please enter a valid email address."
                                        }
                                        else -> {
                                            val pending = PendingPatientSignUp(
                                                fullName = trimmedName,
                                                mobile = trimmedPhone,
                                                email = trimmedEmail
                                            )
                                            isLoading = true
                                            errorMsg = null
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    isLoading = false
                                                    onAuthSuccess(pending)
                                                },
                                                onAuthError = { msg ->
                                                    isLoading = false
                                                    errorMsg = msg
                                                },
                                                scope = scope,
                                                onAuthCancelled = {
                                                    isLoading = false
                                                }
                                            )
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("create_patient_account_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        "Verify with Google & Create Account",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Sign in to your CareNex Portal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CareTeal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Real-time OPD slots, hospital beds & ICU status", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CareTeal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Emergency ambulance, online pharmacy & video consults", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = CareTeal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Role-isolated records for Patient, Doctor & Admin", style = MaterialTheme.typography.bodySmall)
                            }

                            if (errorMsg != null) {
                                Text(
                                    text = errorMsg!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmergencyRed
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMsg = null
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess(null)
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMsg = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupDialog(
    initialEmail: String,
    initialDisplayName: String,
    initialMobile: String = "",
    existingProfile: UserProfile?,
    busy: Boolean,
    isDismissible: Boolean,
    onSave: (
        fullName: String,
        mobile: String,
        email: String,
        age: Int,
        gender: String,
        address: String,
        bloodGroup: String,
        emergencyContact: String,
        photoUri: String
    ) -> Unit,
    onSignOut: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf(existingProfile?.fullName ?: initialDisplayName) }
    var mobile by remember { mutableStateOf(existingProfile?.mobile?.ifBlank { initialMobile } ?: initialMobile) }
    var email by remember { mutableStateOf(existingProfile?.email ?: initialEmail) }
    var ageText by remember { mutableStateOf(existingProfile?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var gender by remember { mutableStateOf(existingProfile?.gender ?: "Female") }
    var address by remember { mutableStateOf(existingProfile?.address ?: "") }
    var bloodGroup by remember { mutableStateOf(existingProfile?.bloodGroup ?: "O+") }
    var emergencyContact by remember { mutableStateOf(existingProfile?.emergencyContact ?: "") }
    var photoUri by remember { mutableStateOf(existingProfile?.photoUri ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var bloodMenuExpanded by remember { mutableStateOf(false) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    val genders = listOf("Female", "Male", "Other")

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = { if (isDismissible) onDismiss() },
        title = {
            Text(
                if (existingProfile == null) "Complete Patient Registration" else "Your CareNex Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MedicalBlueLight)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Pick photo", tint = MedicalBlue)
                        }
                    }
                    Column {
                        Text("Profile Photo", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text("Choose via Photo Picker")
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_mobile_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = bloodMenuExpanded,
                        onExpandedChange = { bloodMenuExpanded = !bloodMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = bloodMenuExpanded,
                            onDismissRequest = { bloodMenuExpanded = false }
                        ) {
                            bloodGroups.forEach { bg ->
                                DropdownMenuItem(
                                    text = { Text(bg) },
                                    onClick = {
                                        bloodGroup = bg
                                        bloodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genders.forEach { g ->
                        OutlinedButton(
                            onClick = { gender = g },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (gender == g) MedicalBlueLight else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(g, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact (Name & Phone)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(validationError!!, color = EmergencyRed, style = MaterialTheme.typography.bodySmall)
                }

                if (onSignOut != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    val age = ageText.toIntOrNull() ?: 0
                    when {
                        fullName.trim().length < 2 -> validationError = "Please enter your full name."
                        mobile.trim().length < 6 -> validationError = "Please enter a valid mobile number."
                        !email.contains("@") -> validationError = "Please enter a valid email."
                        else -> {
                            validationError = null
                            onSave(
                                fullName,
                                mobile,
                                email,
                                age,
                                gender,
                                address,
                                bloodGroup,
                                emergencyContact,
                                photoUri
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(if (busy) "Saving..." else "Save Profile")
            }
        },
        dismissButton = {
            if (isDismissible) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
