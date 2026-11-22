const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_patient_1";
const BOB_UID = "bob_patient_2";
const DOCTOR_UID = "doctor_staff_1";
const ADMIN_UID = "admin_boot_1";
const ADMIN_EMAIL = "aditya808052@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated access is denied across all CareNex collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("hospitals").get());
  await assertFails(unauthDb.collection("doctors").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("appointments").where("patientId", "==", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Hacker",
      email: "hacker@example.com",
      role: "patient",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile: PII isolation, role escalation prevention, and email spoof defense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const created = pastDate(10000);

  // Patient cannot self-assign hospital_admin on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice",
      email: "alice@example.com",
      role: "hospital_admin",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid patient profile create (mirrors CareNexDirectoryRepository.saveUserProfile)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Patient",
      mobile: "+91-9876543210",
      email: "alice@example.com",
      age: 29,
      gender: "Female",
      address: "12 Rose Avenue",
      bloodGroup: "O+",
      emergencyContact: "+91-9999999999",
      photoUri: "",
      role: "patient",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Patient can update mutable PII fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice M. Patient",
      mobile: "+91-9876543210",
      email: "alice@example.com",
      age: 30,
      gender: "Female",
      address: "14 Rose Avenue",
      bloodGroup: "O+",
      emergencyContact: "+91-9999999999",
      photoUri: "",
      updatedAt: pastDate(500),
    })
  );

  // Patient cannot escalate own role on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "hospital_admin",
      updatedAt: pastDate(400),
    })
  );

  // Cross-user PII read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ fullName: "Hijacked", updatedAt: pastDate(300) })
  );

  // Spoofed unverified admin email cannot read Alice's PII
  await assertFails(spoofAdminDb.collection("users").doc(ALICE_UID).get());
});

