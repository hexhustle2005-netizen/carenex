package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ConsultationStatus
import com.example.data.model.Doctor
import com.example.data.model.Hospital
import com.example.data.model.Invoice
import com.example.data.model.UserRole
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOutUser
import com.example.data.repository.CareNexClinicalRepository
import com.example.data.repository.CareNexDirectoryRepository
import com.example.ui.components.CareNexTopBar
import com.example.ui.components.NotificationsSheetDialog
import com.example.ui.components.PortalSwitcherDialog
import com.example.ui.components.UpiPaymentSheetDialog
import com.example.ui.screens.AdminPortalRootScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DoctorPortalRootScreen
import com.example.ui.screens.EmergencyAndAmbulanceScreen
import com.example.ui.screens.HospitalAndOpdScreen
import com.example.ui.screens.MedicalRecordsScreen
import com.example.ui.screens.OnlineConsultationScreen
import com.example.ui.screens.PatientDest
import com.example.ui.screens.PatientHomeScreen
import com.example.ui.screens.PendingPatientSignUp
import com.example.ui.screens.PharmacyScreen
import com.example.ui.screens.ProfileSetupDialog
import com.example.ui.theme.CareNexTheme
import com.example.ui.viewmodel.CareNexViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.LocalNotifier
import com.example.util.LocationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareNexTheme {
                CareNexRootApp()
            }
        }
    }
}

