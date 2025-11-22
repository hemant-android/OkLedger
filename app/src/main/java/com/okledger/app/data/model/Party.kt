package com.okledger.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "party_table")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val balance: Double = 0.0,
    val ledgerType: String = "PURCHASES",
    val createdDate: Long = System.currentTimeMillis(),
    val openingBalance: Double = 0.0,
    val openingType: String = "Received"
)

