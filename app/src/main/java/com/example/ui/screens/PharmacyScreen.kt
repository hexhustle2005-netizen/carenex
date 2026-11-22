package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Invoice
import com.example.data.model.Medicine
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.Prescription
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

@Composable
fun PharmacyScreen(
    userProfile: UserProfile?,
    medicines: List<Medicine>,
    cart: List<CartItem>,
    prescriptions: List<Prescription>,
    orders: List<PharmacyOrder>,
    invoices: List<Invoice>,
    busy: Boolean,
    onAddToCart: (Medicine) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onCheckoutCart: (
        deliveryAddress: String,
        prescriptionRefId: String,
        prescriptionAttachmentUri: String
    ) -> Unit,
    onPayOrderInvoice: (Invoice) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showCartDialog by remember { mutableStateOf(false) }

    val filteredMeds = remember(medicines, search) {
        if (search.isBlank()) medicines
        else medicines.filter {
            it.name.contains(search, true) || it.category.contains(search, true)
        }
    }
    val cartTotal = cart.sumOf { it.medicine.price * it.quantity }
    val cartCount = cart.sumOf { it.quantity }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Medicines (${medicines.size})") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("My Orders (${orders.size})") })
        }

        if (tabIndex == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Image(
                        painter = painterResource(R.drawable.img_pharmacy_care),
                        contentDescription = "CareNex Pharmacy",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search medicines, dosage...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showCartDialog = true },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.height(54.dp).testTag("open_cart_button")
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$cartCount")
                        }
                    }
                }

                if (filteredMeds.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.LocalPharmacy,
                            title = "Pharmacy Catalog Empty",
                            subtitle = "Medicines appear once added by Hospital Admin."
                        )
                    }
                } else {
                    items(filteredMeds, key = { it.id }) { med ->
                        val inCart = cart.firstOrNull { it.medicine.id == med.id }?.quantity ?: 0
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${med.dosage} • ${med.category}", style = MaterialTheme.typography.bodySmall, color = MedicalBlue)
                                    Text("Timing: ${med.timing}", style = MaterialTheme.typography.bodySmall)
                                    if (med.description.isNotBlank()) {
                                        Text(med.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("INR ${med.price.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    StatusPill(
                                        text = if (med.requiresPrescription) "Rx Required" else "OTC",
                                        containerColor = if (med.requiresPrescription) WarningAmberLight else SuccessGreenLight,
                                        contentColor = if (med.requiresPrescription) WarningAmber else SuccessGreen
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("In Stock: ${med.stockQuantity}", style = MaterialTheme.typography.bodySmall, color = CareTeal)
                                if (inCart == 0) {
                                    Button(
                                        onClick = { onAddToCart(med) },
                                        enabled = med.stockQuantity > 0,
                                        modifier = Modifier.testTag("add_to_cart_${med.id}")
                                    ) {
                                        Text("Add to Cart")
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onUpdateCartQty(med.id, -1) }) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                        }
                                        Text("$inCart", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { onUpdateCartQty(med.id, 1) }) {
                                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (orders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.LocalPharmacy,
                            title = "No Pharmacy Orders",
                            subtitle = "Order medicines from the catalog to track delivery and verification."
                        )
                    }
                } else {
                    items(orders.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { order ->
                        val matchingInvoice = invoices.firstOrNull { it.referenceId == order.id }
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(order.itemsSummary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusPill(
                                    text = order.parsedOrderStatus.label,
                                    containerColor = when (order.parsedOrderStatus) {
                                        OrderStatus.DELIVERED -> SuccessGreenLight
                                        OrderStatus.REJECTED -> EmergencyRedLight
                                        OrderStatus.AWAITING_VERIFICATION -> WarningAmberLight
                                        else -> MedicalBlueLight
                                    },
                                    contentColor = when (order.parsedOrderStatus) {
                                        OrderStatus.DELIVERED -> SuccessGreen
                                        OrderStatus.REJECTED -> EmergencyRed
                                        OrderStatus.AWAITING_VERIFICATION -> WarningAmber
                                        else -> MedicalBlue
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Delivery: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                            Text("Total: INR ${order.totalAmount.toInt()} • Payment: ${order.parsedPaymentStatus.label}", style = MaterialTheme.typography.bodySmall, color = CareTeal)
                            if (order.requiresPrescription) {
                                Text(
                                    if (order.pharmacistVerified) "Pharmacist Verified: Yes"
                                    else "Pharmacist Verification Pending (Rx items cannot ship until verified)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (order.pharmacistVerified) SuccessGreen else WarningAmber
                                )
                            }
                            if (order.parsedPaymentStatus == PaymentStatus.PENDING &&
                                matchingInvoice != null &&
                                (!order.requiresPrescription || order.pharmacistVerified)
                            ) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onPayOrderInvoice(matchingInvoice) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Pay via UPI (INR ${matchingInvoice.totalAmount.toInt()})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCartDialog) {
        PharmacyCheckoutDialog(
            cart = cart,
            cartTotal = cartTotal,
            defaultAddress = userProfile?.address ?: "",
            prescriptions = prescriptions,
            busy = busy,
            onDismiss = { showCartDialog = false },
            onConfirmOrder = { addr, rxId, rxUri ->
                onCheckoutCart(addr, rxId, rxUri)
                showCartDialog = false
                tabIndex = 1
            }
        )
    }
}

@Composable
private fun PharmacyCheckoutDialog(
    cart: List<CartItem>,
    cartTotal: Double,
    defaultAddress: String,
    prescriptions: List<Prescription>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirmOrder: (String, String, String) -> Unit
) {
    val requiresRx = cart.any { it.medicine.requiresPrescription }
    var address by remember { mutableStateOf(defaultAddress) }
    var selectedRxId by remember { mutableStateOf(prescriptions.firstOrNull()?.id ?: "") }
    var uploadedRxUri by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val rxPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedRxUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pharmacy Checkout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cart.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.medicine.name} x${item.quantity}", style = MaterialTheme.typography.bodySmall)
                        Text("INR ${(item.medicine.price * item.quantity).toInt()}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Total: INR ${cartTotal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Delivery Address *") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (requiresRx) {
                    Text(
                        "Prescription Required for Rx medicines in cart:",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarningAmber
                    )
                    if (prescriptions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(prescriptions, key = { it.id }) { rx ->
                                FilterChip(
                                    selected = selectedRxId == rx.id,
                                    onClick = { selectedRxId = rx.id },
                                    label = { Text("${rx.doctorName}: ${rx.diagnosis}") }
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            rxPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (uploadedRxUri.isBlank()) "Upload Rx Photo (Photo Picker)" else "Rx Photo Attached")
                    }
                }
                if (errorText != null) {
                    Text(errorText!!, color = EmergencyRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    when {
                        address.trim().length < 3 -> errorText = "Please enter a valid delivery address."
                        requiresRx && selectedRxId.isBlank() && uploadedRxUri.isBlank() -> {
                            errorText = "Select a digital prescription or upload an Rx image."
                        }
                        else -> onConfirmOrder(address, selectedRxId, uploadedRxUri)
                    }
                },
                modifier = Modifier.testTag("place_pharmacy_order_button")
            ) {
                Text("Place Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
