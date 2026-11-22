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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Ambulance
import com.example.data.model.EmergencyRequest
import com.example.data.model.EmergencyRequestType
import com.example.data.model.EmergencyStatus
import com.example.data.model.Hospital
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
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
import com.example.util.LocationHelper
import com.example.util.UserCoords
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EmergencyAndAmbulanceScreen(
    userProfile: UserProfile?,
    hospitals: List<Hospital>,
    ambulances: List<Ambulance>,
    requests: List<EmergencyRequest>,
    userCoords: UserCoords?,
    busy: Boolean,
    onRequestEmergency: (
        hospital: Hospital,
        ambulance: Ambulance?,
        pickupLocation: String,
        emergencyType: String,
        requestType: EmergencyRequestType
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedHospital by remember(hospitals) { mutableStateOf(hospitals.firstOrNull()) }
    var pickupLocation by remember(userProfile, userCoords) {
        mutableStateOf(
            userProfile?.address?.takeIf { it.isNotBlank() }
                ?: userCoords?.let { "GPS: ${String.format(Locale.US, "%.4f, %.4f", it.latitude, it.longitude)}" }
                ?: ""
        )
    }
    var emergencyType by remember { mutableStateOf("Cardiac / Trauma Emergency") }

    val availableAmbulances = remember(ambulances) { ambulances.filter { it.available } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Emergency, contentDescription = null, tint = EmergencyRed)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Real-Time Emergency & Ambulance Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Live bed, ICU & ambulance telemetry directly synced from Hospital Admin desks. Always check timestamp before dispatch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hospitals.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalHospital,
                    title = "No Live Hospital Feeds",
                    subtitle = "Hospitals appear here once configured by a Hospital Admin."
                )
            }
        } else {
            item {
                Text("Live Hospital Emergency & ICU Capacity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(hospitals, key = { it.id }) { hosp ->
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val updatedLabel = hosp.updatedAt?.toDate()?.let { "Live sync: ${sdf.format(it)}" } ?: "Sync pending"
                val dist = userCoords?.let {
                    LocationHelper.distanceKm(it.latitude, it.longitude, hosp.latitude, hosp.longitude)
                }
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hosp.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(hosp.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(updatedLabel, style = MaterialTheme.typography.labelSmall, color = CareTeal)
                        }
                        if (dist != null) {
                            StatusPill(
                                text = String.format(Locale.US, "%.1f km", dist),
                                containerColor = MedicalBlueLight,
                                contentColor = MedicalBlue
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = if (hosp.emergencyAvailable) "ER Beds: ${hosp.emergencyBedsAvailable}" else "ER Closed",
                            containerColor = if (hosp.emergencyAvailable && hosp.emergencyBedsAvailable > 0) SuccessGreenLight else EmergencyRedLight,
                            contentColor = if (hosp.emergencyAvailable && hosp.emergencyBedsAvailable > 0) SuccessGreen else EmergencyRed
                        )
                        StatusPill(
                            text = if (hosp.icuAvailable) "ICU Free: ${hosp.icuBedsAvailable}" else "ICU Full",
                            containerColor = if (hosp.icuAvailable && hosp.icuBedsAvailable > 0) CareTealLight else WarningAmberLight,
                            contentColor = if (hosp.icuAvailable && hosp.icuBedsAvailable > 0) CareTeal else WarningAmber
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { LocationHelper.dialPhoneNumber(context, hosp.contactNumber) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call")
                        }
                        OutlinedButton(
                            onClick = { LocationHelper.openInGoogleMaps(context, hosp.latitude, hosp.longitude, hosp.name) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Maps")
                        }
                        Button(
                            onClick = {
                                selectedHospital = hosp
                                if (pickupLocation.isNotBlank()) {
                                    onRequestEmergency(hosp, null, pickupLocation, emergencyType, EmergencyRequestType.EMERGENCY_BED)
                                }
                            },
                            enabled = !busy && hosp.emergencyAvailable && pickupLocation.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Book ER Bed", maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("Ambulance Booking & Dispatch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickupLocation,
                    onValueChange = { pickupLocation = it },
                    label = { Text("Patient Pickup Location *") },
                    modifier = Modifier.fillMaxWidth().testTag("ambulance_pickup_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyType,
                    onValueChange = { emergencyType = it },
                    label = { Text("Emergency Type (e.g. Cardiac, Trauma, Stroke)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Destination Hospital:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hospitals, key = { it.id }) { h ->
                        FilterChip(
                            selected = selectedHospital?.id == h.id,
                            onClick = { selectedHospital = h },
                            label = { Text(h.name) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Available Ambulances (${availableAmbulances.size}):",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(6.dp))
                if (availableAmbulances.isEmpty()) {
                    Text(
                        "No ambulances currently idle. You can still send an Emergency Bed request above or call the hospital directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmergencyRed
                    )
                } else {
                    availableAmbulances.forEach { amb ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${amb.vehicleNumber} • ${amb.parsedType.label}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("${amb.driverName} (${amb.driverPhone}) • ${amb.hospitalName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val dest = selectedHospital ?: hospitals.firstOrNull { it.id == amb.hospitalId } ?: hospitals.firstOrNull()
                                    if (dest != null && pickupLocation.isNotBlank()) {
                                        onRequestEmergency(dest, amb, pickupLocation, emergencyType, EmergencyRequestType.AMBULANCE)
                                    }
                                },
                                enabled = !busy && pickupLocation.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                modifier = Modifier.testTag("dispatch_ambulance_button")
                            ) {
                                Icon(Icons.Filled.AirportShuttle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Dispatch")
                            }
                        }
                    }
                }
            }
        }

        if (requests.isNotEmpty()) {
            item {
                Text("Your Active Emergency & Ambulance Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(requests.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { req ->
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(req.parsedRequestType.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        val st = req.parsedStatus
                        StatusPill(
                            text = "${st.label} • ETA ${req.etaMinutes}m",
                            containerColor = if (st == EmergencyStatus.CANCELLED) EmergencyRedLight else WarningAmberLight,
                            contentColor = if (st == EmergencyStatus.CANCELLED) EmergencyRed else WarningAmber
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Hospital: ${req.hospitalName}", style = MaterialTheme.typography.bodySmall)
                    Text("Pickup: ${req.pickupLocation}", style = MaterialTheme.typography.bodySmall)
                    if (req.driverName.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Driver: ${req.driverName} (${req.driverPhone})", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                            OutlinedButton(onClick = { LocationHelper.dialPhoneNumber(context, req.driverPhone) }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call driver", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call Driver")
                            }
                        }
                    }
                }
            }
        }
    }
}
