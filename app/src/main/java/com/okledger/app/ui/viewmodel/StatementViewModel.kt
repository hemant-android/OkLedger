package com.okledger.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.okledger.app.data.model.StatementItem
import com.okledger.app.data.model.Transaction
import com.okledger.app.data.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatementViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {
    val allTransactions: LiveData<List<Transaction>> = repository.getAllTransactions()

    val statementItems: LiveData<List<StatementItem>> = repository.getStatementItems()

}