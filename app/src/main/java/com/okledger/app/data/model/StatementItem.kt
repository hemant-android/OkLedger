package com.okledger.app.data.model

data class StatementItem(
    val transactionId: Int,
    val partyId: Int,
    val partyName: String,
    val amount: Double,
    val type: String, // Given / Received
    val note: String,
    val date: Long

)
