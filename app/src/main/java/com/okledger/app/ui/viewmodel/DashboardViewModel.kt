package com.okledger.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.okledger.app.data.model.PartyWithTotals
import com.okledger.app.data.model.TotalSummary
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repository: LedgerRepository) :
    ViewModel() {

    private val _selectedLedgerType = MutableLiveData<LedgerType>(LedgerType.PURCHASES)
    val selectedLedgerType: LiveData<LedgerType> = _selectedLedgerType

    fun setLedgerType(type: LedgerType) {
        _selectedLedgerType.value = type
    }

//    val dashboardSummary = repository.getDashboardSummary()
//    val partyWithTotalsList = repository.getPartiesWithTotals()

    val dashboardSummary: LiveData<TotalSummary> = _selectedLedgerType.switchMap { type ->
        repository.getDashboardSummary(type.value)
    }

    val partyWithTotalsList: LiveData<List<PartyWithTotals>> =
        _selectedLedgerType.switchMap { type ->
            repository.getPartiesWithTotals(type.value)
        }

}
