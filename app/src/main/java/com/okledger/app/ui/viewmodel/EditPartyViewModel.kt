package com.okledger.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okledger.app.data.model.Party
import com.okledger.app.data.repository.LedgerRepository
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPartyViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    private val _party = MutableLiveData<Party?>()
    val party: LiveData<Party?> get() = _party

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    private val _partyDeleted = MutableLiveData<Boolean>()
    val partyDeleted: LiveData<Boolean> get() = _partyDeleted

    fun loadParty(id: Int, ledgerType: LedgerType) {
        viewModelScope.launch {
            repository.getPartyByIdAndLedgerType(id,ledgerType.name)?.let {
                _party.postValue(it)
            }
        }
    }

    fun updateParty(id: Int, name: String, mobile: String,date: Long,openingBalance: Double,
                    openingType: String,ledgerType: LedgerType) {
        viewModelScope.launch {
            val existingParty = repository.getPartyByIdAndLedgerType(id,ledgerType.name)
            if (existingParty != null) {
                val finalBalance = if (openingType == "Given") -openingBalance else openingBalance

                val updatedParty = existingParty.copy(
                    name = name,
                    mobile = mobile,
                    createdDate = date,
                    openingBalance = openingBalance,
                    openingType = openingType,
                    balance = finalBalance,
                    ledgerType = ledgerType.name
                )
                repository.updateParty(updatedParty)
                _updateStatus.postValue(true)
            } else {
                _updateStatus.postValue(false)
            }
        }
    }

    fun deleteParty(partyId: Int,ledgerType: LedgerType) {
        viewModelScope.launch {
            val party = repository.getPartyByIdAndLedgerType(partyId,ledgerType.name)
            party?.let {
                repository.deletePartyCompletely(it)
                _partyDeleted.postValue(true)
            }
        }
    }

}
