package com.okledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    fun getTransactionById(id: Int) = repository.getTransactionById(id)

    fun updateTransaction(id: Int, newAmount: Double, newNote: String, newDate: Long,ledgerType: LedgerType) {
        viewModelScope.launch {
            val txn = repository.getTransactionByIdSuspend(id)
            txn?.let { oldTxn ->
                // Update transaction
                val updatedTxn = oldTxn.copy(
                    amount = newAmount,
                    note = newNote,
                    date = newDate
                )
                repository.updateTransaction(updatedTxn)

                // Adjust party balance
                val party = repository.getPartyByIdAndLedgerType(oldTxn.partyId,ledgerType.value)
                party?.let { p ->
                    val diff = when (oldTxn.type) {
                        "Given" -> newAmount - oldTxn.amount
                        "Received" -> oldTxn.amount - newAmount
                        else -> 0.0
                    }
                    val newBalance = p.balance + diff
                    repository.updateParty(p.copy(balance = newBalance))
                }
            }
        }
    }

    fun deleteTransaction(transactionId: Int,ledgerType: LedgerType) {
        viewModelScope.launch {
            val txn = repository.getTransactionByIdSuspend(transactionId)
            txn?.let {
                repository.updateTransaction(it.copy(amount = 0.0)) // optional
                repository.deleteTransaction(it) // if you have delete in repo
            }
        }
    }

}
