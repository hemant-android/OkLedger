package com.okledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okledger.app.data.model.Transaction
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(private val repository: LedgerRepository) : ViewModel() {
    fun addTransaction(partyId: Int, amount: Double, note: String, type: String,date: Long,ledgerType: LedgerType) {
        viewModelScope.launch {
            val txn = Transaction(partyId = partyId, amount = amount, note = note, type = type, ledgerType = ledgerType.name,date = date)
            repository.insertTransaction(txn)

            val party = repository.getPartyByIdAndLedgerType(partyId,ledgerType.name)
            if (party != null) {
                val newBalance = when (type) {
                    "Given" -> party.balance + amount
                    "Received" -> party.balance - amount
                    else -> party.balance
                }

                val updatedParty = party.copy(balance = newBalance)
                repository.updateParty(updatedParty)
            }

        }
    }
}
