package com.okledger.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.okledger.app.data.model.Party

@Dao
interface PartyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: Party)
    @Query("SELECT * FROM party_table ORDER BY id DESC")
    fun getAllParties(): LiveData<List<Party>>
    @Delete
    suspend fun deleteParty(party: Party)
    @Query("SELECT * FROM party_table WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: Int): Party?
    @Query("SELECT * FROM party_table WHERE id = :partyId AND ledgerType = :ledgerType LIMIT 1")
    suspend fun getPartyByIdAndLedgerType(partyId: Int, ledgerType: String): Party?

    @Query("SELECT * FROM party_table WHERE id = :partyId")
    fun getPartyByIdLive(partyId: Int): LiveData<Party>
    @Update
    suspend fun updateParty(party: Party)
    @Query("SELECT * FROM party_table WHERE (:ledgerType IS NULL OR ledgerType = :ledgerType) ORDER BY name ASC")
    fun getAllParties(ledgerType: String? = null): LiveData<List<Party>>

}
