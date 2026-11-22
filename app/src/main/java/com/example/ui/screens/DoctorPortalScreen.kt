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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.Consultation
import com.example.data.model.ConsultationMessage
import com.example.data.model.ConsultationStatus
import com.example.data.model.Doctor
import com.example.data.model.MedicalReport
import com.example.data.model.ReportType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CareTeal
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MedicalBlueLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.util.OpdSlotUtils

@Composable
fun DoctorPortalRootScreen(
    doctors: List<Doctor>,
    appointments: List<Appointment>,
    consultations: List<Consultation>,
    authorizedReports: List<MedicalReport>,
    activeMessages: List<ConsultationMessage>,
    currentUserId: String,
    busy: Boolean,
    onUpdateDoctorAvailability: (String, Boolean, String) -> Unit,
    onUpdateAppointmentStatus: (String, AppointmentStatus) -> Unit,
    onCreatePrescription: (String, String, Doctor, String, String, List<String>, String) -> Unit,
    onCreateMedicalReport: (String, String, Doctor, String, ReportType, String, String) -> Unit,
    onWatchChat: (String) -> Unit,
    onSendMessage: (Consultation, String) -> Unit,
    onUpdateConsultationStatus: (String, ConsultationStatus) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val matchedDoctor = remember(doctors, currentUserId) {
        doctors.firstOrNull { it.staffUid == currentUserId } ?: doctors.firstOrNull()
    }
    var activeDoctor by remember(matchedDoctor) { mutableStateOf(matchedDoctor) }
    var rxTargetAppt by remember { mutableStateOf<Appointment?>(null) }
    var reportTargetAppt by remember { mutableStateOf<Appointment?>(null) }
    var activeChatSession by remember { mutableStateOf<Consultation?>(null) }
    val scheduleDates = remember { OpdSlotUtils.upcomingDates(5) }
    var scheduleDate by remember { mutableStateOf(scheduleDates.first()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Appointments") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Telehealth") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Records") })
        }

        when (tabIndex) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeDoctor == null) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.MedicalServices,
                                title = "No Doctor Profile Linked",
                                subtitle = "Ask a Hospital Admin to add your doctor profile to manage appointments & prescriptions."
                            )
                        }
                    } else {
                        val doc = activeDoctor!!
                        val slotMap = OpdSlotUtils.slotAppointmentMap(appointments, doc.id, scheduleDate)
                        val freeSummary = OpdSlotUtils.freeUntilSummary(
                            doctor = doc,
                            bookedKeys = slotMap.keys,
                            remainingCapacity = (doc.dailyCapacity - slotMap.size).coerceAtLeast(0L)
                        )
                        item {
                            if (doctors.size > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(doctors, key = { it.id }) { d ->
                                        FilterChip(
                                            selected = d.id == doc.id,
                                            onClick = { activeDoctor = d },
                                            label = { Text(d.name) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${doc.speciality} • ${doc.hospitalName}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                                        Text("Hours: ${doc.workingHours}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Switch(
                                        checked = doc.available,
                                        onCheckedChange = { next ->
                                            onUpdateDoctorAvailability(doc.id, next, doc.workingHours)
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            SectionCard {
                                Text("OPD Timing Slots & Free Window", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(scheduleDates) { d ->
                                        FilterChip(
                                            selected = scheduleDate == d,
                                            onClick = { scheduleDate = d },
                                            label = { Text(d) }
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

                        item {
                            Text("OPD Queue (${appointments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (appointments.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.MedicalServices,
                                    title = "No OPD Appointments Today",
                                    subtitle = "Booked patient appointments appear here in real time."
                                )
                            }
                        } else {
                            items(appointments.sortedBy { it.serialNumber }, key = { it.id }) { appt ->
                                SectionCard {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Token #${appt.serialNumber} • ${appt.patientName}", fontWeight = FontWeight.Bold)
                                        StatusPill(appt.parsedStatus.label, MedicalBlueLight, MedicalBlue)
                                    }
                                    Text("${appt.date} • ${OpdSlotUtils.formatSlotRange(appt.timeSlot)} • Phone: ${appt.patientPhone.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                    if (appt.symptoms.isNotBlank()) {
                                        Text("Symptoms: ${appt.symptoms}", style = MaterialTheme.typography.bodySmall, color = CareTeal)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        if (appt.parsedStatus == AppointmentStatus.CONFIRMED) {
                                            OutlinedButton(
                                                onClick = { onUpdateAppointmentStatus(appt.id, AppointmentStatus.IN_CONSULTATION) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Start") }
                                        } else if (appt.parsedStatus == AppointmentStatus.IN_CONSULTATION) {
                                            OutlinedButton(
                                                onClick = { onUpdateAppointmentStatus(appt.id, AppointmentStatus.COMPLETED) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Complete") }
                                        }
                                        Button(
                                            onClick = { rxTargetAppt = appt },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Write Rx")
                                        }
                                        OutlinedButton(
                                            onClick = { reportTargetAppt = appt },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+ Report")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (consultations.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.VideoCall,
                                title = "No Online Consultations",
                                subtitle = "Patient video, audio, and chat requests appear here."
                            )
                        }
                    } else {
                        items(consultations, key = { it.id }) { con ->
                            SectionCard {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Patient: ${con.patientName}", fontWeight = FontWeight.Bold)
                                        Text("${con.parsedMode.label} • ${con.scheduledTime}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                                    }
                                    StatusPill(con.parsedStatus.label, SuccessGreenLight, SuccessGreen)
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onWatchChat(con.id)
                                        onUpdateConsultationStatus(con.id, ConsultationStatus.LIVE)
                                        activeChatSession = con
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Join ${con.parsedMode.label} Room")
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
                    item {
                        Text("Patient-Authorized Medical Records (${authorizedReports.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (authorizedReports.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Description,
                                title = "No Authorized Patient Records",
                                subtitle = "Only records explicitly shared by patients with you appear here."
                            )
                        }
                    } else {
                        items(authorizedReports, key = { it.id }) { rep ->
                            SectionCard {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(rep.title, fontWeight = FontWeight.Bold)
                                    StatusPill(rep.parsedType.label, MedicalBlueLight, MedicalBlue)
                                }
                                Text("Patient: ${rep.patientName}", style = MaterialTheme.typography.bodySmall, color = CareTeal)
                                Text(rep.summary, style = MaterialTheme.typography.bodySmall)
                                if (rep.findings.isNotBlank()) {
                                    Text("Findings: ${rep.findings}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rxTargetAppt?.let { appt ->
        activeDoctor?.let { doc ->
            var diagnosis by remember { mutableStateOf("") }
            var medsText by remember { mutableStateOf("Paracetamol 650mg - 1 tab SOS\nVitamin D3 - 1 cap weekly") }
            var notes by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { rxTargetAppt = null },
                title = { Text("Write Digital Prescription") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Patient: ${appt.patientName}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Diagnosis *") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = medsText, onValueChange = { medsText = it }, label = { Text("Medications (one per line) *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Clinical Advice") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !busy && diagnosis.trim().isNotEmpty() && medsText.trim().isNotEmpty(),
                        onClick = {
                            val meds = medsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            onCreatePrescription(appt.patientId, appt.patientName, doc, appt.id, diagnosis, meds, notes)
                            rxTargetAppt = null
                        }
                    ) { Text("Issue Rx") }
                },
                dismissButton = { TextButton(onClick = { rxTargetAppt = null }) { Text("Cancel") } }
            )
        }
    }

    reportTargetAppt?.let { appt ->
        activeDoctor?.let { doc ->
            var title by remember { mutableStateOf("Clinical Consultation Summary") }
            var type by remember { mutableStateOf(ReportType.HISTORY) }
            var summary by remember { mutableStateOf("") }
            var findings by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { reportTargetAppt = null },
                title = { Text("Upload Patient Medical Report") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ReportType.entries) { t ->
                                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                            }
                        }
                        OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary *") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = findings, onValueChange = { findings = it }, label = { Text("Findings") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !busy && title.trim().length >= 2 && summary.trim().isNotEmpty(),
                        onClick = {
                            onCreateMedicalReport(appt.patientId, appt.patientName, doc, title, type, summary, findings)
                            reportTargetAppt = null
                        }
                    ) { Text("Upload") }
                },
                dismissButton = { TextButton(onClick = { reportTargetAppt = null }) { Text("Cancel") } }
            )
        }
    }

    activeChatSession?.let { con ->
        LiveConsultationRoomDialog(
            consultation = con,
            messages = activeMessages,
            currentUserId = currentUserId,
            onSendMessage = { text -> onSendMessage(con, text) },
            onEndCall = {
                onUpdateConsultationStatus(con.id, ConsultationStatus.COMPLETED)
                activeChatSession = null
            },
            onDismiss = { activeChatSession = null }
        )
    }
}