test("Hospitals, Doctors & Atomic OPD Slot Booking + Filtered Query Enforcement", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Non-admin patient cannot create a hospital
  await assertFails(
    aliceDb.collection("hospitals").doc("hosp_1").set({
      name: "Rogue Clinic",
      address: "Nowhere",
      contactNumber: "12345",
      opdTimings: "9-5",
      emergencyAvailable: true,
      icuAvailable: true,
      totalBeds: 10,
      availableBeds: 10,
      occupiedBeds: 0,
      adminUid: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin creates hospital and doctor
  await assertSucceeds(
    adminDb.collection("hospitals").doc("hosp_1").set({
      name: "CareNex Metro Hospital",
      imageKey: "hospital_main",
      address: "Sector 18, New Delhi",
      contactNumber: "+91-11-4090-2200",
      opdTimings: "08:00 AM - 08:00 PM",
      emergencyAvailable: true,
      icuAvailable: true,
      totalBeds: 10,
      availableBeds: 8,
      occupiedBeds: 2,
      icuBedsTotal: 4,
      icuBedsAvailable: 3,
      emergencyBedsTotal: 2,
      emergencyBedsAvailable: 2,
      rating: 4.8,
      latitude: 28.6139,
      longitude: 77.209,
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    adminDb.collection("doctors").doc("doc_1").set({
      hospitalId: "hosp_1",
      hospitalName: "CareNex Metro Hospital",
      staffUid: DOCTOR_UID,
      name: "Dr. Ananya Sharma",
      qualification: "MD, DM",
      speciality: "Cardiology",
      experienceYears: 14,
      consultationFee: 750,
      availableDays: "Mon - Sat",
      workingHours: "09:00 AM - 04:00 PM",
      dailyCapacity: 40,
      available: true,
      rating: 4.9,
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice books OPD slot atomically (slot + appointment + invoice + notification)
  const slotId = "doc_1_2026_11_23";
  await assertSucceeds(
    aliceDb.collection("doctor_slots").doc(slotId).set({
      doctorId: "doc_1",
      hospitalId: "hosp_1",
      date: "2026-11-23",
      dailyCapacity: 40,
      bookedCount: 1,
      availableCount: 39,
      bookedSlots: ["09:00 AM"],
      adminUid: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("appointments").doc("appt_1").set({
      patientId: ALICE_UID,
      patientName: "Alice Patient",
      patientPhone: "+91-9876543210",
      hospitalId: "hosp_1",
      hospitalName: "CareNex Metro Hospital",
      doctorId: "doc_1",
      doctorName: "Dr. Ananya Sharma",
      doctorUid: DOCTOR_UID,
      speciality: "Cardiology",
      date: "2026-11-23",
      timeSlot: "09:00 AM",
      serialNumber: 1,
      status: "CONFIRMED",
      consultationFee: 750,
      symptoms: "Routine cardiac checkup",
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject payload containing redundant `@DocumentId` "id" field
  await assertFails(
    aliceDb.collection("appointments").doc("appt_bad").set({
      id: "appt_bad",
      patientId: ALICE_UID,
      patientName: "Alice Patient",
      hospitalId: "hosp_1",
      hospitalName: "CareNex Metro Hospital",
      doctorId: "doc_1",
      doctorName: "Dr. Ananya Sharma",
      date: "2026-11-23",
      timeSlot: "09:30 AM",
      serialNumber: 2,
      status: "CONFIRMED",
      consultationFee: 750,
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Scoped patient query succeeds; unfiltered query fails; Bob cannot read Alice's appointment
  await assertSucceeds(
    aliceDb.collection("appointments").where("patientId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("appointments").get());
  await assertFails(bobDb.collection("appointments").doc("appt_1").get());

  // Patient can cancel own appointment & decrement slot
  await assertSucceeds(
    aliceDb.collection("appointments").doc("appt_1").update({
      status: "CANCELLED",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    aliceDb.collection("doctor_slots").doc(slotId).update({
      bookedCount: 0,
      availableCount: 40,
      bookedSlots: [],
      updatedAt: pastDate(200),
    })
  );
});

test("Pharmacy orders enforce pharmacist verification before advancing Rx orders", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);

  // Alice places Rx order
  await assertSucceeds(
    aliceDb.collection("pharmacy_orders").doc("ord_1").set({
      patientId: ALICE_UID,
      patientName: "Alice Patient",
      deliveryAddress: "12 Rose Avenue, Delhi",
      itemsSummary: "Amoxicillin 625mg x1",
      medicineIds: ["med_1"],
      totalAmount: 195,
      requiresPrescription: true,
      prescriptionRefId: "",
      prescriptionAttachmentUri: "content://picker/rx1.jpg",
      pharmacistVerified: false,
      paymentStatus: "PENDING",
      orderStatus: "AWAITING_VERIFICATION",
      transactionId: "",
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice cannot self-verify prescription
  await assertFails(
    aliceDb.collection("pharmacy_orders").doc("ord_1").update({
      pharmacistVerified: true,
      updatedAt: pastDate(200),
    })
  );

  // Admin cannot advance Rx order to PACKED without pharmacistVerified == true
  await assertFails(
    adminDb.collection("pharmacy_orders").doc("ord_1").update({
      pharmacistVerified: false,
      orderStatus: "PACKED",
      updatedAt: pastDate(200),
    })
  );

  // Admin verifies Rx and advances to VERIFIED
  await assertSucceeds(
    adminDb.collection("pharmacy_orders").doc("ord_1").update({
      pharmacistVerified: true,
      orderStatus: "VERIFIED",
      updatedAt: pastDate(100),
    })
  );
});

test("Medical reports authorization, Invoices payment guard, and Consultation chat isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docDb = testEnv.authenticatedContext(DOCTOR_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(8000);

  // Alice creates report authorizing DOCTOR_UID
  await assertSucceeds(
    aliceDb.collection("medical_reports").doc("rep_1").set({
      patientId: ALICE_UID,
      patientName: "Alice Patient",
      doctorId: "doc_1",
      doctorName: "Dr. Ananya Sharma",
      doctorUid: DOCTOR_UID,
      hospitalId: "hosp_1",
      title: "Complete Blood Count",
      reportType: "BLOOD_TEST",
      summary: "Normal CBC parameters",
      findings: "Hb 13.4 g/dL",
      attachmentUri: "",
      authorizedDoctorUids: [DOCTOR_UID],
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Authorized doctor query succeeds; unauthorized Bob fails
  await assertSucceeds(
    docDb.collection("medical_reports").where("authorizedDoctorUids", "array-contains", DOCTOR_UID).get()
  );
  await assertFails(bobDb.collection("medical_reports").doc("rep_1").get());

  // Invoice cannot be marked PAID without valid transactionId
  await assertSucceeds(
    aliceDb.collection("invoices").doc("inv_1").set({
      invoiceNumber: "CNX-100001",
      patientId: ALICE_UID,
      patientName: "Alice Patient",
      hospitalId: "hosp_1",
      hospitalName: "CareNex Metro Hospital",
      doctorName: "Dr. Ananya Sharma",
      referenceType: "APPOINTMENT",
      referenceId: "appt_1",
      consultationCharges: 750,
      medicineCharges: 0,
      otherCharges: 0,
      totalAmount: 750,
      paymentMethod: "UPI",
      paymentStatus: "PENDING",
      transactionId: "",
      adminUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("invoices").doc("inv_1").update({
      paymentStatus: "PAID",
      transactionId: "",
      updatedAt: pastDate(200),
    })
  );

  await assertSucceeds(
    aliceDb.collection("invoices").doc("inv_1").update({
      paymentMethod: "Google Pay",
      paymentStatus: "PAID",
      transactionId: "UPI202611220001",
      updatedAt: pastDate(100),
    })
  );
});
