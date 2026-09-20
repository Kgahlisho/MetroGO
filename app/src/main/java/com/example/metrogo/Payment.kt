package com.example.metrogo

import org.json.JSONObject


data class Payment(
    val paymentId: String,
    val userId: String,       // FK
    val ticketId: String?,    // FK
    val type: String,
    val description: String,
    val amount: Int,
    val balanceAfter: Int,
    val paymentMethod: String = "wallet",
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentStatus: String = STATUS_COMPLETED
) {
    companion object {
        const val TYPE_TOPUP = "topup"
        const val TYPE_PURCHASE = "purchase"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}