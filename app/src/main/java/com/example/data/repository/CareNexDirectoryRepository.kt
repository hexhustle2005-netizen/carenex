package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Admission
import com.example.data.model.AdmissionStatus
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BedInventory
import com.example.data.model.Doctor
import com.example.data.model.DoctorSlot
import com.example.data.model.Hospital
import com.example.data.model.InvoiceRefType
import com.example.data.model.NotificationCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.WardType
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

class CareNexDirectoryRepository(
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

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .guardStream(uid, OperationType.GET, path)
        )
    }

    fun observeIsAdminDoc(userId: String): Flow<Boolean> = flow {
        if (userId.isBlank()) {
            emit(false)
            return@flow
        }
        val uid = requireUserId()
        if (isBootstrappedAdmin()) {
            emit(true)
            return@flow
        }
        val path = "admins/$userId"
        emitAll(
            db.collection("admins").document(userId)
                .snapshots()
                .map { it.exists() }
                .guardStream(uid, OperationType.GET, path)
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        if (!isBootstrappedAdmin()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users")
                .snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "users")
        )
    }

    suspend fun saveUserProfile(
        fullName: String,
        mobile: String,
        email: String,
        age: Int,
        gender: String,
        address: String,
        bloodGroup: String,
        emergencyContact: String,
        photoUri: String,
        existingProfile: UserProfile?
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(fullName.trim().isNotEmpty()) { "Full name is required" }
            require(email.trim().length >= 3) { "Valid email is required" }
            val docRef = db.collection("users").document(uid)
            path = docRef.path

            if (existingProfile == null) {
                val defaultRole = if (isBootstrappedAdmin()) UserRole.HOSPITAL_ADMIN.wireValue else UserRole.PATIENT.wireValue
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to fullName.trim().take(120),
                    "mobile" to mobile.trim().take(30),
                    "email" to email.trim().take(200),
                    "age" to age.coerceIn(0, 130),
                    "gender" to gender.trim().take(30),
                    "address" to address.trim().take(300),
                    "bloodGroup" to bloodGroup.trim().take(16),
                    "emergencyContact" to emergencyContact.trim().take(60),
                    "photoUri" to photoUri.take(500),
                    "role" to defaultRole,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "fullName" to fullName.trim().take(120),
                    "mobile" to mobile.trim().take(30),
                    "email" to email.trim().take(200),
                    "age" to age.coerceIn(0, 130),
                    "gender" to gender.trim().take(30),
                    "address" to address.trim().take(300),
                    "bloodGroup" to bloodGroup.trim().take(16),
                    "emergencyContact" to emergencyContact.trim().take(60),
                    "photoUri" to photoUri.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- HOSPITALS ---

    fun observeHospitals(): Flow<List<Hospital>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("hospitals")
                .snapshots()
                .map { it.toObjects(Hospital::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "hospitals")
        )
    }

    suspend fun saveHospital(
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
        longitude: Double
    ): Result<String> {
        var path = "hospitals"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Hospital name must be at least 2 characters" }
            val safeTotal = totalBeds.coerceAtLeast(0)
            val safeAvail = availableBeds.coerceIn(0, safeTotal)
            val safeOcc = (safeTotal - safeAvail).coerceAtLeast(0)

            val docRef = if (hospitalId.isNullOrBlank()) {
                db.collection("hospitals").document()
            } else {
                db.collection("hospitals").document(hospitalId)
            }
            path = docRef.path

            if (hospitalId.isNullOrBlank()) {
                val payload = mapOf(
                    "name" to name.trim().take(160),
                    "imageKey" to "hospital_main",
                    "address" to address.trim().take(300),
                    "contactNumber" to contactNumber.trim().take(40),
                    "opdTimings" to opdTimings.trim().take(120),
                    "emergencyAvailable" to emergencyAvailable,
                    "icuAvailable" to icuAvailable,
                    "totalBeds" to safeTotal,
                    "availableBeds" to safeAvail,
                    "occupiedBeds" to safeOcc,
                    "icuBedsTotal" to icuBedsTotal.coerceAtLeast(0),
                    "icuBedsAvailable" to icuBedsAvailable.coerceIn(0, icuBedsTotal.coerceAtLeast(0)),
                    "emergencyBedsTotal" to emergencyBedsTotal.coerceAtLeast(0),
                    "emergencyBedsAvailable" to emergencyBedsAvailable.coerceIn(0, emergencyBedsTotal.coerceAtLeast(0)),
                    "rating" to rating.coerceIn(0.0, 5.0),
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(160),
                        "address" to address.trim().take(300),
                        "contactNumber" to contactNumber.trim().take(40),
                        "opdTimings" to opdTimings.trim().take(120),
                        "emergencyAvailable" to emergencyAvailable,
                        "icuAvailable" to icuAvailable,
                        "totalBeds" to safeTotal,
                        "availableBeds" to safeAvail,
                        "occupiedBeds" to safeOcc,
                        "icuBedsTotal" to icuBedsTotal.coerceAtLeast(0),
                        "icuBedsAvailable" to icuBedsAvailable.coerceIn(0, icuBedsTotal.coerceAtLeast(0)),
                        "emergencyBedsTotal" to emergencyBedsTotal.coerceAtLeast(0),
                        "emergencyBedsAvailable" to emergencyBedsAvailable.coerceIn(0, emergencyBedsTotal.coerceAtLeast(0)),
                        "rating" to rating.coerceIn(0.0, 5.0),
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- DOCTORS ---

    fun observeDoctors(): Flow<List<Doctor>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("doctors")
                .snapshots()
                .map { it.toObjects(Doctor::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "doctors")
        )
    }

    suspend fun saveDoctor(
        doctorId: String?,
        hospitalId: String,
        hospitalName: String,
        staffUid: String,
        name: String,
        qualification: String,
        speciality: String,
        experienceYears: Int,
        consultationFee: Double,
        availableDays: String,
        workingHours: String,
        dailyCapacity: Int,
        available: Boolean
    ): Result<String> {
        var path = "doctors"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Doctor name is required" }
            require(hospitalId.isNotBlank()) { "Select a hospital" }
            val docRef = if (doctorId.isNullOrBlank()) {
                db.collection("doctors").document()
            } else {
                db.collection("doctors").document(doctorId)
            }
            path = docRef.path

            if (doctorId.isNullOrBlank()) {
                val payload = mapOf(
                    "hospitalId" to hospitalId,
                    "hospitalName" to hospitalName.take(160),
                    "staffUid" to staffUid.trim().ifBlank { uid },
                    "name" to name.trim().take(120),
                    "qualification" to qualification.trim().take(120),
                    "speciality" to speciality.trim().take(100),
                    "experienceYears" to experienceYears.coerceIn(0, 80),
                    "consultationFee" to consultationFee.coerceIn(0.0, 100000.0),
                    "availableDays" to availableDays.trim().take(120),
                    "workingHours" to workingHours.trim().take(120),
                    "dailyCapacity" to dailyCapacity.coerceIn(1, 500),
                    "available" to available,
                    "rating" to 4.8,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "hospitalId" to hospitalId,
                        "hospitalName" to hospitalName.take(160),
                        "staffUid" to staffUid.trim().ifBlank { uid },
                        "name" to name.trim().take(120),
                        "qualification" to qualification.trim().take(120),
                        "speciality" to speciality.trim().take(100),
                        "experienceYears" to experienceYears.coerceIn(0, 80),
                        "consultationFee" to consultationFee.coerceIn(0.0, 100000.0),
                        "availableDays" to availableDays.trim().take(120),
                        "workingHours" to workingHours.trim().take(120),
                        "dailyCapacity" to dailyCapacity.coerceIn(1, 500),
                        "available" to available,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDoctor(doctorId: String): Result<Unit> {
        val path = "doctors/$doctorId"
        return try {
            requireUserId()
            require(doctorId.isNotBlank())
            db.collection("doctors").document(doctorId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDoctorAvailability(doctorId: String, available: Boolean, workingHours: String): Result<Unit> {
        val path = "doctors/$doctorId"
        return try {
            requireUserId()
            db.collection("doctors").document(doctorId).update(
                mapOf(
                    "available" to available,
                    "workingHours" to workingHours.trim().take(120),
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

    // --- SLOTS & ATOMIC OPD APPOINTMENT BOOKING ---

    fun observeDoctorSlot(doctorId: String, date: String): Flow<DoctorSlot?> = flow {
        if (doctorId.isBlank() || date.isBlank()) {
            emit(null)
            return@flow
        }
        val uid = requireUserId()
        val slotId = "${doctorId}_${date.replace("-", "_")}"
        val path = "doctor_slots/$slotId"
        emitAll(
            db.collection("doctor_slots").document(slotId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(DoctorSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .guardStream(uid, OperationType.GET, path)
        )
    }

    fun observePatientAppointments(patientId: String): Flow<List<Appointment>> = flow {
        if (patientId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        emitAll(
            db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .snapshots()
                .map { it.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "appointments")
        )
    }

    fun observeDoctorAppointments(doctorUid: String): Flow<List<Appointment>> = flow {
        if (doctorUid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val query: Query = if (isBootstrappedAdmin()) {
            db.collection("appointments")
        } else {
            db.collection("appointments").whereEqualTo("doctorUid", doctorUid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "appointments")
        )
    }

    fun observeAdminAppointments(adminUid: String): Flow<List<Appointment>> = flow {
        if (adminUid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val query: Query = if (isBootstrappedAdmin()) {
            db.collection("appointments")
        } else {
            db.collection("appointments").whereEqualTo("adminUid", adminUid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "appointments")
        )
    }

    suspend fun bookAppointmentAtomic(
        patientName: String,
        patientPhone: String,
        hospital: Hospital,
        doctor: Doctor,
        date: String,
        timeSlot: String,
        symptoms: String
    ): Result<Appointment> {
        val slotId = "${doctor.id}_${date.replace("-", "_")}"
        var path = "appointments"
        return try {
            val uid = requireUserId()
            require(timeSlot.isNotBlank()) { "Please select a time slot" }

            val slotRef = db.collection("doctor_slots").document(slotId)
            val apptRef = db.collection("appointments").document()
            val invoiceRef = db.collection("invoices").document()
            val notifRef = db.collection("notifications").document()
            path = apptRef.path

            val createdSerial = db.runTransaction { tx ->
                val slotSnap = tx.get(slotRef)
                val capacity = doctor.dailyCapacity.coerceIn(1L, 500L)
                val currentBooked: Long
                val bookedSlots: List<String>

                if (slotSnap.exists()) {
                    currentBooked = slotSnap.getLong("bookedCount") ?: 0L
                    @Suppress("UNCHECKED_CAST")
                    bookedSlots = (slotSnap.get("bookedSlots") as? List<String>) ?: emptyList()
                } else {
                    currentBooked = 0L
                    bookedSlots = emptyList()
                }

                if (currentBooked >= capacity) {
                    throw IllegalStateException("Doctor daily capacity ($capacity) is full for $date.")
                }
                if (bookedSlots.contains(timeSlot)) {
                    throw IllegalStateException("Slot $timeSlot is already booked. Please choose a vacant slot.")
                }

                val nextBooked = currentBooked + 1L
                val nextAvailable = (capacity - nextBooked).coerceAtLeast(0L)
                val nextSlots = bookedSlots + timeSlot

                val slotPayload = mapOf(
                    "doctorId" to doctor.id,
                    "hospitalId" to hospital.id,
                    "date" to date,
                    "dailyCapacity" to capacity,
                    "bookedCount" to nextBooked,
                    "availableCount" to nextAvailable,
                    "bookedSlots" to nextSlots,
                    "adminUid" to doctor.adminUid.ifBlank { hospital.adminUid },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (slotSnap.exists()) {
                    tx.update(
                        slotRef,
                        mapOf(
                            "bookedCount" to nextBooked,
                            "availableCount" to nextAvailable,
                            "bookedSlots" to nextSlots,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(slotRef, slotPayload)
                }

                val adminOwner = doctor.adminUid.ifBlank { hospital.adminUid.ifBlank { uid } }
                val apptPayload = mapOf(
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "patientPhone" to patientPhone.trim().take(30),
                    "hospitalId" to hospital.id,
                    "hospitalName" to hospital.name.take(160),
                    "doctorId" to doctor.id,
                    "doctorName" to doctor.name.take(120),
                    "doctorUid" to doctor.staffUid.ifBlank { adminOwner },
                    "speciality" to doctor.speciality.take(100),
                    "date" to date,
                    "timeSlot" to timeSlot,
                    "serialNumber" to nextBooked,
                    "status" to AppointmentStatus.CONFIRMED.name,
                    "consultationFee" to doctor.consultationFee,
                    "symptoms" to symptoms.trim().take(500),
                    "adminUid" to adminOwner,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(apptRef, apptPayload)

                val invNumber = "CNX-${System.currentTimeMillis().toString().takeLast(6)}"
                val invoicePayload = mapOf(
                    "invoiceNumber" to invNumber,
                    "patientId" to uid,
                    "patientName" to patientName.trim().ifBlank { "CareNex Patient" }.take(120),
                    "hospitalId" to hospital.id,
                    "hospitalName" to hospital.name.take(160),
                    "doctorName" to doctor.name.take(120),
                    "referenceType" to InvoiceRefType.APPOINTMENT.name,
                    "referenceId" to apptRef.id,
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
                tx.set(invoiceRef, invoicePayload)

                val notifPayload = mapOf(
                    "userId" to uid,
                    "title" to "OPD Appointment Confirmed (#$nextBooked)",
                    "message" to "Dr. ${doctor.name} at ${hospital.name} on $date at $timeSlot.",
                    "category" to NotificationCategory.APPOINTMENT.name,
                    "read" to false,
                    "referenceId" to apptRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                tx.set(notifRef, notifPayload)
                nextBooked
            }.await()

            Result.success(
                Appointment(
                    id = apptRef.id,
                    patientId = uid,
                    patientName = patientName,
                    hospitalId = hospital.id,
                    hospitalName = hospital.name,
                    doctorId = doctor.id,
                    doctorName = doctor.name,
                    speciality = doctor.speciality,
                    date = date,
                    timeSlot = timeSlot,
                    serialNumber = createdSerial,
                    status = AppointmentStatus.CONFIRMED.name,
                    consultationFee = doctor.consultationFee
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelAppointmentAtomic(appointment: Appointment): Result<Unit> {
        val apptRef = db.collection("appointments").document(appointment.id)
        val slotId = "${appointment.doctorId}_${appointment.date.replace("-", "_")}"
        val slotRef = db.collection("doctor_slots").document(slotId)
        val notifRef = db.collection("notifications").document()
        return try {
            val uid = requireUserId()
            db.runTransaction { tx ->
                val apptSnap = tx.get(apptRef)
                if (!apptSnap.exists()) throw IllegalStateException("Appointment not found")
                if (apptSnap.getString("status") == AppointmentStatus.CANCELLED.name) {
                    throw IllegalStateException("Appointment is already cancelled")
                }
                val slotSnap = tx.get(slotRef)
                tx.update(
                    apptRef,
                    mapOf(
                        "status" to AppointmentStatus.CANCELLED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (slotSnap.exists()) {
                    val capacity = slotSnap.getLong("dailyCapacity") ?: 40L
                    val currentBooked = slotSnap.getLong("bookedCount") ?: 0L
                    @Suppress("UNCHECKED_CAST")
                    val bookedSlots = (slotSnap.get("bookedSlots") as? List<String>) ?: emptyList()
                    val nextBooked = (currentBooked - 1L).coerceAtLeast(0L)
                    val nextAvail = (capacity - nextBooked).coerceIn(0L, capacity)
                    tx.update(
                        slotRef,
                        mapOf(
                            "bookedCount" to nextBooked,
                            "availableCount" to nextAvail,
                            "bookedSlots" to bookedSlots.filterNot { it == appointment.timeSlot },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to appointment.patientId.ifBlank { uid },
                        "title" to "Appointment Cancelled",
                        "message" to "OPD slot ${appointment.timeSlot} with Dr. ${appointment.doctorName} on ${appointment.date} was cancelled.",
                        "category" to NotificationCategory.APPOINTMENT.name,
                        "read" to false,
                        "referenceId" to appointment.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, apptRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: AppointmentStatus): Result<Unit> {
        val path = "appointments/$appointmentId"
        return try {
            requireUserId()
            db.collection("appointments").document(appointmentId).update(
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

    // --- BEDS & ATOMIC ADMISSIONS / DISCHARGE ---

    fun observeBeds(): Flow<List<BedInventory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("bed_inventory")
                .snapshots()
                .map { it.toObjects(BedInventory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "bed_inventory")
        )
    }

    fun observeAdmissions(adminUid: String, patientOnly: Boolean): Flow<List<Admission>> = flow {
        val uid = requireUserId()
        val query: Query = when {
            patientOnly -> db.collection("admissions").whereEqualTo("patientId", uid)
            isBootstrappedAdmin() -> db.collection("admissions")
            else -> db.collection("admissions").whereEqualTo("adminUid", adminUid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Admission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(uid, OperationType.LIST, "admissions")
        )
    }

    suspend fun addBedToInventory(
        hospital: Hospital,
        wardType: WardType,
        roomNumber: String,
        bedNumber: String
    ): Result<Unit> {
        val bedRef = db.collection("bed_inventory").document()
        val hospRef = db.collection("hospitals").document(hospital.id)
        return try {
            val uid = requireUserId()
            db.runTransaction { tx ->
                val hSnap = tx.get(hospRef)
                val total = (hSnap.getLong("totalBeds") ?: 0L) + 1L
                val avail = (hSnap.getLong("availableBeds") ?: 0L) + 1L
                val icuTotal = (hSnap.getLong("icuBedsTotal") ?: 0L) + if (wardType == WardType.ICU) 1L else 0L
                val icuAvail = (hSnap.getLong("icuBedsAvailable") ?: 0L) + if (wardType == WardType.ICU) 1L else 0L
                val emTotal = (hSnap.getLong("emergencyBedsTotal") ?: 0L) + if (wardType == WardType.EMERGENCY) 1L else 0L
                val emAvail = (hSnap.getLong("emergencyBedsAvailable") ?: 0L) + if (wardType == WardType.EMERGENCY) 1L else 0L

                tx.set(
                    bedRef,
                    mapOf(
                        "hospitalId" to hospital.id,
                        "hospitalName" to hospital.name,
                        "wardType" to wardType.name,
                        "roomNumber" to roomNumber.trim().take(40),
                        "bedNumber" to bedNumber.trim().take(40),
                        "occupied" to false,
                        "currentAdmissionId" to "",
                        "adminUid" to hospital.adminUid.ifBlank { uid },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    hospRef,
                    mapOf(
                        "totalBeds" to total,
                        "availableBeds" to avail,
                        "icuBedsTotal" to icuTotal,
                        "icuBedsAvailable" to icuAvail,
                        "icuAvailable" to (icuAvail > 0L),
                        "emergencyBedsTotal" to emTotal,
                        "emergencyBedsAvailable" to emAvail,
                        "emergencyAvailable" to (emAvail > 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, bedRef.path)
            Result.failure(e)
        }
    }

    suspend fun admitPatientAtomic(
        bed: BedInventory,
        patientId: String,
        patientName: String,
        patientPhone: String,
        diagnosis: String,
        admittingDoctorName: String
    ): Result<Unit> {
        val bedRef = db.collection("bed_inventory").document(bed.id)
        val hospRef = db.collection("hospitals").document(bed.hospitalId)
        val admRef = db.collection("admissions").document()
        return try {
            val uid = requireUserId()
            db.runTransaction { tx ->
                val bedSnap = tx.get(bedRef)
                val hospSnap = tx.get(hospRef)
                if (bedSnap.getBoolean("occupied") == true) {
                    throw IllegalStateException("Bed ${bed.bedNumber} is already occupied.")
                }
                val total = hospSnap.getLong("totalBeds") ?: 1L
                val avail = ((hospSnap.getLong("availableBeds") ?: 1L) - 1L).coerceAtLeast(0L)
                val occ = (total - avail).coerceAtLeast(0L)
                val ward = bed.parsedWard
                val icuAvail = ((hospSnap.getLong("icuBedsAvailable") ?: 0L) - if (ward == WardType.ICU) 1L else 0L).coerceAtLeast(0L)
                val emAvail = ((hospSnap.getLong("emergencyBedsAvailable") ?: 0L) - if (ward == WardType.EMERGENCY) 1L else 0L).coerceAtLeast(0L)

                tx.update(
                    bedRef,
                    mapOf(
                        "occupied" to true,
                        "currentAdmissionId" to admRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    hospRef,
                    mapOf(
                        "availableBeds" to avail,
                        "occupiedBeds" to occ,
                        "icuBedsAvailable" to icuAvail,
                        "icuAvailable" to (icuAvail > 0L),
                        "emergencyBedsAvailable" to emAvail,
                        "emergencyAvailable" to (emAvail > 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    admRef,
                    mapOf(
                        "hospitalId" to bed.hospitalId,
                        "hospitalName" to bed.hospitalName,
                        "bedId" to bed.id,
                        "roomNumber" to bed.roomNumber,
                        "bedNumber" to bed.bedNumber,
                        "wardType" to bed.wardType,
                        "patientId" to patientId.ifBlank { uid },
                        "patientName" to patientName.trim().take(120),
                        "patientPhone" to patientPhone.trim().take(30),
                        "diagnosis" to diagnosis.trim().take(400),
                        "admittingDoctorName" to admittingDoctorName.trim().take(120),
                        "status" to AdmissionStatus.ADMITTED.name,
                        "dischargeNotes" to "",
                        "adminUid" to bed.adminUid.ifBlank { uid },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, admRef.path)
            Result.failure(e)
        }
    }

    suspend fun dischargePatientAtomic(admission: Admission, dischargeNotes: String): Result<Unit> {
        val admRef = db.collection("admissions").document(admission.id)
        val bedRef = db.collection("bed_inventory").document(admission.bedId)
        val hospRef = db.collection("hospitals").document(admission.hospitalId)
        return try {
            requireUserId()
            db.runTransaction { tx ->
                val admSnap = tx.get(admRef)
                if (admSnap.getString("status") == AdmissionStatus.DISCHARGED.name) {
                    throw IllegalStateException("Patient already discharged")
                }
                val hospSnap = tx.get(hospRef)
                val bedSnap = tx.get(bedRef)
                val total = hospSnap.getLong("totalBeds") ?: 1L
                val avail = ((hospSnap.getLong("availableBeds") ?: 0L) + 1L).coerceAtMost(total)
                val occ = (total - avail).coerceAtLeast(0L)
                val icuTotal = hospSnap.getLong("icuBedsTotal") ?: 0L
                val icuAvail = ((hospSnap.getLong("icuBedsAvailable") ?: 0L) + if (admission.wardType == WardType.ICU.name) 1L else 0L).coerceAtMost(icuTotal)
                val emTotal = hospSnap.getLong("emergencyBedsTotal") ?: 0L
                val emAvail = ((hospSnap.getLong("emergencyBedsAvailable") ?: 0L) + if (admission.wardType == WardType.EMERGENCY.name) 1L else 0L).coerceAtMost(emTotal)

                tx.update(
                    admRef,
                    mapOf(
                        "status" to AdmissionStatus.DISCHARGED.name,
                        "dischargeNotes" to dischargeNotes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (bedSnap.exists()) {
                    tx.update(
                        bedRef,
                        mapOf(
                            "occupied" to false,
                            "currentAdmissionId" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.update(
                    hospRef,
                    mapOf(
                        "availableBeds" to avail,
                        "occupiedBeds" to occ,
                        "icuBedsAvailable" to icuAvail,
                        "icuAvailable" to (icuAvail > 0L),
                        "emergencyBedsAvailable" to emAvail,
                        "emergencyAvailable" to (emAvail > 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, admRef.path)
            Result.failure(e)
        }
    }

    // --- ADMIN NETWORK INITIALIZER (REAL FIRESTORE DATA FOR NEWLY PROVISIONED DB) ---

    suspend fun seedInitialHospitalNetworkForAdmin(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val existing = db.collection("hospitals").limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            val h1Id = "hosp_carenex_central_${UUID.randomUUID().toString().take(6)}"
            val h2Id = "hosp_meridian_heart_${UUID.randomUUID().toString().take(6)}"

            val h1Ref = db.collection("hospitals").document(h1Id)
            val h2Ref = db.collection("hospitals").document(h2Id)

            batch.set(
                h1Ref,
                mapOf(
                    "name" to "CareNex Metro Multispeciality Hospital",
                    "imageKey" to "hospital_main",
                    "address" to "Sector 18 Medical District, New Delhi 110001",
                    "contactNumber" to "+91-11-4090-2200",
                    "opdTimings" to "08:00 AM - 08:00 PM",
                    "emergencyAvailable" to true,
                    "icuAvailable" to true,
                    "totalBeds" to 6,
                    "availableBeds" to 5,
                    "occupiedBeds" to 1,
                    "icuBedsTotal" to 2,
                    "icuBedsAvailable" to 2,
                    "emergencyBedsTotal" to 2,
                    "emergencyBedsAvailable" to 1,
                    "rating" to 4.8,
                    "latitude" to 28.6139,
                    "longitude" to 77.2090,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                h2Ref,
                mapOf(
                    "name" to "Apex Heart & Trauma Institute",
                    "imageKey" to "hospital_main",
                    "address" to "42 Green Park Avenue, South Delhi 110016",
                    "contactNumber" to "+91-11-4180-9900",
                    "opdTimings" to "09:00 AM - 07:00 PM",
                    "emergencyAvailable" to true,
                    "icuAvailable" to true,
                    "totalBeds" to 4,
                    "availableBeds" to 4,
                    "occupiedBeds" to 0,
                    "icuBedsTotal" to 2,
                    "icuBedsAvailable" to 2,
                    "emergencyBedsTotal" to 1,
                    "emergencyBedsAvailable" to 1,
                    "rating" to 4.7,
                    "latitude" to 28.5597,
                    "longitude" to 77.2066,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Doctors
            val doctors = listOf(
                mapOf(
                    "hospitalId" to h1Id,
                    "hospitalName" to "CareNex Metro Multispeciality Hospital",
                    "staffUid" to uid,
                    "name" to "Dr. Ananya Sharma",
                    "qualification" to "MD, DM (Cardiology)",
                    "speciality" to "Cardiology",
                    "experienceYears" to 14,
                    "consultationFee" to 750.0,
                    "availableDays" to "Mon - Sat",
                    "workingHours" to "09:00 AM - 04:00 PM",
                    "dailyCapacity" to 40,
                    "available" to true,
                    "rating" to 4.9,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "hospitalId" to h1Id,
                    "hospitalName" to "CareNex Metro Multispeciality Hospital",
                    "staffUid" to uid,
                    "name" to "Dr. Rohan Kulkarni",
                    "qualification" to "MS (Orthopedics), FRCS",
                    "speciality" to "Orthopedics",
                    "experienceYears" to 11,
                    "consultationFee" to 650.0,
                    "availableDays" to "Mon - Fri",
                    "workingHours" to "10:00 AM - 06:00 PM",
                    "dailyCapacity" to 30,
                    "available" to true,
                    "rating" to 4.7,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "hospitalId" to h2Id,
                    "hospitalName" to "Apex Heart & Trauma Institute",
                    "staffUid" to uid,
                    "name" to "Dr. Meera Nair",
                    "qualification" to "MD (General Medicine)",
                    "speciality" to "Internal Medicine",
                    "experienceYears" to 9,
                    "consultationFee" to 500.0,
                    "availableDays" to "Mon - Sat",
                    "workingHours" to "08:30 AM - 03:30 PM",
                    "dailyCapacity" to 40,
                    "available" to true,
                    "rating" to 4.8,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            doctors.forEach { d ->
                batch.set(db.collection("doctors").document(), d)
            }

            // Beds for Hospital 1 (6 beds, 1 occupied) & Hospital 2 (4 beds)
            val beds = listOf(
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("ICU", "ICU-101", "B1", false)),
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("ICU", "ICU-102", "B2", false)),
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("EMERGENCY", "ER-01", "E1", true)),
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("EMERGENCY", "ER-02", "E2", false)),
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("GENERAL", "GW-201", "G1", false)),
                Triple(h1Id, "CareNex Metro Multispeciality Hospital", listOf("PRIVATE", "PV-301", "P1", false)),
                Triple(h2Id, "Apex Heart & Trauma Institute", listOf("ICU", "ICU-201", "A1", false)),
                Triple(h2Id, "Apex Heart & Trauma Institute", listOf("ICU", "ICU-202", "A2", false)),
                Triple(h2Id, "Apex Heart & Trauma Institute", listOf("EMERGENCY", "ER-10", "AE1", false)),
                Triple(h2Id, "Apex Heart & Trauma Institute", listOf("GENERAL", "GW-105", "AG1", false))
            )
            beds.forEach { (hId, hName, meta) ->
                batch.set(
                    db.collection("bed_inventory").document(),
                    mapOf(
                        "hospitalId" to hId,
                        "hospitalName" to hName,
                        "wardType" to meta[0],
                        "roomNumber" to meta[1],
                        "bedNumber" to meta[2],
                        "occupied" to meta[3],
                        "currentAdmissionId" to "",
                        "adminUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Ambulances
            val ambulances = listOf(
                mapOf(
                    "hospitalId" to h1Id,
                    "hospitalName" to "CareNex Metro Multispeciality Hospital",
                    "vehicleNumber" to "DL-01-CN-1081",
                    "driverName" to "Vikram Singh",
                    "driverPhone" to "+91-98110-10811",
                    "vehicleType" to "ICU_MOBILE",
                    "available" to true,
                    "currentBookingId" to "",
                    "adminUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "hospitalId" to h2Id,
                    "hospitalName" to "Apex Heart & Trauma Institute",
                    "vehicleNumber" to "DL-01-AP-2042",
                    "driverName" to "Suresh Yadav",
                    "driverPhone" to "+91-98110-20422",
                    "vehicleType" to "ALS",
                    "available" to true,
                    "currentBookingId" to "",
                    "adminUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            ambulances.forEach { amb ->
                batch.set(db.collection("ambulances").document(), amb)
            }

            // Medicines
            val medicines = listOf(
                mapOf(
                    "name" to "Paracetamol 650mg",
                    "dosage" to "650 mg Tablet",
                    "timing" to "After meals • SOS / 8 hrs",
                    "price" to 38.0,
                    "stockQuantity" to 240,
                    "requiresPrescription" to false,
                    "category" to "Fever & Pain",
                    "description" to "Analgesic and antipyretic relief.",
                    "imageKey" to "pharmacy",
                    "hospitalId" to h1Id,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "name" to "Amoxicillin + Clavulanate 625mg",
                    "dosage" to "625 mg Tablet",
                    "timing" to "Twice daily after meals (5 days)",
                    "price" to 195.0,
                    "stockQuantity" to 120,
                    "requiresPrescription" to true,
                    "category" to "Antibiotics (Rx)",
                    "description" to "Broad-spectrum antibiotic. Pharmacist Rx verification required.",
                    "imageKey" to "pharmacy",
                    "hospitalId" to h1Id,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "name" to "Atorvastatin 20mg",
                    "dosage" to "20 mg Tablet",
                    "timing" to "Once daily at bedtime",
                    "price" to 145.0,
                    "stockQuantity" to 180,
                    "requiresPrescription" to true,
                    "category" to "Cardiac Care (Rx)",
                    "description" to "Lipid-lowering therapy. Pharmacist Rx verification required.",
                    "imageKey" to "pharmacy",
                    "hospitalId" to h2Id,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "name" to "Electral ORS Hydration Pack",
                    "dosage" to "21.8 g Sachet",
                    "timing" to "Dissolve in 1L clean water",
                    "price" to 24.0,
                    "stockQuantity" to 400,
                    "requiresPrescription" to false,
                    "category" to "Hydration & Wellness",
                    "description" to "WHO-formula oral rehydration salts.",
                    "imageKey" to "pharmacy",
                    "hospitalId" to h1Id,
                    "adminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            medicines.forEach { med ->
                batch.set(db.collection("medicines").document(), med)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "hospitals")
            Result.failure(e)
        }
    }
}
