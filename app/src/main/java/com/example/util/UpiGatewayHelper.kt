package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.UpiGatewayApp
import java.util.Locale

data class UpiIntentResponse(
    val success: Boolean,
    val transactionId: String,
    val responseCode: String,
    val message: String
)

object UpiGatewayHelper {

    fun buildUpiPaymentIntent(
        context: Context,
        gatewayApp: UpiGatewayApp,
        payeeVpa: String,
        payeeName: String,
        amount: Double,
        transactionNote: String,
        transactionRef: String
    ): Intent? {
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", payeeVpa)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("tr", transactionRef)
            .appendQueryParameter("tn", transactionNote)
            .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            gatewayApp.packageName?.let { setPackage(it) }
        }

        val pm = context.packageManager
        val resolved = intent.resolveActivity(pm)
        if (resolved != null) return intent

        if (gatewayApp.packageName != null) {
            val fallback = Intent(Intent.ACTION_VIEW, uri)
            if (fallback.resolveActivity(pm) != null) return fallback
        }
        return null
    }

    fun parseUpiResponse(data: Intent?): UpiIntentResponse {
        val raw = data?.getStringExtra("response") ?: ""
        if (raw.isBlank()) {
            return UpiIntentResponse(
                success = false,
                transactionId = "",
                responseCode = "CANCELLED",
                message = "No gateway confirmation received. Payment remains Pending."
            )
        }
        val params = raw.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) parts[0].lowercase(Locale.US) to parts[1] else null
        }.toMap()

        val status = params["status"]?.uppercase(Locale.US) ?: "FAILURE"
        val txnId = params["txnid"] ?: params["txnref"] ?: params["approvalrefno"] ?: ""
        val responseCode = params["responsecode"] ?: if (status == "SUCCESS") "00" else "ERR"

        return if (status == "SUCCESS" && txnId.isNotBlank()) {
            UpiIntentResponse(
                success = true,
                transactionId = txnId,
                responseCode = responseCode,
                message = "UPI payment verified ($txnId)"
            )
        } else {
            UpiIntentResponse(
                success = false,
                transactionId = txnId,
                responseCode = responseCode,
                message = "Payment status: $status"
            )
        }
    }
}
