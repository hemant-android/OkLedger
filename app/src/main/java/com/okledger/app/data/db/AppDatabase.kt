package com.okledger.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.okledger.app.data.dao.PartyDao
import com.okledger.app.data.dao.TransactionDao
import com.okledger.app.data.model.Party
import com.okledger.app.data.model.Transaction

@Database(entities = [Party::class, Transaction::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun transactionDao(): TransactionDao
}