package com.okledger.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
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
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    fun setLedgerType(type: LedgerType) {
        _selectedLedgerType.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val dashboardSummary: LiveData<TotalSummary> = _selectedLedgerType.switchMap { type ->
        repository.getDashboardSummary(type.value)
    }

    val partyWithTotalsList: LiveData<List<PartyWithTotals>> =
        _selectedLedgerType.switchMap { type ->
            repository.getPartiesWithTotals(type.value)
        }

    val filteredParties: LiveData<List<PartyWithTotals>> =
        _searchQuery.switchMap { query ->
            _selectedLedgerType.switchMap { type ->
                repository.getPartiesWithTotals(type.value).map { list ->
                    if (query.isEmpty()) list
                    else list.filter { item ->
                        item.party.name.contains(query, ignoreCase = true)
                    }
                }
            }
        }

}
