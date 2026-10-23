package com.shopmate.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.CartItem
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SaleItem
import com.shopmate.domain.repository.BarcodeRepository
import com.shopmate.domain.repository.CategoryRepository
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.model.LicenceState
import com.shopmate.domain.repository.LicenceRepository
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.SaleRepository
import com.shopmate.domain.util.ReceiptNumberGenerator
import com.shopmate.scan.ScanSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** What the always-visible number pad is currently editing. */
enum class NumberPadMode {
    IDLE,           // no active input — keys do nothing visible
    QUANTITY,       // editing quantity of a selected line item
    MANUAL_PRICE,   // entering a custom price for a line (loose goods)
    CASH_TENDERED,  // entering cash received to calculate change
}

/**
 * The tablet sale ticket is a three-phase state machine, and **the three zones do not reflow
 * between phases** (CLAUDE.md § Adaptive layouts). Only the ticket's middle band changes.
 *
 * | Phase    | Ticket shows                                  | Charge does              |
 * |----------|-----------------------------------------------|--------------------------|
 * | [SALE]   | lines, quantity hint, number pad              | goes to [TENDER]         |
 * | [TENDER] | lines, payment method, cash/change, pad       | commits the sale, [DONE] |
 * | [DONE]   | lines, success banner in the product area      | clears back to [SALE]    |
 *
 * The number pad has **one layout and two destinations** — the quantity buffer in [SALE], the
 * cash buffer in [TENDER]. It never moves, resizes, or relabels: the muscle-memory rule applied
 * within a screen rather than across device sizes.
 */
enum class TicketPhase { SALE, TENDER, DONE }

