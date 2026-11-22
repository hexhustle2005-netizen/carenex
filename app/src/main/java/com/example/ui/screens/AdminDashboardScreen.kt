package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Admission
import com.example.data.model.Ambulance
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BedInventory
import com.example.data.model.Doctor
import com.example.data.model.EmergencyRequest
import com.example.data.model.EmergencyStatus
import com.example.data.model.Hospital
import com.example.data.model.Invoice
import com.example.data.model.Medicine
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.UserProfile
import com.example.data.model.WardType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CareTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedLight
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MedicalBlueLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPortalRootScreen(
    hospitals: List<Hospital>,
    doctors: List<Doctor>,
    appointments: List<Appointment>,
    beds: List<BedInventory>,
    admissions: List<Admission>,
    medicines: List<Medicine>,
    pharmacyOrders: List<PharmacyOrder>,
    ambulances: List<Ambulance>,
    emergencyRequests: List<EmergencyRequest>,
    invoices: List<Invoice>,
    users: List<UserProfile>,
    busy: Boolean,
    onInitSampleNetwork: () -> Unit,
    onSaveHospital: (
        hospitalId: String?,
        name: String,
        address: String,
        contact: String,
        opdTimings: String,
        emergencyAvailable: Boolean,
        icuAvailable: Boolean,
        totalBeds: Int,
        availableBeds: Int,
        icuTotal: Int,
        icuAvail: Int,
        emTotal: Int,
        emAvail: Int,
        rating: Double,
        lat: Double,
        lng: Double
    ) -> Unit,
    onSaveDoctor: (
        doctorId: String?,
        hospital: Hospital,
        staffUid: String,
        name: String,
        qualification: String,
        speciality: String,
        experience: Int,
        fee: Double,
        days: String,
        hours: String,
        capacity: Int,
        available: Boolean
    ) -> Unit,
    onDeleteDoctor: (String) -> Unit,
    onUpdateAppointmentStatus: (String, AppointmentStatus) -> Unit,
    onAddBed: (Hospital, WardType, String, String) -> Unit,
    onAdmitPatient: (BedInventory, String, String, String, String, String) -> Unit,
    onDischargePatient: (Admission, String) -> Unit,
    onSaveMedicine: (String?, String, String, String, Double, Int, Boolean, String, String) -> Unit,
    onUpdatePharmacyOrder: (PharmacyOrder, Boolean, OrderStatus) -> Unit,
    onAddAmbulance: (Hospital, String, String, String, String) -> Unit,
    onUpdateEmergencyStatus: (EmergencyRequest, EmergencyStatus, Int) -> Unit,
    onCreateCustomInvoice: (String, String, Hospital, String, Double, Double, Double) -> Unit,
    onMessage: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Doctors", "OPD", "Beds", "Rx & Amb", "Billing")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabIndex == idx,
                    onClick = { tabIndex = idx },
                    text = { Text(label) }
                )
            }
        }

        when (tabIndex) {
            0 -> AdminOverviewScreen(
                hospitals = hospitals,
                doctors = doctors,
                appointments = appointments,
                invoices = invoices,
                users = users,
                busy = busy,
                onInitSampleNetwork = onInitSampleNetwork,
                onSaveHospital = onSaveHospital
            )
            1 -> AdminDoctorsScreen(
                hospitals = hospitals,
                doctors = doctors,
                busy = busy,
                onSaveDoctor = onSaveDoctor,
                onDeleteDoctor = onDeleteDoctor
            )
            2 -> AdminOpdScreen(
                doctors = doctors,
                appointments = appointments,
                onUpdateStatus = onUpdateAppointmentStatus
            )
            3 -> AdminBedsScreen(
                hospitals = hospitals,
                beds = beds,
                admissions = admissions,
                doctors = doctors,
                busy = busy,
                onAddBed = onAddBed,
                onAdmitPatient = onAdmitPatient,
                onDischargePatient = onDischargePatient
            )
            4 -> AdminPharmacyAndAmbulanceScreen(
                hospitals = hospitals,
                medicines = medicines,
                orders = pharmacyOrders,
                ambulances = ambulances,
                emergencyRequests = emergencyRequests,
                busy = busy,
                onSaveMedicine = onSaveMedicine,
                onUpdatePharmacyOrder = onUpdatePharmacyOrder,
                onAddAmbulance = onAddAmbulance,
                onUpdateEmergencyStatus = onUpdateEmergencyStatus
            )
            5 -> AdminBillingScreen(
                hospitals = hospitals,
                invoices = invoices,
                busy = busy,
                onCreateCustomInvoice = onCreateCustomInvoice,
                onMessage = onMessage
            )
        }
    }
}

