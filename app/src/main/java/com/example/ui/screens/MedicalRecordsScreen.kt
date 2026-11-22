package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Appointment
import com.example.data.model.Doctor
import com.example.data.model.MedicalReport
import com.example.data.model.Prescription
import com.example.data.model.ReportType
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CareTeal
import com.example.ui.theme.CareTealLight
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MedicalBlueLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.util.PdfGenerator

@Composable
fun MedicalRecordsScreen(
    userProfile: UserProfile?,
    reports: List<MedicalReport>,
    prescriptions: List<Prescription>,
    appointments: List<Appointment>,
    doctors: List<Doctor>,
    busy: Boolean,
    onUploadReport: (
        title: String,
        type: ReportType,
        summary: String,
        findings: String,
        attachmentUri: String,
        authorizedDoctorUids: List<String>
    ) -> Unit,
    onUpdateDoctorAccess: (String, List<String>) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var accessReport by remember { mutableStateOf<MedicalReport?>(null) }

    val filters = listOf("ALL" to "All", "RX" to "Prescriptions") +
        ReportType.entries.map { it.name to it.label }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            userProfile?.fullName?.ifBlank { "Patient Health Record" } ?: "Patient Health Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Age: ${userProfile?.age ?: "--"} • Blood Group: ${userProfile?.bloodGroup ?: "O+"} • Past Appts: ${appointments.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier.testTag("upload_record_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Upload")
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (selectedFilter == "ALL" || selectedFilter == "RX") {
            if (prescriptions.isNotEmpty()) {
                item {
                    Text("Doctor Prescriptions (${prescriptions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(prescriptions.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { "rx_${it.id}" }) { rx ->
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Medication, contentDescription = null, tint = CareTeal)
                                Spacer(Modifier.width(8.dp))
                                Text(rx.diagnosis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            StatusPill("Verified Rx", SuccessGreenLight, SuccessGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${rx.doctorName} • ${rx.hospitalName}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        Spacer(Modifier.height(6.dp))
                        rx.medications.forEach { med ->
                            Text("• $med", style = MaterialTheme.typography.bodySmall)
                        }
                        if (rx.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Notes: ${rx.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                PdfGenerator.generateAndOpenReportPdf(context, null, rx)
                                    .onFailure { onMessage("Unable to export PDF: ${it.localizedMessage}") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download Prescription PDF")
                        }
                    }
                }
            }
        }

        if (selectedFilter != "RX") {
            val filteredReports = if (selectedFilter == "ALL") reports
            else reports.filter { it.reportType == selectedFilter }

            item {
                Text("Lab, Scan & History Reports (${filteredReports.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (filteredReports.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Description,
                        title = "No Medical Reports Found",
                        subtitle = "Upload blood tests, X-rays, scans, or medical history documents.",
                        actionLabel = "Upload Document",
                        onAction = { showUploadDialog = true }
                    )
                }
            } else {
                items(filteredReports.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { rep ->
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rep.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusPill(rep.parsedType.label, CareTealLight, CareTeal)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Recorded by: ${rep.doctorName.ifBlank { "Self" }}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        Spacer(Modifier.height(4.dp))
                        Text(rep.summary, style = MaterialTheme.typography.bodySmall)
                        if (rep.findings.isNotBlank()) {
                            Text("Findings: ${rep.findings}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { accessReport = rep },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Doctor Access (${rep.authorizedDoctorUids.size})")
                            }
                            Button(
                                onClick = {
                                    PdfGenerator.generateAndOpenReportPdf(context, rep, null)
                                        .onFailure { onMessage("Unable to export PDF: ${it.localizedMessage}") }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadReportDialog(
            doctors = doctors,
            busy = busy,
            onDismiss = { showUploadDialog = false },
            onSubmit = { title, type, summary, findings, uri, authUids ->
                onUploadReport(title, type, summary, findings, uri, authUids)
                showUploadDialog = false
            }
        )
    }

    accessReport?.let { rep ->
        AuthorizeDoctorsDialog(
            report = rep,
            doctors = doctors,
            onDismiss = { accessReport = null },
            onSave = { updatedUids ->
                onUpdateDoctorAccess(rep.id, updatedUids)
                accessReport = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadReportDialog(
    doctors: List<Doctor>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, ReportType, String, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReportType.BLOOD_TEST) }
    var summary by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }
    var selectedDocUids by remember { mutableStateOf(emptySet<String>()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) attachmentUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Medical Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Report Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary / History *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = findings,
                    onValueChange = { findings = it },
                    label = { Text("Lab Values / Clinical Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        docPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (attachmentUri.isBlank()) "Attach Scan/Report Image" else "Document Attached")
                }
                if (doctors.isNotEmpty()) {
                    Text("Authorize Doctors:", style = MaterialTheme.typography.labelLarge)
                    doctors.take(3).forEach { doc ->
                        val docUid = doc.staffUid.ifBlank { doc.adminUid }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedDocUids.contains(docUid),
                                onCheckedChange = { checked ->
                                    selectedDocUids = if (checked) selectedDocUids + docUid else selectedDocUids - docUid
                                }
                            )
                            Text(doc.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = EmergencyRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    if (title.trim().length < 2 || summary.trim().isEmpty()) {
                        errorMsg = "Title and summary are required."
                    } else {
                        onSubmit(title, selectedType, summary, findings, attachmentUri, selectedDocUids.toList())
                    }
                }
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AuthorizeDoctorsDialog(
    report: MedicalReport,
    doctors: List<Doctor>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var authorized by remember { mutableStateOf(report.authorizedDoctorUids.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Doctor Record Authorization") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Only selected doctors can view '${report.title}'.",
                    style = MaterialTheme.typography.bodySmall
                )
                doctors.forEach { doc ->
                    val docUid = doc.staffUid.ifBlank { doc.adminUid }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = authorized.contains(docUid),
                            onCheckedChange = { checked ->
                                authorized = if (checked) authorized + docUid else authorized - docUid
                            }
                        )
                        Text("${doc.name} (${doc.speciality})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(authorized.toList()) }) { Text("Update Access") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
