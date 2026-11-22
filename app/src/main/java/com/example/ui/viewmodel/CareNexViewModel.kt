package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Admission
import com.example.data.model.Ambulance
import com.example.data.model.AppNotification
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BedInventory
import com.example.data.model.CartItem
import com.example.data.model.Consultation
import com.example.data.model.ConsultationMessage
import com.example.data.model.ConsultationMode
import com.example.data.model.ConsultationStatus
import com.example.data.model.Doctor
import com.example.data.model.DoctorSlot
import com.example.data.model.EmergencyRequest
import com.example.data.model.EmergencyRequestType
import com.example.data.model.EmergencyStatus
import com.example.data.model.Hospital
import com.example.data.model.Invoice
import com.example.data.model.MedicalReport
import com.example.data.model.Medicine
import com.example.data.model.OrderStatus
import com.example.data.model.Payment
import com.example.data.model.PharmacyOrder
import com.example.data.model.Prescription
import com.example.data.model.ReportType
import com.example.data.model.UpiGatewayApp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.WardType
import com.example.data.repository.CareNexClinicalRepository
import com.example.data.repository.CareNexDirectoryRepository
import com.example.util.UserCoords
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class CareNexViewModel(
    private val directoryRepo: CareNexDirectoryRepository,
    private val clinicalRepo: CareNexClinicalRepository,
    val currentUserId: String,
    val currentUserEmail: String
) : ViewModel() {

    private companion object {
        const val TAG = "CareNexVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _activePortal = MutableStateFlow(UserRole.PATIENT)
    val activePortal: StateFlow<UserRole> = _activePortal.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _userCoords = MutableStateFlow<UserCoords?>(null)
    val userCoords: StateFlow<UserCoords?> = _userCoords.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Live slot tracking inputs
    private val _selectedSlotDoctorId = MutableStateFlow("")
    private val _selectedSlotDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
    val selectedSlotDate: StateFlow<String> = _selectedSlotDate.asStateFlow()

    // Live consultation chat room ID
    private val _activeConsultationId = MutableStateFlow("")

    val userProfile: StateFlow<UiState<UserProfile?>> =
        directoryRepo.observeUserProfile(currentUserId).asUiState("profile")

    val isAdmin: StateFlow<Boolean> =
        directoryRepo.observeIsAdminDoc(currentUserId)
            .catch { emit(false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val hospitals: StateFlow<UiState<List<Hospital>>> =
        directoryRepo.observeHospitals().asUiState("hospitals")

    val doctors: StateFlow<UiState<List<Doctor>>> =
        directoryRepo.observeDoctors().asUiState("doctors")

    val beds: StateFlow<UiState<List<BedInventory>>> =
        directoryRepo.observeBeds().asUiState("bed inventory")

    val medicines: StateFlow<UiState<List<Medicine>>> =
        clinicalRepo.observeMedicines().asUiState("medicines")

    val ambulances: StateFlow<UiState<List<Ambulance>>> =
        clinicalRepo.observeAmbulances().asUiState("ambulances")

    val notifications: StateFlow<UiState<List<AppNotification>>> =
        clinicalRepo.observeNotifications(currentUserId).asUiState("notifications")

    val selectedDoctorSlot: StateFlow<DoctorSlot?> =
        combine(_selectedSlotDoctorId, _selectedSlotDate) { docId, date -> docId to date }
            .flatMapLatest { (docId, date) ->
                if (docId.isBlank() || date.isBlank()) flowOf(null)
                else directoryRepo.observeDoctorSlot(docId, date)
            }
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val patientAppointments: StateFlow<UiState<List<Appointment>>> =
        directoryRepo.observePatientAppointments(currentUserId).asUiState("appointments")

    val doctorAppointments: StateFlow<UiState<List<Appointment>>> =
        directoryRepo.observeDoctorAppointments(currentUserId).asUiState("doctor appointments")

    val adminAppointments: StateFlow<UiState<List<Appointment>>> =
        directoryRepo.observeAdminAppointments(currentUserId).asUiState("admin appointments")

    val adminAdmissions: StateFlow<UiState<List<Admission>>> =
        directoryRepo.observeAdmissions(currentUserId, patientOnly = false).asUiState("admissions")

    val patientPrescriptions: StateFlow<UiState<List<Prescription>>> =
        clinicalRepo.observePrescriptions(forDoctor = false).asUiState("prescriptions")

    val doctorPrescriptions: StateFlow<UiState<List<Prescription>>> =
        clinicalRepo.observePrescriptions(forDoctor = true).asUiState("doctor prescriptions")

    val patientReports: StateFlow<UiState<List<MedicalReport>>> =
        clinicalRepo.observeMedicalReports(forDoctor = false).asUiState("medical reports")

    val doctorAuthorizedReports: StateFlow<UiState<List<MedicalReport>>> =
        clinicalRepo.observeMedicalReports(forDoctor = true).asUiState("authorized reports")

    val patientPharmacyOrders: StateFlow<UiState<List<PharmacyOrder>>> =
        clinicalRepo.observePharmacyOrders(forAdmin = false).asUiState("pharmacy orders")

    val adminPharmacyOrders: StateFlow<UiState<List<PharmacyOrder>>> =
        clinicalRepo.observePharmacyOrders(forAdmin = true).asUiState("admin pharmacy orders")

    val patientEmergencyRequests: StateFlow<UiState<List<EmergencyRequest>>> =
        clinicalRepo.observeEmergencyRequests(forAdmin = false).asUiState("emergency requests")

    val adminEmergencyRequests: StateFlow<UiState<List<EmergencyRequest>>> =
        clinicalRepo.observeEmergencyRequests(forAdmin = true).asUiState("admin emergency requests")

    val patientConsultations: StateFlow<UiState<List<Consultation>>> =
        clinicalRepo.observeConsultations(forDoctor = false).asUiState("consultations")

    val doctorConsultations: StateFlow<UiState<List<Consultation>>> =
        clinicalRepo.observeConsultations(forDoctor = true).asUiState("doctor consultations")

    val consultationMessages: StateFlow<List<ConsultationMessage>> =
        _activeConsultationId
            .flatMapLatest { id ->
                if (id.isBlank()) flowOf(emptyList())
                else clinicalRepo.observeConsultationMessages(id)
            }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val patientInvoices: StateFlow<UiState<List<Invoice>>> =
        clinicalRepo.observeInvoices(forAdmin = false).asUiState("invoices")

    val adminInvoices: StateFlow<UiState<List<Invoice>>> =
        clinicalRepo.observeInvoices(forAdmin = true).asUiState("admin invoices")

    val adminUsers: StateFlow<UiState<List<UserProfile>>> =
        directoryRepo.observeAllUsersForAdmin().asUiState("users")

    val adminPayments: StateFlow<UiState<List<Payment>>> =
        clinicalRepo.observePayments(forAdmin = true).asUiState("payments")

    fun setActivePortal(role: UserRole) {
        _activePortal.value = role
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun updateUserCoords(coords: UserCoords?) {
        _userCoords.value = coords
    }

    fun selectSlotWatch(doctorId: String, date: String) {
        _selectedSlotDoctorId.value = doctorId
        _selectedSlotDate.value = date
    }

    fun setActiveConsultationChat(consultationId: String) {
        _activeConsultationId.value = consultationId
    }

    // --- CART ACTIONS ---

    fun addToCart(medicine: Medicine) {
        val current = _cart.value.toMutableList()
        val idx = current.indexOfFirst { it.medicine.id == medicine.id }
        if (idx >= 0) {
            val item = current[idx]
            if (item.quantity < medicine.stockQuantity) {
                current[idx] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            current.add(CartItem(medicine, 1))
        }
        _cart.value = current
        _statusMessage.value = "Added ${medicine.name} to cart"
    }

    fun updateCartQuantity(medicineId: String, delta: Int) {
        val current = _cart.value.toMutableList()
        val idx = current.indexOfFirst { it.medicine.id == medicineId }
        if (idx >= 0) {
            val nextQty = current[idx].quantity + delta
            if (nextQty <= 0) current.removeAt(idx)
            else current[idx] = current[idx].copy(quantity = nextQty)
            _cart.value = current
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // --- PROFILE & PORTAL ---

    fun saveUserProfile(
        fullName: String,
        mobile: String,
        email: String,
        age: Int,
        gender: String,
        address: String,
        bloodGroup: String,
        emergencyContact: String,
        photoUri: String,
        existing: UserProfile?,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.saveUserProfile(
                fullName, mobile, email, age, gender, address, bloodGroup, emergencyContact, photoUri, existing
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Profile saved securely."
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save profile"
                onFailure()
            }
        }
    }

    fun initializeSampleHospitalNetwork() {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.seedInitialHospitalNetworkForAdmin()
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Hospital network, doctors, beds, ambulances & pharmacy initialized."
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Initialization requires Hospital Admin permission."
            }
        }
    }

    // --- APPOINTMENTS ---

    fun bookOpdAppointment(
        patientName: String,
        patientPhone: String,
        hospital: Hospital,
        doctor: Doctor,
        date: String,
        timeSlot: String,
        symptoms: String,
        onBooked: (Appointment) -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.bookAppointmentAtomic(
                patientName, patientPhone, hospital, doctor, date, timeSlot, symptoms
            )
            _busy.value = false
            res.onSuccess { appt ->
                _statusMessage.value = "Confirmed! Token #${appt.serialNumber} at ${appt.timeSlot}."
                onBooked(appt)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Booking failed"
            }
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.cancelAppointmentAtomic(appointment)
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Appointment cancelled and slot released."
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to cancel appointment"
            }
        }
    }

    fun updateAppointmentStatus(appointmentId: String, status: AppointmentStatus) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.updateAppointmentStatus(appointmentId, status)
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Appointment marked ${status.label}"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update status"
            }
        }
    }

    // --- ADMIN HOSPITALS, DOCTORS, BEDS ---

    fun saveHospital(
        hospitalId: String?,
        name: String,
        address: String,
        contactNumber: String,
        opdTimings: String,
        emergencyAvailable: Boolean,
        icuAvailable: Boolean,
        totalBeds: Int,
        availableBeds: Int,
        icuBedsTotal: Int,
        icuBedsAvailable: Int,
        emergencyBedsTotal: Int,
        emergencyBedsAvailable: Int,
        rating: Double,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.saveHospital(
                hospitalId, name, address, contactNumber, opdTimings,
                emergencyAvailable, icuAvailable, totalBeds, availableBeds,
                icuBedsTotal, icuBedsAvailable, emergencyBedsTotal, emergencyBedsAvailable,
                rating, latitude, longitude
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Hospital saved"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save hospital"
            }
        }
    }

    fun saveDoctor(
        doctorId: String?,
        hospital: Hospital,
        staffUid: String,
        name: String,
        qualification: String,
        speciality: String,
        experienceYears: Int,
        consultationFee: Double,
        availableDays: String,
        workingHours: String,
        dailyCapacity: Int,
        available: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.saveDoctor(
                doctorId, hospital.id, hospital.name, staffUid, name,
                qualification, speciality, experienceYears, consultationFee,
                availableDays, workingHours, dailyCapacity, available
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Doctor saved"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save doctor"
            }
        }
    }

    fun deleteDoctor(doctorId: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.deleteDoctor(doctorId)
            _busy.value = false
            res.onSuccess { _statusMessage.value = "Doctor removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun updateDoctorAvailability(doctorId: String, available: Boolean, workingHours: String) {
        viewModelScope.launch {
            val res = directoryRepo.updateDoctorAvailability(doctorId, available, workingHours)
            res.onSuccess { _statusMessage.value = "Availability updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun addBed(hospital: Hospital, wardType: WardType, roomNumber: String, bedNumber: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.addBedToInventory(hospital, wardType, roomNumber, bedNumber)
            _busy.value = false
            res.onSuccess { _statusMessage.value = "Bed $bedNumber added to ${wardType.label}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add bed" }
        }
    }

    fun admitPatient(
        bed: BedInventory,
        patientId: String,
        patientName: String,
        patientPhone: String,
        diagnosis: String,
        admittingDoctorName: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.admitPatientAtomic(
                bed, patientId, patientName, patientPhone, diagnosis, admittingDoctorName
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Patient admitted to Room ${bed.roomNumber}, Bed ${bed.bedNumber}"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Admission failed"
            }
        }
    }

    fun dischargePatient(admission: Admission, notes: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = directoryRepo.dischargePatientAtomic(admission, notes)
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "${admission.patientName} discharged; bed ${admission.bedNumber} is now vacant"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Discharge failed"
            }
        }
    }

    // --- PRESCRIPTIONS & MEDICAL REPORTS ---

    fun createPrescription(
        patientId: String,
        patientName: String,
        doctor: Doctor,
        appointmentId: String,
        diagnosis: String,
        medications: List<String>,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.createPrescription(
                patientId, patientName, doctor, appointmentId, diagnosis, medications, notes
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Prescription issued & verified for pharmacy"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to issue prescription"
            }
        }
    }

    fun createMedicalReport(
        patientId: String,
        patientName: String,
        doctor: Doctor?,
        title: String,
        reportType: ReportType,
        summary: String,
        findings: String,
        attachmentUri: String,
        authorizedDoctorUids: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.createMedicalReport(
                patientId, patientName, doctor, title, reportType, summary, findings, attachmentUri, authorizedDoctorUids
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Medical report saved"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to upload report"
            }
        }
    }

    fun updateReportDoctorAccess(reportId: String, authorizedDoctorUids: List<String>) {
        viewModelScope.launch {
            val res = clinicalRepo.updateReportDoctorAccess(reportId, authorizedDoctorUids)
            res.onSuccess { _statusMessage.value = "Doctor access authorization updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update access" }
        }
    }

    // --- PHARMACY ---

    fun saveMedicine(
        medicineId: String?,
        name: String,
        dosage: String,
        timing: String,
        price: Double,
        stockQuantity: Int,
        requiresPrescription: Boolean,
        category: String,
        description: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.saveMedicine(
                medicineId, name, dosage, timing, price, stockQuantity, requiresPrescription, category, description
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Medicine catalog updated"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save medicine"
            }
        }
    }

    fun checkoutPharmacyCart(
        patientName: String,
        deliveryAddress: String,
        prescriptionRefId: String,
        prescriptionAttachmentUri: String,
        onOrderCreated: (PharmacyOrder, Invoice) -> Unit = { _, _ -> }
    ) {
        val items = _cart.value
        if (items.isEmpty()) {
            _statusMessage.value = "Your pharmacy cart is empty"
            return
        }
        val requiresRx = items.any { it.medicine.requiresPrescription }
        val total = items.sumOf { it.medicine.price * it.quantity }
        val summary = items.joinToString(", ") { "${it.medicine.name} x${it.quantity}" }
        val ids = items.map { it.medicine.id }
        val adminOwner = items.firstOrNull()?.medicine?.adminUid ?: currentUserId

        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.placePharmacyOrder(
                patientName = patientName,
                deliveryAddress = deliveryAddress,
                itemsSummary = summary,
                medicineIds = ids,
                totalAmount = total,
                requiresPrescription = requiresRx,
                prescriptionRefId = prescriptionRefId,
                prescriptionAttachmentUri = prescriptionAttachmentUri,
                adminUid = adminOwner
            )
            _busy.value = false
            res.onSuccess { (order, invoice) ->
                clearCart()
                _statusMessage.value = if (requiresRx) {
                    "Order placed! Awaiting pharmacist Rx verification."
                } else {
                    "Order placed! Complete payment to dispatch."
                }
                onOrderCreated(order, invoice)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Order checkout failed"
            }
        }
    }

    fun updatePharmacyOrderByAdmin(
        order: PharmacyOrder,
        pharmacistVerified: Boolean,
        newStatus: OrderStatus
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.updatePharmacyOrderStatusByAdmin(order, pharmacistVerified, newStatus)
            _busy.value = false
            res.onSuccess { _statusMessage.value = "Order updated to ${newStatus.label}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update order" }
        }
    }

    // --- AMBULANCE & EMERGENCY ---

    fun addAmbulance(
        hospital: Hospital,
        vehicleNumber: String,
        driverName: String,
        driverPhone: String,
        vehicleType: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.addAmbulance(hospital, vehicleNumber, driverName, driverPhone, vehicleType)
            _busy.value = false
            res.onSuccess { _statusMessage.value = "Ambulance $vehicleNumber added" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add ambulance" }
        }
    }

    fun requestEmergencyOrAmbulance(
        patientName: String,
        patientPhone: String,
        hospital: Hospital,
        ambulance: Ambulance?,
        pickupLocation: String,
        emergencyType: String,
        requestType: EmergencyRequestType,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.requestEmergencyOrAmbulanceAtomic(
                patientName, patientPhone, hospital, ambulance, pickupLocation, emergencyType, requestType
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "${requestType.label} sent to ${hospital.name}"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Emergency request failed"
            }
        }
    }

    fun updateEmergencyStatus(request: EmergencyRequest, newStatus: EmergencyStatus, etaMinutes: Int) {
        viewModelScope.launch {
            val res = clinicalRepo.updateEmergencyRequestStatus(request, newStatus, etaMinutes)
            res.onSuccess { _statusMessage.value = "Emergency status: ${newStatus.label}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update request" }
        }
    }

    // --- ONLINE CONSULTATIONS & CHAT ---

    fun bookConsultation(
        patientName: String,
        doctor: Doctor,
        mode: ConsultationMode,
        scheduledTime: String,
        onBooked: (Consultation, Invoice) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.bookConsultation(patientName, doctor, mode, scheduledTime)
            _busy.value = false
            res.onSuccess { (consultation, invoice) ->
                _statusMessage.value = "${mode.label} booked with Dr. ${doctor.name}"
                onBooked(consultation, invoice)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to book consultation"
            }
        }
    }

    fun updateConsultationStatus(consultationId: String, status: ConsultationStatus) {
        viewModelScope.launch {
            val res = clinicalRepo.updateConsultationStatus(consultationId, status)
            res.onSuccess { _statusMessage.value = "Consultation ${status.label}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun sendConsultationMessage(consultation: Consultation, senderName: String, senderRole: String, text: String) {
        viewModelScope.launch {
            val res = clinicalRepo.sendConsultationMessage(consultation, senderName, senderRole, text)
            res.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to send message"
            }
        }
    }

    // --- BILLING, VERIFIED PAYMENTS & NOTIFICATIONS ---

    fun createCustomInvoice(
        patientId: String,
        patientName: String,
        hospital: Hospital,
        doctorName: String,
        consultationCharges: Double,
        medicineCharges: Double,
        otherCharges: Double
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.createCustomInvoice(
                patientId, patientName, hospital, doctorName, consultationCharges, medicineCharges, otherCharges
            )
            _busy.value = false
            res.onSuccess { _statusMessage.value = "Invoice generated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to generate invoice" }
        }
    }

    fun confirmVerifiedUpiPayment(
        invoice: Invoice,
        gatewayApp: UpiGatewayApp,
        upiHandle: String,
        transactionId: String,
        responseCode: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = clinicalRepo.recordVerifiedPaymentAtomic(
                invoice, gatewayApp, upiHandle, transactionId, responseCode
            )
            _busy.value = false
            res.onSuccess {
                _statusMessage.value = "Payment verified! Invoice ${invoice.invoiceNumber} marked Paid."
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Payment verification failed"
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            clinicalRepo.markNotificationRead(notificationId)
        }
    }
}
