package com.okledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okledger.app.data.model.Party
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPartyViewModel @Inject constructor(private val repository: LedgerRepository) : ViewModel() {
    fun addParty(name: String, mobile: String,date: Long,ledgerType: LedgerType) {
        viewModelScope.launch {
            repository.insertParty(Party(name = name, mobile = mobile, createdDate = date,ledgerType = ledgerType.value))
        }
    }
}
