package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Invoice
import com.example.data.model.MedicalReport
import com.example.data.model.Prescription
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private fun dateFormatted(date: Date?): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(date ?: Date())
    }

    fun generateAndOpenInvoicePdf(context: Context, invoice: Invoice): Result<Uri> {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.rgb(11, 99, 206)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("CareNex Healthcare", 40f, 50f, brandPaint)

            val subBrandPaint = Paint().apply {
                color = Color.rgb(214, 230, 250)
                textSize = 13f
            }
            canvas.drawText("Official Tax Invoice & Payment Receipt", 40f, 76f, subBrandPaint)

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 12f
            }
            val valuePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.rgb(213, 223, 235)
                strokeWidth = 1.5f
            }

            var y = 150f
            canvas.drawText("Invoice #: ${invoice.invoiceNumber}", 40f, y, titlePaint)
            canvas.drawText("Date: ${dateFormatted(invoice.createdAt?.toDate())}", 360f, y, labelPaint)

            y += 30f
            canvas.drawLine(40f, y, 555f, y, linePaint)

            y += 30f
            canvas.drawText("Hospital Facility", 40f, y, labelPaint)
            canvas.drawText("Patient Details", 320f, y, labelPaint)

            y += 18f
            canvas.drawText(invoice.hospitalName.ifBlank { "CareNex Partner Hospital" }, 40f, y, valuePaint)
            canvas.drawText(invoice.patientName, 320f, y, valuePaint)

            y += 18f
            canvas.drawText("Doctor: ${invoice.doctorName.ifBlank { "N/A" }}", 40f, y, labelPaint)
            canvas.drawText("Patient ID: ${invoice.patientId.take(16)}", 320f, y, labelPaint)

            y += 32f
            canvas.drawLine(40f, y, 555f, y, linePaint)

            y += 28f
            canvas.drawText("Charge Breakdown", 40f, y, titlePaint)
            y += 24f

            fun drawRow(label: String, amount: Double) {
                canvas.drawText(label, 40f, y, labelPaint)
                canvas.drawText("INR ${String.format(Locale.US, "%.2f", amount)}", 440f, y, valuePaint)
                y += 24f
            }

            drawRow("Consultation Charges", invoice.consultationCharges)
            drawRow("Medicine Charges", invoice.medicineCharges)
            drawRow("Other / Facility Charges", invoice.otherCharges)

            y += 8f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 28f
            canvas.drawText("Total Amount", 40f, y, titlePaint)
            canvas.drawText(
                "INR ${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                430f,
                y,
                titlePaint
            )

            y += 40f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 28f
            canvas.drawText("Payment Summary", 40f, y, titlePaint)
            y += 24f
            canvas.drawText("Payment Method: ${invoice.paymentMethod}", 40f, y, labelPaint)
            y += 20f
            canvas.drawText("Payment Status: ${invoice.paymentStatus}", 40f, y, valuePaint)
            y += 20f
            canvas.drawText(
                "Transaction ID: ${invoice.transactionId.ifBlank { "Awaiting Verified Payment" }}",
                40f,
                y,
                labelPaint
            )

            canvas.drawText(
                "Generated securely by CareNex • Digital Healthcare Platform",
                40f,
                800f,
                labelPaint
            )

            pdfDoc.finishPage(page)

            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val file = File(pdfDir, "CareNex_Invoice_${invoice.invoiceNumber}.pdf")
            FileOutputStream(file).use { pdfDoc.writeTo(it) }
            pdfDoc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            openPdfUri(context, uri)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateAndOpenReportPdf(
        context: Context,
        report: MedicalReport?,
        prescription: Prescription?
    ): Result<Uri> {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.rgb(13, 148, 136)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 100f, headerPaint)
            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("CareNex Medical Record", 40f, 52f, brandPaint)

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 13f
            }

            var y = 140f
            if (report != null) {
                canvas.drawText(report.title, 40f, y, titlePaint)
                y += 24f
                canvas.drawText("Type: ${report.parsedType.label}", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Patient: ${report.patientName} (${report.patientId.take(12)})", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Doctor: ${report.doctorName.ifBlank { "Self Uploaded" }}", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Date: ${dateFormatted(report.createdAt?.toDate())}", 40f, y, bodyPaint)
                y += 32f
                canvas.drawText("Summary:", 40f, y, titlePaint)
                y += 22f
                drawWrappedText(canvas, report.summary, 40f, y, bodyPaint, 515f).also { y = it }
                if (report.findings.isNotBlank()) {
                    y += 24f
                    canvas.drawText("Clinical Findings:", 40f, y, titlePaint)
                    y += 22f
                    drawWrappedText(canvas, report.findings, 40f, y, bodyPaint, 515f)
                }
            } else if (prescription != null) {
                canvas.drawText("Digital Prescription (Rx)", 40f, y, titlePaint)
                y += 24f
                canvas.drawText("Hospital: ${prescription.hospitalName}", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Doctor: ${prescription.doctorName}", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Patient: ${prescription.patientName}", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Diagnosis: ${prescription.diagnosis}", 40f, y, bodyPaint)
                y += 28f
                canvas.drawText("Prescribed Medications:", 40f, y, titlePaint)
                y += 22f
                prescription.medications.forEachIndexed { idx, med ->
                    canvas.drawText("${idx + 1}. $med", 48f, y, bodyPaint)
                    y += 22f
                }
                if (prescription.notes.isNotBlank()) {
                    y += 16f
                    canvas.drawText("Instructions:", 40f, y, titlePaint)
                    y += 22f
                    drawWrappedText(canvas, prescription.notes, 40f, y, bodyPaint, 515f)
                }
            }

            pdfDoc.finishPage(page)

            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val fileName = report?.let { "CareNex_Report_${it.id.ifBlank { "doc" }}.pdf" }
                ?: "CareNex_Prescription_${prescription?.id?.ifBlank { "rx" }}.pdf"
            val file = File(pdfDir, fileName)
            FileOutputStream(file).use { pdfDoc.writeTo(it) }
            pdfDoc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            openPdfUri(context, uri)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float
    ): Float {
        var y = startY
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += 20f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(test)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += 20f
        }
        return y
    }

    private fun openPdfUri(context: Context, uri: Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, "Open CareNex PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share CareNex PDF"))
        }
    }
}
