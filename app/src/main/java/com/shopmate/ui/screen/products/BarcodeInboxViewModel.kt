package com.shopmate.ui.screen.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.UnknownBarcode
import com.shopmate.domain.repository.BarcodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeInboxViewModel @Inject constructor(
    private val barcodeRepository: BarcodeRepository,
) : ViewModel() {

    val unknownBarcodes: StateFlow<List<UnknownBarcode>> = barcodeRepository.unknownBarcodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Attaches a named code to its product and clears it out of the inbox. */
    fun claim(code: String, productId: String) = viewModelScope.launch {
        barcodeRepository.claimUnknown(code, productId)
    }
}
