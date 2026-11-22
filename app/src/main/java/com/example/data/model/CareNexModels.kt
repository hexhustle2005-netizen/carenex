package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}

enum class UserRole(val wireValue: String, val label: String) {
    PATIENT("patient", "Patient"),
    DOCTOR("doctor", "Doctor"),
    HOSPITAL_ADMIN("hospital_admin", "Hospital Admin");

    companion object {
        fun fromWire(raw: String?): UserRole =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: PATIENT
    }
}

enum class AppointmentStatus(val label: String) {
    CONFIRMED("Confirmed"),
    IN_CONSULTATION("In Consultation"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class WardType(val label: String) {
    GENERAL("General Ward"),
    ICU("ICU"),
    EMERGENCY("Emergency"),
    PRIVATE("Private Suite")
}

enum class AdmissionStatus(val label: String) {
    ADMITTED("Admitted"),
    DISCHARGED("Discharged")
}

enum class ReportType(val label: String) {
    BLOOD_TEST("Blood Test"),
    XRAY("X-Ray"),
    SCAN("CT / MRI Scan"),
    UPLOADED_DOC("Uploaded Document"),
    HISTORY("Medical History")
}

enum class OrderStatus(val label: String) {
    AWAITING_VERIFICATION("Awaiting Rx Verification"),
    VERIFIED("Verified"),
    PACKED("Packed"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    REJECTED("Rejected")
}

enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    PAID("Paid"),
    FAILED("Failed")
}

enum class AmbulanceType(val label: String) {
    ALS("Advanced Life Support"),
    BLS("Basic Life Support"),
    ICU_MOBILE("Mobile ICU")
}

enum class EmergencyRequestType(val label: String) {
    AMBULANCE("Ambulance Dispatch"),
    EMERGENCY_BED("Emergency Bed")
}

enum class EmergencyStatus(val label: String) {
    REQUESTED("Requested"),
    DISPATCHED("Dispatched"),
    EN_ROUTE("En Route"),
    ARRIVED("Arrived"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class ConsultationMode(val label: String) {
    VIDEO("Video Call"),
    AUDIO("Audio Call"),
    CHAT("Live Chat")
}

enum class ConsultationStatus(val label: String) {
    SCHEDULED("Scheduled"),
    LIVE("Live Now"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class InvoiceRefType(val label: String) {
    APPOINTMENT("OPD Appointment"),
    PHARMACY("Pharmacy Order"),
    CONSULTATION("Online Consultation"),
    ADMISSION("Inpatient Admission")
}

enum class UpiGatewayApp(val label: String, val packageName: String?) {
    GOOGLE_PAY("Google Pay", "com.google.android.apps.nbu.paisa.user"),
    PHONEPE("PhonePe", "com.phonepe.app"),
    UPI_INTENT("Any UPI App", null)
}

enum class NotificationCategory(val label: String) {
    APPOINTMENT("Appointment"),
    PAYMENT("Payment"),
    PHARMACY("Pharmacy"),
    REPORT("Medical Report"),
    AMBULANCE("Ambulance"),
    EMERGENCY("Emergency")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val age: Long = 0L,
    val gender: String = "Prefer not to say",
    val address: String = "",
    val bloodGroup: String = "O+",
    val emergencyContact: String = "",
    val photoUri: String = "",
    val role: String = "patient",
    val hospitalId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedRole: UserRole get() = UserRole.fromWire(role)
}

@Keep
@IgnoreExtraProperties
data class Hospital(
    @DocumentId val id: String = "",
    val name: String = "",
    val imageKey: String = "hospital_main",
    val address: String = "",
    val contactNumber: String = "",
    val opdTimings: String = "08:00 AM - 08:00 PM",
    val emergencyAvailable: Boolean = true,
    val icuAvailable: Boolean = true,
    val totalBeds: Long = 0L,
    val availableBeds: Long = 0L,
    val occupiedBeds: Long = 0L,
    val icuBedsTotal: Long = 0L,
    val icuBedsAvailable: Long = 0L,
    val emergencyBedsTotal: Long = 0L,
    val emergencyBedsAvailable: Long = 0L,
    val rating: Double = 4.6,
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Doctor(
    @DocumentId val id: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val staffUid: String = "",
    val name: String = "",
    val qualification: String = "",
    val speciality: String = "",
    val experienceYears: Long = 0L,
    val consultationFee: Double = 500.0,
    val availableDays: String = "Mon - Sat",
    val workingHours: String = "09:00 AM - 05:00 PM",
    val dailyCapacity: Long = 40L,
    val available: Boolean = true,
    val rating: Double = 4.8,
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DoctorSlot(
    @DocumentId val id: String = "",
    val doctorId: String = "",
    val hospitalId: String = "",
    val date: String = "",
    val dailyCapacity: Long = 40L,
    val bookedCount: Long = 0L,
    val availableCount: Long = 40L,
    val bookedSlots: List<String> = emptyList(),
    val adminUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Appointment(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorUid: String = "",
    val speciality: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val serialNumber: Long = 1L,
    val status: String = AppointmentStatus.CONFIRMED.name,
    val consultationFee: Double = 0.0,
    val symptoms: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: AppointmentStatus get() = enumOrDefault(status, AppointmentStatus.CONFIRMED)
}

@Keep
@IgnoreExtraProperties
data class BedInventory(
    @DocumentId val id: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val wardType: String = WardType.GENERAL.name,
    val roomNumber: String = "",
    val bedNumber: String = "",
    val occupied: Boolean = false,
    val currentAdmissionId: String = "",
    val adminUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedWard: WardType get() = enumOrDefault(wardType, WardType.GENERAL)
}

@Keep
@IgnoreExtraProperties
data class Admission(
    @DocumentId val id: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val bedId: String = "",
    val roomNumber: String = "",
    val bedNumber: String = "",
    val wardType: String = WardType.GENERAL.name,
    val patientId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val diagnosis: String = "",
    val admittingDoctorName: String = "",
    val status: String = AdmissionStatus.ADMITTED.name,
    val dischargeNotes: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: AdmissionStatus get() = enumOrDefault(status, AdmissionStatus.ADMITTED)
}

@Keep
@IgnoreExtraProperties
data class Prescription(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorUid: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val appointmentId: String = "",
    val diagnosis: String = "",
    val medications: List<String> = emptyList(),
    val notes: String = "",
    val verifiedForPharmacy: Boolean = true,
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicalReport(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorUid: String = "",
    val hospitalId: String = "",
    val title: String = "",
    val reportType: String = ReportType.BLOOD_TEST.name,
    val summary: String = "",
    val findings: String = "",
    val attachmentUri: String = "",
    val authorizedDoctorUids: List<String> = emptyList(),
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: ReportType get() = enumOrDefault(reportType, ReportType.BLOOD_TEST)
}

@Keep
@IgnoreExtraProperties
data class Medicine(
    @DocumentId val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val timing: String = "",
    val price: Double = 0.0,
    val stockQuantity: Long = 0L,
    val requiresPrescription: Boolean = false,
    val category: String = "",
    val description: String = "",
    val imageKey: String = "pharmacy",
    val hospitalId: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PharmacyOrder(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val deliveryAddress: String = "",
    val itemsSummary: String = "",
    val medicineIds: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val requiresPrescription: Boolean = false,
    val prescriptionRefId: String = "",
    val prescriptionAttachmentUri: String = "",
    val pharmacistVerified: Boolean = false,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val orderStatus: String = OrderStatus.AWAITING_VERIFICATION.name,
    val transactionId: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedOrderStatus: OrderStatus get() = enumOrDefault(orderStatus, OrderStatus.AWAITING_VERIFICATION)
    val parsedPaymentStatus: PaymentStatus get() = enumOrDefault(paymentStatus, PaymentStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class Ambulance(
    @DocumentId val id: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val vehicleNumber: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val vehicleType: String = AmbulanceType.ALS.name,
    val available: Boolean = true,
    val currentBookingId: String = "",
    val adminUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: AmbulanceType get() = enumOrDefault(vehicleType, AmbulanceType.ALS)
}

@Keep
@IgnoreExtraProperties
data class EmergencyRequest(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val ambulanceId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val pickupLocation: String = "",
    val emergencyType: String = "",
    val requestType: String = EmergencyRequestType.AMBULANCE.name,
    val etaMinutes: Long = 12L,
    val status: String = EmergencyStatus.REQUESTED.name,
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedRequestType: EmergencyRequestType get() = enumOrDefault(requestType, EmergencyRequestType.AMBULANCE)
    val parsedStatus: EmergencyStatus get() = enumOrDefault(status, EmergencyStatus.REQUESTED)
}

@Keep
@IgnoreExtraProperties
data class Consultation(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorUid: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val mode: String = ConsultationMode.VIDEO.name,
    val scheduledTime: String = "",
    val fee: Double = 0.0,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val status: String = ConsultationStatus.SCHEDULED.name,
    val roomCode: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedMode: ConsultationMode get() = enumOrDefault(mode, ConsultationMode.VIDEO)
    val parsedPaymentStatus: PaymentStatus get() = enumOrDefault(paymentStatus, PaymentStatus.PENDING)
    val parsedStatus: ConsultationStatus get() = enumOrDefault(status, ConsultationStatus.SCHEDULED)
}

@Keep
@IgnoreExtraProperties
data class ConsultationMessage(
    @DocumentId val id: String = "",
    val consultationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "patient",
    val patientId: String = "",
    val doctorUid: String = "",
    val adminUid: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val invoiceNumber: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val doctorName: String = "",
    val referenceType: String = InvoiceRefType.APPOINTMENT.name,
    val referenceId: String = "",
    val consultationCharges: Double = 0.0,
    val medicineCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "UPI",
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val transactionId: String = "",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedPaymentStatus: PaymentStatus get() = enumOrDefault(paymentStatus, PaymentStatus.PENDING)
    val parsedRefType: InvoiceRefType get() = enumOrDefault(referenceType, InvoiceRefType.APPOINTMENT)
}

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val invoiceId: String = "",
    val patientId: String = "",
    val amount: Double = 0.0,
    val gatewayApp: String = UpiGatewayApp.UPI_INTENT.name,
    val upiHandle: String = "",
    val transactionId: String = "",
    val gatewayResponseCode: String = "",
    val status: String = "VERIFIED",
    val adminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = NotificationCategory.APPOINTMENT.name,
    val read: Boolean = false,
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val parsedCategory: NotificationCategory get() = enumOrDefault(category, NotificationCategory.APPOINTMENT)
}

data class CartItem(
    val medicine: Medicine,
    val quantity: Int
)
