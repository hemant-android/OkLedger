package com.okledger.app.di

import android.content.Context
import androidx.room.Room
import com.okledger.app.data.dao.PartyDao
import com.okledger.app.data.dao.TransactionDao
import com.okledger.app.data.db.AppDatabase
import com.okledger.app.data.repository.LedgerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "okledger_db")
            .fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePartyDao(db: AppDatabase): PartyDao = db.partyDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideRepository(partyDao: PartyDao, txnDao: TransactionDao): LedgerRepository {
        return LedgerRepository(partyDao, txnDao)
    }
}