@Composable
private fun CareNexRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val authUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }
    var pendingPatientSignUp by remember { mutableStateOf<PendingPatientSignUp?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = scope
            )
        }
    }

    val currentUser = authUser
    if (!autoSignInChecked && currentUser == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { pending ->
                pendingPatientSignUp = pending
                autoSignInChecked = true
            }
        )
    } else {
        key(currentUser.uid) {
            AuthenticatedCareNexApp(
                currentUser = currentUser,
                pendingPatientSignUp = pendingPatientSignUp,
                onConsumePendingSignUp = { pendingPatientSignUp = null },
                onSignOut = {
                    pendingPatientSignUp = null
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedCareNexApp(
    currentUser: FirebaseUser,
    pendingPatientSignUp: PendingPatientSignUp?,
    onConsumePendingSignUp: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val uid = currentUser.uid
    val email = currentUser.email ?: ""
    val displayName = currentUser.displayName ?: "CareNex User"

    val vm: CareNexViewModel = viewModel(
        key = uid,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CareNexViewModel(
                    directoryRepo = CareNexDirectoryRepository(appContext),
                    clinicalRepo = CareNexClinicalRepository(appContext),
                    currentUserId = uid,
                    currentUserEmail = email
                ) as T
            }
        }
    )

    val activePortal by vm.activePortal.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val userCoords by vm.userCoords.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedSlotDate.collectAsStateWithLifecycle()
    val selectedSlot by vm.selectedDoctorSlot.collectAsStateWithLifecycle()

    val userProfileState by vm.userProfile.collectAsStateWithLifecycle()
    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()
    val hospitalsState by vm.hospitals.collectAsStateWithLifecycle()
    val doctorsState by vm.doctors.collectAsStateWithLifecycle()
    val bedsState by vm.beds.collectAsStateWithLifecycle()
    val medicinesState by vm.medicines.collectAsStateWithLifecycle()
    val ambulancesState by vm.ambulances.collectAsStateWithLifecycle()
    val notificationsState by vm.notifications.collectAsStateWithLifecycle()

    val patientApptsState by vm.patientAppointments.collectAsStateWithLifecycle()
    val doctorApptsState by vm.doctorAppointments.collectAsStateWithLifecycle()
    val adminApptsState by vm.adminAppointments.collectAsStateWithLifecycle()
    val adminAdmissionsState by vm.adminAdmissions.collectAsStateWithLifecycle()
    val patientPrescriptionsState by vm.patientPrescriptions.collectAsStateWithLifecycle()
    val patientReportsState by vm.patientReports.collectAsStateWithLifecycle()
    val doctorAuthorizedReportsState by vm.doctorAuthorizedReports.collectAsStateWithLifecycle()
    val patientOrdersState by vm.patientPharmacyOrders.collectAsStateWithLifecycle()
    val adminOrdersState by vm.adminPharmacyOrders.collectAsStateWithLifecycle()
    val patientEmergencyState by vm.patientEmergencyRequests.collectAsStateWithLifecycle()
    val adminEmergencyState by vm.adminEmergencyRequests.collectAsStateWithLifecycle()
    val patientConsultationsState by vm.patientConsultations.collectAsStateWithLifecycle()
    val doctorConsultationsState by vm.doctorConsultations.collectAsStateWithLifecycle()
    val consultationMessages by vm.consultationMessages.collectAsStateWithLifecycle()
    val patientInvoicesState by vm.patientInvoices.collectAsStateWithLifecycle()
    val adminInvoicesState by vm.adminInvoices.collectAsStateWithLifecycle()
    val adminUsersState by vm.adminUsers.collectAsStateWithLifecycle()

    val hospitals = (hospitalsState as? UiState.Success)?.data ?: emptyList()
    val doctors = (doctorsState as? UiState.Success)?.data ?: emptyList()
    val beds = (bedsState as? UiState.Success)?.data ?: emptyList()
    val medicines = (medicinesState as? UiState.Success)?.data ?: emptyList()
    val ambulances = (ambulancesState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val userProfile = (userProfileState as? UiState.Success)?.data

    val patientAppointments = (patientApptsState as? UiState.Success)?.data ?: emptyList()
    val doctorAppointments = (doctorApptsState as? UiState.Success)?.data ?: emptyList()
    val adminAppointments = (adminApptsState as? UiState.Success)?.data ?: emptyList()
    val adminAdmissions = (adminAdmissionsState as? UiState.Success)?.data ?: emptyList()
    val patientPrescriptions = (patientPrescriptionsState as? UiState.Success)?.data ?: emptyList()
    val patientReports = (patientReportsState as? UiState.Success)?.data ?: emptyList()
    val doctorReports = (doctorAuthorizedReportsState as? UiState.Success)?.data ?: emptyList()
    val patientOrders = (patientOrdersState as? UiState.Success)?.data ?: emptyList()
    val adminOrders = (adminOrdersState as? UiState.Success)?.data ?: emptyList()
    val patientEmergency = (patientEmergencyState as? UiState.Success)?.data ?: emptyList()
    val adminEmergency = (adminEmergencyState as? UiState.Success)?.data ?: emptyList()
    val patientConsultations = (patientConsultationsState as? UiState.Success)?.data ?: emptyList()
    val doctorConsultations = (doctorConsultationsState as? UiState.Success)?.data ?: emptyList()
    val patientInvoices = (patientInvoicesState as? UiState.Success)?.data ?: emptyList()
    val adminInvoices = (adminInvoicesState as? UiState.Success)?.data ?: emptyList()
    val adminUsers = (adminUsersState as? UiState.Success)?.data ?: emptyList()

    var currentPatientDest by remember { mutableStateOf(PatientDest.HOME) }
    var selectedHospitalForOpd by remember { mutableStateOf<Hospital?>(null) }
    var selectedDoctorForOpd by remember { mutableStateOf<Doctor?>(null) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showPortalDialog by remember { mutableStateOf(false) }
    var invoiceToPay by remember { mutableStateOf<Invoice?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val seenNotificationIds = remember { mutableSetOf<String>() }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        LocalNotifier.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        vm.updateUserCoords(LocationHelper.getLastKnownCoords(context))
    }

    LaunchedEffect(notifications) {
        notifications.filter { !it.read && it.id.isNotBlank() }.forEach { notif ->
            if (seenNotificationIds.add(notif.id)) {
                LocalNotifier.showNotification(
                    context = context,
                    id = notif.id.hashCode(),
                    title = notif.title,
                    body = notif.message
                )
            }
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearStatusMessage()
        }
    }

    var autoCreatedFromSignUp by remember(uid) { mutableStateOf(false) }
    LaunchedEffect(userProfileState, pendingPatientSignUp) {
        val pending = pendingPatientSignUp ?: return@LaunchedEffect
        val profileState = userProfileState
        if (profileState is UiState.Success) {
            if (profileState.data == null && !autoCreatedFromSignUp) {
                autoCreatedFromSignUp = true
                vm.saveUserProfile(
                    fullName = pending.fullName,
                    mobile = pending.mobile,
                    email = pending.email.ifBlank { email },
                    age = 0,
                    gender = "Unspecified",
                    address = "",
                    bloodGroup = "O+",
                    emergencyContact = "",
                    photoUri = "",
                    existing = null,
                    onSuccess = {
                        onConsumePendingSignUp()
                    },
                    onFailure = {
                        onConsumePendingSignUp()
                    }
                )
            } else if (profileState.data != null) {
                onConsumePendingSignUp()
            }
        }
    }

    BackHandler(enabled = activePortal != UserRole.PATIENT || currentPatientDest != PatientDest.HOME) {
        if (activePortal != UserRole.PATIENT) {
            vm.setActivePortal(UserRole.PATIENT)
        } else if (currentPatientDest != PatientDest.HOME) {
            currentPatientDest = PatientDest.HOME
        }
    }

    val isStaffDoctor = isAdmin || doctors.any { it.staffUid == uid }
    val patientName = userProfile?.fullName?.ifBlank { displayName } ?: displayName
    val patientPhone = userProfile?.mobile ?: ""

    Scaffold(
        topBar = {
            CareNexTopBar(
                activePortal = activePortal,
                unreadNotifications = notifications.count { !it.read },
                onSwitchPortal = { showPortalDialog = true },
                onOpenNotifications = { showNotificationsDialog = true },
                onOpenProfile = { showProfileDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activePortal == UserRole.PATIENT) {
                val careHubActive = currentPatientDest == PatientDest.RECORDS ||
                    currentPatientDest == PatientDest.CONSULT ||
                    currentPatientDest == PatientDest.BILLING

                NavigationBar {
                    NavigationBarItem(
                        selected = currentPatientDest == PatientDest.HOME,
                        onClick = { currentPatientDest = PatientDest.HOME },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentPatientDest == PatientDest.HOSPITALS_OPD,
                        onClick = { currentPatientDest = PatientDest.HOSPITALS_OPD },
                        icon = { Icon(Icons.Filled.LocalHospital, contentDescription = "Hospitals and OPD") },
                        label = { Text("OPD") },
                        modifier = Modifier.testTag("nav_opd")
                    )
                    NavigationBarItem(
                        selected = currentPatientDest == PatientDest.EMERGENCY,
                        onClick = { currentPatientDest = PatientDest.EMERGENCY },
                        icon = { Icon(Icons.Filled.Emergency, contentDescription = "Emergency") },
                        label = { Text("SOS") },
                        modifier = Modifier.testTag("nav_emergency")
                    )
                    NavigationBarItem(
                        selected = currentPatientDest == PatientDest.PHARMACY,
                        onClick = { currentPatientDest = PatientDest.PHARMACY },
                        icon = { Icon(Icons.Filled.LocalPharmacy, contentDescription = "Pharmacy") },
                        label = { Text("Pharmacy") },
                        modifier = Modifier.testTag("nav_pharmacy")
                    )
                    NavigationBarItem(
                        selected = careHubActive,
                        onClick = {
                            if (!careHubActive) currentPatientDest = PatientDest.RECORDS
                        },
                        icon = { Icon(Icons.Filled.Description, contentDescription = "Records, Consult and Billing") },
                        label = { Text("Care Hub") },
                        modifier = Modifier.testTag("nav_care_hub")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activePortal) {
                UserRole.PATIENT -> {
                    when (currentPatientDest) {
                        PatientDest.HOME -> PatientHomeScreen(
                            userProfile = userProfile,
                            isAdmin = isAdmin,
                            hospitalsState = hospitalsState,
                            doctorsState = doctorsState,
                            appointmentsState = patientApptsState,
                            userCoords = userCoords,
                            onUpdateCoords = vm::updateUserCoords,
                            onInitSampleNetwork = vm::initializeSampleHospitalNetwork,
                            onNavigate = { currentPatientDest = it },
                            onSelectHospitalForOpd = { hosp -> selectedHospitalForOpd = hosp },
                            onSelectDoctorForOpd = { doc ->
                                selectedDoctorForOpd = doc
                                selectedHospitalForOpd = hospitals.firstOrNull { it.id == doc.hospitalId }
                            }
                        )

                        PatientDest.HOSPITALS_OPD -> HospitalAndOpdScreen(
                            userProfile = userProfile,
                            defaultPatientName = patientName,
                            initialTab = 1,
                            hospitals = hospitals,
                            doctors = doctors,
                            appointments = patientAppointments,
                            selectedHospital = selectedHospitalForOpd,
                            selectedDoctor = selectedDoctorForOpd,
                            selectedSlot = selectedSlot,
                            selectedDate = selectedDate,
                            userCoords = userCoords,
                            busy = busy,
                            onSelectHospital = { hosp -> selectedHospitalForOpd = hosp },
                            onSelectDoctor = { doc -> selectedDoctorForOpd = doc },
                            onWatchSlot = vm::selectSlotWatch,
                            onBookOpd = { hosp, doc, date, slot, enteredPatientName, symptoms ->
                                vm.bookOpdAppointment(
                                    patientName = enteredPatientName,
                                    patientPhone = patientPhone,
                                    hospital = hosp,
                                    doctor = doc,
                                    date = date,
                                    timeSlot = slot,
                                    symptoms = symptoms
                                )
                            },
                            onCancelAppointment = vm::cancelAppointment,
                            onGoEmergency = { currentPatientDest = PatientDest.EMERGENCY }
                        )

                        PatientDest.EMERGENCY -> EmergencyAndAmbulanceScreen(
                            userProfile = userProfile,
                            hospitals = hospitals,
                            ambulances = ambulances,
                            requests = patientEmergency,
                            userCoords = userCoords,
                            busy = busy,
                            onRequestEmergency = { hosp, amb, pickup, emergencyType, reqType ->
                                vm.requestEmergencyOrAmbulance(
                                    patientName = patientName,
                                    patientPhone = patientPhone,
                                    hospital = hosp,
                                    ambulance = amb,
                                    pickupLocation = pickup,
                                    emergencyType = emergencyType,
                                    requestType = reqType
                                )
                            }
                        )

                        PatientDest.PHARMACY -> PharmacyScreen(
                            userProfile = userProfile,
                            medicines = medicines,
                            cart = cart,
                            prescriptions = patientPrescriptions,
                            orders = patientOrders,
                            invoices = patientInvoices,
                            busy = busy,
                            onAddToCart = vm::addToCart,
                            onUpdateCartQty = vm::updateCartQuantity,
                            onCheckoutCart = { addr, rxId, rxUri ->
                                vm.checkoutPharmacyCart(
                                    patientName = patientName,
                                    deliveryAddress = addr,
                                    prescriptionRefId = rxId,
                                    prescriptionAttachmentUri = rxUri
                                ) { order, inv ->
                                    if (!order.requiresPrescription) {
                                        invoiceToPay = inv
                                    }
                                }
                            },
                            onPayOrderInvoice = { inv -> invoiceToPay = inv }
                        )

                        PatientDest.RECORDS,
                        PatientDest.CONSULT,
                        PatientDest.BILLING -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CareHubSubNavRow(
                                    current = currentPatientDest,
                                    onSelect = { currentPatientDest = it }
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    when (currentPatientDest) {
                                        PatientDest.RECORDS -> MedicalRecordsScreen(
                                            userProfile = userProfile,
                                            reports = patientReports,
                                            prescriptions = patientPrescriptions,
                                            appointments = patientAppointments,
                                            doctors = doctors,
                                            busy = busy,
                                            onUploadReport = { title, type, summary, findings, uri, authUids ->
                                                vm.createMedicalReport(
                                                    patientId = uid,
                                                    patientName = patientName,
                                                    doctor = null,
                                                    title = title,
                                                    reportType = type,
                                                    summary = summary,
                                                    findings = findings,
                                                    attachmentUri = uri,
                                                    authorizedDoctorUids = authUids
                                                )
                                            },
                                            onUpdateDoctorAccess = vm::updateReportDoctorAccess,
                                            onMessage = vm::postMessage
                                        )

                                        PatientDest.CONSULT -> OnlineConsultationScreen(
                                            doctors = doctors,
                                            consultations = patientConsultations,
                                            invoices = patientInvoices,
                                            activeMessages = consultationMessages,
                                            currentUserId = uid,
                                            senderName = patientName,
                                            busy = busy,
                                            onBookConsultation = { doc, mode, time ->
                                                vm.bookConsultation(
                                                    patientName = patientName,
                                                    doctor = doc,
                                                    mode = mode,
                                                    scheduledTime = time
                                                ) { _, inv ->
                                                    invoiceToPay = inv
                                                }
                                            },
                                            onWatchChat = vm::setActiveConsultationChat,
                                            onSendMessage = { con, text ->
                                                vm.sendConsultationMessage(con, patientName, "patient", text)
                                            },
                                            onPayConsultationInvoice = { inv -> invoiceToPay = inv },
                                            onJoinSession = { con ->
                                                vm.updateConsultationStatus(con.id, ConsultationStatus.LIVE)
                                            },
                                            onEndSession = { con ->
                                                vm.updateConsultationStatus(con.id, ConsultationStatus.COMPLETED)
                                            }
                                        )

                                        PatientDest.BILLING -> BillingScreen(
                                            invoices = patientInvoices,
                                            onPayInvoice = { inv -> invoiceToPay = inv },
                                            onMessage = vm::postMessage
                                        )

                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                }

                UserRole.HOSPITAL_ADMIN -> AdminPortalRootScreen(
                    hospitals = hospitals,
                    doctors = doctors,
                    appointments = adminAppointments,
                    beds = beds,
                    admissions = adminAdmissions,
                    medicines = medicines,
                    pharmacyOrders = adminOrders,
                    ambulances = ambulances,
                    emergencyRequests = adminEmergency,
                    invoices = adminInvoices,
                    users = adminUsers,
                    busy = busy,
                    onInitSampleNetwork = vm::initializeSampleHospitalNetwork,
                    onSaveHospital = { hospId, name, addr, contact, timings, emAvail, icuAvail, total, avail, icuT, icuA, emT, emA, rating, lat, lng ->
                        vm.saveHospital(
                            hospId, name, addr, contact, timings, emAvail, icuAvail,
                            total, avail, icuT, icuA, emT, emA, rating, lat, lng
                        )
                    },
                    onSaveDoctor = { docId, hosp, staffUid, name, qual, spec, exp, fee, days, hours, cap, avail ->
                        vm.saveDoctor(docId, hosp, staffUid, name, qual, spec, exp, fee, days, hours, cap, avail)
                    },
                    onDeleteDoctor = vm::deleteDoctor,
                    onUpdateAppointmentStatus = vm::updateAppointmentStatus,
                    onAddBed = vm::addBed,
                    onAdmitPatient = { bed, pId, pName, pPhone, diag, docName ->
                        vm.admitPatient(bed, pId, pName, pPhone, diag, docName)
                    },
                    onDischargePatient = vm::dischargePatient,
                    onSaveMedicine = { medId, name, dosage, timing, price, stock, reqRx, cat, desc ->
                        vm.saveMedicine(medId, name, dosage, timing, price, stock, reqRx, cat, desc)
                    },
                    onUpdatePharmacyOrder = vm::updatePharmacyOrderByAdmin,
                    onAddAmbulance = vm::addAmbulance,
                    onUpdateEmergencyStatus = vm::updateEmergencyStatus,
                    onCreateCustomInvoice = vm::createCustomInvoice,
                    onMessage = vm::postMessage
                )

                UserRole.DOCTOR -> DoctorPortalRootScreen(
                    doctors = doctors,
                    appointments = doctorAppointments,
                    consultations = doctorConsultations,
                    authorizedReports = doctorReports,
                    activeMessages = consultationMessages,
                    currentUserId = uid,
                    busy = busy,
                    onUpdateDoctorAvailability = vm::updateDoctorAvailability,
                    onUpdateAppointmentStatus = vm::updateAppointmentStatus,
                    onCreatePrescription = { pId, pName, doc, apptId, diag, meds, notes ->
                        vm.createPrescription(pId, pName, doc, apptId, diag, meds, notes)
                    },
                    onCreateMedicalReport = { pId, pName, doc, title, type, summary, findings ->
                        vm.createMedicalReport(
                            patientId = pId,
                            patientName = pName,
                            doctor = doc,
                            title = title,
                            reportType = type,
                            summary = summary,
                            findings = findings,
                            attachmentUri = "",
                            authorizedDoctorUids = listOfNotNull(doc.staffUid.takeIf { it.isNotBlank() })
                        )
                    },
                    onWatchChat = vm::setActiveConsultationChat,
                    onSendMessage = { con, text ->
                        vm.sendConsultationMessage(con, con.doctorName.ifBlank { displayName }, "doctor", text)
                    },
                    onUpdateConsultationStatus = vm::updateConsultationStatus
                )
            }
        }
    }

    val needsOnboardingProfile = userProfileState is UiState.Success &&
        userProfile == null &&
        pendingPatientSignUp == null &&
        !autoCreatedFromSignUp
    if (needsOnboardingProfile || showProfileDialog) {
        ProfileSetupDialog(
            initialEmail = pendingPatientSignUp?.email ?: email,
            initialDisplayName = pendingPatientSignUp?.fullName ?: displayName,
            initialMobile = pendingPatientSignUp?.mobile ?: "",
            existingProfile = userProfile,
            busy = busy,
            isDismissible = !needsOnboardingProfile,
            onSave = { fullName, mobile, mail, age, gender, addr, blood, emContact, photoUri ->
                vm.saveUserProfile(
                    fullName = fullName,
                    mobile = mobile,
                    email = mail,
                    age = age,
                    gender = gender,
                    address = addr,
                    bloodGroup = blood,
                    emergencyContact = emContact,
                    photoUri = photoUri,
                    existing = userProfile,
                    onSuccess = { showProfileDialog = false }
                )
            },
            onSignOut = {
                showProfileDialog = false
                onSignOut()
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showNotificationsDialog) {
        NotificationsSheetDialog(
            notifications = notifications,
            onMarkRead = vm::markNotificationRead,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showPortalDialog) {
        PortalSwitcherDialog(
            activePortal = activePortal,
            isAdmin = isAdmin,
            isStaffDoctor = isStaffDoctor,
            onSelectPortal = { role ->
                vm.setActivePortal(role)
                showPortalDialog = false
            },
            onDismiss = { showPortalDialog = false }
        )
    }

    invoiceToPay?.let { inv ->
        UpiPaymentSheetDialog(
            invoice = inv,
            onDismiss = { invoiceToPay = null },
            onVerifiedPayment = { gatewayApp, upiHandle, txnId, respCode ->
                vm.confirmVerifiedUpiPayment(
                    invoice = inv,
                    gatewayApp = gatewayApp,
                    upiHandle = upiHandle,
                    transactionId = txnId,
                    responseCode = respCode
                ) {
                    invoiceToPay = null
                }
            }
        )
    }
}

@Composable
private fun CareHubSubNavRow(
    current: PatientDest,
    onSelect: (PatientDest) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = current == PatientDest.RECORDS,
            onClick = { onSelect(PatientDest.RECORDS) },
            label = { Text("Medical Records") },
            modifier = Modifier.testTag("subtab_records")
        )
        FilterChip(
            selected = current == PatientDest.CONSULT,
            onClick = { onSelect(PatientDest.CONSULT) },
            label = { Text("Online Consult") },
            modifier = Modifier.testTag("subtab_consult")
        )
        FilterChip(
            selected = current == PatientDest.BILLING,
            onClick = { onSelect(PatientDest.BILLING) },
            label = { Text("Billing & PDF") },
            modifier = Modifier.testTag("subtab_billing")
        )
    }
}