@Composable
private fun AdminOverviewScreen(
    hospitals: List<Hospital>,
    doctors: List<Doctor>,
    appointments: List<Appointment>,
    invoices: List<Invoice>,
    users: List<UserProfile>,
    busy: Boolean,
    onInitSampleNetwork: () -> Unit,
    onSaveHospital: (String?, String, String, String, String, Boolean, Boolean, Int, Int, Int, Int, Int, Int, Double, Double, Double) -> Unit
) {
    var editingHospital by remember { mutableStateOf<Hospital?>(null) }
    var showAddHospital by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todaysAppts = appointments.count { it.date == todayStr && it.parsedStatus != AppointmentStatus.CANCELLED }
    val totalBeds = hospitals.sumOf { it.totalBeds }
    val availBeds = hospitals.sumOf { it.availableBeds }
    val occBeds = hospitals.sumOf { it.occupiedBeds }
    val paidRevenue = invoices.filter { it.parsedPaymentStatus == PaymentStatus.PAID }.sumOf { it.totalAmount }
    val pendingPayments = invoices.filter { it.parsedPaymentStatus != PaymentStatus.PAID }.sumOf { it.totalAmount }
    val emergencyReady = hospitals.any { it.emergencyAvailable }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hospitals.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalHospital,
                    title = "Set Up Your Hospital Network",
                    subtitle = "Seed starter CareNex hospitals, doctors, beds, ambulances & pharmacy catalog or add a hospital manually.",
                    actionLabel = if (busy) "Initializing..." else "Initialize Sample Hospital Network",
                    onAction = onInitSampleNetwork
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricCard(
                    title = "Total Patients",
                    value = users.count { it.role == "patient" }.coerceAtLeast(appointments.map { it.patientId }.distinct().size).toString(),
                    icon = Icons.Filled.People,
                    accentColor = MedicalBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Today's OPD",
                    value = todaysAppts.toString(),
                    icon = Icons.Filled.CalendarToday,
                    accentColor = CareTeal,
                    modifier = Modifier.weight(1f),
                    subtitle = "${appointments.size} total"
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricCard(
                    title = "Doctors Ready",
                    value = "${doctors.count { it.available }}/${doctors.size}",
                    icon = Icons.Filled.MedicalServices,
                    accentColor = MedicalBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Vacant / Total Beds",
                    value = "$availBeds / $totalBeds",
                    icon = Icons.Filled.Bed,
                    accentColor = CareTeal,
                    modifier = Modifier.weight(1f),
                    subtitle = "$occBeds occ"
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricCard(
                    title = "Revenue Collected",
                    value = "INR ${paidRevenue.toInt()}",
                    icon = Icons.Filled.AttachMoney,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Pending Bills",
                    value = "INR ${pendingPayments.toInt()}",
                    icon = Icons.Filled.PendingActions,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Emergency, contentDescription = null, tint = EmergencyRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Emergency Desk Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    StatusPill(
                        text = if (emergencyReady) "Active & Ready" else "Inactive",
                        containerColor = if (emergencyReady) SuccessGreenLight else EmergencyRedLight,
                        contentColor = if (emergencyReady) SuccessGreen else EmergencyRed
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Managed Hospitals (${hospitals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddHospital = true },
                    modifier = Modifier.testTag("add_hospital_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Hospital")
                }
            }
        }

        items(hospitals, key = { it.id }) { h ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(h.address, style = MaterialTheme.typography.bodySmall)
                        Text("OPD: ${h.opdTimings} • ${h.contactNumber}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        Text("Beds: ${h.availableBeds} vacant / ${h.occupiedBeds} occupied / ${h.totalBeds} total", style = MaterialTheme.typography.labelSmall, color = CareTeal)
                    }
                    IconButton(onClick = { editingHospital = h }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit hospital")
                    }
                }
            }
        }
    }

    if (showAddHospital || editingHospital != null) {
        HospitalEditorDialog(
            hospital = editingHospital,
            busy = busy,
            onDismiss = {
                showAddHospital = false
                editingHospital = null
            },
            onSave = { name, addr, phone, timings, em, icu, total, avail, icuT, icuA, emT, emA ->
                onSaveHospital(
                    editingHospital?.id, name, addr, phone, timings, em, icu,
                    total, avail, icuT, icuA, emT, emA,
                    editingHospital?.rating ?: 4.7,
                    editingHospital?.latitude ?: 28.6139,
                    editingHospital?.longitude ?: 77.2090
                )
                showAddHospital = false
                editingHospital = null
            }
        )
    }
}

