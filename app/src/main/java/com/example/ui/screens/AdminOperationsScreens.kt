package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Admission
import com.example.data.model.AdmissionStatus
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
import com.example.data.model.WardType
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
import com.example.ui.theme.WarningAmberLight
import com.example.util.OpdSlotUtils
import com.example.util.PdfGenerator

@Composable
fun AdminDoctorsScreen(
    hospitals: List<Hospital>,
    doctors: List<Doctor>,
    busy: Boolean,
    onSaveDoctor: (String?, Hospital, String, String, String, String, Int, Double, String, String, Int, Boolean) -> Unit,
    onDeleteDoctor: (String) -> Unit
) {
    var editingDoc by remember { mutableStateOf<Doctor?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Doctor Directory (${doctors.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAdd = true },
                    enabled = hospitals.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Doctor")
                }
            }
        }
        items(doctors, key = { it.id }) { doc ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${doc.speciality} • ${doc.qualification}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        Text("${doc.hospitalName} • ${doc.experienceYears} yrs exp", style = MaterialTheme.typography.bodySmall)
                        Text("Fee: INR ${doc.consultationFee.toInt()} • Cap: ${doc.dailyCapacity}/day • ${doc.availableDays}", style = MaterialTheme.typography.labelSmall, color = CareTeal)
                    }
                    Row {
                        IconButton(onClick = { editingDoc = doc }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit doctor")
                        }
                        IconButton(onClick = { onDeleteDoctor(doc.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete doctor", tint = EmergencyRed)
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editingDoc != null) {
        DoctorEditorDialog(
            doctor = editingDoc,
            hospitals = hospitals,
            busy = busy,
            onDismiss = {
                showAdd = false
                editingDoc = null
            },
            onSave = { hosp, staffUid, name, qual, spec, exp, fee, days, hrs, cap, avail ->
                onSaveDoctor(editingDoc?.id, hosp, staffUid, name, qual, spec, exp, fee, days, hrs, cap, avail)
                showAdd = false
                editingDoc = null
            }
        )
    }
}

@Composable
private fun DoctorEditorDialog(
    doctor: Doctor?,
    hospitals: List<Hospital>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Hospital, String, String, String, String, Int, Double, String, String, Int, Boolean) -> Unit
) {
    var selectedHospital by remember {
        mutableStateOf(hospitals.firstOrNull { it.id == doctor?.hospitalId } ?: hospitals.first())
    }
    var name by remember { mutableStateOf(doctor?.name ?: "") }
    var qualification by remember { mutableStateOf(doctor?.qualification ?: "MD") }
    var speciality by remember { mutableStateOf(doctor?.speciality ?: "General Medicine") }
    var expText by remember { mutableStateOf((doctor?.experienceYears ?: 5L).toString()) }
    var feeText by remember { mutableStateOf((doctor?.consultationFee?.toInt() ?: 500).toString()) }
    var days by remember { mutableStateOf(doctor?.availableDays ?: "Mon - Sat") }
    var hours by remember { mutableStateOf(doctor?.workingHours ?: "09:00 AM - 05:00 PM") }
    var capacityText by remember { mutableStateOf((doctor?.dailyCapacity ?: 40L).toString()) }
    var staffUid by remember { mutableStateOf(doctor?.staffUid ?: "") }
    var available by remember { mutableStateOf(doctor?.available ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doctor == null) "Add Doctor" else "Edit Doctor") },
        text = {
            Column(
                modifier = Modifier.height(380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hospitals, key = { it.id }) { h ->
                        FilterChip(
                            selected = selectedHospital.id == h.id,
                            onClick = { selectedHospital = h },
                            label = { Text(h.name) }
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Doctor Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = qualification, onValueChange = { qualification = it }, label = { Text("Qualification *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = speciality, onValueChange = { speciality = it }, label = { Text("Speciality *") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expText,
                        onValueChange = { expText = it.filter(Char::isDigit) },
                        label = { Text("Exp (yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filter(Char::isDigit) },
                        label = { Text("Fee (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Available Days") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it.filter(Char::isDigit) },
                        label = { Text("Daily Cap") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Working Hours") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = staffUid, onValueChange = { staffUid = it }, label = { Text("Doctor Staff UID (optional)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = available, onCheckedChange = { available = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Accepting OPD Bookings")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && name.trim().length >= 2 && speciality.trim().length >= 2,
                onClick = {
                    onSave(
                        selectedHospital, staffUid, name, qualification, speciality,
                        expText.toIntOrNull() ?: 0,
                        feeText.toDoubleOrNull() ?: 500.0,
                        days, hours,
                        (capacityText.toIntOrNull() ?: 40).coerceIn(1, 500),
                        available
                    )
                }
            ) {
                Text("Save Doctor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminOpdScreen(
    doctors: List<Doctor>,
    appointments: List<Appointment>,
    onUpdateStatus: (String, AppointmentStatus) -> Unit
) {
    var selectedDoctorId by remember(doctors) { mutableStateOf(doctors.firstOrNull()?.id ?: "") }
    val dates = remember { OpdSlotUtils.upcomingDates(5) }
    var selectedDate by remember { mutableStateOf(dates.first()) }
    val activeDoctor = doctors.firstOrNull { it.id == selectedDoctorId } ?: doctors.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeDoctor != null) {
            val slotMap = OpdSlotUtils.slotAppointmentMap(appointments, activeDoctor.id, selectedDate)
            val freeSummary = OpdSlotUtils.freeUntilSummary(
                doctor = activeDoctor,
                bookedKeys = slotMap.keys,
                remainingCapacity = (activeDoctor.dailyCapacity - slotMap.size).coerceAtLeast(0L)
            )
            item {
                SectionCard {
                    Text("Doctor OPD Timing Slots & Free Window", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(doctors, key = { it.id }) { doc ->
                            FilterChip(
                                selected = doc.id == activeDoctor.id,
                                onClick = { selectedDoctorId = doc.id },
                                label = { Text(doc.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dates) { date ->
                            FilterChip(
                                selected = selectedDate == date,
                                onClick = { selectedDate = date },
                                label = { Text(date) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreenLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Text(freeSummary, style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpdSlotUtils.STANDARD_SLOTS.forEach { slotSpec ->
                            val bookedAppt = slotMap[slotSpec.slotKey]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(slotSpec.rangeLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                if (bookedAppt != null) {
                                    StatusPill(
                                        "${bookedAppt.patientName} (#${bookedAppt.serialNumber})",
                                        MedicalBlueLight,
                                        MedicalBlue
                                    )
                                } else {
                                    StatusPill("Free", SuccessGreenLight, SuccessGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("All OPD Appointments (${appointments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(appointments.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { appt ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("#${appt.serialNumber} • ${appt.patientName}", fontWeight = FontWeight.Bold)
                    StatusPill(appt.parsedStatus.label, MedicalBlueLight, MedicalBlue)
                }
                Text(
                    "${appt.doctorName} (${appt.speciality}) • ${appt.date} • ${OpdSlotUtils.formatSlotRange(appt.timeSlot)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (appt.parsedStatus == AppointmentStatus.CONFIRMED) {
                        Button(onClick = { onUpdateStatus(appt.id, AppointmentStatus.IN_CONSULTATION) }) {
                            Text("Check In")
                        }
                    }
                    if (appt.parsedStatus == AppointmentStatus.CONFIRMED || appt.parsedStatus == AppointmentStatus.IN_CONSULTATION) {
                        OutlinedButton(onClick = { onUpdateStatus(appt.id, AppointmentStatus.COMPLETED) }) {
                            Text("Complete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBedsScreen(
    hospitals: List<Hospital>,
    beds: List<BedInventory>,
    admissions: List<Admission>,
    doctors: List<Doctor>,
    busy: Boolean,
    onAddBed: (Hospital, WardType, String, String) -> Unit,
    onAdmitPatient: (BedInventory, String, String, String, String, String) -> Unit,
    onDischargePatient: (Admission, String) -> Unit
) {
    var showAddBed by remember { mutableStateOf(false) }
    var admitTargetBed by remember { mutableStateOf<BedInventory?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bed Inventory (${beds.count { !it.occupied }} vacant / ${beds.size} total)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { showAddBed = true }, enabled = hospitals.isNotEmpty()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Bed")
                }
            }
        }
        items(beds, key = { it.id }) { bed ->
            val activeAdm = admissions.firstOrNull { it.id == bed.currentAdmissionId && it.parsedStatus == AdmissionStatus.ADMITTED }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (bed.occupied) EmergencyRed.copy(alpha = 0.4f) else SuccessGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${bed.parsedWard.label} • Room ${bed.roomNumber} / Bed ${bed.bedNumber}", fontWeight = FontWeight.Bold)
                        Text(bed.hospitalName, style = MaterialTheme.typography.bodySmall)
                        if (activeAdm != null) {
                            Text("Patient: ${activeAdm.patientName} (${activeAdm.diagnosis})", style = MaterialTheme.typography.bodySmall, color = EmergencyRed)
                        }
                    }
                    if (!bed.occupied) {
                        Button(onClick = { admitTargetBed = bed }, enabled = !busy) {
                            Text("Admit")
                        }
                    } else if (activeAdm != null) {
                        OutlinedButton(
                            onClick = { onDischargePatient(activeAdm, "Recovered & discharged") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CareTeal)
                        ) {
                            Text("Discharge")
                        }
                    } else {
                        StatusPill("Occupied", EmergencyRedLight, EmergencyRed)
                    }
                }
            }
        }
    }

    if (showAddBed && hospitals.isNotEmpty()) {
        var hosp by remember { mutableStateOf(hospitals.first()) }
        var ward by remember { mutableStateOf(WardType.GENERAL) }
        var room by remember { mutableStateOf("GW-202") }
        var bedNum by remember { mutableStateOf("B-12") }
        AlertDialog(
            onDismissRequest = { showAddBed = false },
            title = { Text("Add Hospital Bed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(hospitals, key = { it.id }) { h ->
                            FilterChip(selected = hosp.id == h.id, onClick = { hosp = h }, label = { Text(h.name) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WardType.entries) { w ->
                            FilterChip(selected = ward == w, onClick = { ward = w }, label = { Text(w.label) })
                        }
                    }
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bedNum, onValueChange = { bedNum = it }, label = { Text("Bed Number") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddBed(hosp, ward, room, bedNum)
                    showAddBed = false
                }) { Text("Add Bed") }
            },
            dismissButton = { TextButton(onClick = { showAddBed = false }) { Text("Cancel") } }
        )
    }

    admitTargetBed?.let { bed ->
        var pName by remember { mutableStateOf("") }
        var pPhone by remember { mutableStateOf("") }
        var diag by remember { mutableStateOf("") }
        var docName by remember { mutableStateOf(doctors.firstOrNull()?.name ?: "Attending Physician") }
        AlertDialog(
            onDismissRequest = { admitTargetBed = null },
            title = { Text("Admit Patient to ${bed.bedNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Patient Full Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("Patient Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = diag, onValueChange = { diag = it }, label = { Text("Admission Diagnosis *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = docName, onValueChange = { docName = it }, label = { Text("Admitting Doctor") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy && pName.trim().isNotEmpty() && diag.trim().isNotEmpty(),
                    onClick = {
                        onAdmitPatient(bed, "", pName, pPhone, diag, docName)
                        admitTargetBed = null
                    }
                ) { Text("Confirm Admission") }
            },
            dismissButton = { TextButton(onClick = { admitTargetBed = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminPharmacyAndAmbulanceScreen(
    hospitals: List<Hospital>,
    medicines: List<Medicine>,
    orders: List<PharmacyOrder>,
    ambulances: List<Ambulance>,
    emergencyRequests: List<EmergencyRequest>,
    busy: Boolean,
    onSaveMedicine: (String?, String, String, String, Double, Int, Boolean, String, String) -> Unit,
    onUpdatePharmacyOrder: (PharmacyOrder, Boolean, OrderStatus) -> Unit,
    onAddAmbulance: (Hospital, String, String, String, String) -> Unit,
    onUpdateEmergencyStatus: (EmergencyRequest, EmergencyStatus, Int) -> Unit
) {
    var showAddMed by remember { mutableStateOf(false) }
    var showAddAmb by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pharmacy Orders (${orders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddMed = true }) { Text("+ Medicine") }
            }
        }
        items(orders, key = { it.id }) { order ->
            SectionCard {
                Text("${order.patientName} • ${order.itemsSummary}", fontWeight = FontWeight.Bold)
                Text("Status: ${order.parsedOrderStatus.label} • Rx Required: ${if (order.requiresPrescription) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (order.requiresPrescription && !order.pharmacistVerified) {
                        Button(
                            onClick = { onUpdatePharmacyOrder(order, true, OrderStatus.VERIFIED) }
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verify Rx")
                        }
                    } else if (order.parsedOrderStatus == OrderStatus.VERIFIED) {
                        Button(onClick = { onUpdatePharmacyOrder(order, true, OrderStatus.PACKED) }) { Text("Pack") }
                    } else if (order.parsedOrderStatus == OrderStatus.PACKED) {
                        Button(onClick = { onUpdatePharmacyOrder(order, true, OrderStatus.OUT_FOR_DELIVERY) }) { Text("Dispatch") }
                    } else if (order.parsedOrderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                        Button(onClick = { onUpdatePharmacyOrder(order, true, OrderStatus.DELIVERED) }) { Text("Delivered") }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Emergency & Ambulance Requests (${emergencyRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddAmb = true }, enabled = hospitals.isNotEmpty()) { Text("+ Ambulance") }
            }
        }
        items(emergencyRequests, key = { it.id }) { req ->
            SectionCard {
                Text("${req.patientName} • ${req.parsedRequestType.label}", fontWeight = FontWeight.Bold)
                Text("${req.hospitalName} • Pickup: ${req.pickupLocation}", style = MaterialTheme.typography.bodySmall)
                Text("Status: ${req.parsedStatus.label} • ETA: ${req.etaMinutes} min", style = MaterialTheme.typography.labelSmall, color = EmergencyRed)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (req.parsedStatus == EmergencyStatus.REQUESTED) {
                        Button(onClick = { onUpdateEmergencyStatus(req, EmergencyStatus.DISPATCHED, 8) }) { Text("Dispatch (8m)") }
                    } else if (req.parsedStatus == EmergencyStatus.DISPATCHED) {
                        Button(onClick = { onUpdateEmergencyStatus(req, EmergencyStatus.EN_ROUTE, 4) }) { Text("En Route") }
                    } else if (req.parsedStatus == EmergencyStatus.EN_ROUTE) {
                        Button(onClick = { onUpdateEmergencyStatus(req, EmergencyStatus.COMPLETED, 0) }) { Text("Complete") }
                    }
                }
            }
        }
    }

    if (showAddMed) {
        var name by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("500 mg") }
        var timing by remember { mutableStateOf("Twice daily") }
        var price by remember { mutableStateOf("120") }
        var stock by remember { mutableStateOf("100") }
        var category by remember { mutableStateOf("General") }
        var reqRx by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddMed = false },
            title = { Text("Add Pharmacy Medicine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = timing, onValueChange = { timing = it }, label = { Text("Timing") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Price (INR)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = stock, onValueChange = { stock = it.filter(Char::isDigit) }, label = { Text("Stock") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reqRx, onCheckedChange = { reqRx = it })
                        Text("Requires Doctor Prescription (Rx)")
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && name.trim().length >= 2, onClick = {
                    onSaveMedicine(null, name, dosage, timing, price.toDoubleOrNull() ?: 50.0, stock.toIntOrNull() ?: 50, reqRx, category, "")
                    showAddMed = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddMed = false }) { Text("Cancel") } }
        )
    }

    if (showAddAmb && hospitals.isNotEmpty()) {
        var hosp by remember { mutableStateOf(hospitals.first()) }
        var vehicle by remember { mutableStateOf("DL-01-CN-9001") }
        var driver by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAmb = false },
            title = { Text("Add Ambulance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(hospitals, key = { it.id }) { h ->
                            FilterChip(selected = hosp.id == h.id, onClick = { hosp = h }, label = { Text(h.name) })
                        }
                    }
                    OutlinedTextField(value = vehicle, onValueChange = { vehicle = it }, label = { Text("Vehicle Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = driver, onValueChange = { driver = it }, label = { Text("Driver Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Driver Phone *") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = !busy && driver.trim().length >= 2, onClick = {
                    onAddAmbulance(hosp, vehicle, driver, phone, "ALS")
                    showAddAmb = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddAmb = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminBillingScreen(
    hospitals: List<Hospital>,
    invoices: List<Invoice>,
    busy: Boolean,
    onCreateCustomInvoice: (String, String, Hospital, String, Double, Double, Double) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hospital Invoices (${invoices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showCreate = true }, enabled = hospitals.isNotEmpty()) { Text("+ Invoice") }
            }
        }
        items(invoices.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { inv ->
            SectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${inv.invoiceNumber} • ${inv.patientName}", fontWeight = FontWeight.Bold)
                    StatusPill(
                        inv.parsedPaymentStatus.label,
                        if (inv.parsedPaymentStatus == PaymentStatus.PAID) SuccessGreenLight else WarningAmberLight,
                        if (inv.parsedPaymentStatus == PaymentStatus.PAID) SuccessGreen else WarningAmber
                    )
                }
                Text("INR ${inv.totalAmount.toInt()} • ${inv.paymentMethod} • ${inv.transactionId.ifBlank { "Pending" }}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        PdfGenerator.generateAndOpenInvoicePdf(context, inv)
                            .onFailure { onMessage("PDF export error: ${it.localizedMessage}") }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View PDF Invoice")
                }
            }
        }
    }

    if (showCreate && hospitals.isNotEmpty()) {
        var hosp by remember { mutableStateOf(hospitals.first()) }
        var pName by remember { mutableStateOf("") }
        var docName by remember { mutableStateOf("Attending Specialist") }
        var conAmt by remember { mutableStateOf("500") }
        var medAmt by remember { mutableStateOf("0") }
        var othAmt by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create Hospital Invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Patient Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = docName, onValueChange = { docName = it }, label = { Text("Doctor Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = conAmt, onValueChange = { conAmt = it.filter(Char::isDigit) }, label = { Text("Consult") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = medAmt, onValueChange = { medAmt = it.filter(Char::isDigit) }, label = { Text("Meds") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = othAmt, onValueChange = { othAmt = it.filter(Char::isDigit) }, label = { Text("Other") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(enabled = !busy && pName.trim().isNotEmpty(), onClick = {
                    onCreateCustomInvoice(
                        "", pName, hosp, docName,
                        conAmt.toDoubleOrNull() ?: 0.0,
                        medAmt.toDoubleOrNull() ?: 0.0,
                        othAmt.toDoubleOrNull() ?: 0.0
                    )
                    showCreate = false
                }) { Text("Create Invoice") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}
