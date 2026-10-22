package com.shopmate.ui.screen.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.domain.repository.BarcodeRepository
import com.shopmate.domain.repository.CategoryRepository
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StockInsightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProductUiState {
    object Idle : ProductUiState()
    object Loading : ProductUiState()
    object Success : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val stockInsightRepository: StockInsightRepository,
    private val settingsRepository: SettingsRepository,
    private val barcodeRepository: BarcodeRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    val products: StateFlow<List<Product>> = combine(
        productRepository.getAll(),
        _searchQuery.debounce(300),
        _selectedCategoryId,
    ) { all, query, catId ->
        all.filter { p ->
            (query.isBlank() || p.name.contains(query, ignoreCase = true) || p.sku.contains(query, ignoreCase = true))
                && (catId == null || p.categoryId == catId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The store's target margin, from `SettingsRepository` — never a constant. It positions the
     * [MarginGauge][com.shopmate.ui.components.MarginGauge] notch and decides what "holding
     * target" means, so it belongs to the store, not to the code.
     */
    val targetMargin: StateFlow<Double> = settingsRepository.targetMargin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_TARGET_MARGIN)

    /** Rows grouped by margin health, in [ProductSection] order. */
    @OptIn(FlowPreview::class)
    val sections: StateFlow<Map<ProductSection, List<ProductInsight>>> = combine(
        stockInsightRepository.getInsights(),
        _searchQuery.debounce { if (it.isEmpty()) 0L else 300L },
        _selectedCategoryId,
        settingsRepository.targetMargin,
    ) { insights, query, catId, target ->
        val filtered = insights.filter { insight ->
            val p = insight.product
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                p.sku.contains(query, ignoreCase = true)
            matchesQuery && (catId == null || p.categoryId == catId)
        }
        ProductSection.entries.associateWith { section ->
            // Thinnest margin first inside every section: the money at stake leads.
            filtered.filter { it.section(target) == section }.sortedBy { it.margin }
        }.filterValues { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * How many codes have been scanned at the till but not named yet. Drives the card at the
     * top of Products — they sold anyway, so this is an invitation, not an error.
     */
    val unknownBarcodeCount: StateFlow<Int> = barcodeRepository.unknownBarcodeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    fun searchProducts(query: String) { _searchQuery.value = query }
    fun clearSearch() { _searchQuery.value = "" }
    fun selectCategory(id: String?) { _selectedCategoryId.value = id }

    fun addProduct(product: Product) = viewModelScope.launch {
        _uiState.value = ProductUiState.Loading
        runCatching { productRepository.add(product, openingStock = product.stockQuantity) }
            .onSuccess { _uiState.value = ProductUiState.Success }
            .onFailure { _uiState.value = ProductUiState.Error(it.message ?: "Error") }
    }

    fun updateProduct(product: Product) = viewModelScope.launch {
        _uiState.value = ProductUiState.Loading
        runCatching { productRepository.update(product) }
            .onSuccess { _uiState.value = ProductUiState.Success }
            .onFailure { _uiState.value = ProductUiState.Error(it.message ?: "Error") }
    }

    fun resetState() { _uiState.value = ProductUiState.Idle }

    private companion object {
        /** Mirrors the DataStore default until the first value arrives (CLAUDE.md § Settings). */
        const val DEFAULT_TARGET_MARGIN = 0.30
    }
}
