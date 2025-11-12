package com.okledger.app.data.model

data class PartyWithTotals(
    val party: Party,
    val totalGiven: Double,
    val totalReceived: Double,
    val netBalance: Double
)
