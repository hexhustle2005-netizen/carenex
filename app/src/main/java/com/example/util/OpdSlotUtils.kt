package com.example.util

import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.Doctor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class OpdTimeSlotSpec(
    val slotKey: String,
    val endTimeLabel: String
) {
    val rangeLabel: String get() = "$slotKey - $endTimeLabel"
}

object OpdSlotUtils {

    val STANDARD_SLOTS: List<OpdTimeSlotSpec> = listOf(
        OpdTimeSlotSpec("09:00 AM", "09:30 AM"),
        OpdTimeSlotSpec("09:30 AM", "10:00 AM"),
        OpdTimeSlotSpec("10:00 AM", "10:30 AM"),
        OpdTimeSlotSpec("10:30 AM", "11:00 AM"),
        OpdTimeSlotSpec("11:00 AM", "11:30 AM"),
        OpdTimeSlotSpec("11:30 AM", "12:00 PM"),
        OpdTimeSlotSpec("12:00 PM", "12:30 PM"),
        OpdTimeSlotSpec("02:00 PM", "02:30 PM"),
        OpdTimeSlotSpec("02:30 PM", "03:00 PM"),
        OpdTimeSlotSpec("03:00 PM", "03:30 PM"),
        OpdTimeSlotSpec("03:30 PM", "04:00 PM"),
        OpdTimeSlotSpec("04:00 PM", "04:30 PM")
    )

    fun upcomingDates(days: Int = 5): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (0 until days).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.let { sdf.format(it.time) }
        }
    }

    fun formatSlotRange(slotKey: String): String {
        val match = STANDARD_SLOTS.firstOrNull { it.slotKey.equals(slotKey.trim(), ignoreCase = true) }
        return match?.rangeLabel ?: slotKey
    }

    fun freeSlotsForBookedKeys(bookedKeys: Collection<String>): List<OpdTimeSlotSpec> {
        val bookedSet = bookedKeys.map { it.trim() }.toSet()
        return STANDARD_SLOTS.filter { it.slotKey !in bookedSet }
    }

    fun freeUntilSummary(
        doctor: Doctor,
        bookedKeys: Collection<String>,
        remainingCapacity: Long
    ): String {
        if (!doctor.available) return "Doctor currently offline"
        if (remainingCapacity <= 0L) return "OPD full for selected date"
        val free = freeSlotsForBookedKeys(bookedKeys)
        if (free.isEmpty()) return "All timing slots booked"
        val first = free.first()
        val last = free.last()
        return "Free till ${last.endTimeLabel} • ${free.size} slots open (Next: ${first.rangeLabel})"
    }

    fun slotAppointmentMap(
        appointments: List<Appointment>,
        doctorId: String,
        date: String
    ): Map<String, Appointment> {
        return appointments
            .filter {
                it.doctorId == doctorId &&
                    it.date == date &&
                    it.parsedStatus != AppointmentStatus.CANCELLED
            }
            .associateBy { it.timeSlot.trim() }
    }
}
