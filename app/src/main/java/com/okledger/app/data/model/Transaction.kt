package com.okledger.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_table")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int,
    val amount: Double,
    val note: String = "",
    val type: String,
    val ledgerType: String = "PURCHASES",
    val date: Long = System.currentTimeMillis()
)

