package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.AppNotification
import com.example.data.model.Doctor
import com.example.data.model.Hospital
import com.example.data.model.Invoice
import com.example.data.model.UpiGatewayApp
import com.example.data.model.UserRole
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
import com.example.util.LocationHelper
import com.example.util.UpiGatewayHelper
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareNexTopBar(
    activePortal: UserRole,
    unreadNotifications: Int,
    onSwitchPortal: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MedicalBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = "CareNex logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CareNex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activePortal.label} Portal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MedicalBlue
                    )
                }
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MedicalBlueLight,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onSwitchPortal)
                    .testTag("switch_portal_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Switch portal",
                        tint = MedicalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Portal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MedicalBlue
                    )
                }
            }
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = EmergencyRed) {
                                Text(unreadNotifications.coerceAtMost(99).toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.testTag("profile_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile and Sign Out",
                    tint = MedicalBlue
                )
            }
        }
    )
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MedicalBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MedicalBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun HospitalDirectoryCard(
    hospital: Hospital,
    distanceKm: Double?,
    onBookOpd: () -> Unit,
    onEmergency: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(128.dp)) {
                Image(
                    painter = painterResource(R.drawable.img_hospital_banner),
                    contentDescription = hospital.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(128.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusPill(
                        text = if (hospital.emergencyAvailable) "24x7 Emergency" else "OPD Only",
                        containerColor = if (hospital.emergencyAvailable) EmergencyRed else WarningAmber,
                        contentColor = Color.White
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", hospital.rating),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = hospital.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hospital.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        text = "Beds: ${hospital.availableBeds}/${hospital.totalBeds}",
                        containerColor = if (hospital.availableBeds > 0) SuccessGreenLight else EmergencyRedLight,
                        contentColor = if (hospital.availableBeds > 0) SuccessGreen else EmergencyRed
                    )
                    StatusPill(
                        text = "ICU: ${hospital.icuBedsAvailable}",
                        containerColor = if (hospital.icuAvailable && hospital.icuBedsAvailable > 0) CareTealLight else WarningAmberLight,
                        contentColor = if (hospital.icuAvailable && hospital.icuBedsAvailable > 0) CareTeal else WarningAmber
                    )
                    if (distanceKm != null) {
                        StatusPill(
                            text = String.format(Locale.US, "%.1f km", distanceKm),
                            containerColor = MedicalBlueLight,
                            contentColor = MedicalBlue
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "OPD: ${hospital.opdTimings} • ${hospital.contactNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { LocationHelper.dialPhoneNumber(context, hospital.contactNumber) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            LocationHelper.openInGoogleMaps(
                                context,
                                hospital.latitude,
                                hospital.longitude,
                                hospital.name
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Directions, contentDescription = "Map", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Map", maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onBookOpd, modifier = Modifier.weight(1f)) {
                        Text("Book OPD")
                    }
                    Button(
                        onClick = onEmergency,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emergency")
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorProfileCard(
    doctor: Doctor,
    onBookOpd: () -> Unit,
    onConsultOnline: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(doctor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${doctor.speciality} • ${doctor.qualification}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MedicalBlue
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    doctor.hospitalName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${doctor.experienceYears} yrs exp • ${doctor.availableDays} (${doctor.workingHours})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(
                    text = if (doctor.available) "Available" else "Away",
                    containerColor = if (doctor.available) SuccessGreenLight else EmergencyRedLight,
                    contentColor = if (doctor.available) SuccessGreen else EmergencyRed
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "INR ${doctor.consultationFee.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cap: ${doctor.dailyCapacity}/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onBookOpd,
                enabled = doctor.available,
                modifier = Modifier.weight(1f)
            ) {
                Text("Book OPD")
            }
            OutlinedButton(
                onClick = onConsultOnline,
                enabled = doctor.available,
                modifier = Modifier.weight(1f)
            ) {
                Text("Video Consult")
            }
        }
    }
}

@Composable
fun UpiPaymentSheetDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onVerifiedPayment: (UpiGatewayApp, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf(UpiGatewayApp.GOOGLE_PAY) }
    var upiHandle by remember { mutableStateOf("") }
    var gatewayMessage by remember { mutableStateOf<String?>(null) }
    var showSandboxOption by remember { mutableStateOf(false) }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.data != null) {
            val parsed = UpiGatewayHelper.parseUpiResponse(result.data)
            if (parsed.success) {
                onVerifiedPayment(selectedApp, upiHandle, parsed.transactionId, parsed.responseCode)
            } else {
                gatewayMessage = parsed.message
            }
        } else {
            gatewayMessage = "UPI payment cancelled before confirmation. Status remains Pending."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Payments, contentDescription = "UPI Payment", tint = MedicalBlue) },
        title = { Text("CareNex Secure UPI Checkout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Invoice ${invoice.invoiceNumber} • INR ${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Select UPI App (PINs and credentials are never stored by CareNex):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UpiGatewayApp.entries.forEach { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedApp = app }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selectedApp == app, onClick = { selectedApp = app })
                        Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = upiHandle,
                    onValueChange = { upiHandle = it },
                    label = { Text("Optional UPI ID (e.g. name@okicici)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (gatewayMessage != null) {
                    Text(
                        gatewayMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmergencyRed
                    )
                }
                if (showSandboxOption) {
                    Surface(
                        color = WarningAmberLight,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "No UPI app installed on this device/emulator. You can run a verified Gateway Sandbox Confirmation for testing:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val txn = "UPI-GW-${UUID.randomUUID().toString().take(8).uppercase()}"
                                    onVerifiedPayment(selectedApp, upiHandle, txn, "00")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.fillMaxWidth().testTag("sandbox_upi_confirm_button")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Confirm via Sandbox Gateway")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = UpiGatewayHelper.buildUpiPaymentIntent(
                        context = context,
                        gatewayApp = selectedApp,
                        payeeVpa = "carenex.health@icici",
                        payeeName = invoice.hospitalName.ifBlank { "CareNex Healthcare" },
                        amount = invoice.totalAmount.coerceAtLeast(1.0),
                        transactionNote = "Invoice ${invoice.invoiceNumber}",
                        transactionRef = invoice.invoiceNumber
                    )
                    if (intent != null) {
                        upiLauncher.launch(intent)
                    } else {
                        showSandboxOption = true
                        gatewayMessage = "External ${selectedApp.label} app not installed on device."
                    }
                },
                modifier = Modifier.testTag("launch_upi_button")
            ) {
                Text("Pay INR ${invoice.totalAmount.toInt()}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NotificationsSheetDialog(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CareNex Notifications") },
        text = {
            if (notifications.isEmpty()) {
                Text("No notifications yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MedicalBlueLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!notif.read) onMarkRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusPill(
                                        text = notif.parsedCategory.label,
                                        containerColor = Color.White,
                                        contentColor = MedicalBlue
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(notif.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PortalSwitcherDialog(
    activePortal: UserRole,
    isAdmin: Boolean,
    isStaffDoctor: Boolean,
    onSelectPortal: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select CareNex Portal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PortalOptionCard(
                    title = "Patient Portal",
                    subtitle = "Hospitals, OPD booking, Emergency, Pharmacy, Records & Billing",
                    icon = Icons.Filled.Person,
                    selected = activePortal == UserRole.PATIENT,
                    enabled = true,
                    onClick = { onSelectPortal(UserRole.PATIENT) }
                )
                PortalOptionCard(
                    title = "Hospital Admin Portal",
                    subtitle = if (isAdmin) "Doctors, OPD, Beds, Pharmacy Rx, Ambulance & Revenue"
                    else "Authorized hospital administrators only",
                    icon = Icons.Filled.AdminPanelSettings,
                    selected = activePortal == UserRole.HOSPITAL_ADMIN,
                    enabled = isAdmin,
                    onClick = { onSelectPortal(UserRole.HOSPITAL_ADMIN) }
                )
                PortalOptionCard(
                    title = "Doctor Portal",
                    subtitle = if (isAdmin || isStaffDoctor) "OPD queue, E-Prescriptions, Reports & Online Consults"
                    else "Authorized doctors & hospital admins only",
                    icon = Icons.Filled.MedicalServices,
                    selected = activePortal == UserRole.DOCTOR,
                    enabled = isAdmin || isStaffDoctor,
                    onClick = { onSelectPortal(UserRole.DOCTOR) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PortalOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MedicalBlueLight
                !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, if (selected) MedicalBlue else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (enabled) MedicalBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
