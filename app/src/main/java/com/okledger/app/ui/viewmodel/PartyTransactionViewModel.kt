package com.okledger.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.okledger.app.data.model.Party
import com.okledger.app.data.model.Transaction
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartyTransactionViewModel @Inject constructor(private val repository: LedgerRepository) : ViewModel() {
    fun getTransactions(partyId: Int,ledgerType: LedgerType) = repository.getTransactionsForParty(partyId, ledgerType.value)
    fun getPartyById(partyId: Int) = repository.getPartyByIdLive(partyId)

    /**
     * Default - Loads all transactions (no date filter)
     */
    fun getTransactionsWithBalance(
        partyId: Int,
        ledgerType: LedgerType
    ): LiveData<Pair<List<Transaction>, Double>> {

        val transactionsLive = getTransactions(partyId, ledgerType)
        val partyLive = getPartyById(partyId)
        val result = MediatorLiveData<Pair<List<Transaction>, Double>>()

        fun update(txns: List<Transaction>, party: Party?) {
            if (party == null) return

            val filteredTxns = txns.filter { it.ledgerType == ledgerType.value }
            val displayList = buildDisplayList(filteredTxns, party)
            val netBalance = calculateNetBalance(filteredTxns, party)

            result.value = Pair(displayList, netBalance)
        }

        result.addSource(transactionsLive) { txns ->
            update(txns, partyLive.value)
        }

        result.addSource(partyLive) { p ->
            update(transactionsLive.value ?: emptyList(), p)
        }

        return result
    }


    /**
     * Date range filter version
     */
    fun getTransactionsWithBalance(
        partyId: Int,
        ledgerType: LedgerType,
        fromDate: Long,
        toDate: Long
    ): LiveData<Pair<List<Transaction>, Double>> {

        val transactionsLive = getTransactions(partyId, ledgerType)
        val partyLive = getPartyById(partyId)
        val result = MediatorLiveData<Pair<List<Transaction>, Double>>()

        fun update(txns: List<Transaction>, party: Party?) {
            if (party == null) return

            val filteredTxns = txns.filter {
                it.ledgerType == ledgerType.value && it.date in fromDate..toDate
            }

            val displayList = buildDisplayList(filteredTxns, party)
            val netBalance = calculateNetBalance(filteredTxns, party)

            result.value = Pair(displayList, netBalance)
        }

        result.addSource(transactionsLive) { txns ->
            update(txns, partyLive.value)
        }

        result.addSource(partyLive) { p ->
            update(transactionsLive.value ?: emptyList(), p)
        }

        return result
    }



    private fun buildDisplayList(list: List<Transaction>, party: Party): List<Transaction> {
        return if (party.openingBalance != 0.0) {
            val openingTxn = Transaction(
                id = -1,
                partyId = party.id,
                amount = party.openingBalance,
                type = party.openingType,
                note = "Opening Balance",
                date = party.createdDate,
                ledgerType = party.ledgerType
            )
            listOf(openingTxn) + list
        } else list
    }

    private fun calculateNetBalance(list: List<Transaction>, party: Party): Double {
        val totalGiven = list.filter { it.type.equals("Given", true) }.sumOf { it.amount }
        val totalReceived = list.filter { it.type.equals("Received", true) }.sumOf { it.amount }

        val adjustedOpening = when (party.openingType) {
            "Given" -> -party.openingBalance
            "Received" -> party.openingBalance
            else -> 0.0
        }

        return totalReceived - totalGiven + adjustedOpening
    }

}
