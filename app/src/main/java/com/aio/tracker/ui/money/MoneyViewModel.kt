package com.aio.tracker.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.tracker.AppContainer
import com.aio.tracker.data.db.Loan
import com.aio.tracker.data.db.MoneyTxn
import com.aio.tracker.data.prefs.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoneyViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.db.moneyDao()

    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val txns: StateFlow<List<MoneyTxn>?> = dao.observeTxns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val loans: StateFlow<List<Loan>?> = dao.observeLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveTxn(t: MoneyTxn) = viewModelScope.launch { dao.upsertTxn(t) }
    fun deleteTxn(t: MoneyTxn) = viewModelScope.launch { dao.deleteTxn(t) }
    fun saveLoan(l: Loan) = viewModelScope.launch { dao.upsertLoan(l) }
    fun deleteLoan(l: Loan) = viewModelScope.launch { dao.deleteLoan(l) }
    fun repay(l: Loan, amountMinor: Long) = viewModelScope.launch {
        dao.upsertLoan(l.copy(repaidMinor = (l.repaidMinor + amountMinor).coerceIn(0, l.amountMinor)))
    }
}
