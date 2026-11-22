package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.Consultation
import com.example.data.model.ConsultationMessage
import com.example.data.model.ConsultationMode
import com.example.data.model.ConsultationStatus
import com.example.data.model.Doctor
import com.example.data.model.Invoice
import com.example.data.model.PaymentStatus
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
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.util.PdfGenerator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OnlineConsultationScreen(
    doctors: List<Doctor>,
    consultations: List<Consultation>,
    invoices: List<Invoice>,
    activeMessages: List<ConsultationMessage>,
    currentUserId: String,
    senderName: String,
    busy: Boolean,
    onBookConsultation: (Doctor, ConsultationMode, String) -> Unit,
    onWatchChat: (String) -> Unit,
    onSendMessage: (Consultation, String) -> Unit,
    onPayConsultationInvoice: (Invoice) -> Unit,
    onJoinSession: (Consultation) -> Unit,
    onEndSession: (Consultation) -> Unit
) {
    var selectedDoctor by remember(doctors) { mutableStateOf(doctors.firstOrNull()) }
    var selectedMode by remember { mutableStateOf(ConsultationMode.VIDEO) }
    var scheduledTime by remember { mutableStateOf("Today • 05:30 PM") }
    var activeSession by remember { mutableStateOf<Consultation?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.img_telehealth_doc),
                contentDescription = "Online Consultation",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        item {
            SectionCard {
                Text("Book Online Doctor Consultation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (doctors.isEmpty()) {
                    Text("No doctors available yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(doctors, key = { it.id }) { doc ->
                            FilterChip(
                                selected = selectedDoctor?.id == doc.id,
                                onClick = { selectedDoctor = doc },
                                label = { Text("${doc.name} (INR ${doc.consultationFee.toInt()})") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConsultationMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Preferred Date & Time") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            selectedDoctor?.let { onBookConsultation(it, selectedMode, scheduledTime) }
                        },
                        enabled = !busy && selectedDoctor != null,
                        modifier = Modifier.fillMaxWidth().testTag("book_consultation_button")
                    ) {
                        Icon(Icons.Filled.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Book ${selectedMode.label} • INR ${selectedDoctor?.consultationFee?.toInt() ?: 0}")
                    }
                }
            }
        }

        item {
            Text("Your Online Consultations (${consultations.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (consultations.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.VideoCall,
                    title = "No Consultations Scheduled",
                    subtitle = "Book a Video, Audio, or Live Chat consultation with any CareNex specialist."
                )
            }
        } else {
            items(consultations.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { con ->
                val matchingInvoice = invoices.firstOrNull { it.referenceId == con.id }
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(con.doctorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${con.parsedMode.label} • ${con.scheduledTime}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                            Text("Room: ${con.roomCode} • Fee: INR ${con.fee.toInt()}", style = MaterialTheme.typography.labelSmall)
                        }
                        StatusPill(
                            text = con.parsedStatus.label,
                            containerColor = if (con.parsedStatus == ConsultationStatus.LIVE) SuccessGreenLight else MedicalBlueLight,
                            contentColor = if (con.parsedStatus == ConsultationStatus.LIVE) SuccessGreen else MedicalBlue
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (con.parsedPaymentStatus == PaymentStatus.PENDING && matchingInvoice != null) {
                            Button(
                                onClick = { onPayConsultationInvoice(matchingInvoice) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pay INR ${con.fee.toInt()}")
                            }
                        }
                        Button(
                            onClick = {
                                onWatchChat(con.id)
                                onJoinSession(con)
                                activeSession = con
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CareTeal),
                            modifier = Modifier.weight(1f).testTag("join_consultation_${con.id}")
                        ) {
                            Icon(
                                when (con.parsedMode) {
                                    ConsultationMode.VIDEO -> Icons.Filled.VideoCall
                                    ConsultationMode.AUDIO -> Icons.Filled.PhoneInTalk
                                    ConsultationMode.CHAT -> Icons.AutoMirrored.Filled.Chat
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Join ${con.parsedMode.label}")
                        }
                    }
                }
            }
        }
    }

    activeSession?.let { con ->
        LiveConsultationRoomDialog(
            consultation = con,
            messages = activeMessages,
            currentUserId = currentUserId,
            onSendMessage = { text -> onSendMessage(con, text) },
            onEndCall = {
                onEndSession(con)
                activeSession = null
            },
            onDismiss = { activeSession = null }
        )
    }
}

@Composable
fun LiveConsultationRoomDialog(
    consultation: Consultation,
    messages: List<ConsultationMessage>,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    onEndCall: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var micMuted by remember { mutableStateOf(false) }
    var camOff by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var chatInput by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCamera = perms[Manifest.permission.CAMERA] == true || hasCamera
        hasMic = perms[Manifest.permission.RECORD_AUDIO] == true || hasMic
    }

    LaunchedEffect(consultation.id) {
        if (consultation.parsedMode != ConsultationMode.CHAT && (!hasCamera || !hasMic)) {
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
        while (true) {
            delay(1000)
            elapsedSec += 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(consultation.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Secure Room ${consultation.roomCode} • ${String.format(Locale.US, "%02d:%02d", elapsedSec / 60, elapsedSec % 60)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CareTeal
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Minimize") }
                }

                Spacer(Modifier.height(10.dp))

                if (consultation.parsedMode == ConsultationMode.VIDEO && hasCamera && !camOff) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val provider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_FRONT_CAMERA,
                                            preview
                                        )
                                    } catch (_: Exception) {
                                        // Emulator may not have front camera
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        StatusPill(
                            text = "Encrypted Video Feed • ${consultation.roomCode}",
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        )
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                ProcessCameraProvider.getInstance(context).get().unbindAll()
                            } catch (_: Exception) {
                            }
                        }
                    }
                } else if (consultation.parsedMode != ConsultationMode.CHAT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MedicalBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.PhoneInTalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (consultation.parsedMode == ConsultationMode.AUDIO) "HD Encrypted Audio Consultation"
                                else "Video Camera Off / Permission Pending",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                if (consultation.parsedMode != ConsultationMode.CHAT) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { micMuted = !micMuted },
                            modifier = Modifier.clip(CircleShape).background(MedicalBlueLight)
                        ) {
                            Icon(
                                if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Toggle Mic",
                                tint = MedicalBlue
                            )
                        }
                        if (consultation.parsedMode == ConsultationMode.VIDEO) {
                            IconButton(
                                onClick = { camOff = !camOff },
                                modifier = Modifier.clip(CircleShape).background(MedicalBlueLight)
                            ) {
                                Icon(
                                    if (camOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                                    contentDescription = "Toggle Camera",
                                    tint = MedicalBlue
                                )
                            }
                        }
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier.clip(CircleShape).background(EmergencyRed)
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "End Call", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Real-Time Doctor-Patient Chat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val mine = msg.senderId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (mine) MedicalBlue else CareTealLight,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mine) Color.White.copy(alpha = 0.8f) else CareTeal
                                    )
                                    Text(
                                        msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Type message to doctor/patient...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                onSendMessage(chatInput)
                                chatInput = ""
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(MedicalBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BillingScreen(
    invoices: List<Invoice>,
    onPayInvoice: (Invoice) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (invoices.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Billing Invoices Yet",
                    subtitle = "Invoices for OPD appointments, pharmacy orders, and online consultations appear here."
                )
            }
        } else {
            items(invoices.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { inv ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.invoiceNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${inv.parsedRefType.label} • ${inv.hospitalName}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                        }
                        StatusPill(
                            text = inv.parsedPaymentStatus.label,
                            containerColor = if (inv.parsedPaymentStatus == PaymentStatus.PAID) SuccessGreenLight else WarningAmberLight,
                            contentColor = if (inv.parsedPaymentStatus == PaymentStatus.PAID) SuccessGreen else WarningAmber
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Doctor: ${inv.doctorName.ifBlank { "CareNex Desk" }} • Date: ${sdf.format(inv.createdAt?.toDate() ?: java.util.Date())}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Consultation: INR ${inv.consultationCharges.toInt()} • Medicine: INR ${inv.medicineCharges.toInt()} • Other: INR ${inv.otherCharges.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Total: INR ${String.format(Locale.US, "%.2f", inv.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (inv.transactionId.isNotBlank()) {
                        Text(
                            "Method: ${inv.paymentMethod} • Txn ID: ${inv.transactionId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CareTeal
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (inv.parsedPaymentStatus != PaymentStatus.PAID) {
                            Button(
                                onClick = { onPayInvoice(inv) },
                                modifier = Modifier.weight(1f).testTag("pay_invoice_${inv.id}")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pay UPI")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                PdfGenerator.generateAndOpenInvoicePdf(context, inv)
                                    .onFailure { onMessage("Unable to generate PDF: ${it.localizedMessage}") }
                            },
                            modifier = Modifier.weight(1f).testTag("download_pdf_${inv.id}")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF Invoice")
                        }
                    }
                }
            }
        }
    }
}
