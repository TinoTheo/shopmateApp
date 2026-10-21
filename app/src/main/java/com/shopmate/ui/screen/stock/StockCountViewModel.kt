package com.shopmate.ui.screen.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.Product
import com.shopmate.domain.repository.CountedLine
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.StockCountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** One line on the count sheet: what the ledger expects, and what the shelf actually holds. */
data class CountLine(
    val product: Product,
    val counted: Int,
) {
    val expected: Int get() = product.stockQuantity
    val variance: Int get() = counted - expected
    val matches: Boolean get() = variance == 0
}

data class StockCountUiState(
    val lines: List<CountLine> = emptyList(),
    val committed: Boolean = false,
    /** Set once the count commits, so the verb can carry through: "Counted 5 items". */
    val committedItemCount: Int = 0,
    val error: String? = null,
) {
    val countedCount: Int get() = lines.size
    val varianceLines: List<CountLine> get() = lines.filterNot { it.matches }
    val shortCount: Int get() = lines.count { it.variance < 0 }
}

@HiltViewModel
class StockCountViewModel @Inject constructor(
    productRepository: ProductRepository,
    private val stockCountRepository: StockCountRepository,
) : ViewModel() {

    private val startedAt = Instant.now()

    /** Counts keyed by product id. Absent means "not touched yet" — it starts at expected. */
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _committed = MutableStateFlow<Int?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StockCountUiState> = combine(
        productRepository.getAll(),
        _counts,
        _committed,
        _error,
    ) { products, counts, committed, error ->
        StockCountUiState(
            lines = products.map { product ->
                CountLine(
                    product = product,
                    counted = counts[product.id] ?: product.stockQuantity,
                )
            },
            committed = committed != null,
            committedItemCount = committed ?: 0,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockCountUiState())

    fun setCount(productId: String, counted: Int) {
        _counts.value = _counts.value + (productId to counted.coerceAtLeast(0))
    }

    /**
     * Commits the session. A commit is a **batch of signed deltas, never an assignment**
     * (architecture rule 5) — the repository computes `counted - expected` per line and pushes
     * each through the same path a restock takes.
     */
    fun commit() {
        val lines = uiState.value.lines
        if (lines.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                stockCountRepository.commit(
                    startedAt = startedAt,
                    lines = lines.map {
                        CountedLine(
                            productId = it.product.id,
                            expected = it.expected,
                            counted = it.counted,
                        )
                    },
                )
            }.onSuccess {
                _committed.value = it.itemCount
            }.onFailure {
                _error.value = it.message ?: "Could not commit the count"
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }
}
