package com.shopmate.ui.screen.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.domain.repository.BarcodeRepository
import com.shopmate.domain.repository.CategoryRepository
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.StorePermissionRepository
import com.shopmate.domain.util.BarcodeNormaliser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val barcodeRepository: BarcodeRepository,
    storePermissions: StorePermissionRepository,
) : ViewModel() {

    /** Whether the form offers opening stock; see [StorePermissionRepository.canManageStock]. */
    val canManageStock: StateFlow<Boolean> = storePermissions.canManageStock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val productId: String? = savedStateHandle["productId"]
    val isEditMode: Boolean = productId != null

    /**
     * A code carried in from the unknown-barcode inbox, already normalised on the way in. The
     * form starts with it filled so naming a scanned code is one step, not a re-type.
     */
    val prefilledBarcode: String? = savedStateHandle.get<String>("code")
        ?.let(BarcodeNormaliser::normalise)

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        if (productId != null) {
            viewModelScope.launch {
                _product.value = productRepository.getById(productId)
            }
        }
    }

    fun save(
        name: String,
        sku: String,
        barcode: String?,
        categoryId: String?,
        sellingPrice: BigDecimal,
        costPrice: BigDecimal,
        initialStock: Int,
        lowStockThreshold: Int,
        unit: String,
    ) = viewModelScope.launch {
        _uiState.value = ProductUiState.Loading
        runCatching {
            if (isEditMode) {
                val existing = _product.value ?: error("Product not loaded")
                val now = Instant.now()
                // Pricing history is what lets Products say "cost $1.95 → $2.12 on 28 Aug"
                // rather than only today's margin. Only a real move is recorded.
                val costMoved = costPrice.compareTo(existing.costPrice) != 0
                val priceMoved = sellingPrice.compareTo(existing.sellingPrice) != 0
                productRepository.update(
                    existing.copy(
                        name = name,
                        sku = sku,
                        barcode = barcode,
                        categoryId = categoryId,
                        sellingPrice = sellingPrice,
                        costPrice = costPrice,
                        previousCostPrice = if (costMoved) existing.costPrice else existing.previousCostPrice,
                        costPriceChangedAt = if (costMoved) now else existing.costPriceChangedAt,
                        sellingPriceSetAt = if (priceMoved) now else existing.sellingPriceSetAt,
                        lowStockThreshold = lowStockThreshold,
                        unit = unit,
                        updatedAt = now,
                    )
                )
                registerBarcode(existing.id, barcode)
            } else {
                val id = UUID.randomUUID().toString()
                productRepository.add(
                    product = Product(
                        id = id,
                        name = name,
                        sku = sku,
                        barcode = barcode,
                        categoryId = categoryId,
                        sellingPrice = sellingPrice,
                        costPrice = costPrice,
                        stockQuantity = 0,
                        lowStockThreshold = lowStockThreshold,
                        unit = unit,
                        sellingPriceSetAt = Instant.now(),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                    openingStock = initialStock,
                )
                registerBarcode(id, barcode)
            }
        }.onSuccess { _uiState.value = ProductUiState.Success }
            .onFailure { _uiState.value = ProductUiState.Error(it.message ?: "Error") }
    }

    /**
     * Adds the code to `product_barcodes` — one product, many codes — and, when it came from
     * the inbox, clears it out of `unknown_barcodes` in the same step. `products.barcode` stays
     * populated for compatibility, but lookups go through the table.
     */
    private suspend fun registerBarcode(productId: String, barcode: String?) {
        val code = BarcodeNormaliser.normalise(barcode) ?: return
        if (code == prefilledBarcode) {
            barcodeRepository.claimUnknown(code, productId)
        } else {
            barcodeRepository.addBarcode(productId, code)
        }
    }
}
