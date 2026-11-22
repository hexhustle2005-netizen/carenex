package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Ambulance
import com.example.data.model.AppNotification
import com.example.data.model.Consultation
import com.example.data.model.ConsultationMessage
import com.example.data.model.ConsultationMode
import com.example.data.model.ConsultationStatus
import com.example.data.model.Doctor
import com.example.data.model.EmergencyRequest
import com.example.data.model.EmergencyRequestType
import com.example.data.model.EmergencyStatus
import com.example.data.model.Hospital
import com.example.data.model.Invoice
import com.example.data.model.InvoiceRefType
import com.example.data.model.MedicalReport
import com.example.data.model.Medicine
import com.example.data.model.NotificationCategory
import com.example.data.model.OrderStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.Prescription
import com.example.data.model.ReportType
import com.example.data.model.UpiGatewayApp
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CareNexClinicalRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals("aditya808052@gmail.com", ignoreCase = true) && user.isEmailVerified
    }

    private fun <T> Flow<T>.guardStream(uid: String, op: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- PRESCRIPTIONS & MEDICAL REPORTS ---

    fun observePrescriptions(forDoctor: Boolean): Flow<List<Prescription>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            isBootstrappedAdmin() && forDoctor -> db.collection("prescriptions")
            forDoctor -> db.collection("prescriptions").whereEqualTo("doctorUid", uid)
            else -> db.collection("prescriptions").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Prescription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "prescriptions")
        )
    }

    fun observeMedicalReports(forDoctor: Boolean): Flow<List<MedicalReport>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            isBootstrappedAdmin() && forDoctor -> db.collection("medical_reports")
            forDoctor -> db.collection("medical_reports").whereArrayContains("authorizedDoctorUids", uid)
            else -> db.collection("medical_reports").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(MedicalReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "medical_reports")
        )
    }

    suspend fun createPrescription(
        patientId: String,
        patientName: String,
        doctor: Doctor,
        appointmentId: String,
        diagnosis: String,
        medications: List<String>,
        notes: String
    ): Result<Unit> {
        val rxRef = db.collection("prescriptions").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            val cleanMeds = medications.map { it.trim() }.filter { it.isNotEmpty() }.take(30)
            require(cleanMeds.isNotEmpty()) { "At least one medication is required" }
            require(diagnosis.trim().isNotEmpty()) { "Diagnosis is required" }

            val batch = db.batch()
            batch.set(
                rxRef,
                mapOf(
                    "patientId" to patientId.ifBlank { uid },
                    "patientName" to patientName.trim().take(120),
                    "doctorId" to doctor.id,
                    "doctorName" to doctor.name.take(120),
                    "doctorUid" to doctor.staffUid.ifBlank { uid },
                    "hospitalId" to doctor.hospitalId,
                    "hospitalName" to doctor.hospitalName.take(160),
                    "appointmentId" to appointmentId,
                    "diagnosis" to diagnosis.trim().take(400),
                    "medications" to cleanMeds,
                    "notes" to notes.trim().take(600),
                    "verifiedForPharmacy" to true,
                    "adminUid" to doctor.adminUid.ifBlank { uid },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to patientId.ifBlank { uid },
                    "title" to "New Digital Prescription",
                    "message" to "Dr. ${doctor.name} issued a prescription for $diagnosis.",
                    "category" to NotificationCategory.REPORT.name,
                    "read" to false,
                    "referenceId" to rxRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, rxRef.path)
            Result.failure(e)
        }
    }

    suspend fun createMedicalReport(
        patientId: String,
        patientName: String,
        doctor: Doctor?,
        title: String,
        reportType: ReportType,
        summary: String,
        findings: String,
        attachmentUri: String,
        authorizedDoctorUids: List<String>
    ): Result<Unit> {
        val repRef = db.collection("medical_reports").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            require(title.trim().length >= 2) { "Report title must be at least 2 characters" }
            require(summary.trim().isNotEmpty()) { "Summary is required" }

            val targetPatientId = patientId.ifBlank { uid }
            val authDoctors = (authorizedDoctorUids + listOfNotNull(doctor?.staffUid?.takeIf { it.isNotBlank() }))
                .distinct()
                .take(20)

            val batch = db.batch()
            batch.set(
                repRef,
                mapOf(
                    "patientId" to targetPatientId,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "doctorId" to (doctor?.id ?: ""),
                    "doctorName" to (doctor?.name ?: "Self Uploaded").take(120),
                    "doctorUid" to (doctor?.staffUid ?: ""),
                    "hospitalId" to (doctor?.hospitalId ?: ""),
                    "title" to title.trim().take(160),
                    "reportType" to reportType.name,
                    "summary" to summary.trim().take(600),
                    "findings" to findings.trim().take(1000),
                    "attachmentUri" to attachmentUri.take(500),
                    "authorizedDoctorUids" to authDoctors,
                    "adminUid" to (doctor?.adminUid?.ifBlank { uid } ?: uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to targetPatientId,
                    "title" to "Medical Report Uploaded",
                    "message" to "$title (${reportType.label}) is now available in Medical Records.",
                    "category" to NotificationCategory.REPORT.name,
                    "read" to false,
                    "referenceId" to repRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, repRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReportDoctorAccess(reportId: String, authorizedDoctorUids: List<String>): Result<Unit> {
        val path = "medical_reports/$reportId"
        return try {
            requireUserId()
            db.collection("medical_reports").document(reportId).update(
                mapOf(
                    "authorizedDoctorUids" to authorizedDoctorUids.distinct().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PHARMACY & MEDICINE ORDERS ---

    fun observeMedicines(): Flow<List<Medicine>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("medicines")
                .snapshots()
                .map { it.toObjects(Medicine::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "medicines")
        )
    }

    suspend fun saveMedicine(
        medicineId: String?,
        name: String,
        dosage: String,
        timing: String,
        price: Double,
        stockQuantity: Int,
        requiresPrescription: Boolean,
        category: String,
        description: String
    ): Result<Unit> {
        var path = "medicines"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Medicine name is required" }
            val docRef = if (medicineId.isNullOrBlank()) {
                db.collection("medicines").document()
            } else {
                db.collection("medicines").document(medicineId)
            }
            path = docRef.path
            if (medicineId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "name" to name.trim().take(160),
                        "dosage" to dosage.trim().take(80),
                        "timing" to timing.trim().take(120),
                        "price" to price.coerceIn(0.0, 100000.0),
                        "stockQuantity" to stockQuantity.coerceIn(0, 100000),
                        "requiresPrescription" to requiresPrescription,
                        "category" to category.trim().take(80),
                        "description" to description.trim().take(500),
                        "imageKey" to "pharmacy",
                        "adminUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(160),
                        "dosage" to dosage.trim().take(80),
                        "timing" to timing.trim().take(120),
                        "price" to price.coerceIn(0.0, 100000.0),
                        "stockQuantity" to stockQuantity.coerceIn(0, 100000),
                        "requiresPrescription" to requiresPrescription,
                        "category" to category.trim().take(80),
                        "description" to description.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observePharmacyOrders(forAdmin: Boolean): Flow<List<PharmacyOrder>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            forAdmin && isBootstrappedAdmin() -> db.collection("pharmacy_orders")
            forAdmin -> db.collection("pharmacy_orders").whereEqualTo("adminUid", uid)
            else -> db.collection("pharmacy_orders").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "pharmacy_orders")
        )
    }

    suspend fun placePharmacyOrder(
        patientName: String,
        deliveryAddress: String,
        itemsSummary: String,
        medicineIds: List<String>,
        totalAmount: Double,
        requiresPrescription: Boolean,
        prescriptionRefId: String,
        prescriptionAttachmentUri: String,
        adminUid: String
    ): Result<Pair<PharmacyOrder, Invoice>> {
        val orderRef = db.collection("pharmacy_orders").document()
        val invoiceRef = db.collection("invoices").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            require(deliveryAddress.trim().length >= 3) { "Delivery address is required" }
            require(medicineIds.isNotEmpty()) { "Cart cannot be empty" }
            if (requiresPrescription && prescriptionRefId.isBlank() && prescriptionAttachmentUri.isBlank()) {
                throw IllegalArgumentException("Prescription upload or verified doctor Rx is required for Rx medicines.")
            }

            val initialStatus = if (requiresPrescription) {
                OrderStatus.AWAITING_VERIFICATION.name
            } else {
                OrderStatus.VERIFIED.name
            }
            val ownerAdmin = adminUid.ifBlank { uid }
            val invNumber = "CNX-RX-${System.currentTimeMillis().toString().takeLast(6)}"

            val batch = db.batch()
            batch.set(
                orderRef,
                mapOf(
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "deliveryAddress" to deliveryAddress.trim().take(300),
                    "itemsSummary" to itemsSummary.trim().take(500),
                    "medicineIds" to medicineIds.take(30),
                    "totalAmount" to totalAmount.coerceAtLeast(0.0),
                    "requiresPrescription" to requiresPrescription,
                    "prescriptionRefId" to prescriptionRefId,
                    "prescriptionAttachmentUri" to prescriptionAttachmentUri.take(500),
                    "pharmacistVerified" to false,
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "orderStatus" to initialStatus,
                    "transactionId" to "",
                    "adminUid" to ownerAdmin,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                invoiceRef,
                mapOf(
                    "invoiceNumber" to invNumber,
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "hospitalId" to "",
                    "hospitalName" to "CareNex Online Pharmacy",
                    "doctorName" to "Pharmacist Verification Desk",
                    "referenceType" to InvoiceRefType.PHARMACY.name,
                    "referenceId" to orderRef.id,
                    "consultationCharges" to 0.0,
                    "medicineCharges" to totalAmount,
                    "otherCharges" to 0.0,
                    "totalAmount" to totalAmount,
                    "paymentMethod" to "UPI",
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "transactionId" to "",
                    "adminUid" to ownerAdmin,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "title" to if (requiresPrescription) "Rx Order Awaiting Pharmacist Check" else "Medicine Order Placed",
                    "message" to "$itemsSummary • Total INR ${totalAmount.toInt()}",
                    "category" to NotificationCategory.PHARMACY.name,
                    "read" to false,
                    "referenceId" to orderRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val createdOrder = PharmacyOrder(
                id = orderRef.id,
                patientId = uid,
                patientName = patientName,
                deliveryAddress = deliveryAddress,
                itemsSummary = itemsSummary,
                medicineIds = medicineIds,
                totalAmount = totalAmount,
                requiresPrescription = requiresPrescription,
                prescriptionRefId = prescriptionRefId,
                prescriptionAttachmentUri = prescriptionAttachmentUri,
                pharmacistVerified = false,
                paymentStatus = PaymentStatus.PENDING.name,
                orderStatus = initialStatus,
                adminUid = ownerAdmin
            )
            val createdInvoice = Invoice(
                id = invoiceRef.id,
                invoiceNumber = invNumber,
                patientId = uid,
                patientName = patientName,
                hospitalName = "CareNex Online Pharmacy",
                doctorName = "Pharmacist Verification Desk",
                referenceType = InvoiceRefType.PHARMACY.name,
                referenceId = orderRef.id,
                medicineCharges = totalAmount,
                totalAmount = totalAmount,
                paymentMethod = "UPI",
                paymentStatus = PaymentStatus.PENDING.name,
                adminUid = ownerAdmin
            )
            Result.success(createdOrder to createdInvoice)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePharmacyOrderStatusByAdmin(
        order: PharmacyOrder,
        pharmacistVerified: Boolean,
        newStatus: OrderStatus
    ): Result<Unit> {
        val orderRef = db.collection("pharmacy_orders").document(order.id)
        val notifRef = db.collection("notifications").document()
        return try {
            requireUserId()
            if (order.requiresPrescription &&
                newStatus != OrderStatus.AWAITING_VERIFICATION &&
                newStatus != OrderStatus.REJECTED &&
                !pharmacistVerified
            ) {
                throw IllegalStateException("Pharmacist verification is mandatory before processing Rx medicines.")
            }
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "pharmacistVerified" to pharmacistVerified,
                    "orderStatus" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to order.patientId,
                    "title" to "Pharmacy Order: ${newStatus.label}",
                    "message" to "${order.itemsSummary} is now ${newStatus.label}.",
                    "category" to NotificationCategory.PHARMACY.name,
                    "read" to false,
                    "referenceId" to order.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    // --- AMBULANCES & EMERGENCY ---

    fun observeAmbulances(): Flow<List<Ambulance>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("ambulances")
                .snapshots()
                .map { it.toObjects(Ambulance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "ambulances")
        )
    }

    suspend fun addAmbulance(
        hospital: Hospital,
        vehicleNumber: String,
        driverName: String,
        driverPhone: String,
        vehicleType: String
    ): Result<Unit> {
        val ambRef = db.collection("ambulances").document()
        return try {
            val uid = requireUserId()
            ambRef.set(
                mapOf(
                    "hospitalId" to hospital.id,
                    "hospitalName" to hospital.name,
                    "vehicleNumber" to vehicleNumber.trim().take(40),
                    "driverName" to driverName.trim().take(120),
                    "driverPhone" to driverPhone.trim().take(30),
                    "vehicleType" to vehicleType,
                    "available" to true,
                    "currentBookingId" to "",
                    "adminUid" to hospital.adminUid.ifBlank { uid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ambRef.path)
            Result.failure(e)
        }
    }

    fun observeEmergencyRequests(forAdmin: Boolean): Flow<List<EmergencyRequest>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            forAdmin && isBootstrappedAdmin() -> db.collection("emergency_requests")
            forAdmin -> db.collection("emergency_requests").whereEqualTo("adminUid", uid)
            else -> db.collection("emergency_requests").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(EmergencyRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "emergency_requests")
        )
    }

    suspend fun requestEmergencyOrAmbulanceAtomic(
        patientName: String,
        patientPhone: String,
        hospital: Hospital,
        ambulance: Ambulance?,
        pickupLocation: String,
        emergencyType: String,
        requestType: EmergencyRequestType
    ): Result<Unit> {
        val reqRef = db.collection("emergency_requests").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            require(pickupLocation.trim().length >= 2) { "Pickup location is required" }

            db.runTransaction { tx ->
                var assignedDriverName = ambulance?.driverName ?: ""
                var assignedDriverPhone = ambulance?.driverPhone ?: hospital.contactNumber
                var assignedAmbId = ""
                if (ambulance != null) {
                    val ambRef = db.collection("ambulances").document(ambulance.id)
                    val ambSnap = tx.get(ambRef)
                    if (ambSnap.getBoolean("available") != true) {
                        throw IllegalStateException("Selected ambulance ${ambulance.vehicleNumber} is no longer available.")
                    }
                    assignedAmbId = ambulance.id
                    assignedDriverName = ambSnap.getString("driverName") ?: ambulance.driverName
                    assignedDriverPhone = ambSnap.getString("driverPhone") ?: ambulance.driverPhone
                    tx.update(
                        ambRef,
                        mapOf(
                            "available" to false,
                            "currentBookingId" to reqRef.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    reqRef,
                    mapOf(
                        "patientId" to uid,
                        "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                        "patientPhone" to patientPhone.trim().take(30),
                        "hospitalId" to hospital.id,
                        "hospitalName" to hospital.name,
                        "ambulanceId" to assignedAmbId,
                        "driverName" to assignedDriverName,
                        "driverPhone" to assignedDriverPhone,
                        "pickupLocation" to pickupLocation.trim().take(300),
                        "emergencyType" to emergencyType.trim().ifBlank { "General Medical Emergency" }.take(120),
                        "requestType" to requestType.name,
                        "etaMinutes" to 10,
                        "status" to EmergencyStatus.REQUESTED.name,
                        "adminUid" to hospital.adminUid.ifBlank { uid },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "${requestType.label} Sent",
                        "message" to "Request sent to ${hospital.name}. Contact: $assignedDriverPhone",
                        "category" to if (requestType == EmergencyRequestType.AMBULANCE) NotificationCategory.AMBULANCE.name else NotificationCategory.EMERGENCY.name,
                        "read" to false,
                        "referenceId" to reqRef.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateEmergencyRequestStatus(
        request: EmergencyRequest,
        newStatus: EmergencyStatus,
        etaMinutes: Int
    ): Result<Unit> {
        val reqRef = db.collection("emergency_requests").document(request.id)
        val notifRef = db.collection("notifications").document()
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                reqRef,
                mapOf(
                    "status" to newStatus.name,
                    "etaMinutes" to etaMinutes.coerceIn(0, 240),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if ((newStatus == EmergencyStatus.COMPLETED || newStatus == EmergencyStatus.CANCELLED) &&
                request.ambulanceId.isNotBlank()
            ) {
                batch.update(
                    db.collection("ambulances").document(request.ambulanceId),
                    mapOf(
                        "available" to true,
                        "currentBookingId" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                notifRef,
                mapOf(
                    "userId" to request.patientId,
                    "title" to "Ambulance / Emergency Update",
                    "message" to "Status: ${newStatus.label} • ETA: ${etaMinutes} min (${request.hospitalName})",
                    "category" to NotificationCategory.AMBULANCE.name,
                    "read" to false,
                    "referenceId" to request.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    // --- ONLINE CONSULTATIONS & REAL-TIME CHAT ---

    fun observeConsultations(forDoctor: Boolean): Flow<List<Consultation>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            forDoctor && isBootstrappedAdmin() -> db.collection("consultations")
            forDoctor -> db.collection("consultations").whereEqualTo("doctorUid", uid)
            else -> db.collection("consultations").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Consultation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "consultations")
        )
    }

    suspend fun bookConsultation(
        patientName: String,
        doctor: Doctor,
        mode: ConsultationMode,
        scheduledTime: String
    ): Result<Pair<Consultation, Invoice>> {
        val conRef = db.collection("consultations").document()
        val invRef = db.collection("invoices").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            val roomCode = "CNX-${UUID.randomUUID().toString().take(8).uppercase()}"
            val invNum = "CNX-CON-${System.currentTimeMillis().toString().takeLast(6)}"
            val adminOwner = doctor.adminUid.ifBlank { uid }
            val docUid = doctor.staffUid.ifBlank { adminOwner }

            val batch = db.batch()
            batch.set(
                conRef,
                mapOf(
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "doctorId" to doctor.id,
                    "doctorName" to doctor.name.take(120),
                    "doctorUid" to docUid,
                    "hospitalId" to doctor.hospitalId,
                    "hospitalName" to doctor.hospitalName.take(160),
                    "mode" to mode.name,
                    "scheduledTime" to scheduledTime.trim().take(80),
                    "fee" to doctor.consultationFee,
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "status" to ConsultationStatus.SCHEDULED.name,
                    "roomCode" to roomCode,
                    "adminUid" to adminOwner,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                invRef,
                mapOf(
                    "invoiceNumber" to invNum,
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "hospitalId" to doctor.hospitalId,
                    "hospitalName" to doctor.hospitalName.take(160),
                    "doctorName" to doctor.name.take(120),
                    "referenceType" to InvoiceRefType.CONSULTATION.name,
                    "referenceId" to conRef.id,
                    "consultationCharges" to doctor.consultationFee,
                    "medicineCharges" to 0.0,
                    "otherCharges" to 0.0,
                    "totalAmount" to doctor.consultationFee,
                    "paymentMethod" to "UPI",
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "transactionId" to "",
                    "adminUid" to adminOwner,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "title" to "${mode.label} Scheduled",
                    "message" to "Online consultation with Dr. ${doctor.name} ($scheduledTime).",
                    "category" to NotificationCategory.APPOINTMENT.name,
                    "read" to false,
                    "referenceId" to conRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val consultation = Consultation(
                id = conRef.id,
                patientId = uid,
                patientName = patientName,
                doctorId = doctor.id,
                doctorName = doctor.name,
                doctorUid = docUid,
                hospitalId = doctor.hospitalId,
                hospitalName = doctor.hospitalName,
                mode = mode.name,
                scheduledTime = scheduledTime,
                fee = doctor.consultationFee,
                paymentStatus = PaymentStatus.PENDING.name,
                status = ConsultationStatus.SCHEDULED.name,
                roomCode = roomCode,
                adminUid = adminOwner
            )
            val invoice = Invoice(
                id = invRef.id,
                invoiceNumber = invNum,
                patientId = uid,
                patientName = patientName,
                hospitalId = doctor.hospitalId,
                hospitalName = doctor.hospitalName,
                doctorName = doctor.name,
                referenceType = InvoiceRefType.CONSULTATION.name,
                referenceId = conRef.id,
                consultationCharges = doctor.consultationFee,
                totalAmount = doctor.consultationFee,
                paymentMethod = "UPI",
                paymentStatus = PaymentStatus.PENDING.name,
                adminUid = adminOwner
            )
            Result.success(consultation to invoice)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, conRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateConsultationStatus(consultationId: String, status: ConsultationStatus): Result<Unit> {
        val path = "consultations/$consultationId"
        return try {
            requireUserId()
            db.collection("consultations").document(consultationId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeConsultationMessages(consultationId: String): Flow<List<ConsultationMessage>> = flow {
        if (consultationId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val path = "consultations/$consultationId/messages"
        emitAll(
            db.collection("consultations").document(consultationId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ConsultationMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .guardStream(uid, OperationType.LIST, path)
        )
    }

    suspend fun sendConsultationMessage(
        consultation: Consultation,
        senderName: String,
        senderRole: String,
        text: String
    ): Result<Unit> {
        val msgRef = db.collection("consultations").document(consultation.id)
            .collection("messages").document()
        return try {
            val uid = requireUserId()
            require(text.trim().isNotEmpty()) { "Message cannot be empty" }
            msgRef.set(
                mapOf(
                    "consultationId" to consultation.id,
                    "senderId" to uid,
                    "senderName" to senderName.trim().ifBlank { "CareNex User" }.take(120),
                    "senderRole" to senderRole,
                    "patientId" to consultation.patientId,
                    "doctorUid" to consultation.doctorUid,
                    "adminUid" to consultation.adminUid.ifBlank { uid },
                    "text" to text.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    // --- BILLING, VERIFIED UPI PAYMENTS & NOTIFICATIONS ---

    fun observeInvoices(forAdmin: Boolean): Flow<List<Invoice>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            forAdmin && isBootstrappedAdmin() -> db.collection("invoices")
            forAdmin -> db.collection("invoices").whereEqualTo("adminUid", uid)
            else -> db.collection("invoices").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Invoice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "invoices")
        )
    }

    fun observePayments(forAdmin: Boolean): Flow<List<Payment>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            forAdmin && isBootstrappedAdmin() -> db.collection("payments")
            forAdmin -> db.collection("payments").whereEqualTo("adminUid", uid)
            else -> db.collection("payments").whereEqualTo("patientId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Payment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "payments")
        )
    }

    suspend fun createCustomInvoice(
        patientId: String,
        patientName: String,
        hospital: Hospital,
        doctorName: String,
        consultationCharges: Double,
        medicineCharges: Double,
        otherCharges: Double
    ): Result<Unit> {
        val invRef = db.collection("invoices").document()
        return try {
            val uid = requireUserId()
            val total = (consultationCharges + medicineCharges + otherCharges).coerceAtLeast(0.0)
            val invNum = "CNX-${System.currentTimeMillis().toString().takeLast(6)}"
            invRef.set(
                mapOf(
                    "invoiceNumber" to invNum,
                    "patientId" to patientId.ifBlank { uid },
                    "patientName" to patientName.trim().take(120),
                    "hospitalId" to hospital.id,
                    "hospitalName" to hospital.name.take(160),
                    "doctorName" to doctorName.trim().take(120),
                    "referenceType" to InvoiceRefType.ADMISSION.name,
                    "referenceId" to invNum,
                    "consultationCharges" to consultationCharges.coerceAtLeast(0.0),
                    "medicineCharges" to medicineCharges.coerceAtLeast(0.0),
                    "otherCharges" to otherCharges.coerceAtLeast(0.0),
                    "totalAmount" to total,
                    "paymentMethod" to "UPI",
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "transactionId" to "",
                    "adminUid" to hospital.adminUid.ifBlank { uid },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, invRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordVerifiedPaymentAtomic(
        invoice: Invoice,
        gatewayApp: UpiGatewayApp,
        upiHandle: String,
        verifiedTransactionId: String,
        responseCode: String
    ): Result<Unit> {
        val invRef = db.collection("invoices").document(invoice.id)
        val payRef = db.collection("payments").document()
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            require(verifiedTransactionId.length >= 6) { "Verified gateway transaction ID is required" }

            db.runTransaction { tx ->
                val invSnap = tx.get(invRef)
                if (!invSnap.exists()) throw IllegalStateException("Invoice not found")

                tx.update(
                    invRef,
                    mapOf(
                        "paymentMethod" to gatewayApp.label,
                        "paymentStatus" to PaymentStatus.PAID.name,
                        "transactionId" to verifiedTransactionId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (invoice.referenceType == InvoiceRefType.PHARMACY.name && invoice.referenceId.isNotBlank()) {
                    val orderRef = db.collection("pharmacy_orders").document(invoice.referenceId)
                    val orderSnap = tx.get(orderRef)
                    if (orderSnap.exists()) {
                        val reqRx = orderSnap.getBoolean("requiresPrescription") == true
                        val verifiedRx = orderSnap.getBoolean("pharmacistVerified") == true
                        if (!reqRx || verifiedRx) {
                            tx.update(
                                orderRef,
                                mapOf(
                                    "paymentStatus" to PaymentStatus.PAID.name,
                                    "transactionId" to verifiedTransactionId,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                } else if (invoice.referenceType == InvoiceRefType.CONSULTATION.name && invoice.referenceId.isNotBlank()) {
                    val conRef = db.collection("consultations").document(invoice.referenceId)
                    val conSnap = tx.get(conRef)
                    if (conSnap.exists()) {
                        tx.update(
                            conRef,
                            mapOf(
                                "paymentStatus" to PaymentStatus.PAID.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                tx.set(
                    payRef,
                    mapOf(
                        "invoiceId" to invoice.id,
                        "patientId" to invoice.patientId.ifBlank { uid },
                        "amount" to invoice.totalAmount.coerceAtLeast(1.0),
                        "gatewayApp" to gatewayApp.name,
                        "upiHandle" to upiHandle.trim().take(100),
                        "transactionId" to verifiedTransactionId,
                        "gatewayResponseCode" to responseCode.take(40),
                        "status" to "VERIFIED",
                        "adminUid" to invoice.adminUid.ifBlank { uid },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to invoice.patientId.ifBlank { uid },
                        "title" to "Payment Successful (${invoice.invoiceNumber})",
                        "message" to "INR ${invoice.totalAmount.toInt()} paid via ${gatewayApp.label}. Txn: $verifiedTransactionId",
                        "category" to NotificationCategory.PAYMENT.name,
                        "read" to false,
                        "referenceId" to invoice.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, invRef.path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        emitAll(
            db.collection("notifications")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .guardStream(uid, OperationType.LIST, "notifications")
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
