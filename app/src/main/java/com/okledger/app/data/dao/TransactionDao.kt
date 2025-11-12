package com.okledger.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.okledger.app.data.model.PartyBalance
import com.okledger.app.data.model.TotalSummary
import com.okledger.app.data.model.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transaction_table WHERE partyId = :partyId AND (:ledgerType IS NULL OR ledgerType = :ledgerType) ORDER BY date ASC")
    fun getTransactionsForParty(partyId: Int, ledgerType: String? = null): LiveData<List<Transaction>>

    @Query(
        """
        SELECT 
            partyId, 
            SUM(
                CASE 
                    WHEN type = 'Received' THEN amount 
                    WHEN type = 'Given' THEN -amount 
                    ELSE 0 
                END
            ) AS balance
        FROM transaction_table
        WHERE (:ledgerType IS NULL OR ledgerType = :ledgerType)
        GROUP BY partyId
    """
    )
    fun getPartyBalances(ledgerType: String? = null): LiveData<List<PartyBalance>>

    @Query("SELECT * FROM transaction_table WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdSuspend(id: Int): Transaction?

    @Query("SELECT * FROM transaction_table WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Int): LiveData<Transaction>

    @Query(
        """
    SELECT 
        IFNULL(SUM(CASE WHEN type = 'Given' THEN amount END), 0) AS totalGiven,
        IFNULL(SUM(CASE WHEN type = 'Received' THEN amount END), 0) AS totalReceived
    FROM transaction_table
    WHERE (:ledgerType IS NULL OR ledgerType = :ledgerType)
"""
    )
    fun getTotalGivenAndReceived(ledgerType: String? = null): LiveData<TotalSummary>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transaction_table WHERE (:ledgerType IS NULL OR ledgerType = :ledgerType)")
    fun getAllTransactions(ledgerType: String? = null): LiveData<List<Transaction>>

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("""
        SELECT * FROM transaction_table 
        WHERE partyId = :partyId 
        AND (:ledgerType IS NULL OR ledgerType = :ledgerType)
    """)
    suspend fun getTransactionsForPartySuspend(partyId: Int, ledgerType: String? = null): List<Transaction>

}