data class PosUiState(
    val cartItems: List<CartItem> = emptyList(),
    val cartTotal: BigDecimal = BigDecimal.ZERO,
    val cartItemCount: Int = 0,           // sum of quantities
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val isCheckoutSheetOpen: Boolean = false,
    val isProcessingSale: Boolean = false,
    val lastSaleReceipt: String? = null,  // flash briefly after sale
    val lastSaleTotal: BigDecimal? = null,
    val error: String? = null,
    val numberPadMode: NumberPadMode = NumberPadMode.IDLE,
    val numberPadBuffer: String = "",         // current typed value
    val selectedLineItemId: String? = null,   // which cart line is active
    // === Checkout ===
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashReceived: BigDecimal? = null,     // null = nothing keyed yet
    val cashBuffer: String = "",              // raw keystrokes, so "12." reads back as typed
    val changeDue: BigDecimal = BigDecimal.ZERO, // (cash − total), floored at zero
    val isShort: Boolean = false,             // cash keyed but less than the total
    // === Tablet sale ticket ===
    val phase: TicketPhase = TicketPhase.SALE,
) {
    /** Cash sales need enough money on the counter; every other method just confirms the total. */
    val canConfirmSale: Boolean
        get() = selectedPaymentMethod != PaymentMethod.CASH ||
            (cashReceived != null && cashReceived >= cartTotal)

    /**
     * Card and mobile-money tenders skip the cash/change pair — there is no change to give, and
     * showing a zeroed calculator implies otherwise (§ Adaptive layouts).
     */
    val showsCashPair: Boolean get() = selectedPaymentMethod == PaymentMethod.CASH

    /**
     * The quantity typed but not yet applied. Applying it is its own control — a button that
     * sometimes edits a line and sometimes takes money is the overloading § Copy and voice
     * already forbids at the label level.
     */
    val pendingQuantity: String?
        get() = numberPadBuffer.takeIf {
            phase == TicketPhase.SALE && numberPadMode == NumberPadMode.QUANTITY && it.isNotEmpty()
        }

    /** Charge is disabled on an empty cart, in every phase. */
    val canCharge: Boolean
        get() = when (phase) {
            TicketPhase.SALE -> cartItems.isNotEmpty()
            TicketPhase.TENDER -> cartItems.isNotEmpty() && canConfirmSale
            TicketPhase.DONE -> true
        }
}

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val saleRepository: SaleRepository,
    private val receiptNumberGenerator: ReceiptNumberGenerator,
    private val barcodeRepository: BarcodeRepository,
    private val scanSource: ScanSource,
    licences: LicenceRepository,
    private val staffRepository: StaffRepository,
) : ViewModel() {

    /**
     * Where the plan stands for this till (CLAUDE.md § Licensing). The POS shows it only in grace
     * and after; the owner hears about an ending plan in Settings, not the cashier at the till.
     * Null until the licence has been read once — and an unread licence never blocks a sale.
     */
    val licenceState: StateFlow<LicenceState?> = licences.licence
        .map { it?.state() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun salesBlocked(): Boolean {
        if (licenceState.value?.blocksNewSales != true) return false
        _error.value = LICENCE_ENDED_MESSAGE
        return true
    }

    /** Number pad state lives together so mode transitions are atomic. */
    private data class PadState(
        val mode: NumberPadMode = NumberPadMode.IDLE,
        val buffer: String = "",
        val selectedLineItemId: String? = null,
        val cashTendered: BigDecimal? = null,
    )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _padState = MutableStateFlow(PadState())
    private val _isCheckoutSheetOpen = MutableStateFlow(false)
    private val _isProcessingSale = MutableStateFlow(false)
    private val _lastSaleReceipt = MutableStateFlow<LastSale?>(null)
    private val _checkout = MutableStateFlow(CheckoutState())
    private val _error = MutableStateFlow<String?>(null)
    private val _phase = MutableStateFlow(TicketPhase.SALE)

    private val allProducts: StateFlow<List<Product>> = productRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Exposed so the screen can colour-code product category dots. */
    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // The POS listens to whichever ScanSource is bound — HID today, a camera source later.
        // The screen never learns which; that is the point of the abstraction.
        viewModelScope.launch {
            scanSource.codes.collect(::onCodeScanned)
        }
    }

    @OptIn(FlowPreview::class)
    private val searchResults: Flow<List<Product>> = combine(
        allProducts,
        // Empty query reflects instantly; typing is debounced for snappy-but-cheap filtering.
        _searchQuery.debounce { if (it.isEmpty()) 0L else 300L },
        _selectedCategoryId,
    ) { products, query, categoryId ->
        products
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { p ->
                query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true) ||
                    (p.barcode?.contains(query, ignoreCase = true) == true)
            }
    }

    val uiState: StateFlow<PosUiState> = combine(
        searchResults,
        combine(_searchQuery, _selectedCategoryId) { query, categoryId -> query to categoryId },
        _cartItems,
        _padState,
        combine(
            _isCheckoutSheetOpen,
            _isProcessingSale,
            _lastSaleReceipt,
            _error,
            combine(_checkout, _phase) { checkout, phase -> checkout to phase },
        ) { open, processing, lastSale, error, (checkout, phase) ->
            CheckoutFlags(open, processing, lastSale, error, checkout, phase)
        },
    ) { results, (query, categoryId), cart, pad, flags ->
        val total = cart.fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal }
        PosUiState(
            cartItems = cart,
            cartTotal = total,
            cartItemCount = cart.sumOf { it.quantity },
            searchQuery = query,
            searchResults = results,
            selectedCategoryId = categoryId,
            isCheckoutSheetOpen = flags.open,
            isProcessingSale = flags.processing,
            lastSaleReceipt = flags.lastSale?.receipt,
            lastSaleTotal = flags.lastSale?.total,
            error = flags.error,
            numberPadMode = pad.mode,
            numberPadBuffer = pad.buffer,
            selectedLineItemId = pad.selectedLineItemId,
            selectedPaymentMethod = flags.checkout.method,
            cashReceived = flags.checkout.cashBuffer.toBigDecimalOrNull(),
            cashBuffer = flags.checkout.cashBuffer,
            // Change never shows negative — a shortfall is its own state, not "minus change".
            changeDue = flags.checkout.cashBuffer.toBigDecimalOrNull()
                ?.subtract(total)?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO,
            isShort = flags.checkout.cashBuffer.toBigDecimalOrNull()?.let { it < total } ?: false,
            phase = flags.phase,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PosUiState())

    private data class CheckoutFlags(
        val open: Boolean,
        val processing: Boolean,
        val lastSale: LastSale?,
        val error: String?,
        val checkout: CheckoutState,
        val phase: TicketPhase,
    )

    /**
     * Checkout-only state, deliberately separate from [PadState] so the till's cash entry and
     * the ticket's quantity/price pad can never overwrite each other.
     */
    data class CheckoutState(
        val method: PaymentMethod = PaymentMethod.CASH,
        val cashBuffer: String = "",
    )

    /** The just-completed sale, held briefly so the success banner can name the amount taken. */
    data class LastSale(val receipt: String, val total: BigDecimal)

    /**
     * A code arrived from a [ScanSource][com.shopmate.scan.ScanSource].
     *
     * Two outcomes, and **the sale continues in both** (architecture rule 11): a known code adds
     * its product to the cart; an unknown one is recorded to `unknown_barcodes` to be named
     * later. An unknown barcode never blocks a sale, so this reports rather than fails.
     */
    fun onCodeScanned(rawCode: String) {
        viewModelScope.launch {
            val product = barcodeRepository.lookup(rawCode)
            if (product != null) {
                addToCart(product)
            } else {
                _error.value = UNKNOWN_CODE_MESSAGE
            }
        }
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun addToCart(product: Product) {
        val current = _cartItems.value
        val existing = current.find { it.product.id == product.id }
        val newQty = (existing?.quantity ?: 0) + 1
        // Deliberate policy: never block a sale — the item in the customer's hand outranks the
        // recorded count. Warn, allow, and let stock go negative; the Stock screen flags
        // negative quantities for correction.
        if (newQty > product.stockQuantity) {
            _error.value = oversellWarning(product.name, product.stockQuantity)
        }
        _cartItems.value = if (existing == null) {
            current + CartItem(product = product, quantity = 1, lineTotal = product.sellingPrice)
        } else {
            current.map { item ->
                if (item.product.id == product.id) {
                    // Refresh the snapshot so price/stock stay current with the catalog,
                    // but keep any cashier-entered price override.
                    val unitPrice = item.priceOverride ?: product.sellingPrice
                    item.copy(
                        product = product,
                        quantity = newQty,
                        lineTotal = unitPrice.multiply(BigDecimal(newQty)),
                    )
                } else {
                    item
                }
            }
        }
    }

    fun updateCartItemQty(productId: String, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(productId)
            return
        }
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                if (newQty > item.product.stockQuantity) {
                    _error.value = oversellWarning(item.product.name, item.product.stockQuantity)
                }
                item.copy(
                    quantity = newQty,
                    lineTotal = item.unitPrice.multiply(BigDecimal(newQty)),
                )
            } else {
                item
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
        if (_padState.value.selectedLineItemId == productId) _padState.value = PadState()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _padState.value = PadState()
    }

    fun openCheckout() {
        if (_cartItems.value.isEmpty()) return
        if (salesBlocked()) return
        commitNumberPad()
        _padState.value = PadState()
        _checkout.value = CheckoutState()   // every sale starts at Cash, nothing tendered
        _isCheckoutSheetOpen.value = true
    }

    fun closeCheckout() {
        _isCheckoutSheetOpen.value = false
        _padState.value = PadState()
        _checkout.value = CheckoutState()
    }

    // === Checkout ===
    // The till's own state. Cash entry here is independent of the ticket pad, so a cashier
    // editing a line quantity can never clobber the tendered amount (or vice versa).

    /** Choosing a method decides which inputs show. Leaving cash drops the tendered amount. */
    fun selectPaymentMethod(method: PaymentMethod) {
        _checkout.value = CheckoutState(
            method = method,
            cashBuffer = if (method == PaymentMethod.CASH) _checkout.value.cashBuffer else "",
        )
    }

    fun appendCashDigit(digit: String) {
        val buffer = appendMoneyDigit(_checkout.value.cashBuffer, digit) ?: return
        _checkout.value = _checkout.value.copy(cashBuffer = buffer)
    }

    fun backspaceCash() {
        val buffer = _checkout.value.cashBuffer
        if (buffer.isEmpty()) return
        _checkout.value = _checkout.value.copy(cashBuffer = buffer.dropLast(1))
    }

    /**
     * Quick note button: a single tap sets the tendered amount to that denomination. Stored at
     * two decimals so the money column's decimal points stay aligned ("$5.00", not "$5").
     */
    fun setCashReceived(amount: BigDecimal) {
        _checkout.value = _checkout.value.copy(
            cashBuffer = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
        )
    }

    /** "Exact" — the customer handed over the precise total. */
    fun setCashExact() {
        setCashReceived(uiState.value.cartTotal)
    }

    /** Guarded confirm: cash sales need enough on the counter before the sale is recorded. */
    fun confirmSale() {
        val state = uiState.value
        if (!state.canConfirmSale) return
        completeSale(state.selectedPaymentMethod)
    }

    /** Reset the till between sales so the next customer starts clean. */
    fun resetCheckout() {
        _checkout.value = CheckoutState()
    }

    // === Tablet sale ticket ===
    // The three zones never reflow between phases; only the ticket's middle band changes.

    /**
     * The tablet Charge button, which means something different in each phase and says so —
     * `Charge` in [TicketPhase.SALE] and [TicketPhase.TENDER], `Next sale` in [TicketPhase.DONE].
     *
     * It **never** applies a pending quantity: that is [applyPendingQuantity]'s job.
     */
    fun chargeTablet() {
        when (_phase.value) {
            TicketPhase.SALE -> {
                if (_cartItems.value.isEmpty()) return
                if (salesBlocked()) return
                _padState.value = PadState()          // the pad's destination becomes cash
                _checkout.value = CheckoutState()      // every sale starts at Cash, nothing tendered
                _phase.value = TicketPhase.TENDER
            }
            TicketPhase.TENDER -> {
                val state = uiState.value
                if (!state.canConfirmSale) return
                _phase.value = TicketPhase.DONE
                completeSale(state.selectedPaymentMethod)
            }
            TicketPhase.DONE -> {
                _cartItems.value = emptyList()
                _padState.value = PadState()
                _checkout.value = CheckoutState()
                _lastSaleReceipt.value = null
                _phase.value = TicketPhase.SALE
            }
        }
    }

    /**
     * "Set quantity" — the pending buffer's own control. Quantity editing is never bound to
     * Charge (§ Adaptive layouts). A typed 0 removes the line; anything else applies at min 1.
     */
    fun applyPendingQuantity() {
        val pad = _padState.value
        if (pad.mode != NumberPadMode.QUANTITY || pad.buffer.isEmpty()) return
        val productId = pad.selectedLineItemId ?: return
        val qty = pad.buffer.toIntOrNull() ?: return
        if (qty <= 0) removeFromCart(productId) else updateCartItemQty(productId, qty)
        _padState.value = PadState()
    }

    /** Back out of tendering without losing the cart — the ticket returns to [TicketPhase.SALE]. */
    fun cancelTender() {
        if (_phase.value != TicketPhase.TENDER) return
        _checkout.value = CheckoutState()
        _padState.value = PadState()
        _phase.value = TicketPhase.SALE
    }

    // === Number pad ===
    // The pad composable is dumb; every mode/buffer decision lives here.

    /** Tap a ticket line: select it for quantity editing; tap again to deselect. */
    fun selectLineItem(productId: String) {
        val pad = _padState.value
        commitNumberPad()
        _padState.value = if (pad.selectedLineItemId == productId && pad.mode == NumberPadMode.QUANTITY) {
            PadState()
        } else {
            PadState(mode = NumberPadMode.QUANTITY, selectedLineItemId = productId)
        }
    }

    /** Long-press a ticket line: type a custom price for it (loose goods — sugar by kg). */
    fun startManualPrice(productId: String) {
        commitNumberPad()
        _padState.value = PadState(mode = NumberPadMode.MANUAL_PRICE, selectedLineItemId = productId)
    }

    /** Checkout sheet's cash field was tapped — pad digits now build the tendered amount. */
    fun startCashEntry() {
        _padState.value = PadState(mode = NumberPadMode.CASH_TENDERED)
    }

    /** Cashier switched away from cash payment — drop the tendered amount. */
    fun clearCashEntry() {
        if (_padState.value.mode == NumberPadMode.CASH_TENDERED) _padState.value = PadState()
    }

    fun numberPadDigit(digit: String) {
        // One pad, two destinations, chosen by phase: the quantity buffer in SALE, the cash
        // buffer in TENDER. The pad itself never moves, resizes or relabels.
        if (_phase.value == TicketPhase.TENDER) {
            appendCashDigit(digit)
            return
        }
        val pad = _padState.value
        when (pad.mode) {
            NumberPadMode.IDLE -> Unit
            NumberPadMode.QUANTITY -> {
                if (digit == ".") return  // quantities are whole numbers
                // Held pending, not applied live: "Set quantity" is what commits it.
                _padState.value = pad.copy(buffer = (pad.buffer + digit).take(MAX_QTY_DIGITS))
            }
            NumberPadMode.MANUAL_PRICE -> {
                val buffer = appendMoneyDigit(pad.buffer, digit) ?: return
                _padState.value = pad.copy(buffer = buffer)
                val price = buffer.toBigDecimalOrNull() ?: return
                pad.selectedLineItemId?.let { overrideLinePrice(it, price) }
            }
            NumberPadMode.CASH_TENDERED -> {
                val buffer = appendMoneyDigit(pad.buffer, digit) ?: return
                _padState.value = pad.copy(buffer = buffer, cashTendered = buffer.toBigDecimalOrNull())
            }
        }
    }

    fun numberPadBackspace() {
        if (_phase.value == TicketPhase.TENDER) {
            backspaceCash()
            return
        }
        val pad = _padState.value
        if (pad.buffer.isEmpty()) return
        val buffer = pad.buffer.dropLast(1)
        when (pad.mode) {
            NumberPadMode.IDLE -> Unit
            NumberPadMode.QUANTITY -> _padState.value = pad.copy(buffer = buffer)
            NumberPadMode.MANUAL_PRICE -> {
                _padState.value = pad.copy(buffer = buffer)
                val price = buffer.toBigDecimalOrNull() ?: return
                pad.selectedLineItemId?.let { overrideLinePrice(it, price) }
            }
            NumberPadMode.CASH_TENDERED -> {
                _padState.value = pad.copy(buffer = buffer, cashTendered = buffer.toBigDecimalOrNull())
            }
        }
    }

    /** A typed "0" quantity means "remove this line" — applied when editing ends. */
    private fun commitNumberPad() {
        val pad = _padState.value
        if (pad.mode == NumberPadMode.QUANTITY && pad.buffer.toIntOrNull() == 0) {
            pad.selectedLineItemId?.let { removeFromCart(it) }
        }
    }

    private fun overrideLinePrice(productId: String, price: BigDecimal) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                item.copy(
                    priceOverride = price,
                    lineTotal = price.multiply(BigDecimal(item.quantity)),
                )
            } else {
                item
            }
        }
    }

    /** Money entry: one decimal point, max two decimals, sane length. Null = reject key. */
    private fun appendMoneyDigit(buffer: String, digit: String): String? {
        if (digit == "." && buffer.contains(".")) return null
        val next = buffer + digit
        if (next.length > MAX_MONEY_DIGITS) return null
        val decimals = next.substringAfter('.', "")
        if (buffer.contains(".") && decimals.length > 2) return null
        return next
    }

    fun consumeError() {
        _error.value = null
    }

    fun completeSale(paymentMethod: PaymentMethod) {
        if (_isProcessingSale.value) return
        if (salesBlocked()) return
        val cart = _cartItems.value
        if (cart.isEmpty()) return

        _isProcessingSale.value = true
        viewModelScope.launch {
            runCatching {
                val categoryNames = categories.value.associate { it.id to it.name }
                var skippedItem = false

                // Re-read each product so a deleted product is skipped rather than oversold.
                val saleItems = cart.mapNotNull { cartItem ->
                    val product = productRepository.getById(cartItem.product.id)
                    if (product == null) {
                        skippedItem = true
                        return@mapNotNull null
                    }
                    SaleItem(
                        id = UUID.randomUUID().toString(),
                        productId = product.id,
                        productName = product.name,
                        categoryName = product.categoryId?.let { categoryNames[it] },
                        unitPrice = cartItem.unitPrice,
                        quantity = BigDecimal(cartItem.quantity),
                        lineTotal = cartItem.lineTotal,
                    )
                }
                if (saleItems.isEmpty()) error("None of the cart items exist any more")

                val receiptNumber = receiptNumberGenerator.next()
                val sale = Sale(
                    id = UUID.randomUUID().toString(),
                    receiptNumber = receiptNumber,
                    totalAmount = saleItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal },
                    paymentMethod = paymentMethod,
                    // Whoever unlocked the till (§ Staff). A till with no staff keeps the generic
                    // label: it has no person to name, and inventing one would be worse.
                    cashierName = staffRepository.current.value?.name ?: DEFAULT_CASHIER,
                    items = saleItems,
                    createdAt = Instant.now(),
                    syncedAt = null,
                    staffId = staffRepository.current.value?.id,
                )
                saleRepository.recordSale(sale)
                LastSale(receiptNumber, sale.totalAmount) to skippedItem
            }.onSuccess { (lastSale, skippedItem) ->
                _cartItems.value = emptyList()
                _padState.value = PadState()
                _isCheckoutSheetOpen.value = false
                _lastSaleReceipt.value = lastSale
                if (skippedItem) {
                    _error.value = "Some items were skipped because the product no longer exists"
                }
                launch {
                    delay(SUCCESS_FLASH_MS)
                    _lastSaleReceipt.value = null
                }
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Could not complete the sale"
            }
            _isProcessingSale.value = false
        }
    }

    private fun oversellWarning(productName: String, recordedStock: Int): String =
        "$productName: only $recordedStock recorded in stock — this sale will take it negative"

    companion object {
        private const val DEFAULT_CASHIER = "Cashier"

        /** Calm and specific: the code is kept, the sale is unaffected. */
        private const val UNKNOWN_CODE_MESSAGE =
            "The sale continues. The code is saved to unknown codes -- name it when you have a minute."
        private const val SUCCESS_FLASH_MS = 3_000L

        /** Says what stopped and what still happens: sales already taken keep sending. */
        private const val LICENCE_ENDED_MESSAGE =
            "Your plan has ended, so this till can't take new sales. Sales already taken still send."
        private const val MAX_QTY_DIGITS = 4
        private const val MAX_MONEY_DIGITS = 9
    }
}