@Composable
private fun HospitalEditorDialog(
    hospital: Hospital?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean, Boolean, Int, Int, Int, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(hospital?.name ?: "") }
    var address by remember { mutableStateOf(hospital?.address ?: "") }
    var contact by remember { mutableStateOf(hospital?.contactNumber ?: "") }
    var opdTimings by remember { mutableStateOf(hospital?.opdTimings ?: "08:00 AM - 08:00 PM") }
    var emergency by remember { mutableStateOf(hospital?.emergencyAvailable ?: true) }
    var icu by remember { mutableStateOf(hospital?.icuAvailable ?: true) }
    var totalBeds by remember { mutableStateOf((hospital?.totalBeds ?: 20L).toString()) }
    var availBeds by remember { mutableStateOf((hospital?.availableBeds ?: 15L).toString()) }
    var icuTotal by remember { mutableStateOf((hospital?.icuBedsTotal ?: 5L).toString()) }
    var icuAvail by remember { mutableStateOf((hospital?.icuBedsAvailable ?: 4L).toString()) }
    var emTotal by remember { mutableStateOf((hospital?.emergencyBedsTotal ?: 5L).toString()) }
    var emAvail by remember { mutableStateOf((hospital?.emergencyBedsAvailable ?: 4L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hospital == null) "Add Hospital" else "Edit Hospital") },
        text = {
            Column(
                modifier = Modifier.height(380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Hospital Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Number *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = opdTimings, onValueChange = { opdTimings = it }, label = { Text("OPD Timings") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = emergency, onCheckedChange = { emergency = it })
                    Text("Emergency Active")
                    Spacer(Modifier.width(12.dp))
                    Checkbox(checked = icu, onCheckedChange = { icu = it })
                    Text("ICU Active")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalBeds,
                        onValueChange = { totalBeds = it.filter(Char::isDigit) },
                        label = { Text("Total Beds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = availBeds,
                        onValueChange = { availBeds = it.filter(Char::isDigit) },
                        label = { Text("Available Beds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = icuTotal,
                        onValueChange = { icuTotal = it.filter(Char::isDigit) },
                        label = { Text("ICU Total") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = icuAvail,
                        onValueChange = { icuAvail = it.filter(Char::isDigit) },
                        label = { Text("ICU Free") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emTotal,
                        onValueChange = { emTotal = it.filter(Char::isDigit) },
                        label = { Text("ER Total") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = emAvail,
                        onValueChange = { emAvail = it.filter(Char::isDigit) },
                        label = { Text("ER Free") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && name.trim().length >= 2 && address.trim().length >= 2,
                onClick = {
                    val tot = totalBeds.toIntOrNull() ?: 0
                    val av = (availBeds.toIntOrNull() ?: 0).coerceIn(0, tot)
                    onSave(
                        name, address, contact.ifBlank { "+91-11-4000-0000" }, opdTimings,
                        emergency, icu, tot, av,
                        icuTotal.toIntOrNull() ?: 0, icuAvail.toIntOrNull() ?: 0,
                        emTotal.toIntOrNull() ?: 0, emAvail.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text("Save Hospital")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
