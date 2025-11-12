package com.okledger.app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.okledger.app.data.dao.PartyDao
import com.okledger.app.data.dao.TransactionDao
import com.okledger.app.data.model.Party
import com.okledger.app.data.model.PartyWithTotals
import com.okledger.app.data.model.StatementItem
import com.okledger.app.data.model.TotalSummary
import com.okledger.app.data.model.Transaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.find

@Singleton
class LedgerRepository @Inject constructor(
    private val partyDao: PartyDao,
    private val transactionDao: TransactionDao
) {
    suspend fun insertParty(party: Party) = partyDao.insertParty(party)
    fun getTransactionsForParty(partyId: Int, ledgerType: String? = null) = transactionDao.getTransactionsForParty(partyId, ledgerType)
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun getPartyById(id: Int): Party? = partyDao.getPartyById(id)

    suspend fun getPartyByIdAndLedgerType(partyId: Int, ledgerType: String): Party? {
        return partyDao.getPartyByIdAndLedgerType(partyId, ledgerType)
    }
    fun getPartyByIdLive(partyId: Int) = partyDao.getPartyByIdLive(partyId)
    suspend fun updateParty(party: Party) = partyDao.updateParty(party)
    suspend fun getTransactionByIdSuspend(id: Int): Transaction? = transactionDao.getTransactionByIdSuspend(id)
    fun getTransactionById(id: Int) = transactionDao.getTransactionById(id)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    fun getDashboardSummary(ledgerType: String? = null): LiveData<TotalSummary> {
        val totalLive = transactionDao.getTotalGivenAndReceived(ledgerType)
        val partiesLive = partyDao.getAllParties(ledgerType)

        return MediatorLiveData<TotalSummary>().apply {
            addSource(totalLive) { txTotals ->
                val parties = partiesLive.value ?: emptyList()
                value = mergeTotals(txTotals, parties)
            }
            addSource(partiesLive) { parties ->
                val txTotals = totalLive.value ?: TotalSummary(0.0, 0.0)
                value = mergeTotals(txTotals, parties)
            }
        }
    }
    private fun mergeTotals(txTotals: TotalSummary, parties: List<Party>): TotalSummary {
        var totalGiven = txTotals.totalGiven
        var totalReceived = txTotals.totalReceived

        // Include opening balances
        parties.forEach { p ->
            when (p.openingType.lowercase()) {
                "given" -> totalGiven += p.openingBalance
                "received" -> totalReceived += p.openingBalance
            }
        }
        return TotalSummary(totalGiven, totalReceived)
    }
    fun getPartiesWithTotals(ledgerType: String? = null): LiveData<List<PartyWithTotals>> {
        val partiesLive = partyDao.getAllParties(ledgerType)
        val transactionsLive = transactionDao.getAllTransactions(ledgerType) // new DAO LiveData of all transactions

        return MediatorLiveData<List<PartyWithTotals>>().apply {
            fun calculate(): List<PartyWithTotals> {
                val parties = partiesLive.value ?: emptyList()
                val transactions = transactionsLive.value ?: emptyList()

                return parties.map { party ->
                    val partyTxs = transactions.filter { it.partyId == party.id }

                    val txGiven = partyTxs.filter { it.type.equals("Given", true) }.sumOf { it.amount }
                    val txReceived = partyTxs.filter { it.type.equals("Received", true) }.sumOf { it.amount }

                    val totalGiven = txGiven + if (party.openingType.equals("Given", true)) party.openingBalance else 0.0
                    val totalReceived = txReceived + if (party.openingType.equals("Received", true)) party.openingBalance else 0.0
                    val netBalance = totalGiven - totalReceived

                    PartyWithTotals(
                        party = party,
                        totalGiven = totalGiven,
                        totalReceived = totalReceived,
                        netBalance = netBalance
                    )
                }
            }

            addSource(partiesLive) { value = calculate() }
            addSource(transactionsLive) { value = calculate() }
        }
    }
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deletePartyCompletely(party: Party) {
        // Delete all transactions first
        val txs = transactionDao.getTransactionsForPartySuspend(party.id)
        txs.forEach { transactionDao.deleteTransaction(it) }
        // Then delete the party
        partyDao.deleteParty(party)
    }
    fun getAllTransactions(ledgerType: String? = null): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions(ledgerType)
    }
    fun getStatementItems(ledgerType: String? = null): LiveData<List<StatementItem>> {
        val allTransactionsLive = transactionDao.getAllTransactions(ledgerType)
        val partiesLive = partyDao.getAllParties(ledgerType)

        return MediatorLiveData<List<StatementItem>>().apply {
            fun combine(): List<StatementItem> {
                val transactions = allTransactionsLive.value ?: emptyList()
                val parties = partiesLive.value ?: emptyList()
                // Opening balances
                val openingTransactions = parties.flatMap { party ->
                    if (party.openingBalance > 0) {
                        listOf(
                            StatementItem(
                                transactionId = -party.id, // fake id
                                partyId = party.id,
                                partyName = party.name,
                                amount = party.openingBalance,
                                type = party.openingType, // Given / Received
                                note = "Opening Balance",
                                date = party.createdDate
                            )
                        )
                    } else emptyList()
                }

                // Actual transactions
                val txList = transactions.map { tx ->
                    val partyName = parties.find { it.id == tx.partyId }?.name ?: "Unknown"
                    StatementItem(
                        transactionId = tx.id,
                        partyId = tx.partyId,
                        partyName = partyName,
                        amount = tx.amount,
                        type = tx.type,
                        note = tx.note,
                        date = tx.date
                    )
                }

                return openingTransactions + txList
            }
            addSource(allTransactionsLive) { value = combine() }
            addSource(partiesLive) { value = combine() }
        }
    }
}
