package com.shopmate.ui.screen.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.Sale
import com.shopmate.domain.repository.SaleRepository
import com.shopmate.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    saleRepository: SaleRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** The receipt's headline. */
    val storeName: StateFlow<String> = settings.storeName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _selectedSaleId = MutableStateFlow<String?>(null)

    val sales: StateFlow<List<Sale>> = saleRepository.getRecentSales(RECENT_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Resolved from the live list so the receipt sheet reflects sync-status updates. */
    val selectedSale: StateFlow<Sale?> = combine(sales, _selectedSaleId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectSale(saleId: String) {
        _selectedSaleId.value = saleId
    }

    fun closeReceipt() {
        _selectedSaleId.value = null
    }

    companion object {
        private const val RECENT_LIMIT = 100
    }
}
