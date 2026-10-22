package com.shopmate.ui.screen.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.domain.model.StockDelta
import com.shopmate.domain.model.StockDeltaWithProduct
import com.shopmate.domain.repository.CategoryRepository
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.StockInsightRepository
import com.shopmate.domain.repository.StorePermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class StockFilter { ALL, LOW_STOCK, OUT_OF_STOCK }

/**
 * One ledger line with the balance it left behind — the "why does it say 6?" answer the brand
 * promise requires of any screen showing a stock number.
 *
 * The balance is derived by walking the ledger backwards from the current quantity, because
 * stock is the sum of an append-only ledger and is never stored per movement.
 */
data class LedgerEntry(
    val delta: StockDelta,
    val runningBalance: Int,
)

/** One rendered row: the insight plus the section it landed in. */
data class StockRow(
    val insight: ProductInsight,
    val section: StockSection,
)

data class StockUiState(
    val products: List<Product> = emptyList(),
    /** Rows grouped for rendering, in [StockSection] order. */
    val sections: Map<StockSection, List<StockRow>> = emptyMap(),
    /** Items running out per day, index 0 = today. Always seven entries. */
    val weekAhead: List<Int> = List(7) { 0 },
    /** The running balance after each movement, for the selected product's ledger. */
    val selectedLedger: List<LedgerEntry> = emptyList(),
    val searchQuery: String = "",
    val filterMode: StockFilter = StockFilter.ALL,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val netDeltas: Map<String, Int> = emptyMap(),     // productId -> net movement, last 7 days
    val selectedProduct: Product? = null,
    val isAdjustmentSheetOpen: Boolean = false,
    val selectedHistory: List<StockDelta> = emptyList(),
    val recentDeltas: List<StockDeltaWithProduct> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class StockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val stockInsightRepository: StockInsightRepository,
    private val stockDeltaDao: StockDeltaDao,
    storePermissions: StorePermissionRepository,
) : ViewModel() {

    /** Gates Count stock and Adjust; see [StorePermissionRepository.canManageStock]. */
    val canManageStock: StateFlow<Boolean> = storePermissions.canManageStock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _searchQuery = MutableStateFlow("")
    private val _filterMode = MutableStateFlow(StockFilter.ALL)
    private val _selectedProductId = MutableStateFlow<String?>(null)
    private val _isSheetOpen = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val sevenDaysAgoMillis =
        Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()

    private val allProducts: StateFlow<List<Product>> = productRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Exposed so the screen can colour-code product category dots. */
    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val insights: StateFlow<List<ProductInsight>> = stockInsightRepository.getInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val productSection: Flow<ProductSection> = combine(
        insights,
        _searchQuery.debounce { if (it.isEmpty()) 0L else 300L },
        _filterMode,
    ) { all, query, filter ->
        val products = all.map { it.product }
        val lowCount = products.count { it.stockQuantity in 1..it.lowStockThreshold }
        val outCount = products.count { it.stockQuantity <= 0 }
        val filtered = all.filter { insight ->
            val p = insight.product
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                p.sku.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> p.stockQuantity in 1..p.lowStockThreshold
                StockFilter.OUT_OF_STOCK -> p.stockQuantity <= 0
            }
            matchesQuery && matchesFilter
        }

        val rows = filtered.map { StockRow(insight = it, section = it.section()) }
        // Grouped in section order, and within a section the nearest run-out first: the whole
        // screen is ordered by urgency in time, which is the question it answers.
        val sections = StockSection.entries.associateWith { section ->
            rows.filter { it.section == section }
                .sortedBy { it.insight.daysOfCover ?: Double.MAX_VALUE }
        }.filterValues { it.isNotEmpty() }

        val weekAhead = MutableList(WEEK_AHEAD_DAYS) { 0 }
        filtered.forEach { insight ->
            insight.runOut.stripOffset
                ?.takeIf { it < WEEK_AHEAD_DAYS }
                ?.let { weekAhead[it] = weekAhead[it] + 1 }
        }

        ProductSection(
            products = filtered.map { it.product },
            lowCount = lowCount,
            outCount = outCount,
            sections = sections,
            weekAhead = weekAhead,
        )
    }

    private val netDeltas: Flow<Map<String, Int>> =
        stockDeltaDao.getNetDeltasSince(sevenDaysAgoMillis)
            .map { rows -> rows.associate { it.productId to it.net } }

    private val recentDeltas: Flow<List<StockDeltaWithProduct>> = combine(
        stockDeltaDao.getRecentDeltas(50),
        allProducts,
    ) { deltas, products ->
        val names = products.associate { it.id to it.name }
        deltas.map { entity ->
            StockDeltaWithProduct(
                delta = entity.toDomain(),
                productName = names[entity.productId] ?: "Unknown product",
            )
        }
    }

    private val selectedProduct: Flow<Product?> = combine(
        _selectedProductId,
        allProducts,
    ) { id, products -> products.find { it.id == id } }

    private val selectedHistory: Flow<List<StockDelta>> = _selectedProductId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                stockDeltaDao.getByProductId(id).map { list -> list.map { it.toDomain() } }
            }
        }

    /**
     * The ledger with a running balance, newest first. Walked **backwards** from the product's
     * current quantity: the newest movement left the current balance, the one before it left
     * `current - delta`, and so on. Stock is the sum of the ledger, so the balances come out of
     * the ledger rather than being stored alongside it.
     */
    private val selectedLedger: Flow<List<LedgerEntry>> = combine(
        selectedProduct,
        selectedHistory,
    ) { product, history ->
        if (product == null) return@combine emptyList()
        var balance = product.stockQuantity
        history.map { delta ->
            val entry = LedgerEntry(delta = delta, runningBalance = balance)
            balance -= delta.delta
            entry
        }
    }

    val uiState: StateFlow<StockUiState> = combine(
        productSection,
        combine(netDeltas, recentDeltas) { net, recent -> DeltaBundle(net, recent) },
        combine(selectedProduct, selectedHistory, _isSheetOpen, selectedLedger) {
                product, history, open, ledger ->
            SelectionState(product, history, open, ledger)
        },
        combine(_searchQuery, _filterMode) { query, filter -> InputState(query, filter) },
        combine(_message, _error) { message, error -> StatusState(message, error) },
    ) { section, deltas, selection, input, status ->
        StockUiState(
            products = section.products,
            sections = section.sections,
            weekAhead = section.weekAhead,
            selectedLedger = selection.ledger,
            searchQuery = input.query,
            filterMode = input.filter,
            lowStockCount = section.lowCount,
            outOfStockCount = section.outCount,
            netDeltas = deltas.net,
            selectedProduct = selection.product,
            isAdjustmentSheetOpen = selection.open,
            selectedHistory = selection.history,
            recentDeltas = deltas.recent,
            message = status.message,
            error = status.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    fun searchProducts(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: StockFilter) {
        _filterMode.value = filter
    }

    fun selectProduct(product: Product) {
        _selectedProductId.value = product.id
        _isSheetOpen.value = true
    }

    fun loadStockHistory(productId: String) {
        _selectedProductId.value = productId
    }

    fun closeAdjustmentSheet() {
        _isSheetOpen.value = false
    }

    fun adjustStock(productId: String, delta: Int, reason: DeltaReason, note: String?) {
        if (delta == 0) return
        val product = allProducts.value.find { it.id == productId }
        if (product != null && product.stockQuantity + delta < 0) {
            _error.value = "Stock cannot go below 0"
            return
        }
        viewModelScope.launch {
            runCatching {
                productRepository.adjustStock(productId, delta, reason, note?.trim()?.ifBlank { null })
            }.onSuccess {
                _isSheetOpen.value = false
                _message.value = "Stock updated"
            }.onFailure {
                _error.value = it.message ?: "Could not update stock"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun consumeError() {
        _error.value = null
    }

    private data class ProductSection(
        val products: List<Product>,
        val lowCount: Int,
        val outCount: Int,
        val sections: Map<StockSection, List<StockRow>> = emptyMap(),
        val weekAhead: List<Int> = List(WEEK_AHEAD_DAYS) { 0 },
    )

    private data class DeltaBundle(
        val net: Map<String, Int>,
        val recent: List<StockDeltaWithProduct>,
    )

    private data class SelectionState(
        val product: Product?,
        val history: List<StockDelta>,
        val open: Boolean,
        val ledger: List<LedgerEntry>,
    )

    private data class InputState(val query: String, val filter: StockFilter)

    private data class StatusState(val message: String?, val error: String?)
}
