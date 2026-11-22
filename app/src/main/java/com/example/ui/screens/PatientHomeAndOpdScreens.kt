package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.Doctor
import com.example.data.model.DoctorSlot
import com.example.data.model.Hospital
import com.example.data.model.UserProfile
import com.example.ui.components.DoctorProfileCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HospitalDirectoryCard
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CareTeal
import com.example.ui.theme.CareTealLight
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedLight
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MedicalBlueLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.ui.viewmodel.UiState
import com.example.util.LocationHelper
import com.example.util.OpdSlotUtils
import com.example.util.UserCoords
import kotlinx.coroutines.launch

enum class PatientDest(val label: String) {
    HOME("Home"),
    HOSPITALS_OPD("Hospitals & OPD"),
    EMERGENCY("Emergency"),
    PHARMACY("Pharmacy"),
    RECORDS("Records"),
    CONSULT("Consult"),
    BILLING("Billing")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientHomeScreen(
    userProfile: UserProfile?,
    isAdmin: Boolean,
    hospitalsState: UiState<List<Hospital>>,
    doctorsState: UiState<List<Doctor>>,
    appointmentsState: UiState<List<Appointment>>,
    userCoords: UserCoords?,
    onUpdateCoords: (UserCoords?) -> Unit,
    onInitSampleNetwork: () -> Unit,
    onNavigate: (PatientDest) -> Unit,
    onSelectHospitalForOpd: (Hospital) -> Unit,
    onSelectDoctorForOpd: (Doctor) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                onUpdateCoords(LocationHelper.getLastKnownCoords(context))
            }
        }
    }

    LaunchedEffect(Unit) {
        onUpdateCoords(LocationHelper.getLastKnownCoords(context))
    }

    val hospitals = (hospitalsState as? UiState.Success)?.data ?: emptyList()
    val doctors = (doctorsState as? UiState.Success)?.data ?: emptyList()
    val appointments = (appointmentsState as? UiState.Success)?.data ?: emptyList()

    val filteredHospitals = remember(hospitals, searchQuery) {
        if (searchQuery.isBlank()) hospitals
        else hospitals.filter {
            it.name.contains(searchQuery, true) || it.address.contains(searchQuery, true)
        }
    }
    val filteredDoctors = remember(doctors, searchQuery) {
        if (searchQuery.isBlank()) doctors
        else doctors.filter {
            it.name.contains(searchQuery, true) ||
                it.speciality.contains(searchQuery, true) ||
                it.hospitalName.contains(searchQuery, true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalBlue)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.img_hero_care),
                        contentDescription = "CareNex Healthcare Hero",
                        contentScale = ContentScale.Crop,
                        alpha = 0.25f,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Hello, ${userProfile?.fullName?.ifBlank { "CareNex Member" } ?: "CareNex Member"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Blood Group: ${userProfile?.bloodGroup ?: "O+"} • ${appointments.count { it.parsedStatus == AppointmentStatus.CONFIRMED }} Upcoming OPD",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onNavigate(PatientDest.HOSPITALS_OPD) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MedicalBlue),
                                modifier = Modifier.testTag("hero_book_opd_button")
                            ) {
                                Text("Book OPD")
                            }
                            Button(
                                onClick = { onNavigate(PatientDest.EMERGENCY) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                                modifier = Modifier.testTag("hero_emergency_button")
                            ) {
                                Icon(Icons.Filled.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SOS / Ambulance")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hospitals, specialists, cardiology...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (userCoords == null) {
                        TextButton(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Enable location", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("GPS", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("home_search_input")
            )
        }

        item {
            Text("Healthcare Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionTile("Book OPD", Icons.Filled.CalendarMonth, MedicalBlue) {
                    onNavigate(PatientDest.HOSPITALS_OPD)
                }
                QuickActionTile("Emergency", Icons.Filled.Emergency, EmergencyRed) {
                    onNavigate(PatientDest.EMERGENCY)
                }
                QuickActionTile("Ambulance", Icons.Filled.AirportShuttle, EmergencyRed) {
                    onNavigate(PatientDest.EMERGENCY)
                }
                QuickActionTile("Pharmacy", Icons.Filled.LocalPharmacy, CareTeal) {
                    onNavigate(PatientDest.PHARMACY)
                }
                QuickActionTile("Online Consult", Icons.Filled.VideoCall, MedicalBlue) {
                    onNavigate(PatientDest.CONSULT)
                }
                QuickActionTile("Records", Icons.Filled.Description, CareTeal) {
                    onNavigate(PatientDest.RECORDS)
                }
                QuickActionTile("Appointments", Icons.Filled.EventAvailable, MedicalBlue) {
                    onNavigate(PatientDest.HOSPITALS_OPD)
                }
                QuickActionTile("Billing & PDF", Icons.AutoMirrored.Filled.ReceiptLong, CareTeal) {
                    onNavigate(PatientDest.BILLING)
                }
            }
        }

        if (hospitalsState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredHospitals.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalHospital,
                    title = "No Hospitals Listed Yet",
                    subtitle = if (isAdmin) {
                        "Initialize the CareNex hospital network to add hospitals, doctors, beds, ambulances & medicines to Cloud Firestore."
                    } else {
                        "Ask a Hospital Admin to add facilities or search with another keyword."
                    },
                    actionLabel = if (isAdmin) "Initialize Live Hospital Network" else null,
                    onAction = if (isAdmin) onInitSampleNetwork else null
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nearby Hospitals (${filteredHospitals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onNavigate(PatientDest.HOSPITALS_OPD) }) {
                        Text("See all")
                    }
                }
            }
            items(filteredHospitals.take(3), key = { it.id }) { hospital ->
                val dist = userCoords?.let {
                    LocationHelper.distanceKm(it.latitude, it.longitude, hospital.latitude, hospital.longitude)
                }
                HospitalDirectoryCard(
                    hospital = hospital,
                    distanceKm = dist,
                    onBookOpd = {
                        onSelectHospitalForOpd(hospital)
                        onNavigate(PatientDest.HOSPITALS_OPD)
                    },
                    onEmergency = { onNavigate(PatientDest.EMERGENCY) }
                )
            }
        }

        if (filteredDoctors.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Specialist Doctors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onNavigate(PatientDest.HOSPITALS_OPD) }) {
                        Text("Book OPD")
                    }
                }
            }
            items(filteredDoctors.take(4), key = { it.id }) { doctor ->
                DoctorProfileCard(
                    doctor = doctor,
                    onBookOpd = {
                        onSelectDoctorForOpd(doctor)
                        onNavigate(PatientDest.HOSPITALS_OPD)
                    },
                    onConsultOnline = { onNavigate(PatientDest.CONSULT) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(82.dp)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HospitalAndOpdScreen(
    userProfile: UserProfile?,
    defaultPatientName: String,
    initialTab: Int = 1,
    hospitals: List<Hospital>,
    doctors: List<Doctor>,
    appointments: List<Appointment>,
    selectedHospital: Hospital?,
    selectedDoctor: Doctor?,
    selectedSlot: DoctorSlot?,
    selectedDate: String,
    userCoords: UserCoords?,
    busy: Boolean,
    onSelectHospital: (Hospital) -> Unit,
    onSelectDoctor: (Doctor) -> Unit,
    onWatchSlot: (String, String) -> Unit,
    onBookOpd: (Hospital, Doctor, String, String, String, String) -> Unit,
    onCancelAppointment: (Appointment) -> Unit,
    onGoEmergency: () -> Unit
) {
    var tabIndex by remember(initialTab) { mutableIntStateOf(initialTab) }
    var search by remember { mutableStateOf("") }
    var onlyEmergency by remember { mutableStateOf(false) }
    var onlyIcu by remember { mutableStateOf(false) }

    val dates = remember { OpdSlotUtils.upcomingDates(5) }
    val timeSlots = OpdSlotUtils.STANDARD_SLOTS

    var patientNameInput by remember(userProfile?.fullName, defaultPatientName) {
        mutableStateOf(userProfile?.fullName?.ifBlank { defaultPatientName } ?: defaultPatientName)
    }
    var chosenSlot by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }

    val activeHospital = selectedHospital ?: hospitals.firstOrNull()
    val hospitalDoctors = remember(doctors, activeHospital) {
        if (activeHospital == null) doctors else doctors.filter { it.hospitalId == activeHospital.id }
    }
    val activeDoctor = selectedDoctor?.takeIf { activeHospital == null || it.hospitalId == activeHospital.id }
        ?: hospitalDoctors.firstOrNull()

    LaunchedEffect(activeDoctor?.id, selectedDate) {
        if (activeDoctor != null) {
            onWatchSlot(activeDoctor.id, selectedDate)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Hospitals (${hospitals.size})") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Book OPD") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("My Appts (${appointments.size})") })
        }

        when (tabIndex) {
            0 -> {
                val filtered = hospitals.filter { h ->
                    (search.isBlank() || h.name.contains(search, true) || h.address.contains(search, true)) &&
                        (!onlyEmergency || h.emergencyAvailable) &&
                        (!onlyIcu || (h.icuAvailable && h.icuBedsAvailable > 0))
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Filter hospitals by name or area") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = onlyEmergency,
                                onClick = { onlyEmergency = !onlyEmergency },
                                label = { Text("24x7 Emergency") }
                            )
                            FilterChip(
                                selected = onlyIcu,
                                onClick = { onlyIcu = !onlyIcu },
                                label = { Text("ICU Beds Open") }
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { h ->
                        val dist = userCoords?.let {
                            LocationHelper.distanceKm(it.latitude, it.longitude, h.latitude, h.longitude)
                        }
                        HospitalDirectoryCard(
                            hospital = h,
                            distanceKm = dist,
                            onBookOpd = {
                                onSelectHospital(h)
                                tabIndex = 1
                            },
                            onEmergency = onGoEmergency
                        )
                    }
                }
            }

            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (activeHospital == null || activeDoctor == null) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.CalendarMonth,
                                title = "No Doctors Available",
                                subtitle = "Hospitals and doctors must be added by Hospital Admin first."
                            )
                        }
                    } else {
                        item {
                            SectionCard {
                                Text("1. Select Hospital", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(hospitals, key = { it.id }) { h ->
                                        FilterChip(
                                            selected = h.id == activeHospital.id,
                                            onClick = { onSelectHospital(h) },
                                            label = { Text(h.name) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SectionCard {
                                Text("2. Select Doctor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(hospitalDoctors, key = { it.id }) { doc ->
                                        FilterChip(
                                            selected = doc.id == activeDoctor.id,
                                            onClick = {
                                                onSelectDoctor(doc)
                                                chosenSlot = ""
                                            },
                                            label = { Text("${doc.name} (${doc.speciality})") }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${activeDoctor.qualification} • Fee: INR ${activeDoctor.consultationFee.toInt()} • ${activeDoctor.workingHours}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MedicalBlue
                                )
                            }
                        }

                        item {
                            val capacity = activeDoctor.dailyCapacity
                            val booked = selectedSlot?.bookedCount ?: 0L
                            val available = (capacity - booked).coerceAtLeast(0L)
                            val bookedList = selectedSlot?.bookedSlots ?: emptyList()
                            val freeSummary = OpdSlotUtils.freeUntilSummary(activeDoctor, bookedList, available)

                            SectionCard {
                                Text("3. Patient Name, Date & Free Timing Slots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = patientNameInput,
                                    onValueChange = { patientNameInput = it },
                                    label = { Text("Patient Name *") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("opd_patient_name_input")
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(dates) { date ->
                                        FilterChip(
                                            selected = selectedDate == date,
                                            onClick = {
                                                onWatchSlot(activeDoctor.id, date)
                                                chosenSlot = ""
                                            },
                                            label = { Text(date) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (available > 0 && activeDoctor.available) SuccessGreenLight else EmergencyRedLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = if (available > 0 && activeDoctor.available) SuccessGreen else EmergencyRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = freeSummary,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (available > 0 && activeDoctor.available) SuccessGreen else EmergencyRed
                                            )
                                            Text(
                                                text = "OPD Schedule: ${activeDoctor.workingHours} (${activeDoctor.availableDays})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusPill("Daily Limit: $capacity", MedicalBlueLight, MedicalBlue)
                                    StatusPill("Booked: $booked", WarningAmberLight, WarningAmber)
                                    StatusPill(
                                        "Free: $available",
                                        if (available > 0) SuccessGreenLight else EmergencyRedLight,
                                        if (available > 0) SuccessGreen else EmergencyRed
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Next Serial Token: #${booked + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CareTeal
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    timeSlots.forEach { slotSpec ->
                                        val isBooked = bookedList.contains(slotSpec.slotKey)
                                        val isSelected = chosenSlot == slotSpec.slotKey
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                isBooked -> EmergencyRedLight
                                                isSelected -> MedicalBlue
                                                else -> CareTealLight
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isBooked -> EmergencyRed.copy(alpha = 0.4f)
                                                    isSelected -> MedicalBlue
                                                    else -> CareTeal.copy(alpha = 0.4f)
                                                }
                                            ),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = !isBooked && available > 0 && activeDoctor.available) {
                                                    chosenSlot = slotSpec.slotKey
                                                }
                                        ) {
                                            Text(
                                                text = if (isBooked) {
                                                    "${slotSpec.rangeLabel} • Booked"
                                                } else {
                                                    "${slotSpec.rangeLabel} • Free"
                                                },
                                                color = when {
                                                    isBooked -> EmergencyRed
                                                    isSelected -> Color.White
                                                    else -> CareTeal
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = symptoms,
                                    onValueChange = { symptoms = it },
                                    label = { Text("Reason / Symptoms (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onBookOpd(
                                            activeHospital,
                                            activeDoctor,
                                            selectedDate,
                                            chosenSlot,
                                            patientNameInput.trim().ifBlank { defaultPatientName },
                                            symptoms
                                        )
                                        chosenSlot = ""
                                        symptoms = ""
                                        tabIndex = 2
                                    },
                                    enabled = !busy &&
                                        patientNameInput.trim().length >= 2 &&
                                        chosenSlot.isNotBlank() &&
                                        available > 0 &&
                                        activeDoctor.available,
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_opd_booking_button")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (chosenSlot.isBlank()) "Select a Free Timing Slot"
                                        else "Confirm Booking • ${OpdSlotUtils.formatSlotRange(chosenSlot)} (INR ${activeDoctor.consultationFee.toInt()})"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (appointments.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.EventAvailable,
                                title = "No Appointments Yet",
                                subtitle = "Book a real-time OPD slot from the Book OPD tab.",
                                actionLabel = "Book OPD Now",
                                onAction = { tabIndex = 1 }
                            )
                        }
                    } else {
                        items(appointments.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { appt ->
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Token #${appt.serialNumber}", style = MaterialTheme.typography.labelLarge, color = MedicalBlue)
                                    val status = appt.parsedStatus
                                    StatusPill(
                                        text = status.label,
                                        containerColor = when (status) {
                                            AppointmentStatus.CONFIRMED -> MedicalBlueLight
                                            AppointmentStatus.IN_CONSULTATION -> WarningAmberLight
                                            AppointmentStatus.COMPLETED -> SuccessGreenLight
                                            AppointmentStatus.CANCELLED -> EmergencyRedLight
                                        },
                                        contentColor = when (status) {
                                            AppointmentStatus.CONFIRMED -> MedicalBlue
                                            AppointmentStatus.IN_CONSULTATION -> WarningAmber
                                            AppointmentStatus.COMPLETED -> SuccessGreen
                                            AppointmentStatus.CANCELLED -> EmergencyRed
                                        }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Patient: ${appt.patientName}", style = MaterialTheme.typography.labelLarge, color = MedicalBlue)
                                Text(appt.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${appt.speciality} • ${appt.hospitalName}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${appt.date} • ${OpdSlotUtils.formatSlotRange(appt.timeSlot)} • Fee: INR ${appt.consultationFee.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CareTeal
                                )
                                if (appt.symptoms.isNotBlank()) {
                                    Text("Notes: ${appt.symptoms}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (appt.parsedStatus == AppointmentStatus.CONFIRMED) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { onCancelAppointment(appt) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel Appointment & Release Slot")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
